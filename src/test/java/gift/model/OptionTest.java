package gift.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionTest {

    private Product product;

    @BeforeEach
    void setUp() {
        var category = new Category("전자기기", "#1E90FF", "https://example.com/img.jpg", "설명");
        product = new Product("테스트상품", 10000, "https://example.com/img.jpg", category);
    }

    @Test
    @DisplayName("재고 내의 수량을 차감하면 재고가 줄어든다")
    void subtractValidQuantity() {
        var option = new Option(product, "옵션A", 10);

        option.subtractQuantity(3);

        assertThat(option.getQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("재고와 동일한 수량을 차감하면 재고가 0이 된다")
    void subtractExactQuantity() {
        var option = new Option(product, "옵션A", 5);

        option.subtractQuantity(5);

        assertThat(option.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("재고보다 많은 수량을 차감하면 예외가 발생한다")
    void subtractExceedingQuantityThrows() {
        var option = new Option(product, "옵션A", 5);

        assertThatThrownBy(() -> option.subtractQuantity(6))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("재고가 0인 상태에서 차감하면 예외가 발생한다")
    void subtractFromZeroStockThrows() {
        var option = new Option(product, "옵션A", 0);

        assertThatThrownBy(() -> option.subtractQuantity(1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("수량 1일 때 상품 가격과 동일한 총액을 반환한다")
    void calculateTotalPriceWithSingleQuantity() {
        var option = new Option(product, "옵션A", 10);

        assertThat(option.calculateTotalPrice(1)).isEqualTo(10000);
    }

    @Test
    @DisplayName("수량 N일 때 상품 가격 * N의 총액을 반환한다")
    void calculateTotalPriceWithMultipleQuantity() {
        var option = new Option(product, "옵션A", 10);

        assertThat(option.calculateTotalPrice(3)).isEqualTo(30000);
    }
}
