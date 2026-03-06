# ADR-001: 외부 HTTP 호출을 트랜잭션 바깥에서 처리

## 상황 (Context)

- `OrderService.createOrder()`에서 주문 저장 후 카카오 알림을 보내야 함
- `KakaoAuthService.processCallback()`에서 카카오 API로 토큰/유저 정보를 조회한 뒤 DB에 저장해야 함
- 두 경우 모두 `@Transactional` 안에서 외부 HTTP 호출을 하면 응답이 올 때까지 DB 커넥션이 점유됨
- 카카오 API 지연(100ms~수초) 또는 타임아웃 발생 시 커넥션 풀 고갈 위험

## 결정 (Decision)

외부 HTTP 호출(카카오 API)을 트랜잭션 바깥으로 분리한다.

- `OrderService`: 카카오 알림 전송을 컨트롤러로 이동
- `KakaoAuthService`: API 호출 후 DB 저장만 별도 `@Transactional` 메서드(`saveAndIssueToken`)로 분리

## 이유 (Rationale)

- DB 커넥션 점유 시간 최소화 → 커넥션 풀 고갈 방지
- 외부 API 장애가 DB 트랜잭션 타임아웃으로 전파되지 않음
- 알림 실패가 주문 롤백을 유발하지 않도록 책임 분리

## 대안

1. **`@TransactionalEventListener`로 비동기 처리** — 현 단계에서는 이벤트 인프라 도입이 과도함
2. **트랜잭션 안에서 유지** — 커넥션 점유 문제 해결 안 됨
3. **별도 스레드에서 HTTP 호출** — 에러 핸들링 복잡도 증가
