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
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final KakaoMessageClient kakaoMessageClient;
    private final WishRepository wishRepository;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        MemberRepository memberRepository,
        KakaoMessageClient kakaoMessageClient,
        WishRepository wishRepository
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.kakaoMessageClient = kakaoMessageClient;
        this.wishRepository = wishRepository;
    }

    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    // order flow:
    // 1. validate option
    // 2. subtract stock
    // 3. deduct points
    // 4. save order
    // 5. cleanup wish (TODO)
    // 6. send kakao notification
    @Transactional
    public Order createOrder(Member member, Long optionId, int quantity, String message) {
        // validate option
        Option option = optionRepository.findById(optionId)
            .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. id=" + optionId));

        // subtract stock
        option.subtractQuantity(quantity);
        optionRepository.save(option);

        // deduct points
        int price = option.getProduct().getPrice() * quantity;
        member.deductPoint(price);
        memberRepository.save(member);

        // save order
        Order saved = orderRepository.save(new Order(option, member.getId(), quantity, message));

        // cleanup wish
        wishRepository.deleteByMemberIdAndProductId(member.getId(), option.getProduct().getId());

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
