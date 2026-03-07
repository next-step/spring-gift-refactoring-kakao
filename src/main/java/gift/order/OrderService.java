package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.OptionRepository;
import gift.wish.WishRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final WishRepository wishRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        MemberRepository memberRepository,
        WishRepository wishRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.wishRepository = wishRepository;
        this.eventPublisher = eventPublisher;
    }

    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    // order flow:
    // 1. auth check
    // 2. validate option
    // 3. subtract stock
    // 4. deduct points
    // 5. save order
    // 6. cleanup wish
    // 7. send kakao notification
    @Transactional
    public Order createOrder(Member member, OrderRequest request) {
        // validate option
        var option = optionRepository.findById(request.optionId())
            .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. optionId=" + request.optionId()));

        // subtract stock
        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        // create order
        var order = new Order(option, member.getId(), request.quantity(), request.message());

        // deduct points
        member.deductPoint(order.calculateTotalPrice());
        memberRepository.save(member);

        // save order
        var saved = orderRepository.save(order);

        // cleanup wish
        var productId = option.getProduct().getId();
        wishRepository.findByMemberIdAndProductId(member.getId(), productId)
            .ifPresent(wishRepository::delete);

        // publish event for best-effort kakao notification (runs after commit)
        eventPublisher.publishEvent(
            new OrderCreatedEvent(member.getKakaoAccessToken(), saved, option.getProduct())
        );
        return saved;
    }
}
