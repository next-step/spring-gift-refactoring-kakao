package gift.product;

import gift.category.entity.Category;
import gift.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductTest {
    @Test
    @DisplayName("상품 정보를 수정하면 필드가 모두 변경된다")
    void update_changesAllFields() {
        Category oldCategory = new Category("음료", "#000000", "http://old.png", null);
        Category newCategory = new Category("간식", "#111111", "http://new.png", "desc");
        Product product = new Product("아메리카노", 4500, "http://old-image.png", oldCategory);

        product.update("라떼", 5500, "http://new-image.png", newCategory);

        assertEquals("라떼", product.getName());
        assertEquals(5500, product.getPrice());
        assertEquals("http://new-image.png", product.getImageUrl());
        assertEquals(newCategory, product.getCategory());
    }
}
