# Phase 6: 메시지 클라이언트 인터페이스 추출 — 세부 실행 계획

> 유형: **구조 변경** (외부 동작 변경 없음)
> 상태: 계획 수립
> 관련 ADR: [ADR-4: 메시지 클라이언트 인터페이스 설계](../adr/ADR4-메시지_클라이언트_인터페이스_설계.md)

---

## 1. 문제 정의

`OrderService`가 `KakaoMessageClient`(구체 클래스)에 직접 의존하고 있다.
알림 채널 변경이나 테스트 시 대체가 어려우므로 인터페이스를 추출한다.

### 현재 의존 관계

```
OrderService → KakaoMessageClient (구체 클래스)
```

### 목표 의존 관계

```
OrderService → MessageClient (인터페이스) ← KakaoMessageClient (구현체)
```

---

## 2. 동작 변경 분석

| 상황 | 변경 전 | 변경 후 |
|------|--------|--------|
| 정상 주문 | 201 | 201 (동일) |
| 카카오 메시지 발송 | KakaoMessageClient 직접 호출 | 동일 (런타임 동일 구현체) |
| 메시지 발송 실패 | catch로 무시 | 동일 |

외부 동작 변경 없음 — 순수 구조 변경.

---

## 3. 변경 내용

### 신규 파일 (1개)

| 파일 | 설명 |
|------|------|
| `MessageClient.java` | 인터페이스 — `sendToMe(String, Order, Product)` 선언 |

### 수정 파일 (2개)

| 파일 | 변경 |
|------|------|
| `KakaoMessageClient.java` | `implements MessageClient` 추가 |
| `OrderService.java` | 필드/생성자 타입을 `KakaoMessageClient` → `MessageClient`로 변경 |

---

## 4. 구현 단계

### Step 1: `MessageClient` 인터페이스 생성

`gift/order/MessageClient.java`에 현재 시그니처 그대로 선언.

### Step 2: `KakaoMessageClient`에 `implements MessageClient` 추가

### Step 3: `OrderService` 의존성을 `MessageClient`로 변경

필드 타입, 생성자 파라미터, 필드명 변경.

### Step 4: 컴파일 확인 + 전체 테스트 실행 + diff 검토 + 커밋

---

## 5. 완료 조건

```
□ MessageClient 인터페이스 생성
□ KakaoMessageClient가 MessageClient 구현
□ OrderService가 MessageClient에만 의존
□ 전체 인수 테스트(20개 시나리오) 통과
□ diff 검토 완료
□ 커밋 완료
```
