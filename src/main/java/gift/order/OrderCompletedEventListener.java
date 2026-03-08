package gift.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderCompletedEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderCompletedEventListener.class);

    private final OrderMessageClient orderMessageClient;

    public OrderCompletedEventListener(OrderMessageClient orderMessageClient) {
        this.orderMessageClient = orderMessageClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderCompletedEvent event) {
        try {
            orderMessageClient.sendToMe(event);
        } catch (Exception e) {
            log.warn("주문 알림 메시지 전송 실패: orderId={}", event.orderId(), e);
        }
    }
}
