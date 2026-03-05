package gift.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gift.option.OptionRepository;
import gift.order.OrderRepository;
import gift.product.ProductRepository;
import gift.wish.WishRepository;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        wishRepository.deleteAll();
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    @DisplayName("카테고리 수정 시 조회 + 수정이 하나의 단위로 처리된다")
    void update_success() {
        // given
        Category category = categoryService.create("원래", "#000000", "https://test.com/old.jpg", "원래 설명");

        // when
        categoryService.update(category.getId(), "수정됨", "#FFFFFF", "https://test.com/new.jpg", "수정 설명");

        // then: DB 재조회로 변경 확인
        Category reloaded = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("수정됨");
        assertThat(reloaded.getColor()).isEqualTo("#FFFFFF");
        assertThat(reloaded.getImageUrl()).isEqualTo("https://test.com/new.jpg");
        assertThat(reloaded.getDescription()).isEqualTo("수정 설명");
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 수정 시 예외 발생")
    void update_notFound() {
        assertThatThrownBy(() ->
            categoryService.update(999L, "이름", "#000000", "https://test.com/img.jpg", "설명")
        ).isInstanceOf(NoSuchElementException.class);
    }
}
