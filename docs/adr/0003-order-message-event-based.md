# ADR-0003: 주문 완료 메시지를 이벤트 기반으로 변경

## 상태
채택됨 (ADR-0001 대체)

## 맥락
ADR-0001에서 카카오 메시지 발송 실패 시 전체 트랜잭션을 롤백하는 정책을 채택했다.
그러나 `@Transactional` 안에서 외부 HTTP API(카카오 메시지)를 호출하면
응답을 기다리는 동안 DB 커넥션을 불필요하게 점유하는 문제가 있다.
트래픽이 증가하거나 카카오 API 응답이 느려지면 커넥션 풀 고갈로 이어질 수 있다.

## 결정
`@TransactionalEventListener(phase = AFTER_COMMIT)` 이벤트 기반으로 변경한다.

- `OrderService`는 트랜잭션 내에서 `OrderCompletedEvent`를 발행한다.
- `OrderEventListener`가 트랜잭션 커밋 후 이벤트를 수신하여 카카오 메시지를 발송한다.
- 이로써 외부 API 호출이 트랜잭션 밖에서 실행되어 DB 커넥션 점유 문제가 해소된다.

## 결과
- 카카오 메시지 발송 실패 시 주문은 유지되고 알림만 누락된다.
- DB 커넥션이 외부 API 응답을 기다리며 점유되지 않는다.
- `OrderService`에서 `KakaoMessageClient` 직접 의존이 제거되어 관심사가 분리된다.
