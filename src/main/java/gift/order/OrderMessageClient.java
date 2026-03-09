package gift.order;

import gift.product.Product;

public interface OrderMessageClient {

    void sendToMe(String accessToken, Order order, Product product);
}
