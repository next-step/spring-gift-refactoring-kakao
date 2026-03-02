package gift.product.admin;

import gift.category.Category;
import gift.product.Product;

public record ProductDto(
        Long id,
        String name,
        int price,
        String imageUrl,
        CategoryDto category
) {

    public static ProductDto from(Product entity) {
        Long id = entity.getId();
        String name = entity.getName();
        int price = entity.getPrice();
        String imageUrl = entity.getImageUrl();

        Category category = entity.getCategory();
        CategoryDto categoryDto = CategoryDto.from(category);

        return new ProductDto(
                id, name, price,
                imageUrl, categoryDto
        );
    }

    public record CategoryDto(
            Long id,
            String name
    ) {

        public static CategoryDto from(Category entity) {
            Long id = entity.getId();
            String name = entity.getName();

            return new CategoryDto(id, name);
        }
    }
}
