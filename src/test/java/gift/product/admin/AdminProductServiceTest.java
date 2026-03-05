package gift.product.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

import gift.category.Category;
import gift.category.CategoryQueryPort;
import gift.global.NotFoundException;
import gift.product.Product;
import gift.product.admin.ProductDto.CategoryDto;
import java.lang.reflect.Field;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    @InjectMocks
    AdminProductService adminProductService;

    @Mock
    AdminProductRepository productRepo;

    @Mock
    CategoryQueryPort categoryQueryPort;

    @Test
    @DisplayName("전체 상품 목록을 조회한다")
    void testGetAllProducts() {
        // given
        Category category = createCategory(1L);
        Product product = createProduct(10L, "상품", 1000, "https://img.png", category);

        given(productRepo.findAllInnerJoinFetchCategory())
                .willReturn(List.of(product));

        // when
        List<ProductDto> response = adminProductService.getAllProducts();

        // then
        then(productRepo).should()
                .findAllInnerJoinFetchCategory();

        assertThat(response).hasSize(1);

        ProductDto content = response.getFirst();
        assertThat(content.id()).isEqualTo(product.getId());
        assertThat(content.name()).isEqualTo(product.getName());
        assertThat(content.price()).isEqualTo(product.getPrice());
        assertThat(content.imageUrl()).isEqualTo(product.getImageUrl());

        assertThat(content.category().id()).isEqualTo(category.getId());
        assertThat(content.category().name()).isEqualTo(category.getName());
    }

    private static Category createCategory(Long id) {
        Category category = Category.builder()
                .name("카테고리" + id)
                .color("#000000")
                .imageUrl("https://cat.png")
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

    @SuppressWarnings("SameParameterValue")
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
    @DisplayName("전체 카테고리 목록을 조회한다 — categoryQueryPort.findAll() 호출 + DTO 매핑 확인")
    void testGetAllCategories() {
        // given
        gift.category.CategoryDto portDto = new gift.category.CategoryDto(
                1L, "교환권", "#FF0000", "https://cat.png", "설명"
        );

        given(categoryQueryPort.findAll())
                .willReturn(List.of(portDto));

        // when
        List<CategoryDto> response = adminProductService.getAllCategories();

        // then
        then(categoryQueryPort).should()
                .findAll();

        assertThat(response).hasSize(1);

        CategoryDto dto = response.getFirst();
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("교환권");
    }

    @Test
    @DisplayName("상품을 생성한다 — categoryQueryPort.getReference 호출 확인")
    void testCreateProduct() {
        // given
        Long categoryId = 1L;
        Category category = createCategory(categoryId);
        given(categoryQueryPort.getReference(categoryId))
                .willReturn(category);

        // when + then
        assertThatCode(() -> adminProductService.createProduct(
                "새상품", 2000, "https://img.png", categoryId
        ))
                .doesNotThrowAnyException();

        // then
        then(categoryQueryPort).should()
                .getReference(categoryId);
        then(productRepo).should()
                .save(any(Product.class));
    }

    @Test
    @DisplayName("상품 생성 시 카테고리가 없으면 NoSuchElementException 으로 변환된다")
    void testCreateProductCategoryNotFound() {
        // given
        given(categoryQueryPort.getReference(NOT_EXISTING_ID))
                .willThrow(NotFoundException.categoryNotFound());

        // when + then
        assertThatThrownBy(() -> adminProductService.createProduct(
                "상품", 1000, "https://img.png", NOT_EXISTING_ID
        ))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("상품을 단건 조회한다")
    void testGetProduct() {
        // given
        Long productId = 10L;
        Category category = createCategory(1L);
        Product product = createProduct(
                productId, "상품", 1000,
                "https://img.png", category
        );

        given(productRepo.findByIdInnerJoinFetchCategory(productId))
                .willReturn(Optional.of(product));

        // when
        ProductDto result = adminProductService.getProduct(productId);

        // then
        assertThat(result.id()).isEqualTo(productId);
        assertThat(result.name()).isEqualTo(product.getName());
        assertThat(result.price()).isEqualTo(product.getPrice());
        assertThat(result.imageUrl()).isEqualTo(product.getImageUrl());

        assertThat(result.category().id()).isEqualTo(category.getId());
        assertThat(result.category().name()).isEqualTo(category.getName());
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 NoSuchElementException 이 발생한다")
    void testGetProductNotFound() {
        // given
        given(productRepo.findByIdInnerJoinFetchCategory(NOT_EXISTING_ID))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> adminProductService.getProduct(NOT_EXISTING_ID))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("상품을 수정한다 — categoryQueryPort.getReference 호출 확인")
    void testUpdateProduct() {
        // given
        Long productId = 10L;
        Long oldCategoryId = 1L;
        Long newCategoryId = 2L;

        Category newCategory = createCategory(newCategoryId);
        Product existingProduct = createProduct(
                productId, "기존상품", 1000, "https://old.png",
                createCategory(oldCategoryId)
        );

        given(productRepo.findById(productId))
                .willReturn(Optional.of(existingProduct));
        given(categoryQueryPort.getReference(newCategoryId))
                .willReturn(newCategory);

        // when + then
        assertThatCode(() -> adminProductService.updateProduct(
                productId, "수정상품", 3000,
                "https://new.png", newCategoryId
        ))
                .doesNotThrowAnyException();

        // then
        then(categoryQueryPort).should()
                .getReference(newCategoryId);

        assertThat(existingProduct.getName()).isEqualTo("수정상품");
        assertThat(existingProduct.getPrice()).isEqualTo(3000);
        assertThat(existingProduct.getImageUrl()).isEqualTo("https://new.png");
        assertThat(existingProduct.getCategory()).isEqualTo(newCategory);
    }

    @Test
    @DisplayName("상품 수정 시 상품이 없으면 NoSuchElementException 이 발생한다")
    void testUpdateProductNotFound() {
        // given
        Long categoryId = 1L;
        Category category = createCategory(1L);

        lenient().when(categoryQueryPort.getReference(categoryId))
                .thenReturn(category);
        given(productRepo.findById(NOT_EXISTING_ID))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> adminProductService.updateProduct(
                NOT_EXISTING_ID, "상품", 1000,
                "https://img.png", categoryId
        ))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("상품 수정 시 카테고리가 없으면 NoSuchElementException 으로 변환된다")
    void testUpdateProductCategoryNotFound() {
        // given
        Long productId = 10L;
        Long existingCategoryId = 1L;
        Product existingProduct = createProduct(
                productId, "상품", 1000, "https://img.png",
                createCategory(existingCategoryId)
        );

        given(productRepo.findById(productId))
                .willReturn(Optional.of(existingProduct));
        given(categoryQueryPort.getReference(NOT_EXISTING_ID))
                .willThrow(NotFoundException.categoryNotFound());
        lenient().when(categoryQueryPort.getReference(existingCategoryId))
                .thenReturn(createCategory(existingCategoryId));

        // when + then
        assertThatThrownBy(() -> adminProductService.updateProduct(
                productId, "상품", 1000,
                "https://img.png", NOT_EXISTING_ID
        ))
                .isInstanceOf(NoSuchElementException.class);
    }

    // -- fixtures --

    @Test
    @DisplayName("상품을 삭제한다")
    void testDeleteProduct() {
        // given
        Long productId = 10L;
        Product product = createProduct(
                productId, "상품", 1000, "https://img.png",
                createCategory(1L)
        );

        given(productRepo.findById(productId))
                .willReturn(Optional.of(product));

        // when + then
        assertThatCode(() -> adminProductService.deleteProduct(productId))
                .doesNotThrowAnyException();

        // then
        then(productRepo).should()
                .delete(product);
    }

    @Test
    @DisplayName("존재하지 않는 상품 삭제 시 NoSuchElementException 이 발생한다")
    void testDeleteProductNotFound() {
        // given
        given(productRepo.findById(NOT_EXISTING_ID))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> adminProductService.deleteProduct(NOT_EXISTING_ID))
                .isInstanceOf(NoSuchElementException.class);
    }
}
