package gift.wish;

import gift.member.Member;
import gift.product.Product;
import jakarta.validation.constraints.NotNull;

public record WishRequest(@NotNull Long productId) {
    public Wish toEntity(Member member, Product product) {
        return new Wish(member, product);
    }
}
