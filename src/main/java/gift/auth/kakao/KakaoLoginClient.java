package gift.auth.kakao;

import gift.auth.OAuthLoginClient;

import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoLoginClient implements OAuthLoginClient {
    private final KakaoLoginProperties properties;
    private final RestClient restClient;

    public KakaoLoginClient(KakaoLoginProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.build();
    }

    @Override
    public String requestAccessToken(String code) {
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", properties.clientId());
        params.add("redirect_uri", properties.redirectUri());
        params.add("code", code);
        params.add("client_secret", properties.clientSecret());

        KakaoTokenResponse response = restClient.post()
            .uri("https://kauth.kakao.com/oauth/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(params)
            .retrieve()
            .body(KakaoTokenResponse.class);
        return response.accessToken();
    }

    @Override
    public String requestUserEmail(String accessToken) {
        KakaoUserResponse response = restClient.get()
            .uri("https://kapi.kakao.com/v2/user/me")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(KakaoUserResponse.class);
        return response.email();
    }

}
