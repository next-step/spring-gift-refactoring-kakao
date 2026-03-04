package gift.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateProductRequest(
    @NotBlank String name,
    @Positive int price,
    @NotBlank String imageUrl,
    @NotNull Long categoryId) {}
