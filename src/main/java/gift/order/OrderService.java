package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.wish.WishRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.NoSuchElementException;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final KakaoMessageClient kakaoMessageClient;
    private final WishRepository wishRepository;
    private final TransactionTemplate transactionTemplate;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        MemberRepository memberRepository,
        KakaoMessageClient kakaoMessageClient,
        WishRepository wishRepository,
        TransactionTemplate transactionTemplate
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.kakaoMessageClient = kakaoMessageClient;
        this.wishRepository = wishRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    // order flow:
    // 1. validate option
    // 2. subtract stock
    // 3. deduct points
    // 4. save order
    // 5. cleanup wish
    // 6. send kakao notification (after commit)
    public Order createOrder(Member member, Long optionId, int quantity, String message) {
        record OrderResult(Order order, Option option) {}

        OrderResult result = transactionTemplate.execute(status -> {
            Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. id=" + optionId));

            option.subtractQuantity(quantity);
            optionRepository.save(option);

            int price = option.calculateTotalPrice(quantity);
            member.deductPoint(price);
            memberRepository.save(member);

            Order saved = orderRepository.save(new Order(option, member.getId(), quantity, message));
            wishRepository.deleteByMemberIdAndProductId(member.getId(), option.getProduct().getId());

            return new OrderResult(saved, option);
        });

        // best-effort kakao notification (outside transaction — DB connection already released)
        sendKakaoMessageIfPossible(member, result.order(), result.option());

        return result.order();
    }

    private void sendKakaoMessageIfPossible(Member member, Order order, Option option) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, option);
        } catch (Exception e) {
            log.warn("카카오 메시지 전송 실패: memberId={}, orderId={}", member.getId(), order.getId(), e);
        }
    }
}
