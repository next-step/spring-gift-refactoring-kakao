package gift.auth;

import gift.infrastructure.kakao.KakaoLoginClient;
import gift.infrastructure.kakao.KakaoLoginProperties;
import gift.member.Member;
import gift.member.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Transactional(readOnly = true)
public class KakaoAuthService {
  private static final String KAKAO_AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";
  private static final String OAUTH_SCOPE = "account_email,talk_message";

  private final KakaoLoginProperties properties;
  private final KakaoLoginClient kakaoLoginClient;
  private final MemberService memberService;

  public KakaoAuthService(
      KakaoLoginProperties properties,
      KakaoLoginClient kakaoLoginClient,
      MemberService memberService) {
    this.properties = properties;
    this.kakaoLoginClient = kakaoLoginClient;
    this.memberService = memberService;
  }

  public String buildAuthorizationUrl() {
    return UriComponentsBuilder.fromUriString(KAKAO_AUTHORIZE_URL)
        .queryParam("response_type", "code")
        .queryParam("client_id", properties.clientId())
        .queryParam("redirect_uri", properties.redirectUri())
        .queryParam("scope", OAUTH_SCOPE)
        .build()
        .toUriString();
  }

  @Transactional
  public Member processCallback(String code) {
    KakaoLoginClient.KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);
    KakaoLoginClient.KakaoUserResponse kakaoUser =
        kakaoLoginClient.requestUserInfo(kakaoToken.accessToken());
    String email = kakaoUser.email();

    return memberService.findOrCreateAndUpdateKakaoToken(email, kakaoToken.accessToken());
  }
}
