package gift.product;

import gift.category.Category;
import gift.category.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductService productService;

    @Test
    void saveProduct_이름이_15자를_초과하면_예외가_발생한다() {
        assertThatThrownBy(() ->
            productService.saveProduct("일이삼사오육칠팔구십일이삼사오육", 1000, "url", 1L)
        ).isInstanceOf(IllegalArgumentException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void saveProduct_관리자는_카카오_이름을_허용한다() {
        Category category = new Category("전자기기", "#000000", "url", "desc");
        given(categoryService.findById(1L)).willReturn(category);
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

        Product result = productService.saveProduct("카카오 선물", 1000, "url", 1L);

        assertThat(result.getName()).isEqualTo("카카오 선물");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_카카오_포함_이름은_거부된다() {
        assertThatThrownBy(() ->
            productService.create(new ProductRequest("카카오상품", 1000, "url", 1L))
        ).isInstanceOf(IllegalArgumentException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_허용되지_않는_특수문자가_포함되면_예외가_발생한다() {
        assertThatThrownBy(() ->
            productService.updateProduct(1L, "상품@이름", 1000, "url", 1L)
        ).isInstanceOf(IllegalArgumentException.class);

        verify(productRepository, never()).save(any());
    }
}