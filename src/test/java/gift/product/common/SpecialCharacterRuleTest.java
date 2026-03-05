package gift.product.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpecialCharacterRuleTest {

    private final SpecialCharacterRule rule = new SpecialCharacterRule();

    @Test
    @DisplayName("영문, 숫자, 한글로만 구성된 이름은 유효하다")
    void validWithAlphanumericAndKorean() {
        assertThat(rule.notValid("상품abc123")).isFalse();
    }

    @Test
    @DisplayName("허용된 특수 문자가 포함된 이름은 유효하다")
    void validWithAllowedSpecialCharacters() {
        assertThat(rule.notValid("상품 (A) [B] +/-&_")).isFalse();
    }

    @Test
    @DisplayName("허용되지 않은 특수 문자가 포함된 이름은 유효하지 않다")
    void invalidWithDisallowedSpecialCharacters() {
        assertThat(rule.notValid("상품!@#"))
                .isTrue();
    }

    @Test
    @DisplayName("null은 유효하다")
    void invalidWhenNull() {
        assertThat(rule.notValid(null))
                .isFalse();
    }

    @Test
    @DisplayName("빈 문자열은 패턴에 매칭되므로 유효하다")
    void validWhenEmpty() {
        assertThat(rule.notValid(""))
                .isFalse();
    }

    @Test
    @DisplayName("한글 자모음은 유효하다")
    void validWithKoreanJamo() {
        assertThat(rule.notValid("ㄱㄴㄷㅏㅓㅗ"))
                .isFalse();
    }
}
