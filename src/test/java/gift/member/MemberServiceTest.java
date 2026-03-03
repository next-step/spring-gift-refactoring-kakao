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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private MemberService memberService;

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("신규 이메일이면 회원을 생성하고 토큰을 반환한다")
        void success() {
            given(memberRepository.existsByEmail("new@email.com")).willReturn(false);
            given(memberRepository.save(any(Member.class)))
                .willAnswer(inv -> inv.getArgument(0));
            given(jwtProvider.createToken("new@email.com")).willReturn("jwt-token");

            TokenResponse response = memberService.register(new MemberRequest("new@email.com", "pw"));

            assertThat(response.token()).isEqualTo("jwt-token");
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("이미 존재하는 이메일이면 예외가 발생한다")
        void duplicateEmail() {
            given(memberRepository.existsByEmail("dup@email.com")).willReturn(true);

            assertThatThrownBy(() -> memberService.register(new MemberRequest("dup@email.com", "pw")))
                .isInstanceOf(IllegalArgumentException.class);
            verify(memberRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        private Member existingMember;

        @BeforeEach
        void setUp() {
            existingMember = new Member("user@email.com", "correct-pw");
        }

        @Test
        @DisplayName("올바른 자격 증명이면 토큰을 반환한다")
        void success() {
            given(memberRepository.findByEmail("user@email.com")).willReturn(Optional.of(existingMember));
            given(jwtProvider.createToken("user@email.com")).willReturn("jwt-token");

            TokenResponse response = memberService.login(new MemberRequest("user@email.com", "correct-pw"));

            assertThat(response.token()).isEqualTo("jwt-token");
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 예외가 발생한다")
        void emailNotFound() {
            given(memberRepository.findByEmail("none@email.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.login(new MemberRequest("none@email.com", "pw")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid email or password.");
            verify(jwtProvider, never()).createToken(anyString());
        }

        @Test
        @DisplayName("비밀번호가 틀리면 예외가 발생한다")
        void wrongPassword() {
            given(memberRepository.findByEmail("user@email.com")).willReturn(Optional.of(existingMember));

            assertThatThrownBy(() -> memberService.login(new MemberRequest("user@email.com", "wrong-pw")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid email or password.");
            verify(jwtProvider, never()).createToken(anyString());
        }

        @Test
        @DisplayName("이메일 없음과 비밀번호 틀림의 에러 메시지가 동일하다")
        void sameErrorMessage() {
            given(memberRepository.findByEmail("none@email.com")).willReturn(Optional.empty());
            given(memberRepository.findByEmail("user@email.com")).willReturn(Optional.of(existingMember));

            try {
                memberService.login(new MemberRequest("none@email.com", "pw"));
            } catch (IllegalArgumentException emailError) {
                try {
                    memberService.login(new MemberRequest("user@email.com", "wrong"));
                } catch (IllegalArgumentException pwError) {
                    assertThat(emailError.getMessage()).isEqualTo(pwError.getMessage());
                }
            }
        }
    }
}
