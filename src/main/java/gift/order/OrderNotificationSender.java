package gift.order;

import gift.member.Member;
import gift.option.Option;

public interface OrderNotificationSender {
    void send(Member member, Order order, Option option);
}
