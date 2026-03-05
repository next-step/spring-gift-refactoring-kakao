package gift.message;

import gift.external.ExternalProvider;
import gift.order.entity.Order;
import gift.product.entity.Product;

public interface MessageClient {
    ExternalProvider provider();

    void sendToMe(String accessToken, Order order, Product product);
}
