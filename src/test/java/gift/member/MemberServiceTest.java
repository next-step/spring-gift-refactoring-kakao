package gift.member;

import gift.auth.jwt.JwtProvider;
import gift.auth.dto.TokenResponse;
import gift.member.dto.MemberRequest;
import gift.member.entity.Member;
import gift.member.exception.MemberErrorCode;
import gift.member.exception.MemberException;
import gift.member.repository.MemberRepository;
import gift.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원가입 시 이메일이 중복이면 DUPLICATE_EMAIL 예외를 던진다")
    void register_duplicateEmail_throwsException() {
        MemberRequest request = new MemberRequest("test@example.com", "password");
        when(memberRepository.existsByEmail(request.email())).thenReturn(true);

        MemberException exception = assertThrows(MemberException.class, () -> memberService.register(request));

        assertEquals(MemberErrorCode.DUPLICATE_EMAIL, exception.getErrorCode());
    }

    @Test
    @DisplayName("회원가입 성공 시 회원 저장 후 JWT를 반환한다")
    void register_success_returnsToken() {
        MemberRequest request = new MemberRequest("test@example.com", "password");
        Member saved = new Member(request.email(), request.password());
        when(memberRepository.existsByEmail(request.email())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(saved);
        when(jwtProvider.createToken(saved.getEmail())).thenReturn("jwt-token");

        TokenResponse response = memberService.register(request);

        assertEquals("jwt-token", response.token());
        verify(memberRepository).save(any(Member.class));
        verify(jwtProvider).createToken(saved.getEmail());
    }

    @Test
    @DisplayName("로그인 시 회원이 없으면 INVALID_CREDENTIALS 예외를 던진다")
    void login_memberNotFound_throwsException() {
        MemberRequest request = new MemberRequest("test@example.com", "password");
        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        MemberException exception = assertThrows(MemberException.class, () -> memberService.login(request));

        assertEquals(MemberErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    @DisplayName("로그인 시 비밀번호가 다르면 INVALID_CREDENTIALS 예외를 던진다")
    void login_passwordMismatch_throwsException() {
        MemberRequest request = new MemberRequest("test@example.com", "password");
        Member member = new Member(request.email(), "different-password");
        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.of(member));

        MemberException exception = assertThrows(MemberException.class, () -> memberService.login(request));

        assertEquals(MemberErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    @DisplayName("로그인 성공 시 JWT를 반환한다")
    void login_success_returnsToken() {
        MemberRequest request = new MemberRequest("test@example.com", "password");
        Member member = new Member(request.email(), request.password());
        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.of(member));
        when(jwtProvider.createToken(member.getEmail())).thenReturn("jwt-token");

        TokenResponse response = memberService.login(request);

        assertEquals("jwt-token", response.token());
        verify(jwtProvider).createToken(member.getEmail());
    }
}
