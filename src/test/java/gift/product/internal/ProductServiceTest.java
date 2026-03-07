package gift.product.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

import gift.category.Category;
import gift.category.CategoryQueryPort;
import gift.global.NotFoundException;
import gift.product.Product;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PagedModel.PageMetadata;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    @InjectMocks
    ProductService productService;

    @Mock
    ProductRepository productRepo;

    @Mock
    CategoryQueryPort categoryQueryPort;

    @Test
    @DisplayName("상품 목록을 페이징 조회한다")
    void testGetProducts() {
        // given
        int pageNumber = 0;
        int pageSize = 10;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        given(productRepo.findAll(pageable))
                .willReturn(Page.empty());

        // when
        PagedModel<ProductResponse> response = productService.getProducts(null, pageable);

        // then
        then(productRepo).should()
                .findAll(pageable);

        assertThat(response.getContent())
                .isNotNull().isEmpty();

        PageMetadata metadata = response.getMetadata();
        assertThat(metadata.size()).isZero();
        assertThat(metadata.number()).isEqualTo(pageNumber);
        assertThat(metadata.totalElements()).isZero();
        assertThat(metadata.totalPages()).isOne();
    }

    @Test
    @DisplayName("카테고리 ID로 상품 목록을 필터링 조회한다")
    void testGetProductsWithCategoryFilter() {
        // given
        Long categoryId = 1L;
        int pageNumber = 0;
        int pageSize = 10;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        given(productRepo.findByCategoryId(categoryId, pageable))
                .willReturn(Page.empty());

        // when
        PagedModel<ProductResponse> response = productService.getProducts(categoryId, pageable);

        // then
        then(productRepo).should()
                .findByCategoryId(categoryId, pageable);

        assertThat(response.getContent())
                .isNotNull().isEmpty();
    }

    @Test
    @DisplayName("상품을 단건 조회한다")
    void testGetProduct() {
        // given
        Long categoryId = 1L;
        Long productId = 10L;
        Product product = createProduct(
                productId, "상품", 1000,
                "https://img.png", categoryId
        );

        given(productRepo.findById(productId))
                .willReturn(Optional.of(product));

        // when
        ProductResponse response = productService.getProduct(productId);

        // then
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo(product.getName());
        assertThat(response.price()).isEqualTo(product.getPrice());
        assertThat(response.imageUrl()).isEqualTo(product.getImageUrl());
        assertThat(response.categoryId()).isEqualTo(categoryId);
    }

    private static Product createProduct(
            Long id, String name, int price,
            String imageUrl, Long categoryId
    ) {
        Category category = createCategory(categoryId);

        return createProduct(id, name, price, imageUrl, category);
    }

    private static Category createCategory(Long id) {
        Category category = Category.builder()
                .name("카테고리" + id)
                .color("#000000")
                .imageUrl("http://cat.png")
                .description("설명")
                .build();

        try {
            Field idField = Category.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(category, id);
            return category;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Product createProduct(
            Long id, String name, int price,
            String imageUrl, Category category
    ) {
        Product product = Product.builder()
                .name(name)
                .price(price)
                .imageUrl(imageUrl)
                .category(category)
                .build();
        try {
            Field idField = Product.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(product, id);
            return product;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 NotFoundException 이 발생한다")
    void testGetProductNotFound() {
        // given
        given(productRepo.findById(NOT_EXISTING_ID))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> productService.getProduct(NOT_EXISTING_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("상품을 생성한다 — categoryQueryPort.getReference 호출 확인")
    void testCreateProduct() {
        // given
        ProductRequest request = new ProductRequest("새상품", 2000, "http://img.png", 100L);

        Long categoryId = request.categoryId();
        Category category = createCategory(categoryId);
        Long productId = 10L;
        Product product = createProduct(
                productId, request.name(), request.price(), request.imageUrl(),
                categoryId
        );

        given(categoryQueryPort.getReference(categoryId))
                .willReturn(category);
        given(productRepo.save(any(Product.class)))
                .willReturn(product);

        // when
        ProductResponse response = productService.createProduct(request);

        // then
        then(categoryQueryPort).should()
                .getReference(categoryId);
        then(productRepo).should()
                .save(any(Product.class));

        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.price()).isEqualTo(request.price());
        assertThat(response.imageUrl()).isEqualTo(request.imageUrl());
        assertThat(response.categoryId()).isEqualTo(request.categoryId());
    }

    @Test
    @DisplayName("상품 생성 시 카테고리가 없으면 NotFoundException 이 전파된다")
    void testCreateProductCategoryNotFound() {
        // given
        ProductRequest request = new ProductRequest(
                "상품", 1000,
                "http://img.png", NOT_EXISTING_ID
        );

        given(categoryQueryPort.getReference(NOT_EXISTING_ID))
                .willThrow(NotFoundException.categoryNotFound());

        // when + then
        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("상품을 수정한다 — categoryQueryPort.getReference 호출 확인")
    void testUpdateProduct() {
        // given
        ProductRequest request = new ProductRequest(
                "새상품", 2000, "https://img.png", 100L
        );

        Long productId = 10L;
        Long oldCategoryId = 20L;
        Long newCategoryId = request.categoryId();

        Category oldCategory = createCategory(oldCategoryId);
        Category newCategory = createCategory(newCategoryId);
        Product productEntity = createProduct(
                productId, "기존 이름", 1_000,
                "https://origin.png", oldCategoryId
        );

        lenient().when(categoryQueryPort.getReference(oldCategoryId))
                .thenReturn(oldCategory);
        given(categoryQueryPort.getReference(newCategoryId))
                .willReturn(newCategory);
        given(productRepo.findById(productId))
                .willReturn(Optional.of(productEntity));

        // when
        ProductResponse response = productService.updateProduct(productId, request);

        // then
        then(categoryQueryPort).should()
                .getReference(newCategoryId);

        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.price()).isEqualTo(request.price());
        assertThat(response.imageUrl()).isEqualTo(request.imageUrl());
        assertThat(response.categoryId()).isEqualTo(request.categoryId());

        assertThat(productEntity.getName()).isEqualTo(request.name());
        assertThat(productEntity.getPrice()).isEqualTo(request.price());
        assertThat(productEntity.getImageUrl()).isEqualTo(request.imageUrl());
        assertThat(productEntity.getCategory()).isEqualTo(newCategory);
    }

    @Test
    @DisplayName("상품 수정 시 카테고리가 없으면 NotFoundException 이 전파된다")
    void testUpdateProductCategoryNotFound() {
        // given
        ProductRequest request = new ProductRequest(
                "새 상품", 1000, "https://img.png", NOT_EXISTING_ID
        );

        Long productId = 1L;
        Long existingCategoryId = 2L;

        Category existingCategory = createCategory(existingCategoryId);
        Product existingProduct = createProduct(
                productId, "상품", 10, "https://origin.png",
                existingCategory
        );

        given(categoryQueryPort.getReference(NOT_EXISTING_ID))
                .willThrow(NotFoundException.categoryNotFound());
        lenient().when(productRepo.findById(productId))
                .thenReturn(Optional.of(existingProduct));
        lenient().when(categoryQueryPort.getReference(existingCategoryId))
                .thenReturn(existingCategory);

        // when + then
        assertThatThrownBy(() -> productService.updateProduct(productId, request))
                .isInstanceOf(NotFoundException.class);
    }

    // -- fixtures --

    @Test
    @DisplayName("상품 수정 시 상품이 없으면 NotFoundException 이 발생한다")
    void testUpdateProductNotFound() {
        // given
        Long categoryId = 1L;
        Category category = createCategory(categoryId);
        ProductRequest request = new ProductRequest(
                "상품", 10,
                "https://img.png", categoryId
        );

        given(categoryQueryPort.getReference(categoryId))
                .willReturn(category);
        given(productRepo.findById(NOT_EXISTING_ID))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> productService.updateProduct(NOT_EXISTING_ID, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("상품을 삭제한다")
    void testDeleteProduct() {
        // given
        Long productId = 1L;
        Long categoryId = 2L;
        Product product = createProduct(
                productId, "상품", 100,
                "https://img.png", categoryId
        );

        given(productRepo.findById(productId))
                .willReturn(Optional.of(product));

        // when
        productService.deleteProduct(productId);

        // then
        then(productRepo).should()
                .delete(product);
    }

    @Test
    @DisplayName("존재하지 않는 상품 삭제 시 NotFoundException 이 발생한다")
    void testDeleteProductNotFound() {
        // given
        given(productRepo.findById(NOT_EXISTING_ID))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> productService.deleteProduct(NOT_EXISTING_ID))
                .isInstanceOf(NotFoundException.class);
    }
}
