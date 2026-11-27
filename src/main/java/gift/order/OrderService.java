package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import java.util.Optional;
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
    private final OrderNotificationRepository orderNotificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
            OrderRepository orderRepository,
            OptionRepository optionRepository,
            MemberRepository memberRepository,
            OrderNotificationRepository orderNotificationRepository,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.orderNotificationRepository = orderNotificationRepository;
        this.eventPublisher = eventPublisher;
    }

    public Page<OrderResponse> findOrders(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable).map(OrderResponse::from);
    }

    // order flow:
    // 1. auth check
    // 2. validate option
    // 3. subtract stock
    // 4. deduct points
    // 5. save order
    // 6. cleanup wish
    // 7. send kakao notification
    // ADR 0001: 재고·포인트·주문 저장을 한 트랜잭션으로 묶는다. 응답 코드는 500 그대로.
    @Transactional
    public Optional<Order> createOrder(Member member, OrderRequest request) {
        // validate option
        var option = optionRepository.findById(request.optionId()).orElse(null);
        if (option == null) {
            return Optional.empty();
        }

        // subtract stock
        option.subtractStock(request.quantity());
        optionRepository.save(option);

        // deduct points
        var totalPrice = option.getProduct().getPrice() * request.quantity();
        member.deductPoint(totalPrice);
        memberRepository.save(member);

        // save order
        var saved = orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));

        // 6. cleanup wish: 주문마다 발행하고, 커밋 뒤 gift.wish가 받는다(docs/work/08-wish-cleanup/tech-spec.md 4장).
        eventPublisher.publishEvent(new OrderPlacedEvent(
                saved.getId(), member.getId(), option.getProduct().getId()));

        // ADR 0002: 주문 알림을 같은 트랜잭션에 저장하고, 커밋 뒤 OrderNotificationRelay가 보낸다.
        saveOrderNotificationIfPossible(member, saved, option);
        return Optional.of(saved);
    }

    private void saveOrderNotificationIfPossible(Member member, Order order, Option option) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        var templateObject = OrderNotificationMessage.build(order, option.getProduct());
        var notification =
                orderNotificationRepository.save(new OrderNotification(order.getId(), member.getId(), templateObject));
        eventPublisher.publishEvent(new OrderNotificationCreatedEvent(notification.getId()));
    }
}
