package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.NoSuchElementException;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final KakaoMessageClient kakaoMessageClient;
    private final TransactionTemplate transactionTemplate;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        MemberRepository memberRepository,
        KakaoMessageClient kakaoMessageClient,
        TransactionTemplate transactionTemplate
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.kakaoMessageClient = kakaoMessageClient;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(readOnly = true)
    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    public Order createOrder(Member member, Long optionId, int quantity, String message) {
        Order saved = transactionTemplate.execute(status -> {
            Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. id=" + optionId));

            option.subtractQuantity(quantity);
            optionRepository.save(option);

            member.deductPoint(option.calculateTotalPrice(quantity));
            memberRepository.save(member);

            return orderRepository.save(new Order(option, member.getId(), quantity, message));
        });

        sendKakaoMessageIfPossible(member, saved);

        return saved;
    }

    private void sendKakaoMessageIfPossible(Member member, Order order) {
        if (!member.hasKakaoAccessToken()) {
            return;
        }
        try {
            Product product = order.getOption().getProduct();
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception e) {
            log.warn("카카오 메시지 전송 실패: orderId={}, memberId={}", order.getId(), member.getId(), e);
        }
    }
}
