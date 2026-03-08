package gift.order;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gift.member.Member;
import gift.member.MemberNotFoundException;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.wish.WishRepository;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
	private final WishRepository wishRepository;
    private final MemberRepository memberRepository;
    private final MessageClient messageClient;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
		WishRepository wishRepository,
        MemberRepository memberRepository,
        MessageClient messageClient
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
		this.wishRepository = wishRepository;
        this.memberRepository = memberRepository;
        this.messageClient = messageClient;
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable).map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse create(Member member, OrderRequest request) {
        // validate option
        Option option = optionRepository.findByIdForUpdate(request.optionId())
            .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다."));

        // subtract stock
        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        // deduct points
        Member lockedMember = memberRepository.findByIdForUpdate(member.getId())
            .orElseThrow(() -> new MemberNotFoundException(member.getId()));
        int price = option.getProduct().getPrice() * request.quantity();
        lockedMember.deductPoint(price);
        memberRepository.save(lockedMember);

        // save order
        Order saved = orderRepository.save(new Order(option, lockedMember.getId(), request.quantity(), request.message()));

        // cleanup wish
        wishRepository.findByMemberIdAndProductId(lockedMember.getId(), option.getProduct().getId())
            .ifPresent(wishRepository::delete);

        return OrderResponse.from(saved);
    }

    public void sendKakaoMessageIfPossible(Member member, Long orderId) {
        if (!member.hasKakaoAccessToken()) {
            return;
        }
        try {
            Order order = orderRepository.findById(orderId).orElseThrow();
            Product product = order.getOption().getProduct();
            messageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception e) {
            log.warn("카카오 메시지 전송 실패: orderId={}, memberId={}", orderId, member.getId(), e);
        }
    }
}
