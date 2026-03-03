package gift.category;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank String name,
        @NotBlank String color,
        @NotBlank String imageUrl,
        String description
) {
}
