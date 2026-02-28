package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class KakaoAuthServiceTest {

    @Mock
    private KakaoLoginProperties properties;

    @Mock
    private KakaoLoginClient kakaoLoginClient;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private KakaoAuthService kakaoAuthService;

    @Nested
    @DisplayName("buildKakaoAuthUrl")
    class BuildKakaoAuthUrl {

        @Test
        @DisplayName("카카오 인가 URL에 client_id, redirect_uri, scope가 포함된다")
        void buildsUrlWithParams() {
            given(properties.clientId()).willReturn("test-client-id");
            given(properties.redirectUri()).willReturn("http://localhost/callback");

            String url = kakaoAuthService.buildKakaoAuthUrl();

            assertThat(url).startsWith("https://kauth.kakao.com/oauth/authorize");
            assertThat(url).contains("client_id=test-client-id");
            assertThat(url).contains("redirect_uri=");
            assertThat(url).contains("scope=");
            assertThat(url).contains("response_type=code");
        }
    }

    @Nested
    @DisplayName("handleCallback")
    class HandleCallback {

        @Test
        @DisplayName("기존 회원이면 카카오 토큰을 갱신하고 JWT를 반환한다")
        void updatesExistingMember() {
            var kakaoToken = new KakaoLoginClient.KakaoTokenResponse("kakao-access-token");
            var kakaoAccount = new KakaoLoginClient.KakaoUserResponse.KakaoAccount("existing@test.com");
            var kakaoUser = new KakaoLoginClient.KakaoUserResponse(kakaoAccount);
            var existingMember = new Member("existing@test.com", "password");

            given(kakaoLoginClient.requestAccessToken("auth-code")).willReturn(kakaoToken);
            given(kakaoLoginClient.requestUserInfo("kakao-access-token")).willReturn(kakaoUser);
            given(memberRepository.findByEmail("existing@test.com")).willReturn(Optional.of(existingMember));
            given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));
            given(jwtProvider.createToken("existing@test.com")).willReturn("jwt-token");

            TokenResponse result = kakaoAuthService.handleCallback("auth-code");

            assertThat(result.token()).isEqualTo("jwt-token");
            assertThat(existingMember.getKakaoAccessToken()).isEqualTo("kakao-access-token");
        }

        @Test
        @DisplayName("신규 회원이면 생성하고 JWT를 반환한다")
        void createsNewMember() {
            var kakaoToken = new KakaoLoginClient.KakaoTokenResponse("kakao-access-token");
            var kakaoAccount = new KakaoLoginClient.KakaoUserResponse.KakaoAccount("new@test.com");
            var kakaoUser = new KakaoLoginClient.KakaoUserResponse(kakaoAccount);

            given(kakaoLoginClient.requestAccessToken("auth-code")).willReturn(kakaoToken);
            given(kakaoLoginClient.requestUserInfo("kakao-access-token")).willReturn(kakaoUser);
            given(memberRepository.findByEmail("new@test.com")).willReturn(Optional.empty());
            given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));
            given(jwtProvider.createToken("new@test.com")).willReturn("jwt-token");

            TokenResponse result = kakaoAuthService.handleCallback("auth-code");

            assertThat(result.token()).isEqualTo("jwt-token");
            then(memberRepository).should().save(any(Member.class));
        }
    }
}
