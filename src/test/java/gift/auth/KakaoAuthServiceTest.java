package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class KakaoAuthServiceTest {

    @Autowired
    KakaoAuthService kakaoAuthService;

    @MockitoBean
    KakaoLoginClient kakaoLoginClient;

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("신규 회원 카카오 로그인 시 회원이 생성되고 JWT가 반환된다")
    void loginWithKakao_newMember() {
        given(kakaoLoginClient.requestAccessToken(anyString()))
            .willReturn(new KakaoLoginClient.KakaoTokenResponse("access-token"));
        given(kakaoLoginClient.requestUserInfo("access-token"))
            .willReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("new@kakao.com")));

        TokenResponse response = kakaoAuthService.loginWithKakao("auth-code");

        assertThat(response.token()).isNotBlank();
        assertThat(memberRepository.findByEmail("new@kakao.com")).isPresent();
    }

    @Test
    @DisplayName("기존 회원 카카오 로그인 시 토큰이 갱신되고 JWT가 반환된다")
    void loginWithKakao_existingMember() {
        memberRepository.save(new Member("existing@kakao.com", "pw"));

        given(kakaoLoginClient.requestAccessToken(anyString()))
            .willReturn(new KakaoLoginClient.KakaoTokenResponse("new-access-token"));
        given(kakaoLoginClient.requestUserInfo("new-access-token"))
            .willReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("existing@kakao.com")));

        TokenResponse response = kakaoAuthService.loginWithKakao("auth-code");

        assertThat(response.token()).isNotBlank();
        Member member = memberRepository.findByEmail("existing@kakao.com").orElseThrow();
        assertThat(member.getKakaoAccessToken()).isEqualTo("new-access-token");
    }
}
