package gift.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import gift.auth.internal.KakaoLoginClient.KakaoTokenResponse;
import gift.auth.internal.KakaoLoginClient.KakaoUserResponse;
import gift.auth.internal.KakaoLoginClient.KakaoUserResponse.KakaoAccount;
import gift.global.NotFoundException;
import gift.member.MemberCommandPort;
import gift.member.MemberInfo;
import gift.member.MemberQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KakaoAuthServiceTest {

    @InjectMocks
    KakaoAuthService kakaoAuthService;

    @Mock
    KakaoLoginClient kakaoLoginClient;

    @Mock
    MemberQueryPort memberQueryPort;

    @Mock
    MemberCommandPort memberCommandPort;

    @Mock
    JwtProvider jwtProvider;

    @Test
    @DisplayName("기존 회원으로 카카오 로그인 — getIdByEmail 성공 → updateKakaoAccessToken 호출 + JWT 반환")
    void testLoginWithKakaoExistingMember() {
        // given
        String code = "auth-code";
        String email = "user@kakao.com";
        String kakaoAccessToken = "kakao-token";
        Long memberId = 1L;
        String jwt = "jwt-token";

        given(kakaoLoginClient.requestAccessToken(code))
                .willReturn(new KakaoTokenResponse(kakaoAccessToken));
        given(kakaoLoginClient.requestUserInfo(kakaoAccessToken))
                .willReturn(new KakaoUserResponse(new KakaoAccount(email)));
        given(memberQueryPort.getIdByEmail(email))
                .willReturn(memberId);
        given(jwtProvider.createToken(email))
                .willReturn(jwt);

        // when
        TokenResponse response = kakaoAuthService.loginWithKakao(code);

        // then
        then(memberCommandPort).should()
                .updateKakaoAccessToken(memberId, kakaoAccessToken);
        assertThat(response.token()).isEqualTo(jwt);
    }

    @Test
    @DisplayName("신규 회원으로 카카오 로그인 — getIdByEmail NotFoundException → create(MemberInfo) 호출 + JWT 반환")
    void testLoginWithKakaoNewMember() {
        // given
        String code = "auth-code";
        String email = "new@kakao.com";
        String kakaoAccessToken = "kakao-token";
        String jwt = "jwt-token";

        given(kakaoLoginClient.requestAccessToken(code))
                .willReturn(new KakaoTokenResponse(kakaoAccessToken));
        given(kakaoLoginClient.requestUserInfo(kakaoAccessToken))
                .willReturn(new KakaoUserResponse(new KakaoAccount(email)));
        given(memberQueryPort.getIdByEmail(email))
                .willThrow(NotFoundException.memberNotFound());
        given(jwtProvider.createToken(email))
                .willReturn(jwt);

        // when
        TokenResponse response = kakaoAuthService.loginWithKakao(code);

        // then
        then(memberCommandPort).should()
                .create(new MemberInfo(email, null, kakaoAccessToken));
        assertThat(response.token()).isEqualTo(jwt);
    }
}
