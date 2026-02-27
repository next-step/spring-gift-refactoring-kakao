package gift.product;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductNameValidatorTest {

    @Test
    void validate_validName_returnsEmpty() {
        var errors = ProductNameValidator.validate("좋은 상품");
        assertThat(errors).isEmpty();
    }

    @Test
    void validate_exceedsMaxLength_returnsError() {
        var errors = ProductNameValidator.validate("이름이열다섯자를초과하는아주긴상품명");
        assertThat(errors).anyMatch(e -> e.contains("15자"));
    }

    @Test
    void validate_invalidChars_returnsError() {
        var errors = ProductNameValidator.validate("상품@#$");
        assertThat(errors).anyMatch(e -> e.contains("특수 문자"));
    }

    @Test
    void validate_containsKakao_returnsError() {
        var errors = ProductNameValidator.validate("카카오상품");
        assertThat(errors).anyMatch(e -> e.contains("카카오"));
    }

    @Test
    void validate_containsKakao_allowTrue_noError() {
        var errors = ProductNameValidator.validate("카카오상품", true);
        assertThat(errors).noneMatch(e -> e.contains("카카오"));
    }

    @Test
    void validate_blankName_returnsError() {
        var errors = ProductNameValidator.validate("");
        assertThat(errors).anyMatch(e -> e.contains("필수"));
    }
}
