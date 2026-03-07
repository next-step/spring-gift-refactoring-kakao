package gift.order;

import gift.member.Member;
import gift.option.Option;

record OrderTransactionResult(Member member, Order order, Option option) {
}
