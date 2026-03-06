package gift.order;

import gift.product.Product;

public record OrderCompletedEvent(Long memberId, String accessToken, Order order, Product product) {}
