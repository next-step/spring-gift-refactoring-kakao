package gift.product.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotContainsKakaoRuleTest {

    private final NotContainsKakaoRule rule = new NotContainsKakaoRule();

    @Test
    @DisplayName("카카오가 포함되지 않은 이름은 유효하다")
    void validWhenNotContainsKakao() {
        assertThat(rule.notValid("일반상품"))
                .isFalse();
    }

    @Test
    @DisplayName("카카오가 포함된 이름은 유효하지 않다")
    void invalidWhenContainsKakao() {
        assertThat(rule.notValid("카카오상품"))
                .isTrue();
    }

    @Test
    @DisplayName("이름 중간에 카카오가 포함되어도 유효하지 않다")
    void invalidWhenContainsKakaoInMiddle() {
        assertThat(rule.notValid("나의카카오선물"))
                .isTrue();
    }

    @Test
    @DisplayName("null은 유효하다")
    void invalidWhenNull() {
        assertThat(rule.notValid(null))
                .isFalse();
    }
}
