package gift.infrastructure.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao.message")
public record KakaoMessageProperties(String sendUrl) {}
