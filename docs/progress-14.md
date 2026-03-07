# Progress 14: 주문 부수 효과 — 위시 정리 + 카카오 알림 이벤트 분리

**기간**: 2026-03-05

---

## 이 문서의 목적

Phase 2 "누락된 작동 구현" — 2-1번 작업 (ADR-003).

`OrderController.createOrder()`에 두 가지 문제가 있었다:
1. **위시 정리 미구현** — `// TODO: cleanup wish` 주석만 존재
2. **카카오 알림이 Controller에 직접 작성** — 주 흐름이 아닌 부수 효과 코드가 Controller에 섞여 있음

`OrderCreatedEvent` + `@TransactionalEventListener(AFTER_COMMIT)`로 부수 효과를 분리하고, 위시 정리를 구현했다.

**변경 유형**:
- 커밋 1: 구조 변경 (refactor) — 이벤트 인프라 + 카카오 알림 리스너 추출
- 커밋 2: 작동 변경 (feat) — 위시 정리 구현

---

## 1. 설계 결정

| 항목 | 결정 | 이유 |
|---|---|---|
| 이벤트 타입 | `record OrderCreatedEvent(Long memberId, Long productId, Long orderId)` | 위시 정리에 memberId+productId, 카카오 알림에 memberId+orderId 필요 |
| 이벤트 발행 위치 | `OrderService.createOrder()` 내부 (`@Transactional` 안) | `@TransactionalEventListener(AFTER_COMMIT)`가 트랜잭션 커밋 후 실행되려면 트랜잭션 안에서 발행해야 함 |
| 리스너 phase | `AFTER_COMMIT` | 주문 롤백 시 부수 효과가 실행되면 안 됨. 기존 OR-R1, OR-R2 테스트 보장 |
| 위시 정리 구조 | EventListener + Service 별도 빈 분리 | self-invocation 문제 방지. `@Transactional`이 프록시를 통해 작동하도록 |
| 위시 정리 트랜잭션 | `@Transactional(propagation = REQUIRES_NEW)` | `afterCompletion` 콜백 내에서 기존 트랜잭션이 완료 중이므로 새 트랜잭션 필요 |
| 위시 없는 주문 | `orElseThrow(NotFoundException)` + catch | 위시 없는 주문은 정상 케이스이지만, 예외를 통한 흐름 제어로 명시적으로 처리 |
| 카카오 알림 실패 | try-catch + 예외 삼킴 | 기존 Controller의 동작 그대로 이관. 알림 실패가 주문에 영향을 주면 안 됨 |

---

## 2. 변경 내용

### 2-1. OrderCreatedEvent — 이벤트 레코드 (신규)

```java
// order/OrderCreatedEvent.java (order 패키지 루트 — 공개)
public record OrderCreatedEvent(
        Long memberId, Long productId, Long orderId
) {}
```

패키지 루트에 배치하여 `wish` 도메인에서도 import 가능. `order → wish` 직접 의존 없이 이벤트로 디커플링.

### 2-2. OrderService — 이벤트 발행 추가

```java
private final ApplicationEventPublisher eventPublisher;

@Transactional
public OrderResponse createOrder(Long memberId, OrderRequest request) {
    // ... 기존 로직 (재고 차감 → 포인트 차감 → 주문 저장)

    eventPublisher.publishEvent(new OrderCreatedEvent(
            memberId, product.id(), newEntity.getId()
    ));

    return OrderResponse.from(newEntity);
}
```

### 2-3. KakaoNotificationEventListener — Controller에서 추출 (신규)

```java
// order/internal/KakaoNotificationEventListener.java
@Component
@RequiredArgsConstructor
public class KakaoNotificationEventListener {

    private final OrderMessageBuilder orderMessageBuilder;
    private final KakaoMessagingService kakaoMessagingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            OrderMessageDto dto = orderMessageBuilder.buildFrom(event.orderId());
            kakaoMessagingService.sendDefaultTemplateMessageTo(event.memberId(), dto);
        } catch (Exception ignored) {
        }
    }
}
```

Controller의 try-catch 블록을 그대로 이동.

### 2-4. OrderController — 정리

```java
// Before: 4개 의존성
private final OrderService orderService;
private final AuthenticationPort authenticationPort;
private final OrderMessageBuilder orderMessageBuilder;        // 제거
private final KakaoMessagingService kakaoMessagingService;    // 제거

// After: 2개 의존성
private final OrderService orderService;
private final AuthenticationPort authenticationPort;
```

`createOrder()` 메서드에서 TODO + try-catch 전체 제거. 주 흐름만 남음.

### 2-5. WishCleanUpEventListener + WishCleanUpService — 위시 정리 (신규)

```java
// wish/internal/WishCleanUpEventListener.java
@Slf4j
@Component
@RequiredArgsConstructor
public class WishCleanUpEventListener {

    private final WishCleanUpService wishCleanUpService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            wishCleanUpService.cleanWishByMemberAndProductId(
                    event.memberId(), event.productId());
        } catch (NotFoundException e) {
            log.warn("No wish found with memberId={}, productId={}. No wish is deleted.",
                    event.memberId(), event.productId());
        } catch (OptimisticLockException e) {
            log.error("Failed to clean up wish on event=[{}] due to ex {}: {}",
                    event, e.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
}
```

```java
// wish/internal/WishCleanUpService.java
@Service
@RequiredArgsConstructor
public class WishCleanUpService {

    private final WishRepository wishRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanWishByMemberAndProductId(Long memberId, Long productId) {
        Wish find = wishRepo.findByMemberIdAndProductId(memberId, productId)
                .orElseThrow(NotFoundException::wishNotFound);
        wishRepo.delete(find);
    }
}
```

---

## 3. 구현 중 만난 문제들

이번 구현에서 Spring의 `@TransactionalEventListener` 관련 제약 3가지를 순서대로 만났다.

### 문제 1: `@TransactionalEventListener` + `@Transactional` 동일 메서드 금지

**증상**: 모든 인수 테스트 실패 (Spring Context 로딩 실패)

```
Caused by: java.lang.IllegalStateException
    at RestrictedTransactionalEventListenerFactory.java:52
```

**원인**: Spring 6.1+에서 `RestrictedTransactionalEventListenerFactory`가 도입되어, 하나의 메서드에 `@TransactionalEventListener`와 `@Transactional`을 동시에 사용하는 것을 금지한다. 이벤트 리스너가 트랜잭션 동기화 콜백 내에서 실행되는데, 같은 메서드에 `@Transactional`을 걸면 트랜잭션 생명주기가 예측 불가능해지기 때문.

**시도했던 접근**: 단일 클래스 내에서 이벤트 리스너 메서드 → `@Transactional` 메서드 호출로 분리.

**실패 이유**: self-invocation 문제. `this.cleanupWish()`는 Spring AOP 프록시를 거치지 않아 `@Transactional`이 적용되지 않음.

**최종 해결**: 리스너와 서비스를 **별도 빈**으로 분리. `WishCleanUpEventListener`가 `WishCleanUpService`를 주입받아 호출하면 프록시를 통해 `@Transactional`이 정상 작동.

### 문제 2: `afterCompletion` 콜백 내에서 `@Transactional(REQUIRED)` 실패

**증상**: OR-W1 인수 테스트 실패. 위시가 삭제되지 않음.

```
org.springframework.dao.InvalidDataAccessApiUsageException: Executing an update/delete query
```

**원인**: `@TransactionalEventListener(AFTER_COMMIT)` 리스너는 원본 트랜잭션의 `afterCompletion` 콜백 내에서 실행된다. 이 시점에 원본 트랜잭션은 "완료 중" 상태이며, `@Transactional(propagation = REQUIRED)`는 이 "완료 중" 트랜잭션에 참여하려다 실패한다.

**해결**: `@Transactional(propagation = Propagation.REQUIRES_NEW)` 사용. 원본 트랜잭션 상태와 무관하게 완전히 새로운 트랜잭션을 생성한다.

### 문제 3: 파생 delete 쿼리 vs `@Modifying @Query` vs find+delete

**시도 1**: `void deleteByMemberIdAndProductId(Long, Long)` — Spring Data 파생 delete 쿼리. 내부적으로 SELECT → `em.remove()` 순서로 동작하는데, `afterCompletion` 콜백 내에서 트랜잭션 없이 실행되면 `em.remove()`가 플러시되지 않음.

**시도 2**: `@Modifying @Query("DELETE FROM Wish w WHERE ...")` — 단일 DELETE SQL을 직접 실행하지만, 역시 트랜잭션 없이는 `InvalidDataAccessApiUsageException` 발생.

**시도 3**: `TransactionTemplate(REQUIRES_NEW)` + `@Modifying @Query` — 동작하지만 코드가 복잡.

**최종 해결**: 별도 빈 분리 + `@Transactional(REQUIRES_NEW)` + `findByMemberIdAndProductId` + `delete`. 기존 Repository 메서드를 그대로 활용하며, 하나의 트랜잭션 내에서 find와 delete가 같은 영속성 컨텍스트에서 동작.

### 카카오 알림은 왜 트랜잭션 문제가 없는가?

`KakaoNotificationEventListener`는 트랜잭션 문제가 발생하지 않는다:
- `orderMessageBuilder.buildFrom()` — 읽기 전용 조회 (암묵적 읽기 트랜잭션)
- `kakaoMessagingService.sendDefaultTemplateMessageTo()` — 읽기 전용 조회 + 외부 HTTP 호출

쓰기 작업이 없으므로 명시적 트랜잭션이 불필요. 만약 나중에 쓰기 트랜잭션이 필요해지면, `WishCleanUpService`와 동일한 패턴(별도 빈 + `REQUIRES_NEW`)으로 해결 가능.

---

## 4. 도메인 의존성 방향

```
order/OrderCreatedEvent          ← 이벤트 레코드 (패키지 루트, 공개)
       ↑                  ↑
       │                  │
order/internal/            wish/internal/
  OrderService             WishCleanUpEventListener
  (이벤트 발행)               (이벤트 구독)
                               ↓
  KakaoNotification        WishCleanUpService
  EventListener             (별도 트랜잭션)
  (이벤트 구독)
```

`order` 도메인이 `wish` 도메인을 모른다. Port 없이 이벤트로 디커플링.

---

## 5. 테스트

### 5-1. 단위 테스트

**OrderServiceTest** (수정) — 이벤트 발행 검증 추가

| 변경 | 내용 |
|---|---|
| Mock 추가 | `@Mock ApplicationEventPublisher eventPublisher` |
| `testCreateOrder` 수정 | `then(eventPublisher).should().publishEvent(new OrderCreatedEvent(...))` 검증 |

**KakaoNotificationEventListenerTest** (신규 — 2개)

| 테스트 | 검증 내용 |
|---|---|
| `testOnOrderCreated` | 정상 흐름 — `buildFrom` + `sendDefaultTemplateMessageTo` 호출 |
| `testOnOrderCreatedBuildFailure` | 빌드 실패 시 예외 삼킴 + 알림 미전송 |

**WishCleanUpEventListenerTest** (신규 — 1개)

| 테스트 | 검증 내용 |
|---|---|
| `testOnOrderCreated` | 리스너 → `cleanWishByMemberAndProductId(memberId, productId)` 호출 |

**WishCleanUpServiceTest** (신규 — 2개)

| 테스트 | 검증 내용 |
|---|---|
| `testCleanWishByMemberAndProductId` | 위시 존재 → `findByMemberIdAndProductId` + `delete` 호출, 예외 없음 |
| `testCleanWishByMemberAndProductIdNotFound` | 위시 없음 → `NotFoundException` 던짐 |

### 5-2. 인수 테스트 (2개 추가)

파일: `src/test/resources/features/order.feature`

| ID | 시나리오 | 유형 | 검증 내용 |
|---|---|---|---|
| OR-W1 | 주문 후 해당 상품의 위시가 정리된다 | state | 위시 등록 → 주문 → 위시 목록 재조회 → 0건 |
| OR-W2 | 주문 후 다른 상품의 위시는 유지된다 | state | 다른 상품 위시 → 주문 → 위시 목록 재조회 → 1건 유지 |

새 step definition 없음 — 모든 Given/When/Then step이 이미 존재.

---

## 6. 검증 결과

```bash
./gradlew test              # BUILD SUCCESSFUL (전체 단위 테스트 통과)
./gradlew acceptanceTest    # BUILD SUCCESSFUL (기존 87개 + 신규 2개 = 89개 시나리오)
```

---

## 7. 수정/생성 파일 목록

### 커밋 1: 구조 변경 — 이벤트 인프라 + 카카오 알림 리스너 추출

| 파일 | 변경 |
|---|---|
| `src/main/java/gift/order/OrderCreatedEvent.java` | **신규** — 이벤트 레코드 |
| `src/main/java/gift/order/internal/OrderService.java` | `ApplicationEventPublisher` 추가 + 이벤트 발행 |
| `src/main/java/gift/order/internal/KakaoNotificationEventListener.java` | **신규** — 카카오 알림 리스너 (Controller에서 추출) |
| `src/main/java/gift/order/internal/OrderController.java` | `OrderMessageBuilder`, `KakaoMessagingService` 의존성 제거 + try-catch/TODO 제거 |
| `src/test/java/gift/order/internal/OrderServiceTest.java` | `eventPublisher` mock + 이벤트 발행 검증 추가 |
| `src/test/java/gift/order/internal/KakaoNotificationEventListenerTest.java` | **신규** — 단위 테스트 2개 |

### 커밋 2: 작동 변경 — 위시 정리

| 파일 | 변경 |
|---|---|
| `src/main/java/gift/wish/internal/WishCleanUpEventListener.java` | **신규** — 위시 정리 이벤트 리스너 |
| `src/main/java/gift/wish/internal/WishCleanUpService.java` | **신규** — 위시 정리 서비스 (`REQUIRES_NEW`) |
| `src/test/java/gift/wish/internal/WishCleanUpEventListenerTest.java` | **신규** — 단위 테스트 1개 |
| `src/test/java/gift/wish/internal/WishCleanUpServiceTest.java` | **신규** — 단위 테스트 2개 |
| `src/test/resources/features/order.feature` | OR-W1, OR-W2 인수 테스트 시나리오 추가 |

---

## 8. Phase 2 누락 작동 구현 진행 현황

| 작업 | 설명 | 상태 |
|---|---|---|
| 2-1 | 주문 부수 효과 (위시 정리 + 카카오 알림 이벤트 분리) | **완료** |
| 2-2 | Option PUT (옵션 수정 엔드포인트) | **완료** |
| 2-3 | Category GET /{id} (카테고리 단건 조회) | **완료** |
| 2-4 | Product 카테고리 필터 (`?categoryId={id}`) | **완료** |

Phase 2 누락 작동 구현 **전체 완료**.
