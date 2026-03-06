package gift.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(
        @NotBlank String name,
        int price,
        @NotBlank String imageUrl,
        @NotNull Long categoryId
) {
}
