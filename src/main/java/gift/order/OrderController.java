package gift.order;

import gift.auth.AuthenticationResolver;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final AuthenticationResolver authenticationResolver;

    public OrderController(
        OrderService orderService,
        AuthenticationResolver authenticationResolver
    ) {
        this.orderService = orderService;
        this.authenticationResolver = authenticationResolver;
    }

    @GetMapping
    public ResponseEntity<?> getOrders(
        @RequestHeader("Authorization") String authorization,
        Pageable pageable
    ) {
        // auth check
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var orders = orderService.findByMemberId(member.getId(), pageable).map(OrderResponse::from);
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
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var saved = orderService.createOrder(member, request);
        return ResponseEntity.created(URI.create("/api/orders/" + saved.getId()))
            .body(OrderResponse.from(saved));
    }
}
