package gift.category;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CategoryQueryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryQueryService categoryQueryService;

    @Test
    @DisplayName("전체 카테고리 목록을 반환한다")
    void findAll() {
        var categories = List.of(
            new Category("전자기기", "#FF0000", "https://example.com/img1.jpg", "설명1"),
            new Category("의류", "#00FF00", "https://example.com/img2.jpg", "설명2")
        );
        given(categoryRepository.findAll()).willReturn(categories);

        List<Category> result = categoryQueryService.findAll();

        assertThat(result).hasSize(2);
    }
}
