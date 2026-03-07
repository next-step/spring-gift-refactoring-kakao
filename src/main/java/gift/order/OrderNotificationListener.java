package gift.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(OrderNotificationListener.class);
    private final KakaoMessageClient kakaoMessageClient;

    public OrderNotificationListener(KakaoMessageClient kakaoMessageClient) {
        this.kakaoMessageClient = kakaoMessageClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderCreatedEvent event) {
        if (event.kakaoAccessToken() == null) {
            return;
        }
        try {
            kakaoMessageClient.sendToMe(event.kakaoAccessToken(), event.order(), event.product());
        } catch (Exception e) {
            log.warn("카카오 알림 전송 실패: orderId={}", event.order().getId(), e);
        }
    }
}
