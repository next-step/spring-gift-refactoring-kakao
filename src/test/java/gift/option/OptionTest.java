package gift.option;

import gift.category.entity.Category;
import gift.option.entity.Option;
import gift.option.exception.OptionErrorCode;
import gift.option.exception.OptionException;
import gift.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OptionTest {
    @Test
    @DisplayName("차감 수량이 현재 재고보다 많으면 INSUFFICIENT_OPTION_QUANTITY 예외를 던진다")
    void subtractQuantity_insufficient_throwsException() {
        Product product = new Product("아메리카노", 4500, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        Option option = new Option(product, "기본 옵션", 3);

        OptionException exception = assertThrows(OptionException.class, () -> option.subtractQuantity(4));

        assertEquals(OptionErrorCode.INSUFFICIENT_OPTION_QUANTITY, exception.getErrorCode());
    }

    @Test
    @DisplayName("차감 수량이 재고 이하면 재고가 차감된다")
    void subtractQuantity_success_decreasesQuantity() {
        Product product = new Product("아메리카노", 4500, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        Option option = new Option(product, "기본 옵션", 5);

        option.subtractQuantity(2);

        assertEquals(3, option.getQuantity());
    }

    @Test
    @DisplayName("옵션이 요청한 상품에 속하지 않으면 OPTION_NOT_FOUND 예외를 던진다")
    void assertBelongsTo_notBelong_throwsException() {
        Product product = new Product("아메리카노", 4500, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        ReflectionTestUtils.setField(product, "id", 1L);
        Option option = new Option(product, "기본 옵션", 5);

        OptionException exception = assertThrows(OptionException.class, () -> option.assertBelongsTo(2L));

        assertEquals(OptionErrorCode.OPTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("옵션 개수가 1개 이하이면 CANNOT_DELETE_LAST_OPTION 예외를 던진다")
    void assertDeletableIn_lastOption_throwsException() {
        Product product = new Product("아메리카노", 4500, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        Option option = new Option(product, "기본 옵션", 5);

        OptionException exception = assertThrows(OptionException.class, () -> Option.assertDeletableIn(1));

        assertEquals(OptionErrorCode.CANNOT_DELETE_LAST_OPTION, exception.getErrorCode());
    }

    @Test
    @DisplayName("옵션 개수가 2개 이상이면 삭제 가능하다")
    void assertDeletableIn_deletable_doesNotThrow() {
        Product product = new Product("아메리카노", 4500, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        Option option = new Option(product, "기본 옵션", 5);

        assertDoesNotThrow(() -> Option.assertDeletableIn(2));
    }
}
