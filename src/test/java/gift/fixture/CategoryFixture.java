package gift.fixture;

import gift.category.Category;

public class CategoryFixture {

    public static Category 기본카테고리() {
        return 카테고리("교환권");
    }

    public static Category 카테고리(String name) {
        return new Category(name, "#000000", "https://example.com/default.png", null);
    }
}
