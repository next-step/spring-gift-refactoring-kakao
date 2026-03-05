# Phase 5: Order 예외 처리 개선 — 세부 실행 계획

> 유형: **작동 변경** (HTTP 응답 코드 500 → 400)
> 상태: 계획 수립

---

## 1. 문제 정의

`OrderController`가 `GlobalExceptionHandler`의 `assignableTypes`에 포함되지 않아,
비즈니스 예외(`IllegalArgumentException`)가 500으로 응답된다.

### 현재 예외 경로

| 예외 발생 지점 | 예외 메시지 | 현재 HTTP 응답 |
|--------------|-----------|---------------|
| `Option.subtractQuantity()` | "차감할 수량이 현재 재고보다 많습니다." | **500** |
| `Member.deductPoint()` | "포인트가 부족합니다." | **500** |
| `Member.deductPoint()` | "차감 금액은 1 이상이어야 합니다." | **500** |

---

## 2. 동작 변경 분석

| 상황 | 변경 전 | 변경 후 |
|------|--------|--------|
| 정상 주문 | 201 | 201 (동일) |
| 재고 부족 | **500** | **400** |
| 포인트 부족 | **500** | **400** |
| 옵션 미존재 | 404 | 404 (동일) |
| 인증 실패 | 401 | 401 (동일) |

---

## 3. 변경 내용

### 코드 변경 (1개)

| 파일 | 변경 |
|------|------|
| `GlobalExceptionHandler.java` | `assignableTypes`에 `OrderController.class` 추가 |

### 테스트 변경 (1개)

| 파일 | 변경 |
|------|------|
| `order.feature` | 재고 부족, 포인트 부족 시나리오 기대값 500 → 400 |

---

## 4. 구현 단계

### Step 1: 인수 테스트 기대값 수정 (테스트 먼저)

```gherkin
# 변경 전
시나리오: 재고보다 많은 수량을 주문하면 500을 반환한다
시나리오: 포인트가 부족할 때 주문하면 500을 반환한다

# 변경 후
시나리오: 재고보다 많은 수량을 주문하면 400을 반환한다
시나리오: 포인트가 부족할 때 주문하면 400을 반환한다
```

### Step 2: GlobalExceptionHandler 범위 확장

`assignableTypes`에 `OrderController.class` 추가.

### Step 3: 테스트 실행 + diff 검토 + 커밋

---

## 5. 완료 조건

```
□ order.feature 기대값 500 → 400 수정
□ GlobalExceptionHandler assignableTypes에 OrderController 추가
□ 전체 인수 테스트(20개 시나리오) 통과
□ diff 검토 완료
□ 커밋 완료
```
