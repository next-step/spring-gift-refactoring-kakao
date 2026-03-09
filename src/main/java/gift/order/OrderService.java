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
    private final WishRepository wishRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        WishRepository wishRepository,
        MemberRepository memberRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.wishRepository = wishRepository;
        this.memberRepository = memberRepository;
        this.eventPublisher = eventPublisher;
    }

    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(Member member, OrderRequest request) {
        var option = optionRepository.findById(request.optionId())
            .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. optionId=" + request.optionId()));

        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        member.deductPoint(option.calculateAmount(request.quantity()));
        memberRepository.save(member);

        var saved = orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));

        wishRepository.deleteByMemberIdAndProductId(member.getId(), option.getProduct().getId());

        if (member.getKakaoAccessToken() != null) {
            var product = option.getProduct();
            eventPublisher.publishEvent(new OrderCompletedEvent(
                member.getKakaoAccessToken(),
                saved.getId(),
                product.getName(),
                option.getName(),
                request.quantity(),
                product.getPrice(),
                request.message()
            ));
        }

        return saved;
    }
}
