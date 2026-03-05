package gift.member.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import gift.member.Member;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    MemberService memberService;

    @Mock
    MemberRepository memberRepo;

    @Test
    @DisplayName("이메일이 미등록이면 회원을 저장하고 ID를 반환한다")
    void testRegister() {
        // given
        String email = "test@example.com";
        String password = "password123";
        MemberRequest request = new MemberRequest(email, password);

        Member savedMember = createMember(1L, email, password);

        given(memberRepo.existsByEmail(email))
                .willReturn(false);
        given(memberRepo.save(any(Member.class)))
                .willReturn(savedMember);

        // when
        Long resultId = memberService.register(request);

        // then
        then(memberRepo).should().existsByEmail(email);
        then(memberRepo).should().save(any(Member.class));
        assertThat(resultId).isEqualTo(1L);
    }

    @SuppressWarnings("SameParameterValue")
    private static Member createMember(Long id, String email, String password) {
        Member member = Member.builder()
                .email(email)
                .password(password)
                .build();

        setId(member, id);
        return member;
    }

    private static void setId(Member member, Long id) {
        try {
            Field idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, id);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    @DisplayName("이미 등록된 이메일이면 RegisterFailedException을 던진다")
    void testRegisterDuplicateEmail() {
        // given
        String email = "existing@example.com";
        MemberRequest request = new MemberRequest(email, "password123");

        given(memberRepo.existsByEmail(email))
                .willReturn(true);

        // when + then
        assertThatThrownBy(() -> memberService.register(request))
                .isInstanceOf(RegisterFailedException.class);

        then(memberRepo).should(never()).save(any(Member.class));
    }

    // -- fixtures --

    @Test
    @DisplayName("이메일과 비밀번호가 일치하면 회원 ID를 반환한다")
    void testLogin() {
        // given
        String email = "test@example.com";
        String password = "password123";
        MemberRequest request = new MemberRequest(email, password);

        Member member = createMember(1L, email, password);

        given(memberRepo.findByEmailAndPassword(email, password))
                .willReturn(Optional.of(member));

        // when
        Long resultId = memberService.login(request);

        // then
        then(memberRepo).should().findByEmailAndPassword(email, password);
        assertThat(resultId).isEqualTo(1L);
    }

    @Test
    @DisplayName("이메일 또는 비밀번호가 틀리면 LoginFailedException을 던진다")
    void testLoginInvalidCredentials() {
        // given
        String email = "wrong@example.com";
        String password = "wrong-password";
        MemberRequest request = new MemberRequest(email, password);

        given(memberRepo.findByEmailAndPassword(email, password))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(LoginFailedException.class);
    }
}
