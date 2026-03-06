# ADR-010: 읽기 전용 트랜잭션 추가

## 상태
- [x] 승인됨 (2026-03)

## 맥락
읽기 전용 메서드에 `@Transactional(readOnly = true)`를 적용하면 다음과 같은 최적화 효과가 있다:
- Dirty Checking 비활성화로 성능 향상
- 읽기 전용 DB 라우팅 가능 (read replica)
- 의도 명시로 코드 가독성 향상

## 결정
모든 Service의 읽기 전용 메서드에 `@Transactional(readOnly = true)`를 추가한다.

## 변경 사항

### 대상 메서드

| Service | 메서드 |
|---------|--------|
| OrderService | `getOrders()` |
| WishService | `getWishes()` |
| ProductService | `getAllProducts()`, `getProduct()`, `findAll()`, `findById()` |
| CategoryService | `getAllCategories()`, `findAll()`, `findById()` |

### 예시

```java
// Before
public Page<OrderResponse> getOrders(Long memberId, Pageable pageable) {
    return orderRepository.findByMemberId(memberId, pageable).map(OrderResponse::from);
}

// After
@Transactional(readOnly = true)
public Page<OrderResponse> getOrders(Long memberId, Pageable pageable) {
    return orderRepository.findByMemberId(memberId, pageable).map(OrderResponse::from);
}
```

## 결과
- 81개 테스트 통과
- 읽기 전용 메서드의 의도 명시
- 향후 read replica 도입 시 자동 라우팅 가능

## 관련 ADR
- [ADR-004: 트랜잭션 경계](004-transaction-boundary.md) - 쓰기 트랜잭션
