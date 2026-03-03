package gift.member;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private MemberService memberService;

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("정상적으로 회원가입하고 토큰을 반환한다")
        void registersSuccessfully() {
            var request = new MemberRequest("test@test.com", "password");

            given(memberRepository.existsByEmail("test@test.com")).willReturn(false);
            given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));
            given(jwtProvider.createToken("test@test.com")).willReturn("jwt-token");

            TokenResponse result = memberService.register(request);

            assertThat(result.token()).isEqualTo("jwt-token");
        }

        @Test
        @DisplayName("이메일이 중복이면 예외를 던진다")
        void throwsWhenEmailDuplicated() {
            var request = new MemberRequest("dup@test.com", "password");

            given(memberRepository.existsByEmail("dup@test.com")).willReturn(true);

            assertThatThrownBy(() -> memberService.register(request))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("정상적으로 로그인하고 토큰을 반환한다")
        void loginsSuccessfully() {
            var request = new MemberRequest("test@test.com", "password");
            var member = new Member("test@test.com", "password");

            given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(member));
            given(jwtProvider.createToken("test@test.com")).willReturn("jwt-token");

            TokenResponse result = memberService.login(request);

            assertThat(result.token()).isEqualTo("jwt-token");
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 예외를 던진다")
        void throwsWhenEmailNotFound() {
            var request = new MemberRequest("notfound@test.com", "password");

            given(memberRepository.findByEmail("notfound@test.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 예외를 던진다")
        void throwsWhenPasswordWrong() {
            var request = new MemberRequest("test@test.com", "wrong");
            var member = new Member("test@test.com", "password");

            given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(member));

            assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Admin용 메서드")
    class AdminMethods {

        @Test
        @DisplayName("전체 회원 목록을 조회한다")
        void getAllMembers() {
            var members = List.of(new Member("a@test.com", "pw"), new Member("b@test.com", "pw"));
            given(memberRepository.findAll()).willReturn(members);

            List<Member> result = memberService.getAllMembers();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("ID로 회원을 조회한다")
        void getMember() throws Exception {
            var member = new Member("test@test.com", "password");
            setId(member, 1L);
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            Member result = memberService.getMember(1L);

            assertThat(result.getEmail()).isEqualTo("test@test.com");
        }

        @Test
        @DisplayName("존재하지 않는 ID면 예외를 던진다")
        void throwsWhenMemberNotFound() {
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.getMember(999L))
                .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("회원 정보를 수정한다")
        void updatesMember() throws Exception {
            var member = new Member("old@test.com", "oldpw");
            setId(member, 1L);

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

            Member result = memberService.updateMember(1L, "new@test.com", "newpw");

            assertThat(result.getEmail()).isEqualTo("new@test.com");
        }

        @Test
        @DisplayName("포인트를 충전한다")
        void chargesPoint() throws Exception {
            var member = new Member("test@test.com", "password");
            setId(member, 1L);

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            memberService.chargePoint(1L, 5000);

            assertThat(member.getPoint()).isEqualTo(5000);
            then(memberRepository).should().save(member);
        }

        @Test
        @DisplayName("회원을 삭제한다")
        void deletesMember() {
            memberService.deleteMember(1L);

            then(memberRepository).should().deleteById(1L);
        }
    }
}
