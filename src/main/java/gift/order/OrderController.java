package gift.order;

import gift.member.Member;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
        @RequestHeader("Authorization") String authorization,
        Pageable pageable
    ) {
        Member member = orderService.resolveMember(authorization);
        return ResponseEntity.ok(orderService.findByMember(member, pageable));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody OrderRequest request
    ) {
        Member member = orderService.resolveMember(authorization);
        OrderResponse created = orderService.create(member, request);
        return ResponseEntity.created(URI.create("/api/orders/" + created.id()))
            .body(created);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Void> handleUnauthorized(IllegalStateException e) {
        return ResponseEntity.status(401).build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
