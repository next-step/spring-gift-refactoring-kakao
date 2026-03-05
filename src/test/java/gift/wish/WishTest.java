package gift.wish;

import gift.auth.exception.ForbiddenException;
import gift.category.entity.Category;
import gift.product.entity.Product;
import gift.wish.entity.Wish;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WishTest {
    @Test
    @DisplayName("위시 소유자가 아니면 ForbiddenException 예외를 던진다")
    void assertOwner_forbidden_throwsException() {
        Product product = new Product("아메리카노", 3000, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        Wish wish = new Wish(1L, product);

        assertThrows(ForbiddenException.class, () -> wish.assertOwner(999L));
    }

    @Test
    @DisplayName("위시 소유자이면 예외를 던지지 않는다")
    void assertOwner_owner_doesNotThrow() {
        Product product = new Product("아메리카노", 3000, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        Wish wish = new Wish(1L, product);

        assertDoesNotThrow(() -> wish.assertOwner(1L));
    }
}
