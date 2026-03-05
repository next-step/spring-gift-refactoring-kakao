package gift.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gift.member.MemberRepository;
import gift.option.OptionRepository;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final KakaoMessageClient kakaoMessageClient;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        MemberRepository memberRepository,
        KakaoMessageClient kakaoMessageClient
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.kakaoMessageClient = kakaoMessageClient;
    }

    public Page<OrderResponse> getOrders(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable).map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse createOrder(Long memberId, OrderRequest request) {
        var member = memberRepository.findById(memberId)
            .orElseThrow(() -> new java.util.NoSuchElementException("회원을 찾을 수 없습니다: " + memberId));

        var option = optionRepository.findById(request.optionId())
            .orElseThrow(() -> new java.util.NoSuchElementException("옵션을 찾을 수 없습니다: " + request.optionId()));

        option.subtractQuantity(request.quantity());

        var price = option.calculateTotalPrice(request.quantity());
        member.deductPoint(price);

        var saved = orderRepository.save(new Order(option, memberId, request.quantity(), request.message()));

        return OrderResponse.from(saved);
    }

    public void sendKakaoMessageIfPossible(String kakaoAccessToken, Long orderId) {
        if (kakaoAccessToken == null) {
            return;
        }
        try {
            var order = orderRepository.findById(orderId).orElseThrow();
            kakaoMessageClient.sendToMe(kakaoAccessToken, order);
        } catch (Exception e) {
            log.warn("메시지 전송에 실패했습니다: orderId={}", orderId, e);
        }
    }
}
