package gift.member;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MemberCommandServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberCommandService memberCommandService;

    @Test
    @DisplayName("회원을 등록한다")
    void register() {
        given(memberRepository.existsByEmail("new@test.com")).willReturn(false);
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

        Member result = memberCommandService.register("new@test.com", "password123");

        assertThat(result.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    @DisplayName("이미 등록된 이메일로 가입하면 예외가 발생한다")
    void register_DuplicateEmail() {
        given(memberRepository.existsByEmail("exist@test.com")).willReturn(true);

        assertThatThrownBy(() -> memberCommandService.register("exist@test.com", "pw"))
            .isInstanceOf(MemberException.class);
    }

    @Test
    @DisplayName("회원 정보를 수정한다")
    void update() {
        var member = new Member("old@test.com", "oldpw");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.save(any(Member.class))).willReturn(member);

        memberCommandService.update(1L, "new@test.com", "newpw");

        assertThat(member.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    @DisplayName("존재하지 않는 회원을 수정하면 예외가 발생한다")
    void update_NotFound() {
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberCommandService.update(999L, "e", "p"))
            .isInstanceOf(MemberException.class);
    }

    @Test
    @DisplayName("포인트를 충전한다")
    void chargePoint() {
        var member = new Member("a@test.com", "pw");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.save(any(Member.class))).willReturn(member);

        memberCommandService.chargePoint(1L, 5000);

        assertThat(member.getPoint()).isEqualTo(5000);
    }

    @Test
    @DisplayName("존재하지 않는 회원에게 포인트를 충전하면 예외가 발생한다")
    void chargePoint_NotFound() {
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberCommandService.chargePoint(999L, 1000))
            .isInstanceOf(MemberException.class);
    }

    @Test
    @DisplayName("회원을 삭제한다")
    void deleteById() {
        memberCommandService.deleteById(1L);

        then(memberRepository).should().deleteById(1L);
    }
}
