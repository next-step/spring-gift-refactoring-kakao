package gift.auth;

public interface OAuthLoginClient {
    String requestAccessToken(String code);

    String requestUserEmail(String accessToken);
}
