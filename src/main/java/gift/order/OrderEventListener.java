package gift.order;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventListener {
    private final KakaoMessageClient kakaoMessageClient;

    public OrderEventListener(KakaoMessageClient kakaoMessageClient) {
        this.kakaoMessageClient = kakaoMessageClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        try {
            kakaoMessageClient.sendToMe(event.kakaoAccessToken(), event.order(), event.product());
        } catch (Exception ignored) {
        }
    }
}
