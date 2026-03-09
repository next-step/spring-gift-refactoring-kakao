package gift.order;

public record OrderCompletedEvent(
    String kakaoAccessToken,
    Long orderId,
    String productName,
    String optionName,
    int quantity,
    int productPrice,
    String message
) {
}
