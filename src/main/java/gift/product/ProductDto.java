package gift.product;

public record ProductDto(
        Long id,
        String name,
        int price,
        String imageUrl,
        Long categoryId
) {

}