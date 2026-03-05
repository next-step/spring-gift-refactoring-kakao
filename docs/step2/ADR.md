# Architecture Decision Records — 2단계

트레이드오프가 있었던 결정만 기록한다.

---

## ADR-1: OrderService.createOrder 트랜잭션 범위

### 맥락

`createOrder`는 재고 차감 → 포인트 차감 → 주문 저장 → 위시 삭제 → 카카오 알림의 5단계를 수행한다. `@Transactional`이 없어서 포인트 부족 시 재고만 차감되는 부분 반영(partial failure)이 발생했다.

### 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| A. 메서드 전체에 `@Transactional` | 단순, 원자성 보장 | 카카오 알림까지 트랜잭션에 포함 — 외부 호출이 트랜잭션을 잡고 있음 |
| B. 저장 로직만 별도 `@Transactional` 메서드로 분리 | 트랜잭션 범위 최소화 | 클래스 분리 필요 (self-invocation 문제), 복잡도 증가 |

### 결정

**A. 메서드 전체에 `@Transactional` 적용**

### 이유

- 카카오 알림은 best-effort이며 try-catch로 감싸져 있어 트랜잭션 롤백을 유발하지 않는다.
- 현재 규모에서 외부 호출이 트랜잭션을 잡는 시간이 문제가 되지 않는다.
- B는 클래스를 분리해야 하므로 구조 변경 범위가 커진다.
- 추후 카카오 알림이 무거워지면 이벤트 기반(`@TransactionalEventListener`)으로 분리할 수 있다.

---

## ADR-2: 인증 중복 제거 — 예외 기반 vs ArgumentResolver

### 맥락

`extractMember()` + null 체크 + 401 반환 패턴이 OrderController 2회, WishController 3회 반복되었다. 이 보일러플레이트를 제거해야 했다.

### 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| A. `HandlerMethodArgumentResolver`로 `@AuthMember Member member` 자동 주입 | Spring 관례에 맞음, 컨트롤러가 가장 깔끔 | WebMvcConfigurer 설정 필요, 커스텀 어노테이션 추가, 변경 범위 큼 |
| B. `extractMember()`가 예외를 던지도록 변경 + GlobalExceptionHandler에서 401 처리 | 변경 최소, 기존 구조 유지 | 흐름 제어에 예외를 사용 |
| C. Interceptor로 인증 처리 | 관심사 분리 명확 | URL 패턴 관리 필요, 인터셉터 순서 관리 |

### 결정

**B. 예외 기반 전환**

### 이유

- 변경 범위가 가장 작다. `AuthenticationResolver` 1개 수정 + `UnauthorizedException` 1개 추가 + GlobalExceptionHandler 핸들러 1개 추가로 5곳의 중복이 사라진다.
- A는 어노테이션 + ArgumentResolver + WebMvcConfigurer 최소 3개 파일 추가가 필요하다.
- "인증 실패"는 예외적 상황이므로 예외로 표현하는 것이 의미적으로도 적절하다.
- 추후 Spring Security 도입 시 인증 체계 전체가 교체되므로, 지금은 최소 변경으로 중복만 제거한다.

---

## ADR-3: 가격 계산 책임을 Order 도메인에 부여

### 맥락

`price × quantity` 계산이 OrderService(포인트 차감)와 KakaoMessageClient(메시지 구성)에 중복 존재했다.

### 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| A. `Order.getTotalPrice()` 도메인 메서드 | 계산 로직이 데이터 가까이 위치, 중복 제거 | Order가 Option→Product를 탐색해야 함 (Law of Demeter 위반) |
| B. 별도 유틸리티 메서드 `PriceCalculator.calculate(product, quantity)` | 의존 방향 단순 | 새 클래스 추가, 빈약한 도메인 |
| C. OrderService에만 통합하고 결과를 전달 | Service 계층에서 일원화 | KakaoMessageClient에 가격을 파라미터로 넘겨야 함, 시그니처 변경 |

### 결정

**A. `Order.getTotalPrice()` 도메인 메서드**

### 이유

- Order는 이미 `option` 참조를 가지고 있으므로 추가 의존이 없다.
- "주문 총 가격"은 Order의 본질적 속성이다. 외부에서 계산하는 것보다 Order가 알고 있는 것이 자연스럽다.
- Law of Demeter 위반(`option.getProduct().getPrice()`)은 존재하나, 이를 해소하려면 Order에 price 필드를 추가하는 작동 변경이 필요하므로 현 단계에서는 수용한다.

---

## ADR-4: GlobalExceptionHandler 전역 적용 시점 분리

### 맥락

1단계에서 `@ControllerAdvice`로 예외 핸들러를 통합했지만, `assignableTypes`로 기존 3개 컨트롤러에만 한정했다. 전역 적용하면 OrderController 등의 응답이 500→400으로 바뀌는 작동 변경이 발생한다.

### 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| A. 1단계에서 바로 전역 적용 | 한 번에 완료 | 구조 변경과 작동 변경이 섞임 |
| B. 1단계는 구조 변경만, 2단계에서 전역 적용 | 원칙 준수, 커밋 의도 명확 | 2단계 걸쳐 완성 |

### 결정

**B. 단계 분리**

### 이유

- "구조 변경과 작동 변경을 섞지 않는다"는 핵심 원칙을 지킨다.
- 1단계 커밋은 "핸들러 위치 통합"이라는 목적 1개, 2단계 커밋은 "전역 적용으로 500→400 변경"이라는 목적 1개로 각각 30초 안에 설명 가능하다.
- 테스트 수정도 작동 변경 커밋과 함께 묶어 의도가 명확해진다.
