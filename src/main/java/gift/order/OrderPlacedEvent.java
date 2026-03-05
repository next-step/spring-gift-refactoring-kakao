package gift.order;

import gift.product.Product;

public record OrderPlacedEvent(Order order, String kakaoAccessToken, Product product) {
}
