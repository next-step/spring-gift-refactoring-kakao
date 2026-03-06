package gift.order;

import gift.auth.AuthenticatedMember;
import gift.member.Member;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        @AuthenticatedMember Member member,
        Pageable pageable
    ) {
        Page<OrderResponse> orders = orderService.getOrders(member.getId(), pageable);
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @AuthenticatedMember Member member,
        @Valid @RequestBody OrderRequest request
    ) {
        return orderService.createOrder(member, request)
            .map(response -> ResponseEntity
                .created(URI.create("/api/orders/" + response.id()))
                .body(response))
            .orElse(ResponseEntity.notFound().build());
    }
}
