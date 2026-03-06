package gift.option;

import jakarta.validation.constraints.NotBlank;

public record OptionRequest(
        @NotBlank String name,
        int quantity
) {
}
