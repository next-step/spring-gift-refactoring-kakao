package gift.category.internal;

import gift.category.Category;
import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank String name,
        @NotBlank String color,
        @NotBlank String imageUrl,
        String description
) {

    public Category toEntity() {
        return Category.builder()
                .name(name)
                .color(color)
                .imageUrl(imageUrl)
                .description(description)
                .build();
    }
}
