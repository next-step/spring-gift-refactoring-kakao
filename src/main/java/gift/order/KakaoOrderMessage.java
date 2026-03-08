package gift.order;

public record KakaoOrderMessage(
    String productName,
    String optionName,
    int quantity,
    int totalPrice,
    String message
) {
}
