package gift.product;

public record ProductResponse(
    Long id,
    String name,
    int price,
    String imageUrl,
    Long categoryId
) {
    public static ProductResponse from(Product product) {
        var category = product.getCategory();
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getImageUrl(),
            category.getId()
        );
    }
}
