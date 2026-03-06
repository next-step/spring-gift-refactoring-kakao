package gift.option;

import gift.category.Category;
import gift.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionTest {

    private Option createOption(int price, int quantity) {
        Category category = new Category("전자기기", "#000000", "https://example.com/img.jpg", "설명");
        Product product = new Product("테스트상품", price, "https://example.com/img.jpg", category);
        return new Option(product, "옵션A", quantity);
    }

    @Nested
    @DisplayName("calculatePrice")
    class CalculatePrice {

        @Test
        @DisplayName("상품 가격 × 수량으로 주문 금액을 계산한다")
        void success() {
            Option option = createOption(10000, 5);

            int price = option.calculatePrice(3);

            assertThat(price).isEqualTo(30000);
        }

        @Test
        @DisplayName("수량이 1이면 상품 가격과 동일하다")
        void singleQuantity() {
            Option option = createOption(25000, 10);

            int price = option.calculatePrice(1);

            assertThat(price).isEqualTo(25000);
        }
    }

    @Nested
    @DisplayName("subtractQuantity")
    class SubtractQuantity {

        @Test
        @DisplayName("재고 이내 수량을 차감하면 재고가 감소한다")
        void success() {
            Option option = createOption(10000, 10);

            option.subtractQuantity(3);

            assertThat(option.getQuantity()).isEqualTo(7);
        }

        @Test
        @DisplayName("재고보다 많은 수량을 차감하면 예외가 발생한다")
        void insufficientStock() {
            Option option = createOption(10000, 5);

            assertThatThrownBy(() -> option.subtractQuantity(6))
                .isInstanceOf(OptionException.class);
        }

        @Test
        @DisplayName("재고와 동일한 수량을 차감하면 재고가 0이 된다")
        void exactQuantity() {
            Option option = createOption(10000, 5);

            option.subtractQuantity(5);

            assertThat(option.getQuantity()).isEqualTo(0);
        }
    }
}
