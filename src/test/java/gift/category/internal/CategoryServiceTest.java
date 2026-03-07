package gift.category.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import gift.category.Category;
import gift.global.NotFoundException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    @InjectMocks
    CategoryService categoryService;

    @Mock
    CategoryRepository categoryRepo;

    @Test
    @DisplayName("카테고리 목록을 조회한다")
    void testGetCategories() {
        // given
        Category category1 = createCategory(
                1L, "교환권", "#FF0000",
                "http://img1.png", "설명1"
        );
        Category category2 = createCategory(
                2L, "상품권", "#00FF00",
                "http://img2.png", "설명2"
        );

        given(categoryRepo.findAll())
                .willReturn(List.of(category1, category2));

        // when
        List<CategoryResponse> responses = categoryService.getCategories();

        // then
        then(categoryRepo).should()
                .findAll();

        assertThat(responses).hasSize(2);

        List<CategoryResponse> expectedResponses = List.of(
                CategoryResponse.from(category1),
                CategoryResponse.from(category2)
        );
        assertThat(responses).containsExactlyInAnyOrderElementsOf(expectedResponses);
    }

    @Test
    @DisplayName("카테고리가 없으면 빈 목록을 반환한다")
    void testGetCategoriesEmpty() {
        // given
        given(categoryRepo.findAll())
                .willReturn(List.of());

        // when
        List<CategoryResponse> responses = categoryService.getCategories();

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("카테고리를 단건 조회한다")
    void testGetCategory() {
        // given
        Long categoryId = 1L;
        Category category = createCategory(
                categoryId, "교환권", "#FF0000",
                "http://img.png", "설명"
        );

        given(categoryRepo.findById(categoryId))
                .willReturn(Optional.of(category));

        // when
        CategoryResponse response = categoryService.getCategory(categoryId);

        // then
        assertThat(response.id()).isEqualTo(categoryId);
        assertThat(response.name()).isEqualTo(category.getName());
        assertThat(response.color()).isEqualTo(category.getColor());
        assertThat(response.imageUrl()).isEqualTo(category.getImageUrl());
        assertThat(response.description()).isEqualTo(category.getDescription());
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 조회 시 NotFoundException 이 발생한다")
    void testGetCategoryNotFound() {
        // given
        given(categoryRepo.findById(NOT_EXISTING_ID))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> categoryService.getCategory(NOT_EXISTING_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("카테고리를 생성한다")
    void testCreateCategory() {
        // given
        CategoryRequest request = new CategoryRequest(
                "교환권", "#FF0000",
                "http://img.png", "설명"
        );
        Long categoryId = 1L;
        Category saved = createCategory(
                categoryId, request.name(), request.color(), request.imageUrl(),
                request.description()
        );

        given(categoryRepo.save(any(Category.class)))
                .willReturn(saved);

        // when
        CategoryResponse response = categoryService.createCategory(request);

        // then
        then(categoryRepo).should()
                .save(any(Category.class));

        assertThat(response.id()).isEqualTo(categoryId);
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.color()).isEqualTo(request.color());
        assertThat(response.imageUrl()).isEqualTo(request.imageUrl());
        assertThat(response.description()).isEqualTo(request.description());
    }

    @Test
    @DisplayName("카테고리를 수정한다")
    void testUpdateCategory() {
        // given
        Long categoryId = 1L;
        Category categoryEntity = createCategory(
                categoryId, "교환권", "#FF0000",
                "http://img.png", "설명"
        );
        CategoryRequest request = new CategoryRequest(
                "음료", "#0000FF",
                "http://new.png", "음료 설명"
        );

        given(categoryRepo.findById(categoryId))
                .willReturn(Optional.of(categoryEntity));

        // when
        CategoryResponse response = categoryService.updateCategory(categoryId, request);

        // then
        assertThat(response.id()).isEqualTo(categoryId);
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.color()).isEqualTo(request.color());
        assertThat(response.imageUrl()).isEqualTo(request.imageUrl());
        assertThat(response.description()).isEqualTo(request.description());

        assertThat(categoryEntity.getName()).isEqualTo(request.name());
        assertThat(categoryEntity.getColor()).isEqualTo(request.color());
        assertThat(categoryEntity.getImageUrl()).isEqualTo(request.imageUrl());
        assertThat(categoryEntity.getDescription()).isEqualTo(request.description());
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 수정 시 NotFoundException 이 발생한다")
    void testUpdateCategoryNotFound() {
        // given
        CategoryRequest request = new CategoryRequest(
                "수정", "#FF0000",
                "http://img.png", "설명"
        );

        given(categoryRepo.findById(NOT_EXISTING_ID))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> categoryService.updateCategory(NOT_EXISTING_ID, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("카테고리를 삭제한다")
    void testDeleteCategory() {
        // given
        Long categoryId = 1L;
        Category category = createCategory(
                categoryId, "교환권", "#FF0000",
                "http://img.png", "설명"
        );

        given(categoryRepo.findById(categoryId))
                .willReturn(Optional.of(category));

        // when
        categoryService.deleteCategory(categoryId);

        // then
        then(categoryRepo).should()
                .delete(category);
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 삭제 시 NotFoundException 이 발생한다")
    void testDeleteCategoryNotFound() {
        // given
        given(categoryRepo.findById(NOT_EXISTING_ID))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> categoryService.deleteCategory(NOT_EXISTING_ID))
                .isInstanceOf(NotFoundException.class);
    }

    // -- fixtures --

    private static Category createCategory(
            Long id, String name, String color,
            String imageUrl, String description
    ) {
        Category category = Category.builder()
                .name(name)
                .color(color)
                .imageUrl(imageUrl)
                .description(description)
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
}
