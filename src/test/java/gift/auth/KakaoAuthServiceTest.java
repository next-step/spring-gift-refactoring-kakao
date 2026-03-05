package gift.auth;

import gift.TestFixtures;
import gift.member.Member;
import gift.member.MemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KakaoAuthServiceTest {

    @Mock
    private KakaoLoginClient kakaoLoginClient;

    @Mock
    private MemberService memberService;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private KakaoAuthService kakaoAuthService;

    @Test
    void loginOrRegister_existingMember_updatesTokenReturnsJwt() {
        var member = TestFixtures.member(1L, "user@kakao.com", null);
        given(kakaoLoginClient.requestAccessToken("code123"))
            .willReturn(new KakaoLoginClient.KakaoTokenResponse("kakao-token"));
        given(kakaoLoginClient.requestUserInfo("kakao-token"))
            .willReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("user@kakao.com")));
        given(memberService.findOrCreateByEmailAndUpdateKakaoToken("user@kakao.com", "kakao-token"))
            .willReturn(member);
        given(jwtProvider.createToken("user@kakao.com")).willReturn("jwt-token");

        var result = kakaoAuthService.loginOrRegister("code123");

        assertThat(result.token()).isEqualTo("jwt-token");
    }

    @Test
    void loginOrRegister_newMember_createsAndReturnsJwt() {
        var member = TestFixtures.member(null, "new@kakao.com", null);
        given(kakaoLoginClient.requestAccessToken("code123"))
            .willReturn(new KakaoLoginClient.KakaoTokenResponse("kakao-token"));
        given(kakaoLoginClient.requestUserInfo("kakao-token"))
            .willReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("new@kakao.com")));
        given(memberService.findOrCreateByEmailAndUpdateKakaoToken("new@kakao.com", "kakao-token"))
            .willReturn(member);
        given(jwtProvider.createToken("new@kakao.com")).willReturn("jwt-token");

        var result = kakaoAuthService.loginOrRegister("code123");

        assertThat(result.token()).isEqualTo("jwt-token");
        then(memberService).should().findOrCreateByEmailAndUpdateKakaoToken("new@kakao.com", "kakao-token");
    }

    @Test
    void loginOrRegister_delegatesToMemberService() {
        var member = TestFixtures.member(1L, "user@kakao.com", null);
        given(kakaoLoginClient.requestAccessToken("code123"))
            .willReturn(new KakaoLoginClient.KakaoTokenResponse("new-kakao-token"));
        given(kakaoLoginClient.requestUserInfo("new-kakao-token"))
            .willReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("user@kakao.com")));
        given(memberService.findOrCreateByEmailAndUpdateKakaoToken("user@kakao.com", "new-kakao-token"))
            .willReturn(member);
        given(jwtProvider.createToken("user@kakao.com")).willReturn("jwt");

        kakaoAuthService.loginOrRegister("code123");

        then(memberService).should().findOrCreateByEmailAndUpdateKakaoToken("user@kakao.com", "new-kakao-token");
    }

    @Test
    void loginOrRegister_generatesJwtWithEmail() {
        var member = TestFixtures.member(null, "test@kakao.com", null);
        given(kakaoLoginClient.requestAccessToken("code123"))
            .willReturn(new KakaoLoginClient.KakaoTokenResponse("kakao-token"));
        given(kakaoLoginClient.requestUserInfo("kakao-token"))
            .willReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("test@kakao.com")));
        given(memberService.findOrCreateByEmailAndUpdateKakaoToken("test@kakao.com", "kakao-token"))
            .willReturn(member);
        given(jwtProvider.createToken("test@kakao.com")).willReturn("jwt");

        kakaoAuthService.loginOrRegister("code123");

        verify(jwtProvider).createToken("test@kakao.com");
    }

    @Test
    void loginOrRegister_kakaoClientError_propagates() {
        given(kakaoLoginClient.requestAccessToken("bad-code"))
            .willThrow(new RuntimeException("카카오 API 오류"));

        assertThatThrownBy(() -> kakaoAuthService.loginOrRegister("bad-code"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("카카오 API 오류");
    }
}
