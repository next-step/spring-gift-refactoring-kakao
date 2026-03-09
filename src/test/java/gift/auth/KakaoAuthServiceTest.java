package gift.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import gift.member.Member;
import gift.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class KakaoAuthServiceTest {

    @Autowired
    private KakaoAuthService kakaoAuthService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private KakaoLoginClient kakaoLoginClient;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();

        given(kakaoLoginClient.requestAccessToken(anyString()))
            .willReturn(new KakaoLoginClient.KakaoTokenResponse("kakao-access-token-123"));
        given(kakaoLoginClient.requestUserInfo("kakao-access-token-123"))
            .willReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("kakao@example.com")));
    }

    @Test
    @DisplayName("신규 회원 카카오 로그인 시 회원 생성 + 토큰 저장이 하나의 트랜잭션으로 처리된다")
    void processCallback_newMember_createsWithToken() {
        // when
        String jwt = kakaoAuthService.processCallback("auth-code");

        // then: JWT가 발급된다
        assertThat(jwt).isNotBlank();
        String email = jwtProvider.getEmail(jwt);
        assertThat(email).isEqualTo("kakao@example.com");

        // then: 회원이 생성되고 카카오 토큰이 저장된다 (DB 재조회로 검증)
        Member saved = memberRepository.findByEmail("kakao@example.com").orElseThrow();
        assertThat(saved.getKakaoAccessToken()).isEqualTo("kakao-access-token-123");
    }

    @Test
    @DisplayName("기존 회원 카카오 로그인 시 토큰 갱신이 하나의 트랜잭션으로 처리된다")
    void processCallback_existingMember_updatesToken() {
        // given: 기존 회원 (비밀번호 로그인으로 가입, 카카오 토큰 없음)
        Member existing = new Member("kakao@example.com", "password123");
        existing.chargePoint(5000);
        memberRepository.save(existing);

        // when
        String jwt = kakaoAuthService.processCallback("auth-code");

        // then: 기존 회원의 카카오 토큰이 갱신된다
        Member updated = memberRepository.findByEmail("kakao@example.com").orElseThrow();
        assertThat(updated.getKakaoAccessToken()).isEqualTo("kakao-access-token-123");

        // then: 기존 데이터(포인트)가 유지된다
        assertThat(updated.getPoint()).isEqualTo(5000);

        // then: 회원이 중복 생성되지 않는다
        assertThat(memberRepository.count()).isEqualTo(1);
    }
}
