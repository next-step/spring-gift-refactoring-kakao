package gift.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-at-least-32-chars!!";
    private static final long EXPIRATION = 3_600_000L;

    JwtProvider jwtProvider = new JwtProvider(SECRET, EXPIRATION);

    @Test
    @DisplayName("토큰을 생성하고 이메일을 추출한다")
    void testCreateTokenAndGetEmail() {
        // given
        String email = "user@example.com";

        // when
        String token = jwtProvider.createToken(email);
        String extractedEmail = jwtProvider.getEmail(token);

        // then
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("만료된 토큰은 ExpiredJwtException을 던진다")
    void testExpiredToken() {
        // given
        JwtProvider expiredProvider = new JwtProvider(SECRET, -1L);
        String token = expiredProvider.createToken("user@example.com");

        // when + then
        assertThatThrownBy(() -> jwtProvider.getEmail(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰은 JwtException을 던진다")
    void testMalformedToken() {
        // when + then
        assertThatThrownBy(() -> jwtProvider.getEmail("not.a.valid.jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 JwtException을 던진다")
    void testWrongSecretToken() {
        // given
        JwtProvider otherProvider = new JwtProvider(
                "another-secret-key-at-least-32-chars!!", EXPIRATION
        );
        String token = otherProvider.createToken("user@example.com");

        // when + then
        assertThatThrownBy(() -> jwtProvider.getEmail(token))
                .isInstanceOf(JwtException.class);
    }
}
