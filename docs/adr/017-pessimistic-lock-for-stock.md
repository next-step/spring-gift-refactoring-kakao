# ADR-017: 비관적 락으로 동시 주문 시 Overselling 방지

## 상태
- [x] 승인됨 (2026-03)

## 맥락
리뷰어가 동시 주문 시 재고 정합성 문제를 지적했다:

```java
// 기존: Race Condition 가능
option.subtractQuantity(request.quantity());
optionRepository.save(option);
```

### 문제 시나리오
```
T1: SELECT quantity FROM options WHERE id=1  → 10
T2: SELECT quantity FROM options WHERE id=1  → 10
T1: UPDATE options SET quantity=5 WHERE id=1
T2: UPDATE options SET quantity=5 WHERE id=1  ← Overselling!
```

재고 10개에서 두 트랜잭션이 각각 5개씩 주문하면, 결과적으로 5개가 남아야 하지만 둘 다 10에서 시작해 5로 업데이트하여 **5개 overselling** 발생.

## 결정
**비관적 락(PESSIMISTIC_WRITE)**을 사용하여 재고 조회 시 행 잠금을 건다.

## 대안 검토

| 방안 | 장점 | 단점 |
|------|------|------|
| 비관적 락 | 확실한 보장, 구현 단순 | 대기 시간 발생 |
| 낙관적 락 (@Version) | 대기 없음 | 충돌 시 재시도 로직 필요 |
| 원자적 UPDATE | 가장 효율적 | 도메인 로직이 쿼리로 분산 |

주문 도메인은 데이터 정합성이 핵심이므로 **비관적 락**이 가장 적합하다.

## 변경 사항

### OptionRepository.java
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT o FROM Option o WHERE o.id = :id")
Optional<Option> findByIdForUpdate(Long id);
```

### OrderService.java
```java
// Before
optionRepository.findById(request.optionId())

// After
optionRepository.findByIdForUpdate(request.optionId())
```

## 동작 방식

```
T1: SELECT ... FOR UPDATE WHERE id=1  → 행 잠금 획득, quantity=10
T2: SELECT ... FOR UPDATE WHERE id=1  → 대기 (T1 잠금 해제까지)
T1: UPDATE quantity=5, COMMIT          → 잠금 해제
T2: SELECT 재실행                       → quantity=5 읽음
T2: UPDATE quantity=0, COMMIT          → 정상 처리
```

## 결과
- 90개 테스트 통과
- 동시 주문 시 overselling 방지
- 재고 정합성 보장

## 트레이드오프
- 동시 주문 시 대기 시간 발생 (수 ms ~ 수십 ms)
- 높은 트래픽에서는 락 경합 가능 → 추후 낙관적 락이나 분산 락 검토 필요

## 관련 ADR
- [ADR-004: 트랜잭션 경계 설정](004-transaction-boundary.md)
- [ADR-016: 카카오 메시지 전송을 트랜잭션 밖으로 분리](016-kakao-message-after-commit.md)
