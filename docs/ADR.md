# 1. 주문 트랜잭션에서 카카오 알림 전송 분리


## 맥락

`OrderService.createOrder()`는 `@Transactional` 내부에서 재고 차감, 포인트 차감, 주문 저장, 위시리스트 정리, **카카오 알림 전송**을 모두 수행한다.

```java
@Transactional
public Order createOrder(Member member, OrderRequest request) {
    // 옵션 검증 · 재고 차감 · 포인트 차감 · 주문 저장 · 위시리스트 정리
    ...
    // 카카오 알림 전송 (best-effort)
    sendKakaoMessageIfPossible(member, saved, option);
    return saved;
}
```

`sendKakaoMessageIfPossible()`은 `KakaoMessageClient.sendToMe()`를 통해 카카오 API(`https://kapi.kakao.com/...`)에 동기 HTTP 요청을 보낸다. `RestClient`에 타임아웃이 설정되어 있지 않다.

이 구조에는 두 가지 문제가 있다.

### 문제 1: 트랜잭션 중 Network I/O로 DB 커넥션 불필요 점유

트랜잭션이 열린 상태에서 외부 HTTP 호출이 발생하면, 카카오 API 응답을 기다리는 동안 DB 커넥션이 반환되지 않는다. 타임아웃도 없으므로 카카오 API가 느려지면 커넥션 풀이 고갈되어 주문뿐 아니라 전체 서비스가 영향을 받을 수 있다.

### 문제 2: 주문 생성과 알림 발송의 강결합

주문(핵심 도메인 로직)과 알림(부가 기능)이 하나의 트랜잭션에 결합되어 있다. 현재는 `catch (Exception ignored)`로 실패를 무시하지만, 알림 채널이 추가되거나 로직이 변경될 때마다 `createOrder()`를 수정해야 한다. 주문 로직의 변경 이유와 알림 로직의 변경 이유가 다르므로 단일 책임 원칙에 어긋난다.

## 고려한 옵션들

### 옵션 1: 컨트롤러에서 알림 호출 분리

`OrderController`에서 주문 생성 후 알림 전송을 별도로 호출한다.

```java
@PostMapping
public ResponseEntity<OrderResponse> createOrder(...) {
    var order = orderService.createOrder(member, request);
    notificationService.sendKakaoMessage(member, order); // 트랜잭션 밖
    return ResponseEntity.ok(toResponse(order));
}
```

**장점**
- 구현이 단순하다.
- 트랜잭션 밖에서 호출되므로 DB 커넥션 점유 문제가 해결된다.

**단점**
- 컨트롤러가 비즈니스 흐름을 알아야 한다 (알림 전송은 프레젠테이션 계층의 관심사가 아님).
- 주문을 생성하는 다른 진입점(배치, 내부 서비스 호출 등)이 생기면 알림 로직을 중복 호출해야 한다.

### 옵션 2: `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`

주문 생성 시 도메인 이벤트를 발행하고, 별도의 리스너가 커밋 후 비동기로 알림을 전송한다.

```java
// OrderService
@Transactional
public Order createOrder(Member member, OrderRequest request) {
    ...
    eventPublisher.publishEvent(new OrderCompletedEvent(member, saved, option));
    return saved;
}

// OrderNotificationListener
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
public void handle(OrderCompletedEvent event) {
    kakaoMessageClient.sendToMe(...);
}
```

**장점**
- 트랜잭션 커밋 후 실행되므로 DB 커넥션 점유 문제가 해결된다.
- 주문과 알림이 이벤트를 통해 느슨하게 결합된다. 알림 채널 추가 시 리스너만 추가하면 된다.
- 진입점이 여러 개여도 이벤트 발행 한 곳에서 처리된다.
- Spring 기본 기능만으로 구현 가능하다 (외부 인프라 불필요).

**단점**
- `@Async`를 위한 스레드 풀 설정이 필요하다 (`@EnableAsync`, `TaskExecutor` 빈).
- 알림 실패 시 재시도 전략을 직접 구현해야 한다.
- 비동기 처리로 인해 디버깅이 다소 어려워진다.

### 옵션 3: 메시지 큐 (Kafka / RabbitMQ)

주문 완료 이벤트를 메시지 큐에 발행하고, 별도 컨슈머가 알림을 전송한다.

**장점**
- 재시도, 순서 보장, 배압(backpressure) 등을 큐 인프라가 제공한다.
- 알림 서비스를 독립 배포 가능한 별도 모듈로 분리할 수 있다.

**단점**
- 외부 인프라(Kafka/RabbitMQ) 운영 부담이 크다.
- 현재 프로젝트 규모 대비 과도한 복잡도이다.
- 로컬 개발·테스트 환경 구성이 복잡해진다.

## 결정

**옵션 2: `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`를 선택한다.**

근거:
- 외부 인프라 없이 Spring 기본 기능만으로 두 가지 문제(커넥션 점유, 강결합)를 모두 해결한다.
- 옵션 1은 계층 책임 위반 문제가 있고, 옵션 3은 현재 규모에 과도하다.
- 향후 트래픽이 증가하여 옵션 3이 필요해지더라도, 이벤트 기반 구조가 이미 갖춰져 있어 전환이 용이하다.

## 결과

### 긍정적 영향
- 카카오 API 지연이 DB 커넥션 풀에 영향을 주지 않는다.
- 주문 로직과 알림 로직이 분리되어 각각 독립적으로 변경·테스트할 수 있다.
- 새로운 알림 채널(SMS, 이메일 등) 추가 시 리스너만 추가하면 된다.

### 부정적 영향
- `@EnableAsync`와 `TaskExecutor` 설정이 추가로 필요하다.
- 알림 전송 실패 시 재시도 메커니즘을 직접 구현해야 한다 (현재는 best-effort이므로 당장은 불필요).
- 비동기 처리로 인해 알림 전송 시점이 주문 응답 이후로 약간 지연된다 (사용자 체감 영향 없음).

# 2. 도메인 응집화

## 맥락

서비스 레이어에 계산·검증·판단 로직이 누수되어 있었다. 도메인 엔티티가 자신의 데이터를 기반으로 수행할 수 있는 행위를 서비스가 대신 처리하고 있어, 변경 이유가 분산되고 응집도가 낮아진 상태였다.

대표적인 누수 사례:

```java
// OrderService — 가격 계산을 서비스가 직접 수행
var orderAmount = option.getPrice() * request.quantity();

// MemberService — 비밀번호 검증을 서비스가 직접 수행
if (member.getPassword() == null || !member.getPassword().equals(password)) { ... }

// WishService — 소유권 검증을 서비스가 직접 수행
if (!wish.getMemberId().equals(memberId)) { ... }
```

## 개선 내역

### Option.calculateAmount(int quantity)
```java
// Before: OrderService
var orderAmount = option.getPrice() * request.quantity();
member.deductPoint(orderAmount);

// After: Option에 메서드 추가
public int calculateAmount(int quantity) {
    return getPrice() * quantity;
}
// OrderService 호출부
member.deductPoint(option.calculateAmount(request.quantity()));
```

### Member.authenticate(String password)
```java
// Before: MemberService
if (member.getPassword() == null || !member.getPassword().equals(password)) {
    throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
}

// After: Member에 메서드 추가
public void authenticate(String password) {
    if (this.password == null || !this.password.equals(password)) {
        throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
// MemberService 호출부
member.authenticate(password);
```

### Wish.validateOwnership(Long memberId)
```java
// Before: WishService
if (!wish.getMemberId().equals(memberId)) {
    throw new IllegalStateException("본인의 위시만 삭제할 수 있습니다.");
}

// After: Wish에 메서드 추가
public void validateOwnership(Long memberId) {
    if (!this.memberId.equals(memberId)) {
        throw new IllegalStateException("본인의 위시만 삭제할 수 있습니다.");
    }
}
// WishService 호출부
wish.validateOwnership(memberId);
```

## 결정

도메인 엔티티에 행위를 이동하는 방식을 선택했다. 근거:

- **Tell, Don't Ask 원칙**: 객체의 상태를 꺼내서 외부에서 판단하는 대신, 객체에게 행위를 요청한다.
- **변경 지점 최소화**: 가격 계산 규칙(할인, 세금), 인증 방식(해싱), 소유권 규칙이 변경될 때 도메인 엔티티 한 곳만 수정하면 된다.
- **점진적 적용**: 기존 동작을 변경하지 않으면서 메서드 추출만으로 달성할 수 있어 리스크가 낮다.

## 결과

### 긍정적 영향
- 서비스 레이어가 얇아져 비즈니스 흐름(조율)에 집중할 수 있다.
- 도메인 로직이 엔티티에 응집되어 단위 테스트가 용이하다.
- 동일한 검증·계산이 필요한 새로운 서비스가 추가되어도 도메인 메서드를 재사용할 수 있다.

### 부정적 영향
- 엔티티에 메서드가 추가되어 JPA 엔티티가 순수 데이터 객체보다 약간 커진다.
- 팀 내 "엔티티는 getter만 가진다"는 관행이 있다면 합의가 필요하다.
