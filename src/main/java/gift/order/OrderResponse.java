package gift.order;

import java.time.LocalDateTime;

public record OrderResponse(
    Long id,
    Long optionId,
    int quantity,
    int totalPrice,
    LocalDateTime orderDateTime,
    String message
) {
    public static OrderResponse from(Order order) {
        var option = order.getOption();
        return new OrderResponse(
            order.getId(),
            option.getId(),
            order.getQuantity(),
            order.getTotalPrice(),
            order.getOrderDateTime(),
            order.getMessage()
        );
    }
}
