# ADR: Kakao 알림을 주문 트랜잭션 밖으로 분리

## 상태

승인됨 (2026-03-04)

## 맥락

`OrderService.createOrder()`는 DB 원자적 작업(재고 차감, 포인트 차감, 주문 저장, 위시 삭제)과 외부 API 호출(카카오 메시지 전송)을 하나의 트랜잭션 안에서 수행하고 있었다.

문제점:
- 카카오 API 호출이 느리면 DB 커넥션을 불필요하게 점유한다
- 카카오 API 실패가 try-catch로 무시되지만, 트랜잭션 내부에서 실행되므로 의미적으로 경계가 불분명하다
- 트랜잭션이 롤백되더라도 카카오 메시지가 이미 전송될 수 있다

## 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| A. ApplicationEvent + @TransactionalEventListener | Spring 표준, 디커플링, 커밋 후만 실행 | 이벤트 + 리스너 2개 파일 추가 |
| B. Controller에서 호출 | 단순 | Controller에 인프라 책임 누수 |
| C. TransactionTemplate | 단일 클래스 | 선언적/프로그래밍적 스타일 혼용 |

## 결정

**선택지 A**: `OrderCompletedEvent` + `@TransactionalEventListener(AFTER_COMMIT)`

## 경계 규칙

- **주문 트랜잭션** = DB 원자적 작업 (재고 차감 → 포인트 차감 → 주문 저장 → 위시 삭제)
- **알림** = 커밋 후 부수효과 (카카오 메시지 전송)

## 결과

- `OrderService`에서 `KakaoMessageClient` 의존성 제거
- 트랜잭션 커밋 후에만 카카오 메시지가 전송됨
- 트랜잭션 롤백 시 카카오 메시지가 전송되지 않음
