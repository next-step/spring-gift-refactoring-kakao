package gift.category;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("카테고리 조회 성공 시 카테고리를 반환한다")
    void findByIdOrThrow_success_returnsCategory() {
        Category category = new Category("음료", "#000000", "http://image.png", null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Category result = categoryService.findByIdOrThrow(1L);

        assertEquals(category, result);
    }

    @Test
    @DisplayName("카테고리가 없으면 CATEGORY_NOT_FOUND 예외를 던진다")
    void findByIdOrThrow_notFound_throwsException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        CategoryException exception = assertThrows(CategoryException.class, () -> categoryService.findByIdOrThrow(1L));

        assertEquals(CategoryErrorCode.CATEGORY_NOT_FOUND, exception.getErrorCode());
    }
}
