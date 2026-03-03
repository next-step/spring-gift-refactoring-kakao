package gift.wish.internal;

import jakarta.validation.constraints.NotNull;

public record WishRequest(
        @NotNull Long productId
) {

}
