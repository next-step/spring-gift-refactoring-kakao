package gift.order;

import java.time.LocalDateTime;

public record OrderResponse(
    Long id,
    Long optionId,
    int quantity,
    LocalDateTime orderDateTime,
    String message
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getOptionId(),
            order.getQuantity(),
            order.getOrderDateTime(),
            order.getMessage()
        );
    }
}
