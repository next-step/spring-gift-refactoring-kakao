package gift.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;

@ConfigurationProperties(prefix = "kakao.login")
public record KakaoLoginProperties(String clientId, String clientSecret, String redirectUri) {

    public String buildLoginUrl() {
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("scope", "account_email,talk_message")
            .build()
            .toUriString();
    }
}
