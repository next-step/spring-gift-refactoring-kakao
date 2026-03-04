package gift.order;

import gift.error.EntityNotFoundException;
import gift.error.ForbiddenException;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
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

    public Page<Order> getOrders(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    public Order getOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("주문이 존재하지 않습니다."));
        if (!order.getMemberId().equals(memberId)) {
            throw new ForbiddenException("해당 주문에 접근할 수 없습니다.");
        }
        return order;
    }

    @Transactional
    public Order createOrder(Member member, OrderRequest request) {
        // validate option
        Option option = optionRepository.findById(request.optionId())
            .orElseThrow(() -> new EntityNotFoundException("옵션이 존재하지 않습니다."));

        // subtract stock
        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        // deduct points
        int price = option.getProduct().getPrice() * request.quantity();
        member.deductPoint(price);
        memberRepository.save(member);

        // save order
        Order saved = orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));

        // best-effort kakao notification
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
        } catch (Exception ignored) {
        }
    }
}
