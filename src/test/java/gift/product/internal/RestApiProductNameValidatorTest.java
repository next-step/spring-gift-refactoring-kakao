package gift.product.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gift.global.BadRequestException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestApiProductNameValidatorTest {

    private final RestApiProductNameValidator validator = new RestApiProductNameValidator();

    @Test
    @DisplayName("유효한 상품명은 빈 위반 목록을 반환한다")
    void validateReturnsEmptyForValidName() {
        List<String> violations = validator.validate("정상상품");

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("유효한 상품명은 예외를 발생시키지 않는다")
    void validateOrThrowDoesNotThrowForValidName() {
        assertThatCode(() -> validator.validateOrThrowException("정상상품"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("카카오가 포함된 상품명은 위반 목록을 반환한다")
    void validateReturnsViolationForKakao() {
        List<String> violations = validator.validate("카카오상품");

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst()).contains("카카오");
    }

    @Test
    @DisplayName("카카오가 포함된 상품명은 BadRequestException을 발생시킨다")
    void validateOrThrowThrowsForKakao() {
        assertThatThrownBy(() -> validator.validateOrThrowException("카카오상품"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("15자 초과 상품명은 위반 목록을 반환한다")
    void validateReturnsViolationForTooLongName() {
        String longName = "a".repeat(16);

        List<String> violations = validator.validate(longName);

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst()).contains("15");
    }

    @Test
    @DisplayName("허용되지 않은 특수 문자가 포함된 상품명은 위반 목록을 반환한다")
    void validateReturnsViolationForSpecialCharacters() {
        List<String> violations = validator.validate("상품!@#");

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst()).contains("특수 문자");
    }

    @Test
    @DisplayName("빈 문자열은 NotBlank 위반을 반환한다")
    void validateReturnsViolationForEmptyName() {
        List<String> violations = validator.validate("");

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.contains("필수"));
    }

    @Test
    @DisplayName("null은 NotBlank 규칙만 위반한다")
    void validateReturnsOnlyNotBlankViolationForNull() {
        List<String> violations = validator.validate(null);

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst()).contains("필수");
    }

    @Test
    @DisplayName("여러 규칙을 동시에 위반하면 모든 위반 메시지를 반환한다")
    void validateReturnsMultipleViolations() {
        String name = "카카오!".repeat(6); // 카카오 포함 + 특수 문자 + 15자 초과

        List<String> violations = validator.validate(name);

        assertThat(violations).hasSizeGreaterThanOrEqualTo(3);
    }
}
