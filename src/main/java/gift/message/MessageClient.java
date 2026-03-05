package gift.message;

import gift.external.ExternalProvider;
import gift.order.Order;
import gift.product.Product;

public interface MessageClient {
    ExternalProvider provider();

    void sendToMe(String accessToken, Order order, Product product);
}
