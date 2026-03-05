package gift.product;

import gift.IntegrationTest;
import gift.IntegrationTestFixtures;
import gift.category.Category;
import gift.category.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        category = IntegrationTestFixtures.savedCategory(categoryRepository);
        IntegrationTestFixtures.savedProduct(productRepository, category);
    }

    @Test
    void findAll_withCategoryId_filtersCorrectly() {
        var result = productService.findAll(category.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategory().getId()).isEqualTo(category.getId());
    }

    @Test
    void findAll_withoutCategoryId_returnsAll() {
        var result = productService.findAll(null, PageRequest.of(0, 10));

        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    void findAll_withNonExistentCategoryId_returnsEmpty() {
        var result = productService.findAll(99999L, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }
}
