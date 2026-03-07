package gift.order;

import gift.member.Member;
import gift.product.Product;

public record OrderCreatedEvent(String kakaoAccessToken, Order order, Product product) {
    public static OrderCreatedEvent from(Member member, Order order, Product product) {
        return new OrderCreatedEvent(member.getKakaoAccessToken(), order, product);
    }
}
