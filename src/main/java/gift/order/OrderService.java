package gift.order;

import gift.member.Member;
import gift.member.MemberService;
import gift.option.Option;
import gift.option.OptionService;
import gift.wish.WishService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionService optionService;
    private final MemberService memberService;
    private final WishService wishService;
    private final ApplicationEventPublisher publisher;

    public OrderService(
            OrderRepository orderRepository,
            OptionService optionService,
            MemberService memberService,
            WishService wishService,
            ApplicationEventPublisher publisher) {
        this.orderRepository = orderRepository;
        this.optionService = optionService;
        this.memberService = memberService;
        this.wishService = wishService;
        this.publisher = publisher;
    }

    @Transactional(readOnly = true)
    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(Long memberId, Long optionId, int quantity, String message) {
        Option option = optionService.findById(optionId);

        option.subtractQuantity(quantity);
        optionService.save(option);

        Member member = memberService.findById(memberId);
        int price = option.getProduct().getPrice() * quantity;
        member.deductPoint(price);
        memberService.save(member);

        Order saved = orderRepository.save(new Order(option, memberId, quantity, message));

        Long productId = option.getProduct().getId();
        wishService.removeWishByMemberIdAndProductId(memberId, productId);

        publisher.publishEvent(OrderCreatedEvent.from(member, saved, option.getProduct()));

        return saved;
    }
}
