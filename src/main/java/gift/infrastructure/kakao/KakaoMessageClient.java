package gift.infrastructure.kakao;

import gift.member.Member;
import gift.option.Option;
import gift.order.Order;

public interface KakaoMessageClient {
  void send(Member member, Order order, Option option);
}
