package gift.order;

import gift.member.Member;
import gift.option.Option;

public record OrderCompletedEvent(
    Member member,
    Order order,
    Option option
) {
}
