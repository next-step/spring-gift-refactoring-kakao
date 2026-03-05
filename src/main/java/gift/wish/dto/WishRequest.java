package gift.wish.dto;

import gift.product.entity.Product;
import gift.wish.entity.Wish;
import jakarta.validation.constraints.NotNull;

public record WishRequest(@NotNull Long productId) {
    public Wish toEntity(Long memberId, Product product) {
        return new Wish(memberId, product);
    }
}
