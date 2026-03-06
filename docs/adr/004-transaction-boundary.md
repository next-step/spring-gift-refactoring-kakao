# ADR-004: 트랜잭션 경계 설정

## 상태
- [x] 승인됨 (2026-03)

## 맥락
`OrderService.createOrder()` 메서드에서 여러 저장 작업이 개별 트랜잭션으로 실행되어, 중간 단계에서 예외 발생 시 데이터 불일치가 발생할 수 있었다.

### 기존 흐름
```
1. 재고 차감 → save (트랜잭션 A)
2. 포인트 차감 → save (트랜잭션 B) ← 여기서 예외 발생 시
3. 주문 저장 → save (트랜잭션 C)
```

### 문제 시나리오
포인트가 부족한 상태에서 주문 시:
- 1번: 재고 차감 완료 (커밋됨)
- 2번: 포인트 부족으로 `IllegalArgumentException` 발생
- **결과**: 재고만 줄고, 주문은 없는 데이터 불일치

## 선택지

### 1. 순서 변경 (검증 먼저)
- 포인트 차감을 재고 차감보다 먼저 실행
- 단점: 다른 실패 시나리오에서 동일 문제 발생 가능

### 2. @Transactional 적용 (선택)
- 메서드 전체를 하나의 트랜잭션으로 묶음
- 장점: 모든 실패 시나리오에서 원자성 보장

## 결정
`OrderService.createOrder()`에 `@Transactional` 적용

```java
@Transactional
public Optional<OrderResponse> createOrder(Member member, OrderRequest request) {
    // 재고 차감, 포인트 차감, 주문 저장이 하나의 트랜잭션
}
```

## 근거
- 재고 차감, 포인트 차감, 주문 생성은 논리적으로 하나의 작업
- 실패 시 전체 롤백되어야 데이터 일관성 유지
- 테스트로 원자성 검증 완료

## 주의사항

### 카카오 메시지 전송
- `sendKakaoMessageIfPossible()`은 트랜잭션 내부에서 호출됨
- 이미 try-catch로 감싸져 있어 외부 API 실패가 롤백을 유발하지 않음
- 추가 조치 불필요

## 결과
- 76개 테스트 통과 (기존 75개 + 롤백 검증 테스트 1개)
- 부분 실패 시 전체 롤백 보장

## 관련 ADR
- [ADR-001: 리팩토링 전략](001-refactoring-strategy.md)
- [ADR-002: 서비스 계층 추출](002-service-layer-extraction.md)
