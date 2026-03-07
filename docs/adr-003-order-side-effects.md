# ADR-003: 주문 부수 효과 처리 전략

**상태**: 결정됨
**날짜**: 2026-03-04

---

## 맥락

주문 생성 흐름에는 핵심 작업과 부수 효과가 섞여 있다.

```
현재 OrderController.createOrder():
  1. 인증 확인                    ← 핵심
  2. orderService.createOrder()   ← 핵심 (@Transactional)
     - 옵션 검증
     - 재고 차감
     - 포인트 차감
     - 주문 저장
  3. // TODO: cleanup wish        ← 부수 효과 (미구현)
  4. kakaoMessaging.send()        ← 부수 효과 (try-catch ignore)
  5. 응답 반환                    ← 핵심
```

이 중 위시 정리(3단계)가 TODO로 남아 있고, 카카오 알림(4단계)은 `// send kakao notification if possible`이라는 주석과 함께 try-catch로 실패를 무시하고 있다.

### 두 가지 결정이 필요하다

1. **위시 정리의 트랜잭션 경계** — 주문 트랜잭션에 포함할지 분리할지
2. **부수 효과 실행 방식** — Controller에 직접 작성할지, 다른 방식을 쓸지

---

## 결정 1: 위시 정리 트랜잭션 분리

### 사용자 관점 분석

위시는 "나중에 사려고 찜해둔 것"이다. 주문은 Option(상품의 옵션) 단위로 하지만, 위시는 Product(상품) 단위로 걸린다.

주문 후 위시가 정리되지 않으면 어떤 일이 벌어지는가:

| 시나리오                     | 결과                     | 심각도        |
|--------------------------|------------------------|------------|
| 이미 산 상품이 위시에 남아있음        | 사용자가 직접 삭제하면 됨         | 낮음 — UX 불편 |
| 같은 상품의 다른 옵션을 더 사고 싶은 경우 | 위시가 남아있으면 자연스러움        | 해당 없음      |
| 위시 목록이 "구매 이력"처럼 오염됨     | 주문 내역 조회가 별도로 있어 혼동 낮음 | 낮음         |

**위시 미정리는 불편함(UX)이지 기능 장애가 아니다.** 위시 정리 실패가 주문을 롤백시킬 만큼 심각하지 않다.

### 책임 분리

OrderService의 책임은 "주문을 체결하는 것"이다. 위시를 정리하는 것은 OrderService의 책임이 아니다. 트랜잭션을 하나로 묶으면 OrderService가 위시 정리까지 책임지게 되어 역할이 비대해진다.

### 결정

위시 정리는 **주문 트랜잭션에 포함하지 않는다.** 별도 트랜잭션으로 분리하고, 실패 시 주문에 영향을 주지 않는다.

---

## 결정 2: Spring ApplicationEvent로 부수 효과 분리

### 현재 코드가 말하는 설계 의도

기존 코드에 설계 의도가 이미 드러나 있다:

```java
// send kakao notification if possible   ← "가능하면" 보낸다
try {
    kakaoMessagingService.sendDefaultTemplateMessageTo(memberId, orderMessageDto);
} catch (Exception ignored) {            ← 실패해도 무시
}
```

`if possible` + `ignored`는 **"카카오 메시지는 주 흐름이 아니다"**라는 의도이다. 위시 정리도 동일하다. 즉, 주문 생성의 부수 효과들은 모두 "실패해도 주문 응답은 나가야 한다"는 성격을 공유한다.

### 문제: 주 흐름이 아닌 것이 주 흐름에 보인다

현재 Controller에는 카카오 알림 코드가 8줄을 차지하며 주 흐름(인증 → 주문 → 응답)을 방해한다. 위시 정리를 추가하면 더 늘어난다. **주 흐름이 아닌 코드가 주 흐름과 같은 위치에 있는 것**이 오히려 문제이다.

### 검토한 선택지

#### A. Controller에 직접 작성

```java
OrderResponse response = orderService.createOrder(memberId, request);

try { wishCleanupService.cleanup(memberId, productId); }
catch (Exception ignored) {}

try { kakaoMessagingService.send(memberId, orderId); }
catch (Exception ignored) {}

return ResponseEntity.created(...).body(response);
```

- 흐름이 위에서 아래로 한눈에 보임
- 부수 효과가 늘어날수록 Controller가 비대해짐
- 주 흐름이 아닌 코드가 주 흐름 사이에 끼어있음

#### B. Spring ApplicationEvent

```java
// OrderService에서 이벤트 발행
eventPublisher.publishEvent(new OrderCreatedEvent(memberId, productId, orderId));

// 각 리스너가 독립적으로 처리
@TransactionalEventListener(phase = AFTER_COMMIT)
void onOrderCreated(OrderCreatedEvent event) { ...}
```

- Controller에는 주 흐름만 남음
- 부수 효과 추가 = 리스너 추가 (기존 코드 수정 없음, OCP)
- 흐름 추적 시 리스너를 찾아야 함 (가시성 감소)
- ApplicationEventPublisher는 Spring 코어에 포함 (추가 의존성 없음)

### A의 가시성 장점이 실제로 장점인가?

A의 "흐름이 보인다"는 장점은 재검토가 필요하다. 이 부수 효과들은 기획 자체에서 "실패해도 된다"고 정의된 것이다. **주 흐름이 아닌 것이 주 흐름에 보이는 것은 가시성이 아니라 오염이다.**

B를 적용하면:

```
Controller: 인증 → 주문 생성 → 응답           ← 주 흐름 (항상 성공해야 함)
이벤트 리스너: 위시 정리, 카카오 알림            ← 부수 효과 (실패 허용)
```

Controller가 표현하는 것이 정확히 주 흐름만 된다.

### 결정: B. Spring ApplicationEvent

---

## 구현 구조

### 이벤트

```java
// order/OrderCreatedEvent.java (order 패키지 루트 — 공개)
public record OrderCreatedEvent(Long memberId, Long productId, Long orderId) {

}
```

### 발행 (OrderService)

```java
@Transactional
public OrderResponse createOrder(Long memberId, OrderRequest request) {
    // 기존 로직: validate → subtract → deduct → save

    eventPublisher.publishEvent(
            new OrderCreatedEvent(memberId, product.getId(), newEntity.getId())
    );

    return OrderResponse.from(newEntity);
}
```

이벤트는 `@Transactional` 안에서 발행되지만, `@TransactionalEventListener(phase = AFTER_COMMIT)` 리스너는 **트랜잭션이 커밋된 후** 실행된다. 주문이 롤백되면 리스너는 호출되지 않는다.

### 위시 정리 리스너

```java
// wish/internal/WishCleanupService.java
@Service
@RequiredArgsConstructor
public class WishCleanupService {

    private final WishRepository wishRepo;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            wishRepo.findByMemberIdAndProductId(event.memberId(), event.productId())
                    .ifPresent(wishRepo::delete);
        } catch (Exception e) {
            log.warn("위시 정리 실패: memberId={}, productId={}",
                    event.memberId(), event.productId(), e);
        }
    }
}
```

리스너의 `@Transactional`은 주문 트랜잭션과 별개의 자체 트랜잭션을 생성한다.

### 카카오 알림 리스너

```java
// order/internal/KakaoNotificationEventListener.java
@Component
@RequiredArgsConstructor
public class KakaoNotificationEventListener {

    private final OrderMessageBuilder orderMessageBuilder;
    private final KakaoMessagingService kakaoMessagingService;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            OrderMessageDto dto = orderMessageBuilder.buildFrom(event.orderId());
            kakaoMessagingService.sendDefaultTemplateMessageTo(event.memberId(), dto);
        } catch (Exception ignored) {
        }
    }
}
```

### Controller (변경 후)

```java
@PostMapping
public ResponseEntity<OrderResponse> createOrder(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody OrderRequest request
) {
    Long memberId = authenticationPort.getMemberIdFrom(authorization)
            .orElseThrow(UnauthorizedException::new);

    OrderResponse response = orderService.createOrder(memberId, request);

    return ResponseEntity
            .created(URI.create("/api/orders/" + response.id()))
            .body(response);
}
```

Controller에서 `OrderMessageBuilder`, `KakaoMessagingService` 의존성이 제거된다.

### 도메인 의존성 방향

```
order → OrderCreatedEvent (발행)
wish → OrderCreatedEvent (구독)
```

order 도메인이 wish 도메인을 모른다. Port 없이 이벤트로 디커플링된다. 따라서 plan-0.md에서 계획했던 WishCommandPort는 위시 정리 목적으로는 불필요하다.

---

## 리스너 실패 처리

### 검토한 선택지

| 선택지                     | 설명                     | 판단              |
|-------------------------|------------------------|-----------------|
| A. try-catch + 로깅       | 각 리스너에서 catch하고 로그만 남김 | **채택**          |
| B. 공통 EventErrorHandler | 실패 처리 정책을 한 곳에 모음      | 리스너 2개에 추상화는 과도 |
| C. 실패 이벤트 테이블           | DB에 저장 + 스케줄러 재처리      | 현재 규모에 불필요      |

### 결정: A. try-catch + 로깅

현재 리스너가 2개이고, 실패 시 영향도가 낮다. B는 리스너가 늘어나거나 실패 추적이 필요해질 때 도입해도 A → B 전환 비용이 낮다 (catch 내용만 교체). 미리 추상화할 필요 없다.

---

## 트레이드오프 요약

| 항목      | 얻는 것                         | 잃는 것                        |
|---------|------------------------------|-----------------------------|
| 트랜잭션 분리 | 위시 정리 실패가 주문에 영향 없음          | 주문 성공 후 위시가 남을 수 있음 (UX 불편) |
| 이벤트 방식  | Controller 단순화, OCP, 주 흐름 분리 | 전체 흐름 추적 시 리스너 탐색 필요        |
| 로깅만     | 구현 단순, 과도한 인프라 없음            | 실패 이벤트 재처리 불가 (수동 확인 필요)    |
