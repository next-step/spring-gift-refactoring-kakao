package gift.member;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberQueryServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberQueryService memberQueryService;

    @Test
    @DisplayName("전체 회원 목록을 반환한다")
    void findAll() {
        var members = List.of(new Member("a@test.com", "pw1"), new Member("b@test.com", "pw2"));
        given(memberRepository.findAll()).willReturn(members);

        List<Member> result = memberQueryService.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("ID로 회원을 조회한다")
    void findById() {
        var member = new Member("a@test.com", "pw");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        Member result = memberQueryService.findById(1L);

        assertThat(result.getEmail()).isEqualTo("a@test.com");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
    void findById_NotFound() {
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberQueryService.findById(999L))
            .isInstanceOf(MemberException.class);
    }

    @Test
    @DisplayName("이메일 존재 여부를 반환한다")
    void existsByEmail() {
        given(memberRepository.existsByEmail("a@test.com")).willReturn(true);

        assertThat(memberQueryService.existsByEmail("a@test.com")).isTrue();
    }

    @Test
    @DisplayName("올바른 이메일과 비밀번호로 로그인하면 회원을 반환한다")
    void login() {
        var member = new Member("a@test.com", "password123");
        given(memberRepository.findByEmail("a@test.com")).willReturn(Optional.of(member));

        Member result = memberQueryService.login("a@test.com", "password123");

        assertThat(result.getEmail()).isEqualTo("a@test.com");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 예외가 발생한다")
    void login_EmailNotFound() {
        given(memberRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberQueryService.login("unknown@test.com", "pw"))
            .isInstanceOf(MemberException.class);
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인하면 예외가 발생한다")
    void login_WrongPassword() {
        var member = new Member("a@test.com", "password123");
        given(memberRepository.findByEmail("a@test.com")).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberQueryService.login("a@test.com", "wrong"))
            .isInstanceOf(MemberException.class);
    }
}
