package gift.order;

import gift.wish.WishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final KakaoMessageClient kakaoMessageClient;
    private final WishService wishService;

    public OrderEventListener(KakaoMessageClient kakaoMessageClient, WishService wishService) {
        this.kakaoMessageClient = kakaoMessageClient;
        this.wishService = wishService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeWish(OrderCompletedEvent event) {
        try {
            if (wishService.existsWishByMemberAndProduct(
                    event.memberId(), event.product().getId())) {
                wishService.removeWishByMemberAndProduct(
                        event.memberId(), event.product().getId());
            }
        } catch (Exception e) {
            log.warn("위시 삭제 실패: orderId={}", event.order().getId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendKakaoMessage(OrderCompletedEvent event) {
        if (event.accessToken() == null) {
            return;
        }
        try {
            kakaoMessageClient.sendToMe(event.accessToken(), event.order(), event.product());
        } catch (Exception e) {
            log.warn("카카오 메시지 전송 실패: orderId={}", event.order().getId(), e);
        }
    }
}
