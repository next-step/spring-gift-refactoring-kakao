package gift.fixture;

import gift.category.Category;

public class CategoryFixture {

    public static Category 기본카테고리() {
        return new Category("교환권", "#000000", "https://example.com/default.png", null);
    }
}
