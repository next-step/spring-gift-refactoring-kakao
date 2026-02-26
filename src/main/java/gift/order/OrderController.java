package gift.order;

import gift.auth.AuthenticationResolver;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final AuthenticationResolver authenticationResolver;
    private final KakaoMessageClient kakaoMessageClient;

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
        @RequestHeader("Authorization") String authorization,
        Pageable pageable
    ) {
        // auth check
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        Page<OrderResponse> orders = orderRepository.findByMemberId(member.getId(), pageable).map(OrderResponse::from);
        return ResponseEntity.ok(orders);
    }

    /*
     * Creates an order: validates the option, subtracts stock, deducts member points,
     * persists the order, and sends a best-effort Kakao notification.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody OrderRequest request
    ) {
        // auth check
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        // validate option
        Option option = optionRepository.findById(request.optionId()).orElse(null);
        if (option == null) {
            return ResponseEntity.notFound().build();
        }

        // subtract stock
        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        // deduct points
        int price = option.getProduct().getPrice() * request.quantity();
        member.deductPoint(price);
        memberRepository.save(member);

        // save order
        Order saved = orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));

        // Todo: cleanup wish 구현 필요

        // best-effort kakao notification
        sendKakaoMessageIfPossible(member, saved, option);
        return ResponseEntity.created(URI.create("/api/orders/" + saved.getId()))
            .body(OrderResponse.from(saved));
    }

    private void sendKakaoMessageIfPossible(Member member, Order order, Option option) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            Product product = option.getProduct();
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception ignored) {
        }
    }
}
