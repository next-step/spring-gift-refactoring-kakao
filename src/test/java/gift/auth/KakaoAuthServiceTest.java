package gift.auth;

import gift.member.Member;
import gift.member.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KakaoAuthServiceTest {

    @Mock
    private KakaoLoginProperties properties;

    @Mock
    private KakaoLoginClient kakaoLoginClient;

    @Mock
    private MemberService memberService;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private KakaoAuthService kakaoAuthService;

    @Test
    void buildLoginUrl은_카카오_인증_URL을_생성한다() {
        given(properties.clientId()).willReturn("test-client-id");
        given(properties.redirectUri()).willReturn("http://localhost:8080/api/auth/kakao/callback");

        String url = kakaoAuthService.buildLoginUrl();

        assertThat(url).startsWith("https://kauth.kakao.com/oauth/authorize");
        assertThat(url).contains("client_id=test-client-id");
        assertThat(url).contains("redirect_uri=");
        assertThat(url).contains("scope=account_email,talk_message");
    }

    @Test
    void processCallback은_카카오_인증_후_JWT를_반환한다() {
        String code = "auth-code";
        String kakaoAccessToken = "kakao-access-token";
        String email = "test@kakao.com";
        String jwt = "jwt-token";

        Member member = new Member(email);
        KakaoLoginClient.KakaoTokenResponse tokenResponse =
            new KakaoLoginClient.KakaoTokenResponse(kakaoAccessToken);
        KakaoLoginClient.KakaoUserResponse userResponse =
            new KakaoLoginClient.KakaoUserResponse(new KakaoLoginClient.KakaoUserResponse.KakaoAccount(email));

        given(kakaoLoginClient.requestAccessToken(code)).willReturn(tokenResponse);
        given(kakaoLoginClient.requestUserInfo(kakaoAccessToken)).willReturn(userResponse);
        given(memberService.registerOrUpdateKakaoMember(email, kakaoAccessToken)).willReturn(member);
        given(jwtProvider.createToken(email)).willReturn(jwt);

        TokenResponse result = kakaoAuthService.processCallback(code);

        assertThat(result.token()).isEqualTo(jwt);
        verify(memberService).registerOrUpdateKakaoMember(email, kakaoAccessToken);
        verify(jwtProvider).createToken(email);
    }
}