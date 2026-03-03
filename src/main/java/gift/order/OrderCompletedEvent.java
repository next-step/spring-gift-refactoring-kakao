package gift.order;

import gift.product.Product;

public record OrderCompletedEvent(String accessToken, Order order, Product product) {}
