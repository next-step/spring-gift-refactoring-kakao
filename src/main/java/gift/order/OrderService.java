package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        MemberRepository memberRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable).map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse createOrder(Long memberId, OrderRequest request) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다. id=" + memberId));

        Option option = optionRepository.findById(request.optionId())
            .orElseThrow(() -> new NoSuchElementException("옵션을 찾을 수 없습니다. id=" + request.optionId()));

        option.subtractQuantity(request.quantity());

        int price = option.getProduct().getPrice() * request.quantity();
        member.deductPoint(price);

        Order saved = orderRepository.save(request.toEntity(option, member.getId()));
        eventPublisher.publishEvent(new OrderCompletedEvent(member, saved, option));
        return OrderResponse.from(saved);
    }
}
