package gift.order;

import gift.member.Member;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.wish.WishRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final WishRepository wishRepository;
    private final OrderMessageClient messageClient;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        WishRepository wishRepository,
        OrderMessageClient messageClient
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.wishRepository = wishRepository;
        this.messageClient = messageClient;
    }

    public Page<Order> getOrders(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(Member member, Long optionId, int quantity, String message) {
        Option option = optionRepository.findById(optionId)
            .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. id=" + optionId));

        option.subtractQuantity(quantity);
        member.deductPoint(option.calculatePrice(quantity));

        Order saved = orderRepository.save(new Order(option, member.getId(), quantity, message));
        wishRepository.deleteByMemberIdAndProductId(member.getId(), option.getProduct().getId());

        sendMessageIfPossible(member, saved, option);
        return saved;
    }

    private void sendMessageIfPossible(Member member, Order order, Option option) {
        if (member.getOAuthAccessToken() == null) {
            return;
        }
        try {
            Product product = option.getProduct();
            messageClient.sendToMe(member.getOAuthAccessToken(), order, product);
        } catch (Exception e) {
            log.warn("주문 메시지 전송 실패: orderId={}, memberId={}", order.getId(), member.getId(), e);
        }
    }
}
