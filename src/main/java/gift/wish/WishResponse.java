package gift.wish;

import java.time.LocalDateTime;

public record WishResponse(
    Long id,
    Long productId,
    String name,
    int price,
    String imageUrl,
    LocalDateTime createdDate
) {
    public static WishResponse from(Wish wish) {
        return new WishResponse(
            wish.getId(),
            wish.getProduct().getId(),
            wish.getProduct().getName(),
            wish.getProduct().getPrice(),
            wish.getProduct().getImageUrl(),
            wish.getCreatedDate()
        );
    }
}
