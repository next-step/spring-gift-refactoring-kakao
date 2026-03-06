package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.wish.WishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

@Service
@Transactional
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final WishRepository wishRepository;
    private final MemberRepository memberRepository;
    private final KakaoMessageClient kakaoMessageClient;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        WishRepository wishRepository,
        MemberRepository memberRepository,
        KakaoMessageClient kakaoMessageClient
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.wishRepository = wishRepository;
        this.memberRepository = memberRepository;
        this.kakaoMessageClient = kakaoMessageClient;
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable).map(OrderResponse::from);
    }

    public Optional<OrderResponse> createOrder(Member member, OrderRequest request) {
        return optionRepository.findByIdForUpdate(request.optionId())
            .map(option -> {
                // subtract stock
                option.subtractQuantity(request.quantity());
                optionRepository.save(option);

                // deduct points
                long price = option.calculateTotalPrice(request.quantity());
                member.deductPoint(Math.toIntExact(price));
                memberRepository.save(member);

                // save order
                Order saved = orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));

                // cleanup wishlist
                wishRepository.findByMemberIdAndProductId(member.getId(), option.getProduct().getId())
                    .ifPresent(wishRepository::delete);

                // best-effort kakao notification (트랜잭션 커밋 후 실행)
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        sendKakaoMessageIfPossible(member, saved, option);
                    }
                });

                return OrderResponse.from(saved);
            });
    }

    private void sendKakaoMessageIfPossible(Member member, Order order, Option option) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            Product product = option.getProduct();
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception e) {
            log.warn("카카오 메시지 전송 실패: {}", e.getMessage());
        }
    }
}
