package gift.option;

import gift.IntegrationTest;
import gift.IntegrationTestFixtures;
import gift.category.Category;
import gift.category.CategoryRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class OptionServiceIntegrationTest {

    @Autowired
    private OptionService optionService;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Product product;

    @BeforeEach
    void setUp() {
        Category category = IntegrationTestFixtures.savedCategory(categoryRepository);
        product = IntegrationTestFixtures.savedProduct(productRepository, category);
        IntegrationTestFixtures.savedOption(optionRepository, product);
    }

    @Test
    void create_persistsOption() {
        var request = new OptionRequest("새 옵션", 50);

        var result = optionService.create(product.getId(), request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("새 옵션");
    }

    @Test
    void create_duplicateName_throws() {
        var request = new OptionRequest("기본 옵션", 50);

        assertThatThrownBy(() -> optionService.create(product.getId(), request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 옵션명");
    }

    @Test
    void update_changesNameAndQuantity() {
        var options = optionService.findByProductId(product.getId());
        Long optionId = options.get(0).getId();
        var request = new OptionRequest("수정된 옵션", 999);

        var result = optionService.update(product.getId(), optionId, request);

        assertThat(result.getName()).isEqualTo("수정된 옵션");
        assertThat(result.getQuantity()).isEqualTo(999);
    }

    @Test
    void update_duplicateNameExcludingSelf_throws() {
        optionService.create(product.getId(), new OptionRequest("두번째 옵션", 50));
        var options = optionService.findByProductId(product.getId());
        Long firstOptionId = options.stream()
            .filter(o -> o.getName().equals("기본 옵션"))
            .findFirst().orElseThrow().getId();

        assertThatThrownBy(() -> optionService.update(product.getId(), firstOptionId,
            new OptionRequest("두번째 옵션", 100)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 옵션명");
    }

    @Test
    void update_sameNameAsSelf_succeeds() {
        var options = optionService.findByProductId(product.getId());
        Long optionId = options.get(0).getId();
        var request = new OptionRequest("기본 옵션", 999);

        var result = optionService.update(product.getId(), optionId, request);

        assertThat(result.getName()).isEqualTo("기본 옵션");
        assertThat(result.getQuantity()).isEqualTo(999);
    }

    @Test
    void delete_removesOption() {
        optionService.create(product.getId(), new OptionRequest("삭제용 옵션", 10));
        var options = optionService.findByProductId(product.getId());
        assertThat(options).hasSize(2);

        Long deleteId = options.stream()
            .filter(o -> o.getName().equals("삭제용 옵션"))
            .findFirst().orElseThrow().getId();

        optionService.delete(product.getId(), deleteId);

        assertThat(optionService.findByProductId(product.getId())).hasSize(1);
    }

    @Test
    void delete_lastOption_throws() {
        var options = optionService.findByProductId(product.getId());
        Long optionId = options.get(0).getId();

        assertThatThrownBy(() -> optionService.delete(product.getId(), optionId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("옵션이 1개인 상품");
    }
}
