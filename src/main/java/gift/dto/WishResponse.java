package gift.dto;

import gift.model.Wish;

public record WishResponse(
    Long id,
    Long productId,
    String name,
    int price,
    String imageUrl
) {
    public static WishResponse from(Wish wish) {
        return new WishResponse(
            wish.getId(),
            wish.productId(),
            wish.productName(),
            wish.productPrice(),
            wish.productImageUrl()
        );
    }
}
