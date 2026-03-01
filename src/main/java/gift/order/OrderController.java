package gift.order;

import gift.auth.AuthenticationPort;
import gift.global.NotFoundException;
import gift.global.UnauthorizedException;
import gift.member.Member;
import gift.option.Option;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderOptionRepository optionRepository;
    private final OrderWishRepository wishRepository;
    private final OrderMemberRepository memberRepository;
    private final AuthenticationPort authenticationPort;
    private final KakaoMessageClient kakaoMessageClient;

    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestHeader("Authorization") String authorization,
            Pageable pageable
    ) {
        // auth check
        Long memberId = authenticationPort.getMemberIdFrom(authorization)
                .orElseThrow(UnauthorizedException::new);

        var orders = orderRepository.findByMemberId(memberId, pageable)
                .map(OrderResponse::from);
        return ResponseEntity.ok(orders);
    }

    // order flow:
    // 1. auth check
    // 2. validate option
    // 3. subtract stock
    // 4. deduct points
    // 5. save order
    // 6. cleanup wish
    // 7. send kakao notification
    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody OrderRequest request
    ) {
        // auth check
        Long memberId = authenticationPort.getMemberIdFrom(authorization)
                .orElseThrow(UnauthorizedException::new);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundException::memberNotFound);

        // validate option
        var option = optionRepository.findById(request.optionId()).orElse(null);
        if (option == null) {
            return ResponseEntity.notFound().build();
        }

        // subtract stock
        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        // deduct points
        var price = option.getProduct().getPrice() * request.quantity();
        member.deductPoint(price);
        memberRepository.save(member);

        // save order
        var saved = orderRepository.save(
                Order.builder()
                        .option(option)
                        .memberId(member.getId())
                        .quantity(request.quantity())
                        .message(request.message())
                        .build()
        );

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
            var product = option.getProduct();
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception ignored) {
        }
    }
}
