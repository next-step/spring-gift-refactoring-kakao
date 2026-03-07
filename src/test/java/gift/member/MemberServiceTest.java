package gift.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("processSocialLogin 시 socialAccessToken이 DB에 저장된다")
    void processSocialLoginSavesAccessToken() {
        String email = "kakao@test.com";
        String accessToken = "kakao-access-token-123";

        Member member = memberService.processSocialLogin(email, accessToken);

        Member found = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(found.getSocialAccessToken()).isEqualTo(accessToken);
    }

    @Test
    @DisplayName("processSocialLogin 시 기존 회원이면 accessToken만 갱신된다")
    void processSocialLoginUpdatesExistingMember() {
        String email = "kakao@test.com";
        memberService.registerSocialMember(email);

        String newToken = "new-access-token-456";
        Member member = memberService.processSocialLogin(email, newToken);

        Member found = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(found.getSocialAccessToken()).isEqualTo(newToken);
    }
}
