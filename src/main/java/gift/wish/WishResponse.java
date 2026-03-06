package gift.wish;

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
            wish.getProductId(),
            wish.getProductName(),
            wish.getProductPrice(),
            wish.getProductImageUrl()
        );
    }
}
