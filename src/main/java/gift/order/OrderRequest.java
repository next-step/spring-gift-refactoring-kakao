package gift.order;

import jakarta.validation.constraints.NotNull;

public record OrderRequest(
        @NotNull Long optionId,
        int quantity,
        String message
) {
}
