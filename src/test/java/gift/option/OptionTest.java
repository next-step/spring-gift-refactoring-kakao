package gift.option;

import gift.category.Category;
import gift.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionTest {

    @Test
    @DisplayName("calculateTotalPrice: 상품 가격 * 주문 수량을 반환한다")
    void calculateTotalPrice() {
        // Given
        Category category = new Category("카테고리", "#000", "http://img.com", "설명");
        Product product = new Product("상품", 1000, "http://img.com", category);
        Option option = new Option(product, "옵션", 100);

        // When
        long totalPrice = option.calculateTotalPrice(5);

        // Then
        assertThat(totalPrice)
            .as("가격 1000원 * 수량 5개 = 5000원")
            .isEqualTo(5000L);
    }

    @Test
    @DisplayName("calculateTotalPrice: 수량이 1일 때 상품 가격을 반환한다")
    void calculateTotalPrice_singleQuantity() {
        // Given
        Category category = new Category("카테고리", "#000", "http://img.com", "설명");
        Product product = new Product("상품", 2500, "http://img.com", category);
        Option option = new Option(product, "옵션", 100);

        // When
        long totalPrice = option.calculateTotalPrice(1);

        // Then
        assertThat(totalPrice).isEqualTo(2500L);
    }

    @Test
    @DisplayName("calculateTotalPrice: 수량이 0일 때 0을 반환한다")
    void calculateTotalPrice_zeroQuantity() {
        // Given
        Category category = new Category("카테고리", "#000", "http://img.com", "설명");
        Product product = new Product("상품", 1000, "http://img.com", category);
        Option option = new Option(product, "옵션", 100);

        // When
        long totalPrice = option.calculateTotalPrice(0);

        // Then
        assertThat(totalPrice)
            .as("수량 0일 때 가격은 0이어야 한다")
            .isEqualTo(0L);
    }

    @Test
    @DisplayName("calculateTotalPrice: 음수 수량은 예외를 발생시킨다")
    void calculateTotalPrice_negativeQuantity() {
        // Given
        Category category = new Category("카테고리", "#000", "http://img.com", "설명");
        Product product = new Product("상품", 1000, "http://img.com", category);
        Option option = new Option(product, "옵션", 100);

        // When & Then
        assertThatThrownBy(() -> option.calculateTotalPrice(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("0 이상");
    }

    @Test
    @DisplayName("calculateTotalPrice: 대량 주문도 오버플로우 없이 계산한다")
    void calculateTotalPrice_largeQuantity() {
        // Given: 100,000원 * 30,000개 = 3,000,000,000 (int 범위 초과)
        Category category = new Category("카테고리", "#000", "http://img.com", "설명");
        Product product = new Product("상품", 100_000, "http://img.com", category);
        Option option = new Option(product, "옵션", 100_000);

        // When
        long totalPrice = option.calculateTotalPrice(30_000);

        // Then
        assertThat(totalPrice)
            .as("100,000원 * 30,000개 = 3,000,000,000원")
            .isEqualTo(3_000_000_000L);
    }

    @Test
    @DisplayName("subtractQuantity: 재고에서 수량을 차감한다")
    void subtractQuantity() {
        // Given
        Category category = new Category("카테고리", "#000", "http://img.com", "설명");
        Product product = new Product("상품", 1000, "http://img.com", category);
        Option option = new Option(product, "옵션", 100);

        // When
        option.subtractQuantity(30);

        // Then
        assertThat(option.getQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("subtractQuantity: 재고보다 많은 수량을 차감하면 예외를 발생시킨다")
    void subtractQuantity_insufficientStock() {
        // Given
        Category category = new Category("카테고리", "#000", "http://img.com", "설명");
        Product product = new Product("상품", 1000, "http://img.com", category);
        Option option = new Option(product, "옵션", 10);

        // When & Then
        assertThatThrownBy(() -> option.subtractQuantity(20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("재고보다 많습니다");
    }
}
