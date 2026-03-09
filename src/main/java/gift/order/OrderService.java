package gift.order;

import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.wish.WishService;

@Transactional
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final WishService wishService;
    private final MemberRepository memberRepository;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        WishService wishService,
        MemberRepository memberRepository
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.wishService = wishService;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable).map(OrderResponse::from);
    }

    public Order create(Member member, OrderRequest request) {
        // validate option
        Option option = optionRepository.findById(request.optionId())
            .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다."));

        // subtract stock
        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        // save order
        Order order = orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));

        // deduct points
        member.deductPoint(order.calculatePrice());
        memberRepository.save(member);

        // cleanup wish
        wishService.removeByMemberAndProduct(member.getId(), option.getProduct().getId());

        return order;
    }
}
