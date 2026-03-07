package gift.order.internal;

import gift.auth.AuthenticationPort;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
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


    private final OrderService orderService;
    private final AuthenticationPort authenticationPort;

    @GetMapping
    public ResponseEntity<PagedModel<OrderResponse>> getOrders(
            @RequestHeader("Authorization") String authorization,
            Pageable pageable
    ) {
        // auth check
        Long memberId = authenticationPort.getMemberIdFrom(authorization);

        PagedModel<OrderResponse> response = orderService.getOrders(memberId, pageable);

        return ResponseEntity
                .ok(response);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody OrderRequest request
    ) {
        // auth check
        Long memberId = authenticationPort.getMemberIdFrom(authorization);

        OrderResponse response = orderService.createOrder(memberId, request);

        Long orderId = response.id();

        return ResponseEntity
                .created(URI.create("/api/orders/" + orderId))
                .body(response);
    }
}
