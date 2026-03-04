package gift.order;

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

    @Transactional(readOnly = true)
    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(Member member, Long optionId, int quantity, String message) {
        Option option = optionRepository.findById(optionId)
            .orElseThrow(() -> new OrderException(OrderErrorCode.OPTION_NOT_FOUND));

        option.subtractQuantity(quantity);
        optionRepository.save(option);

        int price = option.getProduct().getPrice() * quantity;
        member.deductPoint(price);
        memberRepository.save(member);

        Order saved = orderRepository.save(
            new Order(option, member.getId(), quantity, message)
        );

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
            // best-effort: 카카오 메시지 전송 실패는 무시
        }
    }
}
