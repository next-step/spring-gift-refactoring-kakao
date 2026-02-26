package gift;

import gift.category.Category;
import gift.member.Member;
import gift.option.Option;
import gift.order.Order;
import gift.product.Product;
import gift.wish.Wish;

import java.lang.reflect.Field;

public class TestFixtures {

    public static Category category() {
        return category(1L, "교환권");
    }

    public static Category category(Long id, String name) {
        var category = new Category(name, "#ffffff", "http://img.test/" + name + ".png", name + " 카테고리");
        setId(category, id);
        return category;
    }

    public static Product product() {
        return product(1L, "테스트 상품", category());
    }

    public static Product product(Long id, String name, Category category) {
        var product = new Product(name, 10000, "http://img.test/" + name + ".png", category);
        setId(product, id);
        return product;
    }

    public static Member member() {
        return member(1L, "test@test.com", "password");
    }

    public static Member member(Long id, String email, String password) {
        var member = new Member(email, password);
        setId(member, id);
        return member;
    }

    public static Member kakaoMember(Long id, String email, String kakaoToken) {
        var member = new Member(email);
        member.updateKakaoAccessToken(kakaoToken);
        setId(member, id);
        return member;
    }

    public static Option option() {
        return option(1L, product(), "기본 옵션", 100);
    }

    public static Option option(Long id, Product product, String name, int quantity) {
        var option = new Option(product, name, quantity);
        setId(option, id);
        return option;
    }

    public static Wish wish(Long id, Long memberId, Product product) {
        var wish = new Wish(memberId, product);
        setId(wish, id);
        return wish;
    }

    public static Order order(Long id, Option option, Long memberId, int quantity, String message) {
        var order = new Order(option, memberId, quantity, message);
        setId(order, id);
        return order;
    }

    private static void setId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id on " + entity.getClass().getSimpleName(), e);
        }
    }
}
