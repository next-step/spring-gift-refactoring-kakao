package gift.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("평문 비밀번호로 생성한 Password는 같은 평문과 매칭된다")
        void matchesRawPassword() {
            Password password = Password.of("secret123");

            assertThat(password.matches("secret123")).isTrue();
        }

        @Test
        @DisplayName("다른 평문과는 매칭되지 않는다")
        void doesNotMatchDifferentPassword() {
            Password password = Password.of("secret123");

            assertThat(password.matches("wrong")).isFalse();
        }

        @Test
        @DisplayName("같은 평문으로 생성해도 매번 다른 해시값이 생성된다")
        void differentHashEachTime() {
            Password first = Password.of("secret123");
            Password second = Password.of("secret123");

            assertThat(first).isNotSameAs(second);
            assertThat(first.matches("secret123")).isTrue();
            assertThat(second.matches("secret123")).isTrue();
        }
    }

    @Nested
    @DisplayName("fromEncoded")
    class FromEncoded {

        // "admin1234"의 BCrypt 해시 리터럴
        private static final String ENCODED = "$2a$10$/NnD2QUm5RwSFA8dKmvK9.UZlEsVm6vnQxkzAy/9UCDqSARvzTbbi";

        @Test
        @DisplayName("인코딩된 해시값으로 생성한 Password는 원본 평문과 매칭된다")
        void matchesOriginalRawPassword() {
            Password password = Password.fromEncoded(ENCODED);

            assertThat(password.matches("admin1234")).isTrue();
        }

        @Test
        @DisplayName("인코딩된 해시값으로 생성한 Password는 다른 평문과 매칭되지 않는다")
        void doesNotMatchDifferentPassword() {
            Password password = Password.fromEncoded(ENCODED);

            assertThat(password.matches("otherPassword")).isFalse();
        }
    }

    @Nested
    @DisplayName("matches")
    class Matches {

        @Test
        @DisplayName("빈 문자열 비밀번호도 정상적으로 해싱 및 매칭된다")
        void emptyPassword() {
            Password password = Password.of("");

            assertThat(password.matches("")).isTrue();
            assertThat(password.matches("notempty")).isFalse();
        }
    }

}
