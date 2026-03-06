package gift.infrastructure.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao.login")
public record KakaoLoginProperties(
    String clientId,
    String clientSecret,
    String redirectUri,
    String authorizeUrl,
    String tokenUrl,
    String userInfoUrl) {}
