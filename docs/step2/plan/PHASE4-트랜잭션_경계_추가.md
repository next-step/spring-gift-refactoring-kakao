# Phase 4: 트랜잭션 경계 추가 — 세부 실행 계획

> 유형: **작동 변경** (실패 시 롤백 동작 추가)
> 상태: 완료

---

## 1. 문제 정의

`OrderService.create()`에서 재고 차감 → 포인트 차감 → 주문 저장이 하나의 트랜잭션으로 묶이지 않는다.

```java
option.subtractQuantity(request.quantity());  // DB 저장
optionRepository.save(option);
member.deductPoint(price);                     // 여기서 실패하면?
memberRepository.save(member);
var saved = orderRepository.save(...);
```

**위험**: 포인트 차감 실패 시 재고만 빠져나가는 데이터 불일치 발생.

---

## 2. 동작 변경 분석

| 상황 | 변경 전 | 변경 후 |
|------|--------|--------|
| 정상 주문 | 성공 | 성공 (동일) |
| 포인트 부족 | 재고 차감됨 + 예외 → **데이터 불일치** | 전체 롤백 → **일관성 유지** |
| 재고 부족 | 예외 (DB 변경 없음) | 예외 (동일) |

### 카카오 메시지 발송 영향

`sendKakaoMessageIfPossible()`은 try-catch로 감싸져 있어 예외가 전파되지 않는다.
따라서 트랜잭션 롤백을 유발하지 않는다.

---

## 3. 변경 내용

| 파일 | 변경 |
|------|------|
| `OrderService.java` | `create()` 메서드에 `@Transactional` 어노테이션 추가 |

변경 라인: import 1줄 + 어노테이션 1줄 = 총 2줄

---

## 4. 완료 조건

```
☑ @Transactional 추가
☑ 전체 인수 테스트(20개 시나리오) 통과
☑ diff 검토 — @Transactional 추가만 포함
☑ 커밋 완료
```
