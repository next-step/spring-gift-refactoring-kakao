package gift.wish.internal;

import gift.global.NotFoundException;
import gift.order.OrderCreatedEvent;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class WishCleanUpEventListener {

    private final WishCleanUpService wishCleanUpService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        Long memberId = event.memberId();
        Long productId = event.productId();

        try {
            wishCleanUpService.cleanWishByMemberAndProductId(memberId, productId);
        } catch (NotFoundException e) {
            log.warn(
                    "No wish found with memberId={}, productId={}. No wish is deleted.",
                    memberId, productId
            );
        } catch (OptimisticLockException e) {
            log.error(
                    "Failed to clean up wish on event=[{}] due to ex {}: {}",
                    event, e.getClass().getSimpleName(),
                    e.getMessage(), e
            );
        }
    }
}
