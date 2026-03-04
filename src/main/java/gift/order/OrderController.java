package gift.order;

import gift.auth.AuthenticationResolver;
import gift.error.CommonErrorCode;
import gift.error.CommonException;
import jakarta.validation.Valid;
import java.net.URI;
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
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            throw new CommonException(CommonErrorCode.UNAUTHORIZED);
        }
        var orders = orderService.findByMemberId(member.getId(), pageable)
            .map(OrderResponse::from);
        return ResponseEntity.ok(orders);
    }

    // order flow:
    // 1. auth check
    // 2. validate option
    // 3. subtract stock
    // 4. deduct points
    // 5. save order
    // 6. TODO: cleanup wish (미구현)
    // 7. send kakao notification
    @PostMapping
    public ResponseEntity<?> createOrder(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody OrderRequest request
    ) {
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            throw new CommonException(CommonErrorCode.UNAUTHORIZED);
        }

        var saved = orderService.createOrder(
            member, request.optionId(), request.quantity(), request.message()
        );
        return ResponseEntity.created(URI.create("/api/orders/" + saved.getId()))
            .body(OrderResponse.from(saved));
    }
}
