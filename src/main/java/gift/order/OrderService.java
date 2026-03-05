package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import java.util.NoSuchElementException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {
  private final OrderRepository orderRepository;
  private final OptionRepository optionRepository;
  private final MemberRepository memberRepository;
  private final ApplicationEventPublisher eventPublisher;

  public OrderService(
      OrderRepository orderRepository,
      OptionRepository optionRepository,
      MemberRepository memberRepository,
      ApplicationEventPublisher eventPublisher) {
    this.orderRepository = orderRepository;
    this.optionRepository = optionRepository;
    this.memberRepository = memberRepository;
    this.eventPublisher = eventPublisher;
  }

  public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
    return orderRepository.findByMemberId(memberId, pageable);
  }

  @Transactional
  public Order placeOrder(Long memberId, Long optionId, int quantity, String message) {
    Option option =
        optionRepository
            .findById(optionId)
            .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. id=" + optionId));

    option.subtractQuantity(quantity);

    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다. id=" + memberId));

    int price = option.calculateTotalPrice(quantity);
    member.deductPoint(price);

    Order saved = orderRepository.save(new Order(option, memberId, quantity, message));

    eventPublisher.publishEvent(new OrderCompletedEvent(member, saved, option));

    return saved;
  }
}
