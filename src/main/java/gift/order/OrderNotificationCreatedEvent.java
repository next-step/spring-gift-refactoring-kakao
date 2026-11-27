package gift.order;

// ADR 0002: 주문 알림을 저장한 트랜잭션이 커밋되면 OrderNotificationRelay가 받는다.
public record OrderNotificationCreatedEvent(Long notificationId) {}
