package gift.order;

public interface OrderNotificationSender {
    void send(String kakaoAccessToken, String message);
}
