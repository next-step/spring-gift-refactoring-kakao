package gift.category;

import gift.category.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryTest {
    @Test
    @DisplayName("카테고리 정보를 수정하면 필드가 모두 변경된다")
    void update_changesAllFields() {
        Category category = new Category("음료", "#000000", "http://old.png", null);

        category.update("간식", "#111111", "http://new.png", "설명");

        assertEquals("간식", category.getName());
        assertEquals("#111111", category.getColor());
        assertEquals("http://new.png", category.getImageUrl());
        assertEquals("설명", category.getDescription());
    }
}
