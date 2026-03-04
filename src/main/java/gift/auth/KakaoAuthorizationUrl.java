package gift.auth;

import org.springframework.web.util.UriComponentsBuilder;

public class KakaoAuthorizationUrl {
    private static final String AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String SCOPE = "account_email,talk_message";

    private final String clientId;
    private final String redirectUri;

    public KakaoAuthorizationUrl(KakaoLoginProperties properties) {
        this.clientId = properties.clientId();
        this.redirectUri = properties.redirectUri();
    }

    @Override
    public String toString() {
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", SCOPE)
                .build()
                .toUriString();
    }
}
