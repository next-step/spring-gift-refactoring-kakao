package gift.wish.internal;

import static org.mockito.BDDMockito.then;

import gift.order.OrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WishCleanUpEventListenerTest {

    @InjectMocks
    WishCleanUpEventListener listener;

    @Mock
    WishCleanUpService wishCleanUpService;

    @Test
    @DisplayName("주문 이벤트 수신 시 위시 정리 서비스를 호출한다")
    void testOnOrderCreated() {
        // given
        Long memberId = 1L;
        Long productId = 10L;
        Long orderId = 100L;
        OrderCreatedEvent event = new OrderCreatedEvent(memberId, productId, orderId);

        // when
        listener.onOrderCreated(event);

        // then
        then(wishCleanUpService).should()
                .cleanWishByMemberAndProductId(memberId, productId);
    }
}
