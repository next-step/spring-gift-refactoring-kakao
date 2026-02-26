package gift.fixture;

import gift.product.Product;

public class ProductFixture {

    public static Product 기본상품() {
        return 상품("아이스 아메리카노", 4500);
    }

    public static Product 상품(String name, int price) {
        return new Product(name, price, "https://example.com/image.png", null);
    }
}
