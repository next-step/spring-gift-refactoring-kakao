package gift.order;

import gift.auth.AuthenticationResolver;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final AuthenticationResolver authenticationResolver;
    private final KakaoMessageClient kakaoMessageClient;

    public OrderController(
            OrderRepository orderRepository,
            OptionRepository optionRepository,
            MemberRepository memberRepository,
            AuthenticationResolver authenticationResolver,
            KakaoMessageClient kakaoMessageClient) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.authenticationResolver = authenticationResolver;
        this.kakaoMessageClient = kakaoMessageClient;
    }

    @GetMapping
    public ResponseEntity<?> getOrders(@RequestHeader("Authorization") String authorization, Pageable pageable) {
        // 인증 확인
        final Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        final Page<OrderResponse> orders =
                orderRepository.findByMemberId(member.getId(), pageable).map(OrderResponse::from);
        return ResponseEntity.ok(orders);
    }

    // 주문 흐름:
    // 1. 인증 확인
    // 2. 옵션 검증
    // 3. 재고 차감
    // 4. 포인트 차감
    // 5. 주문 저장
    // 6. 위시 정리
    // 7. 카카오 알림 전송
    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestHeader("Authorization") String authorization, @Valid @RequestBody OrderRequest request) {
        // 인증 확인
        final Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 옵션 검증
        final Option option = optionRepository.findById(request.optionId()).orElse(null);
        if (option == null) {
            return ResponseEntity.notFound().build();
        }

        // 재고 차감
        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        // 포인트 차감
        final int price = option.getProduct().getPrice() * request.quantity();
        member.deductPoint(price);
        memberRepository.save(member);

        // 주문 저장
        final Order saved =
                orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));

        // 카카오 알림 전송 (실패해도 주문은 유지)
        sendKakaoMessageIfPossible(member, saved, option);
        return ResponseEntity.created(URI.create("/api/orders/" + saved.getId()))
                .body(OrderResponse.from(saved));
    }

    private void sendKakaoMessageIfPossible(Member member, Order order, Option option) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            final Product product = option.getProduct();
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception ignored) {
        }
    }
}
