package gift.order;

import gift.member.Member;
import gift.option.OptionRepository;
import gift.product.Product;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  private final OrderRepository orderRepository;
  private final OptionRepository optionRepository;
  private final OrderTransactionService orderTransactionService;
  private final KakaoMessageClient kakaoMessageClient;

  public OrderService(
      OrderRepository orderRepository,
      OptionRepository optionRepository,
      OrderTransactionService orderTransactionService,
      KakaoMessageClient kakaoMessageClient) {
    this.orderRepository = orderRepository;
    this.optionRepository = optionRepository;
    this.orderTransactionService = orderTransactionService;
    this.kakaoMessageClient = kakaoMessageClient;
  }

  public Page<OrderResponse> getOrders(Long memberId, Pageable pageable) {
    return orderRepository.findByMemberId(memberId, pageable).map(OrderResponse::from);
  }

  // order flow:
  // 1. validate option
  // 2. subtract stock
  // 3. deduct points
  // 4. save order
  // 5. cleanup wish
  // --- transaction boundary ---
  // 6. send kakao notification (best-effort, outside transaction)
  public Optional<OrderResponse> createOrder(Member member, OrderRequest request) {
    Optional<Order> orderOpt = orderTransactionService.executeOrder(member, request);

    orderOpt.ifPresent(
        order -> {
          optionRepository
              .findById(request.optionId())
              .ifPresent(option -> sendKakaoMessageIfPossible(member, order, option));
        });

    return orderOpt.map(OrderResponse::from);
  }

  private void sendKakaoMessageIfPossible(Member member, Order order, gift.option.Option option) {
    if (member.getKakaoAccessToken() == null) {
      return;
    }
    try {
      Product product = option.getProduct();
      kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
    } catch (Exception e) {
      log.warn("카카오 메시지 전송 실패: {}", e.getMessage());
    }
  }
}
