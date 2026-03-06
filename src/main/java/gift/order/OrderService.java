package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.wish.WishRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final WishRepository wishRepository;
    private final KakaoMessageClient kakaoMessageClient;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        MemberRepository memberRepository,
        WishRepository wishRepository,
        KakaoMessageClient kakaoMessageClient
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.wishRepository = wishRepository;
        this.kakaoMessageClient = kakaoMessageClient;
    }

    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(Member member, OrderRequest request) {
        Option option = optionRepository.findById(request.optionId())
            .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. id=" + request.optionId()));

        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        int price = option.calculateTotalPrice(request.quantity());
        member.deductPoint(price);
        memberRepository.save(member);

        Order saved = orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));

        wishRepository.deleteByMemberIdAndProductId(member.getId(), option.getProduct().getId());

        sendKakaoMessageIfPossible(member, saved, option);
        return saved;
    }

    private void sendKakaoMessageIfPossible(Member member, Order order, Option option) {
        if (member.hasNoKakaoAccessToken()) {
            return;
        }
        try {
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, option);
        } catch (Exception ignored) {
        }
    }
}
