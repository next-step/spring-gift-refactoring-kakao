package gift.wish;

import gift.order.OrderPlacedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문한 회원의 위시에서 주문한 상품을 새 트랜잭션에서 지운다(ADR 0003).
 * 주문 커밋 뒤에 불리므로 REQUIRES_NEW로 연다. 리스너와 빈을 나눈 이유는 OrderWishCleanupListener에 있다.
 */
@Component
public class OrderWishCleanupExecutor {
    private final WishCleaner wishCleaner;

    public OrderWishCleanupExecutor(WishCleaner wishCleaner) {
        this.wishCleaner = wishCleaner;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanUp(OrderPlacedEvent event) {
        wishCleaner.clean(event.memberId(), event.productId());
    }
}
