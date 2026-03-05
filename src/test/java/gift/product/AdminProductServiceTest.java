package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private AdminProductService adminProductService;

    @Test
    @DisplayName("상품 생성 시 카테고리가 없으면 예외를 던진다")
    void createProduct_categoryNotFound_throwsException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
            NoSuchElementException.class,
            () -> adminProductService.createProduct("아메리카노", 4500, "http://image.png", 1L)
        );

        assertEquals("카테고리가 존재하지 않습니다. id=1", exception.getMessage());
    }

    @Test
    @DisplayName("상품 수정 시 상품이 없으면 예외를 던진다")
    void updateProduct_productNotFound_throwsException() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
            NoSuchElementException.class,
            () -> adminProductService.updateProduct(1L, "아메리카노", 4500, "http://image.png", 10L)
        );

        assertEquals("상품이 존재하지 않습니다. id=1", exception.getMessage());
    }

    @Test
    @DisplayName("상품 수정 시 카테고리가 없으면 예외를 던진다")
    void updateProduct_categoryNotFound_throwsException() {
        Category oldCategory = new Category("디저트", "#111111", "http://old.png", null);
        Product product = new Product("케이크", 7000, "http://cake.png", oldCategory);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
            NoSuchElementException.class,
            () -> adminProductService.updateProduct(1L, "아메리카노", 4500, "http://image.png", 10L)
        );

        assertEquals("카테고리가 존재하지 않습니다. id=10", exception.getMessage());
    }

    @Test
    @DisplayName("상품 수정 성공 시 상품을 저장한다")
    void updateProduct_success_savesProduct() {
        Category oldCategory = new Category("디저트", "#111111", "http://old.png", null);
        Category newCategory = new Category("음료", "#000000", "http://new.png", null);
        Product product = new Product("케이크", 7000, "http://cake.png", oldCategory);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        adminProductService.updateProduct(1L, "아메리카노", 4500, "http://image.png", 2L);

        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("상품 삭제 시 저장소 삭제를 호출한다")
    void deleteProduct_success_callsDelete() {
        adminProductService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }
}
