package gift.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoLoginPropertiesTest {

    @Test
    @DisplayName("카카오 로그인 URL에 필요한 파라미터가 모두 포함된다")
    void buildLoginUrl() {
        var properties = new KakaoLoginProperties("my-client-id", "secret", "http://localhost/callback");

        String url = properties.buildLoginUrl();

        assertThat(url).startsWith("https://kauth.kakao.com/oauth/authorize");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("client_id=my-client-id");
        assertThat(url).contains("redirect_uri=http://localhost/callback");
        assertThat(url).contains("scope=account_email,talk_message");
    }
}
