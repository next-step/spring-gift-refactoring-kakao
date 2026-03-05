package gift.auth;

import gift.auth.jwt.AuthenticationResolver;
import gift.auth.jwt.JwtProvider;
import gift.member.entity.Member;
import gift.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationResolverTest {
    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AuthenticationResolver authenticationResolver;

    @Test
    @DisplayName("유효한 Authorization 헤더면 회원을 반환한다")
    void extractMember_success_returnsMember() {
        Member member = new Member("test@example.com", "password");
        when(jwtProvider.getEmail("token")).thenReturn("test@example.com");
        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(member));

        Member result = authenticationResolver.extractMember("Bearer token");

        assertEquals(member, result);
    }

    @Test
    @DisplayName("토큰 파싱 중 예외가 발생하면 null을 반환한다")
    void extractMember_invalidToken_returnsNull() {
        when(jwtProvider.getEmail("invalid")).thenThrow(new RuntimeException("invalid token"));

        Member result = authenticationResolver.extractMember("Bearer invalid");

        assertNull(result);
    }
}
