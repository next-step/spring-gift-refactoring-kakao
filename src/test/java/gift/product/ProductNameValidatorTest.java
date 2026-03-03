package gift.product;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProductNameValidatorTest {

    @Nested
    @DisplayName("정상 케이스")
    class ValidNames {

        @Test
        @DisplayName("한글, 영문, 숫자 조합 15자 이내는 통과한다")
        void validMixedName() {
            List<String> errors = ProductNameValidator.validate("상품ABC 123");

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("허용 특수문자를 포함하면 통과한다")
        void validSpecialCharacters() {
            List<String> errors = ProductNameValidator.validate("상품(A)[B]+C-D&E");

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("정확히 15자는 통과한다")
        void validExactMaxLength() {
            String name = "a".repeat(15);

            List<String> errors = ProductNameValidator.validate(name);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("allowKakao가 true이면 카카오가 포함되어도 통과한다")
        void validKakaoWithFlag() {
            List<String> errors = ProductNameValidator.validate("카카오 상품", true);

            assertTrue(errors.isEmpty());
        }
    }

    @Nested
    @DisplayName("에러 케이스")
    class InvalidNames {

        @Test
        @DisplayName("null이면 에러를 반환한다")
        void nullName() {
            List<String> errors = ProductNameValidator.validate(null);

            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("빈 문자열이면 에러를 반환한다")
        void blankName() {
            List<String> errors = ProductNameValidator.validate("  ");

            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("16자 이상이면 에러를 반환한다")
        void tooLongName() {
            String name = "a".repeat(16);

            List<String> errors = ProductNameValidator.validate(name);

            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("허용되지 않는 특수문자가 포함되면 에러를 반환한다")
        void invalidSpecialCharacter() {
            List<String> errors = ProductNameValidator.validate("상품!@#");

            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("카카오가 포함되면 에러를 반환한다")
        void kakaoNotAllowed() {
            List<String> errors = ProductNameValidator.validate("카카오 상품");

            assertFalse(errors.isEmpty());
        }
    }
}
