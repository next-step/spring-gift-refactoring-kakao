package gift.order;

import gift.auth.Authenticated;
import gift.member.Member;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
        @Authenticated Member member,
        Pageable pageable
    ) {
        Page<OrderResponse> orders = orderService.getOrders(member.getId(), pageable).map(OrderResponse::from);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
        @Authenticated Member member,
        @PathVariable Long id
    ) {
        Order order = orderService.getOrder(member.getId(), id);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @Authenticated Member member,
        @Valid @RequestBody OrderRequest request
    ) {
        Order saved = orderService.createOrder(member, request);
        return ResponseEntity.created(URI.create("/api/orders/" + saved.getId()))
            .body(OrderResponse.from(saved));
    }
}
