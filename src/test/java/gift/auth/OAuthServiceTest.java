package gift.auth;

import gift.auth.dto.TokenResponse;
import gift.auth.jwt.JwtProvider;
import gift.auth.oauth.OAuthClient;
import gift.auth.oauth.OAuthClientRegistry;
import gift.auth.oauth.OAuthUserInfo;
import gift.auth.service.OAuthService;
import gift.external.ExternalProvider;
import gift.member.entity.Member;
import gift.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {
    @Mock
    private OAuthClientRegistry oAuthClientRegistry;

    @Mock
    private OAuthClient oAuthClient;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private OAuthService oAuthService;

    @Test
    @DisplayName("기존 회원 OAuth 로그인 콜백 성공 시 JWT를 반환한다")
    void handleCallback_existingMember_returnsToken() {
        String email = "test@example.com";
        String kakaoAccessToken = "kakao-token";
        Member member = new Member(email, "password");

        OAuthUserInfo userInfo = new OAuthUserInfo(email, kakaoAccessToken);

        when(oAuthClientRegistry.get(ExternalProvider.KAKAO)).thenReturn(oAuthClient);
        when(oAuthClient.getUserInfo("code")).thenReturn(userInfo);
        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
        when(jwtProvider.createToken(email)).thenReturn("jwt-token");

        TokenResponse response = oAuthService.handleCallback(ExternalProvider.KAKAO, "code");

        assertEquals("jwt-token", response.token());
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("신규 회원 OAuth 로그인 콜백 성공 시 회원을 생성하고 JWT를 반환한다")
    void handleCallback_newMember_createsMemberAndReturnsToken() {
        String email = "new@example.com";
        String kakaoAccessToken = "kakao-token";

        OAuthUserInfo userInfo = new OAuthUserInfo(email, kakaoAccessToken);

        when(oAuthClientRegistry.get(ExternalProvider.KAKAO)).thenReturn(oAuthClient);
        when(oAuthClient.getUserInfo("code")).thenReturn(userInfo);
        when(memberRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(jwtProvider.createToken(email)).thenReturn("jwt-token");

        TokenResponse response = oAuthService.handleCallback(ExternalProvider.KAKAO, "code");

        assertEquals("jwt-token", response.token());
        verify(memberRepository).save(any(Member.class));
    }
}
