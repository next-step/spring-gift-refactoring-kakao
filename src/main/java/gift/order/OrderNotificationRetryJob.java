package gift.order;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 실패한 주문 알림을 다시 보낸다(ADR 0002).
 * 주기 실행은 OrderNotificationRetryScheduling이 맡는다. 인수 테스트는 retryOnce()를 직접 부른다.
 */
@Component
public class OrderNotificationRetryJob {
    private static final Logger log = LoggerFactory.getLogger(OrderNotificationRetryJob.class);

    // 이보다 최근의 시도 0번 행은 커밋 직후 발송이 아직 진행 중일 수 있어서 건드리지 않는다.
    private static final long UNATTEMPTED_GRACE_MINUTES = 1;

    private final OrderNotificationRepository orderNotificationRepository;
    private final OrderNotificationDispatcher orderNotificationDispatcher;

    public OrderNotificationRetryJob(
            OrderNotificationRepository orderNotificationRepository,
            OrderNotificationDispatcher orderNotificationDispatcher) {
        this.orderNotificationRepository = orderNotificationRepository;
        this.orderNotificationDispatcher = orderNotificationDispatcher;
    }

    public void retryOnce() {
        var createdBefore = LocalDateTime.now().minusMinutes(UNATTEMPTED_GRACE_MINUTES);
        var targetIds = orderNotificationRepository.findRetryTargetIds(OrderNotification.Status.PENDING, createdBefore);
        for (Long notificationId : targetIds) {
            try {
                // 발송 실패는 dispatch가 시도 횟수와 마지막 오류로 기록한다.
                orderNotificationDispatcher.dispatch(notificationId);
            } catch (Exception e) {
                // 결과를 저장하지 못한 행은 다음 재시도에서 다시 본다. 한 행 때문에 나머지를 멈추지 않는다.
                log.warn("주문 알림 재시도 결과 저장 실패: notificationId={}", notificationId, e);
            }
        }
    }
}
