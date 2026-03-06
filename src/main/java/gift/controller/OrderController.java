package gift.controller;

import gift.auth.LoginMember;
import gift.dto.OrderRequest;
import gift.dto.OrderResponse;
import gift.model.Member;
import gift.model.Order;
import gift.service.KakaoNotificationService;
import gift.service.OrderService;
import jakarta.validation.Valid;
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
    private final KakaoNotificationService kakaoNotificationService;

    public OrderController(OrderService orderService, KakaoNotificationService kakaoNotificationService) {
        this.orderService = orderService;
        this.kakaoNotificationService = kakaoNotificationService;
    }

    @GetMapping
    public ResponseEntity<?> getOrders(
        @LoginMember Member member,
        Pageable pageable
    ) {
        var orders = orderService.findByMemberId(member.getId(), pageable).map(OrderResponse::from);
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
        @LoginMember Member member,
        @Valid @RequestBody OrderRequest request
    ) {
        Order saved = orderService.createOrder(member, request.optionId(), request.quantity(), request.message());
        kakaoNotificationService.sendOrderNotification(member, saved, saved.getOption());
        return ResponseEntity.created(URI.create("/api/orders/" + saved.getId()))
            .body(OrderResponse.from(saved));
    }
}
