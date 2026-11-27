package gift.order;

import gift.product.Product;

public final class OrderNotificationMessage {

    private OrderNotificationMessage() {}

    public static String build(Order order, Product product) {
        var totalPrice = String.format("%,d", product.getPrice() * order.getQuantity());
        var orderMessageLine =
                order.getMessage() != null && !order.getMessage().isBlank() ? "\\n\\n💌 " + order.getMessage() : "";
        return """
            {
                "object_type": "text",
                "text": "🎁 선물이 도착했어요!\\n\\n%s (%s)\\n수량: %d개\\n금액: %s원%s",
                "link": {},
                "button_title": "선물 확인하기"
            }
            """.formatted(
                product.getName(), order.getOption().getName(), order.getQuantity(), totalPrice, orderMessageLine);
    }
}
