package gift.option;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OptionNameValidatorTest {

    @Test
    @DisplayName("정상 이름은 에러가 없다")
    void validName() {
        assertThat(OptionNameValidator.validate("기본 옵션")).isEmpty();
    }

    @Test
    @DisplayName("50자를 초과하면 에러가 발생한다")
    void tooLong() {
        String name = "a".repeat(51);
        List<String> errors = OptionNameValidator.validate(name);
        assertThat(errors).anyMatch(e -> e.contains("50자"));
    }

    @Test
    @DisplayName("50자 이내는 통과한다")
    void exactMax() {
        String name = "a".repeat(50);
        assertThat(OptionNameValidator.validate(name)).isEmpty();
    }

    @Test
    @DisplayName("허용되지 않는 특수문자가 포함되면 에러가 발생한다")
    void invalidSpecialChars() {
        List<String> errors = OptionNameValidator.validate("옵션!@#");
        assertThat(errors).anyMatch(e -> e.contains("특수 문자"));
    }

    @Test
    @DisplayName("null이나 빈 문자열은 에러가 발생한다")
    void nullOrBlank() {
        assertThat(OptionNameValidator.validate(null)).isNotEmpty();
        assertThat(OptionNameValidator.validate("")).isNotEmpty();
    }
}
