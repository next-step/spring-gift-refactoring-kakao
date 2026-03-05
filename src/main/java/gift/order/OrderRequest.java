package gift.order;

import gift.member.Member;
import gift.option.Option;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(
    @NotNull Long optionId,
    @Min(1) int quantity,
    String message
) {
    public Order toEntity(Option option, Member member, int totalPrice) {
        return new Order(option, member, quantity, totalPrice, message);
    }
}
