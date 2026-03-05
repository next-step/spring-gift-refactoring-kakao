package gift.option;

import gift.category.Category;
import gift.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
