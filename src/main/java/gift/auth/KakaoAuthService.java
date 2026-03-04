package gift.auth;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class KakaoAuthService {
    private final KakaoLoginProperties properties;
    private final KakaoLoginClient kakaoLoginClient;
    private final KakaoMemberService kakaoMemberService;

    public KakaoAuthService(
        KakaoLoginProperties properties,
        KakaoLoginClient kakaoLoginClient,
        KakaoMemberService kakaoMemberService
    ) {
        this.properties = properties;
        this.kakaoLoginClient = kakaoLoginClient;
        this.kakaoMemberService = kakaoMemberService;
    }

    public String buildAuthorizationUrl() {
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", properties.clientId())
            .queryParam("redirect_uri", properties.redirectUri())
            .queryParam("scope", "account_email,talk_message")
            .build()
            .toUriString();
    }

    public TokenResponse processCallback(String code) {
        KakaoLoginClient.KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);
        KakaoLoginClient.KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(kakaoToken.accessToken());

        return kakaoMemberService.registerOrUpdate(kakaoUser.email(), kakaoToken.accessToken());
    }
}
