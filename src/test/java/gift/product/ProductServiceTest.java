package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() throws Exception {
        category = new Category("교환권", "#ffffff", "img.png", "");
        setId(category, 1L);

        product = new Product("아메리카노", 5000, "img.png", category);
        setId(product, 1L);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Nested
    @DisplayName("getProduct")
    class GetProduct {

        @Test
        @DisplayName("ID로 상품을 조회한다")
        void returnsProduct() {
            given(productRepository.findById(1L)).willReturn(Optional.of(product));

            Product result = productService.getProduct(1L);

            assertThat(result.getName()).isEqualTo("아메리카노");
        }

        @Test
        @DisplayName("존재하지 않으면 예외를 던진다")
        void throwsWhenNotFound() {
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("getProducts")
    class GetProducts {

        @Test
        @DisplayName("상품 목록을 페이징 조회한다")
        void returnsPagedProducts() {
            var pageable = PageRequest.of(0, 10);
            given(productRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(product)));

            var result = productService.getProducts(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("아메리카노");
        }
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("정상적으로 상품을 생성한다")
        void createsSuccessfully() throws Exception {
            var saved = new Product("라떼", 6000, "img.png", category);
            setId(saved, 2L);

            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willReturn(saved);

            Product result = productService.createProduct("라떼", 6000, "img.png", 1L, false);

            assertThat(result.getName()).isEqualTo("라떼");
        }

        @Test
        @DisplayName("카테고리가 존재하지 않으면 예외를 던진다")
        void throwsWhenCategoryNotFound() {
            given(categoryRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.createProduct("라떼", 6000, "img.png", 999L, false))
                .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("상품명 검증 실패 시 예외를 던진다")
        void throwsWhenNameInvalid() {
            assertThatThrownBy(() -> productService.createProduct("상품@!#", 5000, "img.png", 1L, false))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("allowKakao=false일 때 카카오가 포함된 상품명은 예외를 던진다")
        void throwsWhenKakaoNotAllowed() {
            assertThatThrownBy(() -> productService.createProduct("카카오선물", 5000, "img.png", 1L, false))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("allowKakao=true일 때 카카오가 포함된 상품명은 허용된다")
        void allowsKakaoWhenAllowed() throws Exception {
            var saved = new Product("카카오선물", 5000, "img.png", category);
            setId(saved, 3L);

            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willReturn(saved);

            Product result = productService.createProduct("카카오선물", 5000, "img.png", 1L, true);

            assertThat(result.getName()).isEqualTo("카카오선물");
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("정상적으로 상품을 수정한다")
        void updatesSuccessfully() {
            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

            Product result = productService.updateProduct(1L, "라떼", 6000, "new.png", 1L, false);

            assertThat(result.getName()).isEqualTo("라떼");
            assertThat(result.getPrice()).isEqualTo(6000);
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 예외를 던진다")
        void throwsWhenProductNotFound() {
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(999L, "라떼", 6000, "img.png", 1L, false))
                .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("상품을 삭제한다")
        void deletesProduct() {
            productService.deleteProduct(1L);

            then(productRepository).should().deleteById(1L);
        }
    }

    @Nested
    @DisplayName("getAllProducts")
    class GetAllProducts {

        @Test
        @DisplayName("전체 상품 목록을 조회한다")
        void returnsAllProducts() {
            given(productRepository.findAll()).willReturn(List.of(product));

            List<Product> result = productService.getAllProducts();

            assertThat(result).hasSize(1);
        }
    }
}
