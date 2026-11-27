package gift.order;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 주문 알림 재시도를 1분 간격으로 돌린다(ADR 0002).
 * 인수 테스트에서는 gift.order-notification.retry.enabled=false로 끄고, 시나리오가 재시도를 직접 부른다.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "gift.order-notification.retry.enabled", havingValue = "true", matchIfMissing = true)
public class OrderNotificationRetryScheduling {

    private final OrderNotificationRetryJob orderNotificationRetryJob;

    public OrderNotificationRetryScheduling(OrderNotificationRetryJob orderNotificationRetryJob) {
        this.orderNotificationRetryJob = orderNotificationRetryJob;
    }

    @Scheduled(fixedDelay = 60000)
    public void retry() {
        orderNotificationRetryJob.retryOnce();
    }
}
