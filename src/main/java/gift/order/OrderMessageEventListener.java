package gift.order;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderMessageEventListener {
    private final OrderMessageClient orderMessageClient;

    public OrderMessageEventListener(OrderMessageClient orderMessageClient) {
        this.orderMessageClient = orderMessageClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderCreatedEvent event) {
        if (event.kakaoAccessToken() == null) {
            return;
        }
        try {
            orderMessageClient.sendToMe(event.kakaoAccessToken(), event.order(), event.product());
        } catch (Exception ignored) {
        }
    }
}
