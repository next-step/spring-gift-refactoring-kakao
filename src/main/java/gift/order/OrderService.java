package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.wish.WishRepository;
import java.util.NoSuchElementException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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

    public Page<Order> getOrders(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(Member member, Long optionId, int quantity, String message) {
        Option option = optionRepository.findById(optionId)
            .orElseThrow(() -> new NoSuchElementException("Option not found"));

        option.subtractQuantity(quantity);
        optionRepository.save(option);

        member.deductPoint(option.calculatePrice(quantity));
        memberRepository.save(member);

        Order saved = orderRepository.save(new Order(option, member.getId(), quantity, message));

        wishRepository.findByMemberIdAndProductId(member.getId(), option.getProduct().getId())
            .ifPresent(wishRepository::delete);

        if (member.getKakaoAccessToken() != null) {
            eventPublisher.publishEvent(
                new OrderCompletedEvent(member.getKakaoAccessToken(), saved, option.getProduct())
            );
        }
        return saved;
    }
}
