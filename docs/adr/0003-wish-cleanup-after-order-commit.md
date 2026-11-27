# 0003. 위시 정리는 주문 커밋 뒤 따로 하고, 실패해도 주문은 성공한다

- 날짜: 2026-11-27
- 상태: 채택
- 관련: [`docs/open-questions.md`](../open-questions.md) 8번, 9번 / [`docs/work/08-wish-cleanup/tech-spec.md`](../work/08-wish-cleanup/tech-spec.md) / [ADR 0001](0001-order-transaction-boundary.md), [ADR 0002](0002-order-notification-outbox.md)

## 맥락

- `OrderService`의 주문 흐름 주석에는 `6. cleanup wish`가 있지만, 위시를 정리하는 코드가 없었다. 주문한 상품이 위시에 그대로 남았다(open-questions 8번).
- 주문하기는 ADR 0001로 한 트랜잭션이다. 위시 정리를 그 안에 넣으면, 위시 정리가 실패할 때 주문까지 롤백된다.
- 주문 알림(ADR 0002)은 카카오 액세스 토큰이 있는 회원에게만 이벤트를 발행한다. 위시 정리는 모든 주문에 필요하다.

## 결정

- **이벤트**: `OrderService.createOrder`는 주문을 저장한 뒤 성공한 주문마다 `OrderPlacedEvent(orderId, memberId, productId)`를 발행한다. 카카오 액세스 토큰과 상관없다.
- **커밋 뒤 정리**: `gift.wish`의 `@TransactionalEventListener(AFTER_COMMIT)` 리스너(`OrderWishCleanupListener`)가 이벤트를 받는다. 별도 빈(`OrderWishCleanupExecutor`)의 새 트랜잭션(`REQUIRES_NEW`)에서 그 회원 + 상품의 위시를 모두 지운다. `gift.order`는 `gift.wish`를 모른다.
  - 리스너와 트랜잭션 빈을 나눈 이유는 둘이다. 커밋 때 나는 예외까지 리스너의 catch가 잡아 warn 로그를 남기려는 것이고, 같은 빈 안의 호출은 프록시를 거치지 않아 `@Transactional`이 걸리지 않기 때문이다(07의 `OrderNotificationRelay`/`OrderNotificationDispatcher`와 같은 구조).
  - 응답 코드 때문은 아니다. AFTER_COMMIT 리스너는 afterCompletion에서 불리고, 여기서 난 예외는 Spring이 error 로그만 남기고 삼킨다(spring-tx 6.2 `TransactionSynchronizationUtils.invokeAfterCompletion`). 리스너 밖으로 예외가 나가도 응답은 201이다.
- **실패**: 위시 정리가 실패해도 주문은 201이다. 위시는 남고, 리스너가 warn 로그(주문 번호, 회원 번호, 상품 번호, 예외 메시지)를 남긴다.
- **응답 코드**: 바꾸지 않는다.

## 고려한 선택지

| 선택지 | 고르지 않은 이유 |
|---|---|
| 주문 트랜잭션 안에서 위시를 지우기 | 위시 정리가 실패하면 주문까지 롤백된다. 정해진 결정("실패해도 주문은 성공")과 맞지 않는다 |
| 07의 `OrderNotificationCreatedEvent`를 같이 쓰기 | 카카오 액세스 토큰이 있는 회원에게만 발행돼서, 토큰 없는 회원의 위시가 정리되지 않는다(tech-spec 4장) |

## 결과

- 주문한 상품이 그 회원의 위시에서 빠진다. 다른 회원의 같은 상품 위시와 실패한 주문의 위시는 그대로다(`wish-cleanup.feature` 16-15~16-19).
- **생기는 문제**
  - 위시 정리가 실패하면 주문한 상품이 위시에 남는다. 재시도는 없다.
  - 주문 응답이 위시 정리를 기다린다. 커밋 뒤 같은 스레드에서 하기 때문이다. 같은 커밋 뒤의 주문 알림(ADR 0002)과 함께 기다린다.
  - 주문 커밋과 위시 정리 사이에 앱이 꺼지면 위시가 남는다. 아웃박스처럼 기록하지 않는다.
  - 위시 정리와 주문 알림은 순서를 정하지 않는다.
  - W-5(16-19)는 리스너의 catch가 없어도 Spring이 예외를 삼켜 통과한다. 우리 warn 로그가 남는지는 테스트 출력으로 확인한다.
