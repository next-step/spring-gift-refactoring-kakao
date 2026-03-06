package gift.product;

import gift.category.Category;
import gift.category.CategoryException;
import gift.category.CategoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProductCommandServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductCommandService productCommandService;

    private Category createCategory() {
        return new Category("전자기기", "#000", "https://example.com/img.jpg", "설명");
    }

    @Test
    @DisplayName("상품을 저장한다")
    void save() {
        var category = createCategory();
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

        Product result = productCommandService.save("테스트상품", 10000, "https://example.com/img.jpg", 1L);

        assertThat(result.getName()).isEqualTo("테스트상품");
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 저장하면 예외가 발생한다")
    void save_CategoryNotFound() {
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productCommandService.save("상품", 1000, "https://img.jpg", 999L))
            .isInstanceOf(CategoryException.class);
    }

    @Test
    @DisplayName("상품을 수정한다")
    void update() {
        var category = createCategory();
        var product = new Product("기존상품", 5000, "https://example.com/old.jpg", category);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(productRepository.save(any(Product.class))).willReturn(product);

        Product result = productCommandService.update(1L, "수정상품", 8000, "https://example.com/new.jpg", 1L);

        assertThat(result.getName()).isEqualTo("수정상품");
    }

    @Test
    @DisplayName("존재하지 않는 상품을 수정하면 예외가 발생한다")
    void update_ProductNotFound() {
        var category = createCategory();
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productCommandService.update(999L, "상품", 1000, "https://img.jpg", 1L))
            .isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("상품을 삭제한다")
    void deleteById() {
        productCommandService.deleteById(1L);

        then(productRepository).should().deleteById(1L);
    }
}
