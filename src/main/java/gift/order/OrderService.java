package gift.order;

import gift.member.Member;
import gift.member.MemberService;
import gift.option.Option;
import gift.option.OptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OptionService optionService;
    private final MemberService memberService;
    private final KakaoMessageClient kakaoMessageClient;

    public OrderService(
        OrderRepository orderRepository,
        OptionService optionService,
        MemberService memberService,
        KakaoMessageClient kakaoMessageClient
    ) {
        this.orderRepository = orderRepository;
        this.optionService = optionService;
        this.memberService = memberService;
        this.kakaoMessageClient = kakaoMessageClient;
    }

    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(Member member, OrderRequest request) {
        Option option = optionService.subtractQuantity(request.optionId(), request.quantity());

        Order order = new Order(option, member.getId(), request.quantity(), request.message());
        memberService.deductPoint(member.getId(), order.getTotalPrice());

        Order saved = orderRepository.save(order);

        sendKakaoMessageIfPossible(member, saved);
        return saved;
    }

    private void sendKakaoMessageIfPossible(Member member, Order order) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order);
        } catch (Exception e) {
            log.warn("카카오 메시지 전송 실패: orderId={}, memberId={}", order.getId(), member.getId(), e);
        }
    }
}
