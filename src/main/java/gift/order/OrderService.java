package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.wish.WishRepository;
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
    private final WishRepository wishRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
            OrderRepository orderRepository,
            OptionRepository optionRepository,
            MemberRepository memberRepository,
            WishRepository wishRepository,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.wishRepository = wishRepository;
        this.eventPublisher = eventPublisher;
    }

    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(Long memberId, Long optionId, int quantity, String message) {
        Option option = findOption(optionId);
        Product product = option.getProduct();
        Member member = findMember(memberId);

        option.subtractQuantity(quantity);
        member.deductPoint(product.calculatePrice(quantity));

        Order saved = orderRepository.save(new Order(option, memberId, quantity, message));
        cleanupWish(memberId, option);
        publishOrderCompletedEvent(member, saved, product, option, quantity, message);

        return saved;
    }

    private Option findOption(Long optionId) {
        return optionRepository
                .findById(optionId)
                .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. id=" + optionId));
    }

    private Member findMember(Long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다. id=" + memberId));
    }

    private void cleanupWish(Long memberId, Option option) {
        wishRepository.findByMemberIdAndProductId(memberId, option.productId()).ifPresent(wishRepository::delete);
    }

    private void publishOrderCompletedEvent(
            Member member, Order saved, Product product, Option option, int quantity, String message) {
        member.getKakaoAccessTokenIfIntegrated()
                .ifPresent(token -> eventPublisher.publishEvent(new OrderCompletedEvent(
                        token,
                        saved.getId(),
                        quantity,
                        message,
                        option.getName(),
                        product.getName(),
                        product.getPrice())));
    }
}
