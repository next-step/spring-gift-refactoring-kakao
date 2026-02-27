package gift.option;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OptionNameValidatorTest {

    @Test
    void validate_validName_returnsEmpty() {
        var errors = OptionNameValidator.validate("기본 옵션");
        assertThat(errors).isEmpty();
    }

    @Test
    void validate_exceedsMaxLength_returnsError() {
        var name = "가".repeat(51);
        var errors = OptionNameValidator.validate(name);
        assertThat(errors).anyMatch(e -> e.contains("50자"));
    }

    @Test
    void validate_invalidChars_returnsError() {
        var errors = OptionNameValidator.validate("옵션@#$");
        assertThat(errors).anyMatch(e -> e.contains("특수 문자"));
    }

    @Test
    void validate_blankName_returnsError() {
        var errors = OptionNameValidator.validate("");
        assertThat(errors).anyMatch(e -> e.contains("필수"));
    }

    @Test
    void validate_validSpecialChars_returnsEmpty() {
        var errors = OptionNameValidator.validate("옵션 (A) [B] + - & / _");
        assertThat(errors).isEmpty();
    }
}
