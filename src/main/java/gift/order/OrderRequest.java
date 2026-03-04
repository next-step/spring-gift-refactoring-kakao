package gift.order;

import gift.option.Option;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(
    @NotNull Long optionId,
    @Min(1) int quantity,
    String message
) {
    public Order toEntity(Option option, Long memberId, int totalPrice) {
        return new Order(option, memberId, quantity, totalPrice, message);
    }
}
