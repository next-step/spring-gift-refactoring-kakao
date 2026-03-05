package gift.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gift.member.Member;
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
    public OrderResponse createOrder(Member member, OrderRequest request) {
        var option = optionRepository.findById(request.optionId())
            .orElseThrow(() -> new java.util.NoSuchElementException("옵션을 찾을 수 없습니다: " + request.optionId()));

        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        var price = option.calculateTotalPrice(request.quantity());
        member.deductPoint(price);
        memberRepository.save(member);

        var saved = orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));

        sendKakaoMessageIfPossible(member, saved, option);
        return OrderResponse.from(saved);
    }

    private void sendKakaoMessageIfPossible(Member member, Order order, gift.option.Option option) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            var product = option.getProduct();
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception e) {
            log.warn("메시지 전송에 실패했습니다: orderId={}, memberId={}", order.getId(), member.getId(), e);
        }
    }
}
