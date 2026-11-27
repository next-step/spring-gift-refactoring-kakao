package gift.wish;

import gift.order.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문이 커밋된 뒤 주문한 회원의 위시에서 주문한 상품을 뺀다(주문 흐름 6단계, ADR 0003).
 * 실패해도 주문은 201이고 위시는 남는다. 실패는 warn 로그로 남긴다.
 *
 * <p>트랜잭션(REQUIRES_NEW)은 이 리스너가 아니라 OrderWishCleanupExecutor에 둔다.
 * 커밋 때 나는 예외까지 이 catch가 잡아 warn 로그를 남기려는 것이고,
 * 같은 빈 안의 호출은 프록시를 거치지 않아 @Transactional이 걸리지 않기 때문이다.
 * (07의 OrderNotificationRelay / OrderNotificationDispatcher와 같은 구조)
 */
@Component
public class OrderWishCleanupListener {
    private static final Logger log = LoggerFactory.getLogger(OrderWishCleanupListener.class);

    private final OrderWishCleanupExecutor orderWishCleanupExecutor;

    public OrderWishCleanupListener(OrderWishCleanupExecutor orderWishCleanupExecutor) {
        this.orderWishCleanupExecutor = orderWishCleanupExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void cleanUpAfterCommit(OrderPlacedEvent event) {
        try {
            orderWishCleanupExecutor.cleanUp(event);
        } catch (Exception e) {
            log.warn(
                    "위시 정리 실패: orderId={}, memberId={}, productId={}, message={}",
                    event.orderId(),
                    event.memberId(),
                    event.productId(),
                    e.getMessage(),
                    e);
        }
    }
}
