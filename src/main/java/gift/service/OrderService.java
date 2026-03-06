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

    public OrderService(
        OrderRepository orderRepository,
        OptionService optionService,
        MemberService memberService
    ) {
        this.orderRepository = orderRepository;
        this.optionService = optionService;
        this.memberService = memberService;
    }

    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(Member member, Long optionId, int quantity, String message) {
        Option option = optionService.subtractQuantity(optionId, quantity);

        var price = option.calculatePrice(quantity);
        memberService.deductPoint(member, price);

        return orderRepository.save(new Order(option, member.getId(), quantity, message));
    }
}
