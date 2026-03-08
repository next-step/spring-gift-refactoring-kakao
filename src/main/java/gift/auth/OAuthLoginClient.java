package gift.auth;

public interface OAuthLoginClient {
    String getAuthorizationUrl();

    OAuthTokenResponse requestAccessToken(String code);

    OAuthUserResponse requestUserInfo(String accessToken);
}
