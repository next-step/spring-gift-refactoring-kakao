package gift.wish;

import gift.category.Category;
import gift.product.Product;
import gift.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WishServiceTest {

    @Mock
    private WishRepository wishRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private WishService wishService;

    @Test
    void addWish_위시가_없으면_새로_생성한다() {
        // given
        Category category = new Category("전자기기", "#000000", "url", "desc");
        Product product = new Product("상품", 1000, "img", category);

        given(wishRepository.findByMemberIdAndProductId(1L, 1L)).willReturn(Optional.empty());
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(wishRepository.save(any(Wish.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Wish result = wishService.addWish(1L, 1L);

        // then
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getProduct()).isEqualTo(product);
        verify(wishRepository).save(any(Wish.class));
    }

    @Test
    void addWish_이미_존재하면_기존_위시를_반환한다() {
        // given
        Category category = new Category("전자기기", "#000000", "url", "desc");
        Product product = new Product("상품", 1000, "img", category);
        Wish existing = new Wish(1L, product);

        given(wishRepository.findByMemberIdAndProductId(1L, 1L)).willReturn(Optional.of(existing));

        // when
        Wish result = wishService.addWish(1L, 1L);

        // then
        assertThat(result).isSameAs(existing);
        verify(wishRepository, never()).save(any());
    }

    @Test
    void remove_다른_회원의_위시를_삭제하면_예외가_발생한다() {
        // given
        Category category = new Category("전자기기", "#000000", "url", "desc");
        Product product = new Product("상품", 1000, "img", category);
        Wish wish = new Wish(2L, product); // memberId=2의 위시

        given(wishRepository.findById(1L)).willReturn(Optional.of(wish));

        // when & then
        assertThatThrownBy(() -> wishService.remove(1L, 1L))
            .isInstanceOf(ForbiddenException.class);

        verify(wishRepository, never()).delete(any());
    }
}