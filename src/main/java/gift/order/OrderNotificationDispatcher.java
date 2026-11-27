package gift.order;

import gift.member.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 저장된 주문 알림 하나를 보내고 결과를 기록한다.
 * ADR 0002: 커밋 뒤에 불리므로 새 트랜잭션(REQUIRES_NEW)에서 결과를 저장한다.
 */
@Component
public class OrderNotificationDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OrderNotificationDispatcher.class);

    private final OrderNotificationRepository orderNotificationRepository;
    private final MemberRepository memberRepository;
    private final OrderNotificationSender orderNotificationSender;

    public OrderNotificationDispatcher(
            OrderNotificationRepository orderNotificationRepository,
            MemberRepository memberRepository,
            OrderNotificationSender orderNotificationSender) {
        this.orderNotificationRepository = orderNotificationRepository;
        this.memberRepository = memberRepository;
        this.orderNotificationSender = orderNotificationSender;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(Long notificationId) {
        var notification = orderNotificationRepository.findById(notificationId).orElseThrow();
        // 카카오 액세스 토큰은 아웃박스에 저장하지 않고, 보낼 때 회원에서 읽는다.
        var member = memberRepository.findById(notification.getMemberId()).orElseThrow();
        try {
            orderNotificationSender.send(member.getKakaoAccessToken(), notification.getMessage());
            notification.markSent();
        } catch (Exception e) {
            log.warn(
                    "주문 알림 발송 실패: notificationId={}, attempt={}",
                    notificationId,
                    notification.getAttemptCount() + 1,
                    e);
            notification.recordFailure(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        orderNotificationRepository.save(notification);
    }
}
