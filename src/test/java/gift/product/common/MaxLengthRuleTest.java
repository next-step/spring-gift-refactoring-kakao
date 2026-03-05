package gift.product.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MaxLengthRuleTest {

    private final MaxLengthRule rule = new MaxLengthRule(15);

    @Test
    @DisplayName("최대 길이 이내의 이름은 유효하다")
    void validWhenWithinMaxLength() {
        assertThat(rule.notValid("상품이름"))
                .isFalse();
    }

    @Test
    @DisplayName("최대 길이와 동일한 이름은 유효하다")
    void validWhenExactMaxLength() {
        String name = "a".repeat(15);

        assertThat(rule.notValid(name))
                .isFalse();
    }

    @Test
    @DisplayName("최대 길이를 초과하는 이름은 유효하지 않다")
    void invalidWhenExceedsMaxLength() {
        String name = "a".repeat(16);

        assertThat(rule.notValid(name))
                .isTrue();
    }

    @Test
    @DisplayName("null 이름은 유효하다")
    void invalidWhenNull() {
        assertThat(rule.notValid(null))
                .isFalse();
    }

    @Test
    @DisplayName("빈 문자열은 최대 길이 이내이므로 유효하다")
    void validWhenEmpty() {
        assertThat(rule.notValid(""))
                .isFalse();
    }

    @Test
    @DisplayName("규칙 설명에 최대 길이가 포함된다")
    void describeRuleContainsMaxLength() {
        assertThat(rule.describeRule())
                .contains("15");
    }
}
