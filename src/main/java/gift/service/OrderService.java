package gift.service;

import gift.model.Member;
import gift.model.Option;
import gift.model.Order;
import gift.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionService optionService;
    private final MemberService memberService;
    private final KakaoNotificationService kakaoNotificationService;
    private final WishService wishService;

    public OrderService(
        OrderRepository orderRepository,
        OptionService optionService,
        MemberService memberService,
        KakaoNotificationService kakaoNotificationService,
        WishService wishService
    ) {
        this.orderRepository = orderRepository;
        this.optionService = optionService;
        this.memberService = memberService;
        this.kakaoNotificationService = kakaoNotificationService;
        this.wishService = wishService;
    }

    @Transactional(readOnly = true)
    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(Member member, Long optionId, int quantity, String message) {
        // subtract stock
        Option option = optionService.subtractQuantity(optionId, quantity);

        // deduct points
        var price = option.calculateTotalPrice(quantity);
        memberService.deductPoint(member, price);

        // save order
        var saved = orderRepository.save(new Order(option, member.getId(), quantity, message));

        // remove from wishlist if present
        wishService.removeByMemberIdAndProductId(member.getId(), option.getProduct().getId());

        // best-effort kakao notification
        kakaoNotificationService.sendOrderNotification(member, saved, option);

        return saved;
    }
}
