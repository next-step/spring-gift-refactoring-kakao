package gift.order;

import gift.infrastructure.kakao.KakaoMessageClient;
import gift.member.Member;
import gift.member.MemberService;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {
  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  private final OrderRepository orderRepository;
  private final OptionRepository optionRepository;
  private final MemberService memberService;
  private final KakaoMessageClient kakaoMessageClient;

  public OrderService(
      OrderRepository orderRepository,
      OptionRepository optionRepository,
      MemberService memberService,
      KakaoMessageClient kakaoMessageClient) {
    this.orderRepository = orderRepository;
    this.optionRepository = optionRepository;
    this.memberService = memberService;
    this.kakaoMessageClient = kakaoMessageClient;
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
        memberService
            .findById(memberId)
            .orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다. id=" + memberId));

    int price = option.calculateTotalPrice(quantity);
    member.deductPoint(price);

    Order saved = orderRepository.save(new Order(option, memberId, quantity, message));

    sendKakaoMessageIfPossible(member, saved, option);

    return saved;
  }

  private void sendKakaoMessageIfPossible(Member member, Order order, Option option) {
    if (member.getKakaoAccessToken() == null) {
      return;
    }
    try {
      Product product = option.getProduct();
      kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
    } catch (Exception e) {
      log.warn("카카오 메시지 전송에 실패했습니다. orderId={}, memberId={}", order.getId(), member.getId(), e);
    }
  }
}
