package gift.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문이 커밋된 직후 주문 알림을 한 번 보낸다.
 * ADR 0002: 요청과 같은 스레드에서 보낸다(tech-spec 6장 결정 A). 실패한 알림은 재시도가 다시 보낸다.
 */
@Component
public class OrderNotificationRelay {
    private static final Logger log = LoggerFactory.getLogger(OrderNotificationRelay.class);

    private final OrderNotificationDispatcher orderNotificationDispatcher;

    public OrderNotificationRelay(OrderNotificationDispatcher orderNotificationDispatcher) {
        this.orderNotificationDispatcher = orderNotificationDispatcher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAfterCommit(OrderNotificationCreatedEvent event) {
        try {
            orderNotificationDispatcher.dispatch(event.notificationId());
        } catch (Exception e) {
            // 발송 실패는 dispatch가 기록한다. 여기까지 온 것은 결과를 저장하지 못한 경우라,
            // 알림은 시도 0번의 "보낼 것"으로 남고 재시도가 다시 보낸다. 주문 응답(201)에는 새어 나가지 않게 한다.
            log.warn("주문 알림 결과 저장 실패: notificationId={}", event.notificationId(), e);
        }
    }
}
