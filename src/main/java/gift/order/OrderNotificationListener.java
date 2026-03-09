package gift.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
public class OrderNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(OrderNotificationListener.class);

    private final KakaoMessageClient kakaoMessageClient;

    public OrderNotificationListener(KakaoMessageClient kakaoMessageClient) {
        this.kakaoMessageClient = kakaoMessageClient;
    }

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(OrderCompletedEvent event) {
        try {
            kakaoMessageClient.send(event.kakaoAccessToken(), event);
        } catch (Exception e) {
            log.warn("카카오 알림 전송 실패: orderId={}", event.orderId(), e);
        }
    }
}
