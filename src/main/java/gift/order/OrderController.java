package gift.order;

import gift.auth.AuthenticationResolver;
import gift.member.Member;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final AuthenticationResolver authenticationResolver;

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @RequestHeader("Authorization") String authorization,
            Pageable pageable
    ) {
        Member member = authenticate(authorization);
        Page<OrderResponse> orders = orderService.findByMemberId(member.getId(), pageable).map(OrderResponse::from);
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody OrderRequest request
    ) {
        Member member = authenticate(authorization);

        try {
            Order saved = orderService.createOrder(member.getId(), request.optionId(), request.quantity(), request.message());
            return ResponseEntity.created(URI.create("/api/orders/" + saved.getId()))
                    .body(OrderResponse.from(saved));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Member authenticate(String authorization) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            throw new IllegalStateException("인증에 실패했습니다.");
        }
        return member;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Void> handleUnauthorized(IllegalStateException e) {
        return ResponseEntity.status(401).build();
    }
}
