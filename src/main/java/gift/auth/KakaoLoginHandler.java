package gift.auth;

import org.springframework.stereotype.Component;

@Component
public class KakaoLoginHandler implements ExternalLoginHandler {
    private final KakaoLoginClient kakaoLoginClient;

    public KakaoLoginHandler(KakaoLoginClient kakaoLoginClient) {
        this.kakaoLoginClient = kakaoLoginClient;
    }

    @Override
    public ExternalLoginResult login(String authorizationCode) {
        KakaoLoginClient.KakaoTokenResponse token = kakaoLoginClient.requestAccessToken(authorizationCode);
        KakaoLoginClient.KakaoUserResponse user = kakaoLoginClient.requestUserInfo(token.accessToken());
        return new ExternalLoginResult(user.email(), token.accessToken());
    }
}
