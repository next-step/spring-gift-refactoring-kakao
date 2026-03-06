package gift.order;

import gift.error.CommonErrorCode;
import gift.error.CommonException;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.wish.WishRepository;
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
    private final MessageClient messageClient;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        MemberRepository memberRepository,
        WishRepository wishRepository,
        MessageClient messageClient
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.wishRepository = wishRepository;
        this.messageClient = messageClient;
    }

    @Transactional(readOnly = true)
    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(Long memberId, Long optionId, int quantity, String message) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CommonException(CommonErrorCode.UNAUTHORIZED));

        Option option = optionRepository.findByIdForUpdate(optionId)
            .orElseThrow(() -> new OrderException(OrderErrorCode.OPTION_NOT_FOUND));

        option.subtractQuantity(quantity);

        Order order = new Order(option, memberId, quantity, message);
        member.deductPoint(order.getTotalPrice());

        Order saved = orderRepository.save(order);

        wishRepository.findByMemberIdAndProductId(memberId, option.getProduct().getId())
            .ifPresent(wishRepository::delete);

        sendMessageIfPossible(member, saved, option);
        return saved;
    }

    private void sendMessageIfPossible(Member member, Order order, Option option) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            Product product = option.getProduct();
            messageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception ignored) {
            // best-effort: 메시지 전송 실패는 무시
        }
    }
}
