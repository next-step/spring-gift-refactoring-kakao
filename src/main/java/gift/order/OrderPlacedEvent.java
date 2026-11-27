package gift.order;

// 주문이 저장될 때마다 발행한다. 커밋 뒤 gift.wish가 받아 위시를 정리한다(docs/work/08-wish-cleanup/tech-spec.md 4장).
public record OrderPlacedEvent(Long orderId, Long memberId, Long productId) {}
