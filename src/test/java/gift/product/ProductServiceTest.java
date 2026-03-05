package gift.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.option.OptionRepository;
import gift.order.OrderRepository;
import gift.wish.WishRepository;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WishRepository wishRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        wishRepository.deleteAll();
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        category = categoryRepository.save(
            new Category("전자기기", "#1E90FF", "https://test.com/img.jpg", "설명"));
    }

    @Test
    @DisplayName("상품 수정 시 조회 + 수정이 하나의 단위로 처리된다")
    void update_success() {
        // given
        Product product = productService.create("원래상품", 1000, "https://test.com/old.jpg", category.getId());
        Category newCategory = categoryRepository.save(
            new Category("패션", "#FF6347", "https://test.com/fashion.jpg", "의류"));

        // when
        productService.update(product.getId(), "수정상품", 2000, "https://test.com/new.jpg", newCategory.getId());

        // then: DB 재조회로 변경 확인
        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("수정상품");
        assertThat(reloaded.getPrice()).isEqualTo(2000);
        assertThat(reloaded.getImageUrl()).isEqualTo("https://test.com/new.jpg");
        assertThat(reloaded.getCategory().getId()).isEqualTo(newCategory.getId());
    }

    @Test
    @DisplayName("존재하지 않는 상품 수정 시 예외 발생")
    void update_productNotFound() {
        assertThatThrownBy(() ->
            productService.update(999L, "이름", 1000, "https://test.com/img.jpg", category.getId())
        ).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 수정 시 예외 발생")
    void update_categoryNotFound() {
        Product product = productService.create("상품", 1000, "https://test.com/img.jpg", category.getId());

        assertThatThrownBy(() ->
            productService.update(product.getId(), "수정", 2000, "https://test.com/img.jpg", 999L)
        ).isInstanceOf(NoSuchElementException.class);
    }
}
