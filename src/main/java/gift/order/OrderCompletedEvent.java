package gift.order;

public record OrderCompletedEvent(
        String accessToken,
        Long orderId,
        int quantity,
        String message,
        String optionName,
        String productName,
        int productPrice) {

    public int totalPrice() {
        return productPrice * quantity;
    }
}
