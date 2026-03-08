package gift.wish;

public record WishResponse(
    Long id,
    Long productId,
    String name,
    int price,
    String imageUrl
) {
    public static WishResponse from(Wish wish) {
        var product = wish.getProduct();
        return new WishResponse(
            wish.getId(),
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getImageUrl()
        );
    }
}
