package gift.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import gift.global.NotFoundException;
import gift.global.UnauthorizedException;
import gift.member.MemberQueryPort;
import io.jsonwebtoken.JwtException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationPortAdaptorTest {

    @InjectMocks
    AuthenticationPortAdaptor authenticationPort;

    @Mock
    JwtProvider jwtProvider;

    @Mock
    MemberQueryPort memberQueryPort;

    @Test
    @DisplayName("유효한 토큰 — JWT 파싱 + getIdByEmail 성공 → Optional.of(memberId)")
    void testRequestMemberIdFromValidToken() {
        // given
        String authorization = "Bearer valid-jwt";
        String email = "user@kakao.com";
        Long memberId = 1L;

        given(jwtProvider.getEmail("valid-jwt"))
                .willReturn(email);
        given(memberQueryPort.getIdByEmail(email))
                .willReturn(memberId);

        // when
        Optional<Long> result = authenticationPort.requestMemberIdFrom(authorization);

        // then
        assertThat(result).contains(memberId);
    }

    @Test
    @DisplayName("유효한 토큰이지만 회원 없음 — getIdByEmail NotFoundException → Optional.empty()")
    void testRequestMemberIdFromUnknownEmail() {
        // given
        String authorization = "Bearer valid-jwt";
        String email = "unknown@kakao.com";

        given(jwtProvider.getEmail("valid-jwt"))
                .willReturn(email);
        given(memberQueryPort.getIdByEmail(email))
                .willThrow(NotFoundException.memberNotFound());

        // when
        Optional<Long> result = authenticationPort.requestMemberIdFrom(authorization);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("유효하지 않은 토큰 — JWT 파싱 실패 → Optional.empty()")
    void testRequestMemberIdFromInvalidToken() {
        // given
        String authorization = "Bearer invalid-jwt";

        given(jwtProvider.getEmail("invalid-jwt"))
                .willThrow(new JwtException("invalid"));

        // when
        Optional<Long> result = authenticationPort.requestMemberIdFrom(authorization);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("필수 인증 성공 — 유효한 토큰 → memberId 반환")
    void testGetMemberIdFromSuccess() {
        // given
        String authorization = "Bearer valid-jwt";
        String email = "user@kakao.com";
        Long memberId = 1L;

        given(jwtProvider.getEmail("valid-jwt"))
                .willReturn(email);
        given(memberQueryPort.getIdByEmail(email))
                .willReturn(memberId);

        // when
        Long result = authenticationPort.getMemberIdFrom(authorization);

        // then
        assertThat(result).isEqualTo(memberId);
    }

    @Test
    @DisplayName("필수 인증 실패 — 유효하지 않은 토큰 → UnauthorizedException")
    void testGetMemberIdFromUnauthorized() {
        // given
        String authorization = "Bearer invalid-jwt";

        given(jwtProvider.getEmail("invalid-jwt"))
                .willThrow(new JwtException("invalid"));

        // when + then
        assertThatThrownBy(() -> authenticationPort.getMemberIdFrom(authorization))
                .isInstanceOf(UnauthorizedException.class);
    }
}
