package gift.category;

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
class CategoryCommandServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryCommandService categoryCommandService;

    @Test
    @DisplayName("카테고리를 저장한다")
    void save() {
        var category = new Category("전자기기", "#FF0000", "https://example.com/img.jpg", "설명");
        given(categoryRepository.save(any(Category.class))).willReturn(category);

        Category result = categoryCommandService.save(category);

        assertThat(result.getName()).isEqualTo("전자기기");
    }

    @Test
    @DisplayName("카테고리를 수정한다")
    void update() {
        var category = new Category("전자기기", "#FF0000", "https://example.com/img.jpg", "설명");
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.save(any(Category.class))).willReturn(category);

        Category result = categoryCommandService.update(1L, "의류", "#00FF00", "https://example.com/img2.jpg", "수정됨");

        assertThat(result.getName()).isEqualTo("의류");
    }

    @Test
    @DisplayName("존재하지 않는 카테고리를 수정하면 예외가 발생한다")
    void update_NotFound() {
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryCommandService.update(999L, "의류", "#00FF00", "https://example.com/img.jpg", "설명"))
            .isInstanceOf(CategoryException.class);
    }

    @Test
    @DisplayName("카테고리를 삭제한다")
    void deleteById() {
        categoryCommandService.deleteById(1L);

        then(categoryRepository).should().deleteById(1L);
    }
}
