package gift.order;

public record OrderCreatedEvent(
        Long memberId, Long productId, Long orderId
) {

}
