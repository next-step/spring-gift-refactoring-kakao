package gift.infrastructure.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gift.auth.AuthConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoLoginClient {
  private final KakaoLoginProperties properties;
  private final RestClient restClient;

  public KakaoLoginClient(KakaoLoginProperties properties, RestClient.Builder builder) {
    this.properties = properties;
    this.restClient = builder.build();
  }

  public KakaoTokenResponse requestAccessToken(String code) {
    LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("client_id", properties.clientId());
    params.add("redirect_uri", properties.redirectUri());
    params.add("code", code);
    params.add("client_secret", properties.clientSecret());

    return restClient
        .post()
        .uri(properties.tokenUrl())
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .body(params)
        .retrieve()
        .body(KakaoTokenResponse.class);
  }

  public KakaoUserResponse requestUserInfo(String accessToken) {
    return restClient
        .get()
        .uri(properties.userInfoUrl())
        .header(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + accessToken)
        .retrieve()
        .body(KakaoUserResponse.class);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record KakaoUserResponse(@JsonProperty("kakao_account") KakaoAccount kakaoAccount) {

    public String email() {
      return kakaoAccount.email();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(String email) {}
  }
}
