# ADR-007: Controller 가독성 개선

## 상태
- [x] 승인됨 (2026-03)

## 맥락
Optional 체이닝을 사용한 Controller 코드에서 중첩 map이 3단계까지 깊어져 가독성이 떨어졌다.

## 문제 코드

```java
return authenticationResolver.extractMember(authorization)
    .map(member -> orderService.createOrder(member, request)
        .map(response -> ResponseEntity.created(...)
            .body(response))
        .orElse(ResponseEntity.notFound().build()))
    .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
```

## 결정
중첩된 Optional 처리 로직을 private 메서드로 추출한다.

## 변경 사항

### OrderController

```java
// Before: 중첩 3단계
return authenticationResolver.extractMember(authorization)
    .map(member -> orderService.createOrder(member, request)
        .map(response -> ...)
        .orElse(...))
    .orElse(...);

// After: 메서드 추출로 중첩 1단계
return authenticationResolver.extractMember(authorization)
    .map(member -> createOrderForMember(member, request))
    .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

private ResponseEntity<OrderResponse> createOrderForMember(Member member, OrderRequest request) {
    return orderService.createOrder(member, request)
        .map(response -> ResponseEntity
            .created(URI.create("/api/orders/" + response.id()))
            .body(response))
        .orElse(ResponseEntity.notFound().build());
}
```

### WishController

동일한 패턴으로 `addWishForMember()`, `removeWishForMember()` 추출

## 결과
- 각 메서드의 중첩 단계가 3 → 1로 감소
- 인증 처리와 비즈니스 로직 응답 생성이 분리됨
- 79개 테스트 통과

## 관련 ADR
- [ADR-006: 도메인 책임 되찾기](006-domain-responsibility.md) - Optional 반환 도입
