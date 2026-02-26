package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.wish.WishRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final WishRepository wishRepository;
    private final MemberRepository memberRepository;
    private final KakaoMessageClient kakaoMessageClient;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        WishRepository wishRepository,
        MemberRepository memberRepository,
        KakaoMessageClient kakaoMessageClient
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.wishRepository = wishRepository;
        this.memberRepository = memberRepository;
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
    // 6. send kakao notification
    public Optional<OrderResponse> createOrder(Member member, OrderRequest request) {
        return optionRepository.findById(request.optionId())
            .map(option -> {
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

                return OrderResponse.from(saved);
            });
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
