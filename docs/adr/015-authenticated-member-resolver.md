# ADR-015: HandlerMethodArgumentResolver로 인증 보일러플레이트 제거

## 상태
- [x] 승인됨 (2026-03)

## 맥락
리뷰어가 인증 관련 보일러플레이트 코드의 반복을 지적했다:

```java
// 기존: 모든 인증 필요 엔드포인트에서 반복
@GetMapping
public ResponseEntity<...> getOrders(
    @RequestHeader("Authorization") String authorization,
    Pageable pageable
) {
    return authenticationResolver.extractMember(authorization)
        .map(member -> { ... })
        .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
}
```

이 패턴이 OrderController(2곳), WishController(3곳)에서 반복되었다.

## 결정
`HandlerMethodArgumentResolver`를 사용하여 `@AuthenticatedMember` 커스텀 어노테이션으로 인증된 Member를 직접 주입한다.

## 변경 사항

### 신규 파일
- `gift/auth/AuthenticatedMember.java` - 파라미터 어노테이션
- `gift/auth/AuthenticatedMemberArgumentResolver.java` - ArgumentResolver 구현
- `gift/auth/UnauthorizedException.java` - 인증 실패 예외
- `gift/common/WebConfig.java` - ArgumentResolver 등록

### Controller 변경

```java
// After: 깔끔한 시그니처
@GetMapping
public ResponseEntity<Page<OrderResponse>> getOrders(
    @AuthenticatedMember Member member,
    Pageable pageable
) {
    Page<OrderResponse> orders = orderService.getOrders(member.getId(), pageable);
    return ResponseEntity.ok(orders);
}
```

### GlobalExceptionHandler 확장

```java
@ExceptionHandler(UnauthorizedException.class)
public ResponseEntity<String> handleUnauthorized(UnauthorizedException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
}
```

## 결과
- 90개 테스트 통과
- 인증 로직 중앙화 (5곳 → 1곳)
- Controller에서 AuthenticationResolver 의존성 제거
- 인증 실패 시 일관된 401 응답

## Before / After 비교

| 항목 | Before | After |
|------|--------|-------|
| 인증 코드 위치 | 각 Controller 메서드 | ArgumentResolver |
| Controller 의존성 | AuthenticationResolver | 없음 |
| 인증 실패 응답 | 메서드마다 처리 | GlobalExceptionHandler |
| 메서드 시그니처 | 복잡 (Optional 처리) | 단순 (Member 직접 사용) |

## 관련 ADR
- [ADR-014: 전역 예외 처리 통합](014-global-exception-handler.md)
