package gift.option.dto;

import gift.option.entity.Option;

public record OptionResponse(
    Long id,
    String name,
    int quantity
) {
    public static OptionResponse from(Option option) {
        return new OptionResponse(
            option.getId(),
            option.getName(),
            option.getQuantity()
        );
    }
}
