package gift.order;

public record OrderCompletedEvent(
    String kakaoAccessToken,
    KakaoOrderMessage message
) {
}
