package gift.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원 정보 수정 시 조회 + 수정이 하나의 단위로 처리된다")
    void update_success() {
        // given
        Member member = memberRepository.save(new Member("old@example.com", "oldpass"));

        // when
        Member updated = memberService.update(member.getId(), "new@example.com", "newpass");

        // then: DB 재조회로 변경 확인
        Member reloaded = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("new@example.com");
        assertThat(reloaded.getPassword()).isEqualTo("newpass");
    }

    @Test
    @DisplayName("존재하지 않는 회원 수정 시 예외 발생")
    void update_notFound() {
        assertThatThrownBy(() -> memberService.update(999L, "a@b.com", "pass"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("포인트 충전 시 조회 + 충전이 하나의 단위로 처리된다")
    void chargePoint_success() {
        // given
        Member member = memberRepository.save(new Member("user@example.com", "pass"));

        // when
        memberService.chargePoint(member.getId(), 5000);

        // then: DB 재조회로 포인트 확인
        Member reloaded = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(reloaded.getPoint()).isEqualTo(5000);
    }

    @Test
    @DisplayName("포인트 충전 금액이 0 이하이면 예외 발생하고 포인트는 변경되지 않는다")
    void chargePoint_invalidAmount_noChange() {
        // given
        Member member = new Member("user@example.com", "pass");
        member.chargePoint(1000);
        member = memberRepository.save(member);

        // when & then
        Long memberId = member.getId();
        assertThatThrownBy(() -> memberService.chargePoint(memberId, -100))
            .isInstanceOf(IllegalArgumentException.class);

        // then: 포인트가 원래대로 유지 (DB 재조회)
        Member reloaded = memberRepository.findById(memberId).orElseThrow();
        assertThat(reloaded.getPoint()).isEqualTo(1000);
    }
}
