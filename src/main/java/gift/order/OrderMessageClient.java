package gift.order;

public interface OrderMessageClient {
    void sendToMe(OrderCompletedEvent event);
}
