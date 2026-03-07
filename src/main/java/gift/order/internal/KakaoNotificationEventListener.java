package gift.order.internal;

import gift.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class KakaoNotificationEventListener {

    private final OrderMessageBuilder orderMessageBuilder;
    private final KakaoMessagingService kakaoMessagingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            OrderMessageDto orderMessageDto = orderMessageBuilder.buildFrom(event.orderId());
            kakaoMessagingService.sendDefaultTemplateMessageTo(event.memberId(), orderMessageDto);
        } catch (Exception ignored) {
        }
    }
}
