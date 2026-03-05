package gift.order;

import gift.member.Member;
import gift.member.MemberService;
import gift.option.Option;
import gift.option.OptionService;
import gift.wish.WishService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionService optionService;
    private final MemberService memberService;
    private final WishService wishService;
    private final OrderNotificationSender notificationSender;

    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(Long memberId, Long optionId, int quantity, String message) {
        Option option = optionService.subtractQuantity(optionId, quantity);

        int price = option.calculateTotalPrice(quantity);
        Member member = memberService.deductPoint(memberId, price);

        Order saved = orderRepository.save(new Order(option, member, quantity, message));

        Long productId = option.getProduct().getId();
        wishService.findByMemberIdAndProductId(memberId, productId)
                .ifPresent(wish -> wishService.removeWish(wish.getId(), memberId));

        notificationSender.send(member, saved, option);
        return saved;
    }
}
