package gift.member;

import gift.auth.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JwtProvider jwtProvider;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository, jwtProvider);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("이미 등록된 이메일이면 예외를 발생시킨다")
        void duplicateEmail_throwsException() {
            // Given
            String duplicateEmail = "existing@example.com";
            given(memberRepository.existsByEmail(duplicateEmail)).willReturn(true);

            MemberRequest request = new MemberRequest(duplicateEmail, "password");

            // When & Then
            assertThatThrownBy(() -> memberService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 등록된 이메일");
        }

        @Test
        @DisplayName("새 이메일이면 회원을 저장하고 토큰을 반환한다")
        void newEmail_savesAndReturnsToken() {
            // Given
            String newEmail = "new@example.com";
            given(memberRepository.existsByEmail(newEmail)).willReturn(false);
            given(memberRepository.save(any(Member.class)))
                .willReturn(new Member(newEmail, "password"));
            given(jwtProvider.createToken(newEmail)).willReturn("jwt-token");

            MemberRequest request = new MemberRequest(newEmail, "password");

            // When
            var response = memberService.register(request);

            // Then
            assertThat(response.token()).isEqualTo("jwt-token");
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("존재하지 않는 이메일이면 예외를 발생시킨다")
        void emailNotFound_throwsException() {
            // Given
            String unknownEmail = "unknown@example.com";
            given(memberRepository.findByEmail(unknownEmail)).willReturn(Optional.empty());

            MemberRequest request = new MemberRequest(unknownEmail, "password");

            // When & Then
            assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 예외를 발생시킨다")
        void wrongPassword_throwsException() {
            // Given
            String email = "test@example.com";
            Member member = new Member(email, "correctPassword");
            given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

            MemberRequest request = new MemberRequest(email, "wrongPassword");

            // When & Then
            assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        @Test
        @DisplayName("올바른 자격 증명이면 토큰을 반환한다")
        void validCredentials_returnsToken() {
            // Given
            String email = "test@example.com";
            String password = "password123";
            Member member = new Member(email, password);
            given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
            given(jwtProvider.createToken(email)).willReturn("jwt-token");

            MemberRequest request = new MemberRequest(email, password);

            // When
            var response = memberService.login(request);

            // Then
            assertThat(response.token()).isEqualTo("jwt-token");
        }
    }
}
