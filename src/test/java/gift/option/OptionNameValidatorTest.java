package gift.option;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OptionNameValidatorTest {

    @Nested
    @DisplayName("정상 케이스")
    class ValidNames {

        @Test
        @DisplayName("한글, 영문, 숫자 조합 50자 이내는 통과한다")
        void validMixedName() {
            List<String> errors = OptionNameValidator.validate("옵션ABC 123");

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("허용 특수문자를 포함하면 통과한다")
        void validSpecialCharacters() {
            List<String> errors = OptionNameValidator.validate("옵션(A)[B]+C-D&E/F_G");

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("정확히 50자는 통과한다")
        void validExactMaxLength() {
            String name = "a".repeat(50);

            List<String> errors = OptionNameValidator.validate(name);

            assertTrue(errors.isEmpty());
        }
    }

    @Nested
    @DisplayName("에러 케이스")
    class InvalidNames {

        @Test
        @DisplayName("null이면 에러를 반환한다")
        void nullName() {
            List<String> errors = OptionNameValidator.validate(null);

            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("빈 문자열이면 에러를 반환한다")
        void blankName() {
            List<String> errors = OptionNameValidator.validate("  ");

            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("51자 이상이면 에러를 반환한다")
        void tooLongName() {
            String name = "a".repeat(51);

            List<String> errors = OptionNameValidator.validate(name);

            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("허용되지 않는 특수문자가 포함되면 에러를 반환한다")
        void invalidSpecialCharacter() {
            List<String> errors = OptionNameValidator.validate("옵션!@#");

            assertFalse(errors.isEmpty());
        }
    }
}
