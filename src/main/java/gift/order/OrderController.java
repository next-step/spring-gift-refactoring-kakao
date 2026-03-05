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
        Member member = authenticationResolver.extractMember(authorization);
        Page<OrderResponse> orders = orderService.findByMemberId(member.getId(), pageable).map(OrderResponse::from);
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody OrderRequest request
    ) {
        Member member = authenticationResolver.extractMember(authorization);

        Order saved = orderService.createOrder(member.getId(), request.optionId(), request.quantity(), request.message());
        return ResponseEntity.created(URI.create("/api/orders/" + saved.getId()))
                .body(OrderResponse.from(saved));
    }

}
