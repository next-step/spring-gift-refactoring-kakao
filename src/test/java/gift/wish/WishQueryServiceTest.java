package gift.wish;

import gift.category.Category;
import gift.product.Product;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WishQueryServiceTest {

    @Mock
    private WishRepository wishRepository;

    @InjectMocks
    private WishQueryService wishQueryService;

    private Product createProduct() {
        var category = new Category("전자기기", "#000", "https://example.com/img.jpg", "설명");
        return new Product("테스트상품", 10000, "https://example.com/img.jpg", category);
    }

    @Test
    @DisplayName("회원의 위시리스트를 페이지네이션으로 조회한다")
    void findByMemberId() {
        var pageable = PageRequest.of(0, 10);
        var wish = new Wish(1L, createProduct());
        var page = new PageImpl<>(List.of(wish), pageable, 1);
        given(wishRepository.findByMemberId(1L, pageable)).willReturn(page);

        Page<Wish> result = wishQueryService.findByMemberId(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("회원과 상품으로 위시를 조회한다")
    void findByMemberIdAndProductId() {
        var wish = new Wish(1L, createProduct());
        given(wishRepository.findByMemberIdAndProductId(1L, 1L)).willReturn(Optional.of(wish));

        Wish result = wishQueryService.findByMemberIdAndProductId(1L, 1L);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 위시를 조회하면 null을 반환한다")
    void findByMemberIdAndProductId_NotFound() {
        given(wishRepository.findByMemberIdAndProductId(1L, 999L)).willReturn(Optional.empty());

        Wish result = wishQueryService.findByMemberIdAndProductId(1L, 999L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("ID로 위시를 조회한다")
    void findById() {
        var wish = new Wish(1L, createProduct());
        given(wishRepository.findById(1L)).willReturn(Optional.of(wish));

        Wish result = wishQueryService.findById(1L);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 위시 ID로 조회하면 예외가 발생한다")
    void findById_NotFound() {
        given(wishRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> wishQueryService.findById(999L))
            .isInstanceOf(WishException.class);
    }
}
