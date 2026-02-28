package gift.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() throws Exception {
        category = new Category("교환권", "#ffffff", "img.png", "설명");
        setId(category, 1L);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Nested
    @DisplayName("getCategories")
    class GetCategories {

        @Test
        @DisplayName("전체 카테고리 목록을 조회한다")
        void returnsCategories() {
            given(categoryRepository.findAll()).willReturn(List.of(category));

            List<CategoryResponse> result = categoryService.getCategories();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("교환권");
        }
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategory {

        @Test
        @DisplayName("카테고리를 생성한다")
        void createsSuccessfully() throws Exception {
            var request = new CategoryRequest("상품권", "#000000", "new.png", "새 카테고리");
            var saved = new Category("상품권", "#000000", "new.png", "새 카테고리");
            setId(saved, 2L);

            given(categoryRepository.save(any(Category.class))).willReturn(saved);

            Category result = categoryService.createCategory(request);

            assertThat(result.getName()).isEqualTo("상품권");
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategory {

        @Test
        @DisplayName("카테고리를 수정한다")
        void updatesSuccessfully() {
            var request = new CategoryRequest("수정됨", "#000000", "new.png", "수정 설명");

            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(categoryRepository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

            Category result = categoryService.updateCategory(1L, request);

            assertThat(result.getName()).isEqualTo("수정됨");
            assertThat(result.getColor()).isEqualTo("#000000");
        }

        @Test
        @DisplayName("존재하지 않는 카테고리면 예외를 던진다")
        void throwsWhenNotFound() {
            var request = new CategoryRequest("수정됨", "#000000", "new.png", "");

            given(categoryRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.updateCategory(999L, request))
                .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategory {

        @Test
        @DisplayName("카테고리를 삭제한다")
        void deletesCategory() {
            categoryService.deleteCategory(1L);

            then(categoryRepository).should().deleteById(1L);
        }
    }
}
