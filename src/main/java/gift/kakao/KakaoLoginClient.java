package gift.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gift.auth.KakaoLoginProperties;
import gift.auth.oauth.OAuthClient;
import gift.auth.oauth.OAuthUserInfo;
import gift.external.ExternalProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class KakaoLoginClient implements OAuthClient {
    private final KakaoLoginProperties properties;
    private final RestClient restClient;

    public KakaoLoginClient(KakaoLoginProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.build();
    }

    @Override
    public ExternalProvider provider() {
        return ExternalProvider.KAKAO;
    }

    @Override
    public URI getLoginUri() {
        String url = UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", properties.clientId())
            .queryParam("redirect_uri", properties.redirectUri())
            .queryParam("scope", "account_email,talk_message")
            .build()
            .toUriString();
        return URI.create(url);
    }

    @Override
    public OAuthUserInfo getUserInfo(String code) {
        KakaoTokenResponse tokenResponse = requestAccessToken(code);
        KakaoUserResponse userResponse = requestUserInfo(tokenResponse.accessToken());
        return new OAuthUserInfo(userResponse.email(), tokenResponse.accessToken());
    }

    private KakaoTokenResponse requestAccessToken(String code) {
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", properties.clientId());
        params.add("redirect_uri", properties.redirectUri());
        params.add("code", code);
        params.add("client_secret", properties.clientSecret());

        return restClient.post()
            .uri("https://kauth.kakao.com/oauth/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(params)
            .retrieve()
            .body(KakaoTokenResponse.class);
    }

    private KakaoUserResponse requestUserInfo(String accessToken) {
        return restClient.get()
            .uri("https://kapi.kakao.com/v2/user/me")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(KakaoUserResponse.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoUserResponse(@JsonProperty("kakao_account") KakaoAccount kakaoAccount) {

        public String email() {
            return kakaoAccount.email();
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record KakaoAccount(String email) {
        }
    }
}
