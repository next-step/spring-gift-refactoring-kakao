package gift.option;

import gift.category.Category;
import gift.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionTest {

    @Test
    @DisplayName("재고 이내에서 수량을 차감할 수 있다")
    void subtractWithinStock() {
        Option option = createOption(10);
        option.subtractQuantity(7);
        assertThat(option.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("재고 전부를 차감할 수 있다")
    void subtractExactStock() {
        Option option = createOption(5);
        option.subtractQuantity(5);
        assertThat(option.getQuantity()).isZero();
    }

    @Test
    @DisplayName("재고보다 많이 차감하면 예외가 발생한다")
    void subtractExceedingStock() {
        Option option = createOption(3);
        assertThatThrownBy(() -> option.subtractQuantity(4))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private Option createOption(int quantity) {
        Category category = new Category("카테고리", "#000", "img.png", "설명");
        Product product = new Product("상품", 1000, "img.png", category);
        return new Option(product, "옵션A", quantity);
    }
}
