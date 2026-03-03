package gift.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductNameValidatorTest {

    @Test
    @DisplayName("정상 이름은 에러가 없다")
    void validName() {
        assertThat(ProductNameValidator.validate("맛있는 과자")).isEmpty();
    }

    @Test
    @DisplayName("15자를 초과하면 에러가 발생한다")
    void tooLong() {
        String name = "a".repeat(16);
        List<String> errors = ProductNameValidator.validate(name);
        assertThat(errors).anyMatch(e -> e.contains("15자"));
    }

    @Test
    @DisplayName("15자 이내는 통과한다")
    void exactMax() {
        String name = "a".repeat(15);
        assertThat(ProductNameValidator.validate(name)).isEmpty();
    }

    @Test
    @DisplayName("허용되지 않는 특수문자가 포함되면 에러가 발생한다")
    void invalidSpecialChars() {
        List<String> errors = ProductNameValidator.validate("상품!@#");
        assertThat(errors).anyMatch(e -> e.contains("특수 문자"));
    }

    @Test
    @DisplayName("허용된 특수문자는 통과한다")
    void allowedSpecialChars() {
        assertThat(ProductNameValidator.validate("상품(A)[B]+C-D")).isEmpty();
        assertThat(ProductNameValidator.validate("A&B/C_D")).isEmpty();
    }

    @Test
    @DisplayName("카카오가 포함된 이름은 기본적으로 에러가 발생한다")
    void kakaoForbidden() {
        List<String> errors = ProductNameValidator.validate("카카오 선물");
        assertThat(errors).anyMatch(e -> e.contains("카카오"));
    }

    @Test
    @DisplayName("allowKakao=true이면 카카오를 허용한다")
    void kakaoAllowed() {
        assertThat(ProductNameValidator.validate("카카오 선물", true)).isEmpty();
    }

    @Test
    @DisplayName("null이나 빈 문자열은 에러가 발생한다")
    void nullOrBlank() {
        assertThat(ProductNameValidator.validate(null)).isNotEmpty();
        assertThat(ProductNameValidator.validate("")).isNotEmpty();
        assertThat(ProductNameValidator.validate("   ")).isNotEmpty();
    }
}
