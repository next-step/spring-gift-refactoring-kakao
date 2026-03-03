package gift.order;

import gift.member.Member;
import gift.member.MemberService;
import gift.option.Option;
import gift.option.OptionService;
import gift.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
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

    public Order createOrder(Member member, OrderRequest request) {
        Option option = optionService.subtractQuantity(request.optionId(), request.quantity());

        int price = option.getProduct().getPrice() * request.quantity();
        memberService.deductPoint(member.getId(), price);

        Order saved = orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));

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
