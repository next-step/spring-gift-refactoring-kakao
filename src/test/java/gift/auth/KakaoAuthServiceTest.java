package gift.auth;

import gift.kakao.KakaoLoginClient;
import gift.member.Member;
import gift.member.MemberRepository;
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

    @Test
    @DisplayName("기존 회원 카카오 로그인 콜백 성공 시 JWT를 반환한다")
    void handleCallback_existingMember_returnsToken() {
        String email = "test@example.com";
        String kakaoAccessToken = "kakao-token";
        Member member = new Member(email, "password");

        KakaoLoginClient.KakaoTokenResponse tokenResponse = new KakaoLoginClient.KakaoTokenResponse(kakaoAccessToken);
        KakaoLoginClient.KakaoUserResponse.KakaoAccount account = new KakaoLoginClient.KakaoUserResponse.KakaoAccount(email);
        KakaoLoginClient.KakaoUserResponse userResponse = new KakaoLoginClient.KakaoUserResponse(account);

        when(kakaoLoginClient.requestAccessToken("code")).thenReturn(tokenResponse);
        when(kakaoLoginClient.requestUserInfo(kakaoAccessToken)).thenReturn(userResponse);
        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
        when(jwtProvider.createToken(email)).thenReturn("jwt-token");

        TokenResponse response = kakaoAuthService.handleCallback("code");

        assertEquals("jwt-token", response.token());
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("신규 회원 카카오 로그인 콜백 성공 시 회원을 생성하고 JWT를 반환한다")
    void handleCallback_newMember_createsMemberAndReturnsToken() {
        String email = "new@example.com";
        String kakaoAccessToken = "kakao-token";

        KakaoLoginClient.KakaoTokenResponse tokenResponse = new KakaoLoginClient.KakaoTokenResponse(kakaoAccessToken);
        KakaoLoginClient.KakaoUserResponse.KakaoAccount account = new KakaoLoginClient.KakaoUserResponse.KakaoAccount(email);
        KakaoLoginClient.KakaoUserResponse userResponse = new KakaoLoginClient.KakaoUserResponse(account);

        when(kakaoLoginClient.requestAccessToken("code")).thenReturn(tokenResponse);
        when(kakaoLoginClient.requestUserInfo(kakaoAccessToken)).thenReturn(userResponse);
        when(memberRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(jwtProvider.createToken(email)).thenReturn("jwt-token");

        TokenResponse response = kakaoAuthService.handleCallback("code");

        assertEquals("jwt-token", response.token());
        verify(memberRepository).save(any(Member.class));
    }
}
