package gift.product.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotBlankRuleTest {

    private final NotBlankRule rule = new NotBlankRule();

    @Test
    @DisplayName("값이 있는 이름은 유효하다")
    void validWhenNotBlank() {
        assertThat(rule.notValid("상품이름"))
                .isFalse();
    }

    @Test
    @DisplayName("빈 문자열은 유효하지 않다")
    void invalidWhenEmpty() {
        assertThat(rule.notValid(""))
                .isTrue();
    }

    @Test
    @DisplayName("null은 유효하지 않다")
    void invalidWhenNull() {
        assertThat(rule.notValid(null))
                .isTrue();
    }
}
