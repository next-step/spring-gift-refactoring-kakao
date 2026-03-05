package gift.product;

import gift.category.entity.Category;
import gift.category.repository.CategoryRepository;
import gift.product.dto.ProductRequest;
import gift.product.dto.ProductResponse;
import gift.product.entity.Product;
import gift.product.exception.ProductErrorCode;
import gift.product.exception.ProductException;
import gift.product.repository.ProductRepository;
import gift.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("상품 조회 시 상품이 없으면 PRODUCT_NOT_FOUND 예외를 던진다")
    void getProduct_notFound_throwsException() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        ProductException exception = assertThrows(ProductException.class, () -> productService.getProduct(1L));

        assertEquals(ProductErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 생성 시 카테고리가 없으면 CATEGORY_NOT_FOUND 예외를 던진다")
    void createProduct_categoryNotFound_throwsException() {
        ProductRequest request = new ProductRequest("아메리카노", 4500, "http://image.png", 1L);
        when(categoryRepository.findById(request.categoryId())).thenReturn(Optional.empty());

        ProductException exception = assertThrows(ProductException.class, () -> productService.createProduct(request));

        assertEquals(ProductErrorCode.CATEGORY_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 생성 시 이름이 유효하지 않으면 INVALID_PRODUCT_NAME 예외를 던진다")
    void createProduct_invalidName_throwsException() {
        ProductRequest request = new ProductRequest("카카오 선물", 4500, "http://image.png", 1L);

        ProductException exception = assertThrows(ProductException.class, () -> productService.createProduct(request));

        assertEquals(ProductErrorCode.INVALID_PRODUCT_NAME, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 생성 성공 시 생성된 상품 응답을 반환한다")
    void createProduct_success_returnsResponse() {
        Category category = new Category("음료", "#000000", "http://image.png", null);
        ProductRequest request = new ProductRequest("아메리카노", 4500, "http://image.png", 1L);
        Product saved = new Product(request.name(), request.price(), request.imageUrl(), category);
        when(categoryRepository.findById(request.categoryId())).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse response = productService.createProduct(request);

        assertEquals(request.name(), response.name());
        assertEquals(request.price(), response.price());
    }
}
