package gift.option;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gift.category.Category;
import gift.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OptionTest {

    private Option createOption(int quantity) {
        Category category = new Category("테스트 카테고리", "#000000", "http://img.test/cat.png", "설명");
        Product product = new Product("테스트 상품", 1000, "http://img.test/prod.png", category);
        return new Option(product, "테스트 옵션", quantity);
    }

    @Test
    @DisplayName("재고 이내 수량을 차감하면 수량이 감소한다")
    void subtractWithinStock() {
        Option option = createOption(10);

        option.subtractQuantity(3);

        assertEquals(7, option.getQuantity());
    }

    @Test
    @DisplayName("재고와 동일한 수량을 차감하면 수량이 0이 된다")
    void subtractExactStock() {
        Option option = createOption(10);

        option.subtractQuantity(10);

        assertEquals(0, option.getQuantity());
    }

    @Test
    @DisplayName("재고를 초과하는 수량을 차감하면 예외가 발생한다")
    void subtractOverStockThrows() {
        Option option = createOption(10);

        assertThrows(IllegalArgumentException.class, () -> option.subtractQuantity(11));
    }
}
