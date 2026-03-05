package gift.wish.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import gift.global.NotFoundException;
import gift.wish.Wish;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WishCleanUpServiceTest {

    @InjectMocks
    WishCleanUpService wishCleanUpService;

    @Mock
    WishRepository wishRepo;

    @Test
    @DisplayName("위시가 존재하면 조회 후 삭제한다")
    void testCleanWishByMemberAndProductId() {
        // given
        Long memberId = 1L;
        Long productId = 10L;

        Wish wish = Wish.builder().build();
        given(wishRepo.findByMemberIdAndProductId(memberId, productId))
                .willReturn(Optional.of(wish));

        // when + then
        assertThatCode(() -> wishCleanUpService.cleanWishByMemberAndProductId(
                memberId, productId
        ))
                .doesNotThrowAnyException();

        // then
        then(wishRepo).should()
                .findByMemberIdAndProductId(memberId, productId);
        then(wishRepo).should()
                .delete(wish);
    }

    @Test
    @DisplayName("위시가 존재하지 않으면 NotFoundException을 던진다")
    void testCleanWishByMemberAndProductIdNotFound() {
        // given
        Long memberId = 1L;
        Long productId = 10L;

        given(wishRepo.findByMemberIdAndProductId(memberId, productId))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> wishCleanUpService.cleanWishByMemberAndProductId(
                memberId, productId
        ))
                .isInstanceOf(NotFoundException.class);
    }
}
