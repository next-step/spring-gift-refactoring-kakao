package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.wish.WishRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderTransactionService {
  private final OrderRepository orderRepository;
  private final OptionRepository optionRepository;
  private final WishRepository wishRepository;
  private final MemberRepository memberRepository;

  public OrderTransactionService(
      OrderRepository orderRepository,
      OptionRepository optionRepository,
      WishRepository wishRepository,
      MemberRepository memberRepository) {
    this.orderRepository = orderRepository;
    this.optionRepository = optionRepository;
    this.wishRepository = wishRepository;
    this.memberRepository = memberRepository;
  }

  @Transactional
  public Optional<Order> executeOrder(Member member, OrderRequest request) {
    return optionRepository
        .findById(request.optionId())
        .map(
            option -> {
              option.subtractQuantity(request.quantity());
              optionRepository.save(option);

              Order saved =
                  orderRepository.save(
                      new Order(option, member.getId(), request.quantity(), request.message()));

              member.deductPoint(saved.getTotalPrice());
              memberRepository.save(member);

              Long productId = option.getProduct().getId();
              wishRepository
                  .findByMemberIdAndProductId(member.getId(), productId)
                  .ifPresent(wishRepository::delete);

              return saved;
            });
  }
}
