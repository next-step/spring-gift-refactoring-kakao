package gift.option;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gift.category.Category;
import gift.product.Product;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OptionTest {

    private Product createProduct(Long id, int price) {
        Category category = new Category("테스트 카테고리", "#000000", "http://img.test/cat.png", "설명");
        Product product = new Product("테스트 상품", price, "http://img.test/prod.png", category);
        setId(product, id);
        return product;
    }

    private Option createOption(int quantity) {
        return new Option(createProduct(1L, 1000), "테스트 옵션", quantity);
    }

    private void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    @Test
    @DisplayName("0 이하의 수량을 차감하면 예외가 발생한다")
    void subtractZeroOrNegativeThrows() {
        Option option = createOption(10);

        assertThrows(IllegalArgumentException.class, () -> option.subtractQuantity(0));
        assertThrows(IllegalArgumentException.class, () -> option.subtractQuantity(-1));
    }

    @Test
    @DisplayName("상품 가격과 주문 수량을 곱한 총액을 계산한다")
    void calculateTotalPrice() {
        Option option = createOption(10);

        assertEquals(3000, option.calculateTotalPrice(3));
    }

    @Test
    @DisplayName("자신이 속한 상품의 ID와 일치하면 true를 반환한다")
    void belongsToMatchingProduct() {
        Option option = createOption(10);

        assertTrue(option.belongsTo(1L));
    }

    @Test
    @DisplayName("다른 상품의 ID이면 false를 반환한다")
    void belongsToDifferentProduct() {
        Option option = createOption(10);

        assertFalse(option.belongsTo(999L));
    }
}
