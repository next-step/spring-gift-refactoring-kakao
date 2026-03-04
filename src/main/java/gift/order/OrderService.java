package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import java.util.NoSuchElementException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
            OrderRepository orderRepository,
            OptionRepository optionRepository,
            MemberRepository memberRepository,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.eventPublisher = eventPublisher;
    }

    public Page<Order> getOrders(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(Long memberId, OrderRequest request) {
        final Member member =
                memberRepository.findById(memberId).orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다."));
        final Option option = optionRepository
                .findById(request.optionId())
                .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다."));

        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        final Order saved = orderRepository.save(new Order(option, memberId, request.quantity(), request.message()));

        member.deductPoint(saved.getTotalPrice());
        memberRepository.save(member);

        if (member.isKakaoLinked()) {
            eventPublisher.publishEvent(
                    new OrderCompletedEvent(member.getKakaoAccessToken(), saved, option.getProduct()));
        }
        return saved;
    }
}
