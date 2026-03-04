# Step 2 ADR (Architecture Decision Records)

## ADR-001: @Transactional 적용 범위

### 상태

결정됨

### 맥락

Step 1에서 Controller → Service로 비즈니스 로직을 추출했지만 `@Transactional`은 작동 변경에 해당하므로 추가하지 않았다. Step 2에서 트랜잭션 경계를 설정해야 한다.

현재 `OrderService.createOrder()`는 재고 차감(`optionRepository.save`) → 포인트 차감(`memberRepository.save`) → 주문 저장(`orderRepository.save`) 순서로 3개의 저장소를 호출한다. 포인트 차감 단계에서 예외가 발생하면 재고만 차감된 채 남는 비정합 상태가 된다.

`KakaoAuthService.loginWithKakao()`도 회원 조회/생성 → 토큰 갱신 → 저장이 원자적으로 묶이지 않는다.

### 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| **(A) 다중 저장 메서드만** | 필요한 곳에만 적용, 과잉 방지 | 새 서비스 메서드 작성 시 판단 필요 |
| (B) 모든 서비스 메서드 | 일괄 적용으로 누락 방지 | 읽기 전용 메서드에도 불필요한 트랜잭션 오버헤드, 과잉 |

### 결정

**(A) 다중 저장 메서드만** — `OrderService.createOrder`와 `KakaoAuthService.loginWithKakao`에만 추가한다.

### 근거

- 단일 저장 메서드(`save` 1회)는 `SimpleJpaRepository.save()` 내부에 이미 `@Transactional`이 선언되어 있어 서비스 레벨에서 추가할 필요가 없다.
- 읽기 전용 메서드(`findAll`, `findById` 등)에 `@Transactional(readOnly = true)`를 붙이는 것은 성능 최적화 관점이지만, 현재 규모에서는 과잉이다.
- CLAUDE.md의 "과잉 금지" 원칙에 따라 필요한 곳에만 최소한으로 적용한다.

### 영향

- `OrderService.createOrder()` 내에서 예외 발생 시 재고·포인트·주문이 모두 롤백된다.
- `KakaoAuthService.loginWithKakao()` 내에서 예외 발생 시 회원 생성/토큰 갱신이 롤백된다.

---

## ADR-002: 가격 계산 책임 위치

### 상태

결정됨

### 맥락

현재 `OrderService.createOrder()`에서 `option.getProduct().getPrice() * quantity`로 총 금액을 계산한다. 이 계산의 책임을 어디에 둘지 결정해야 한다.

### 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| **(A) Option 엔티티** | `Option`이 이미 `product` 참조를 보유, 디미터 법칙 준수 | 엔티티에 계산 로직 추가 |
| (B) Order 엔티티 | 주문과 관련된 계산이므로 의미상 적합 | Order에 가격 필드가 없어 추가 변경 필요 |
| (C) 별도 서비스 | 계산 로직 분리 | 단순 곱셈에 서비스 클래스는 과잉 |

### 결정

**(A) Option 엔티티** — `Option.calculateTotalPrice(int quantity)` 메서드를 추가한다.

### 근거

- `Option`은 이미 `product` 참조(`@ManyToOne`)를 갖고 있어 `product.getPrice()`에 자연스럽게 접근할 수 있다.
- `option.getProduct().getPrice() * quantity`는 디미터 법칙(Law of Demeter)을 위반한다. `option.calculateTotalPrice(quantity)`로 위임하면 호출부가 `Product`의 내부 구조를 알 필요가 없다.
- `Order`는 현재 가격 필드를 갖고 있지 않아 (B)를 선택하면 스키마 변경이 필요하다.
- 단순 곱셈에 별도 서비스(C)는 과잉이다.

### 영향

- `Option`에 `calculateTotalPrice` 메서드가 추가된다.
- `OrderService`의 가격 계산 코드가 `option.calculateTotalPrice(quantity)` 한 줄로 단순화된다.
- 향후 가격 계산 로직이 복잡해져도(할인, 세금 등) `Option` 또는 별도 도메인 객체로 자연스럽게 확장할 수 있다.

---

## ADR-003: @RestControllerAdvice 적용 범위

### 상태

결정됨

### 맥락

현재 REST API 컨트롤러(`/api/...`)에서 예외가 발생하면 Spring 기본 에러 응답(500, Whitelabel Error Page 등)이 반환된다. 예외 종류에 따라 적절한 HTTP 상태 코드(400, 404 등)를 반환하도록 일원화해야 한다.

단, 관리자 MVC 컨트롤러(`/admin/...`)는 Thymeleaf 뷰를 반환하므로 JSON 에러 응답과 처리 방식이 다르다.

### 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| **(A) REST API만 적용** | `/api/...` 경로에만 JSON 에러 응답, `/admin/...`은 기존 방식 유지 | `basePackages` 또는 어노테이션으로 범위 지정 필요 |
| (B) 전체 적용 | 단일 핸들러로 통합 | MVC 컨트롤러에서 JSON 에러가 반환되어 Thymeleaf 뷰와 불일치 |

### 결정

**(A) REST API만 적용** — `@RestControllerAdvice(basePackages = ...)`로 `/api/...` 컨트롤러만 대상으로 한다.

### 근거

- REST API는 JSON 에러 응답이 필요하고, 관리자 MVC는 에러 페이지나 리다이렉트가 필요하다. 성격이 다르므로 같은 핸들러로 처리하면 안 된다.
- 관리자 컨트롤러는 개별 try-catch 또는 별도 `@ControllerAdvice`로 처리할 수 있다.

### 영향

- `GlobalExceptionHandler` 클래스가 추가되어 `NoSuchElementException` → 404, `IllegalArgumentException` → 400 등을 매핑한다.
- REST 컨트롤러의 개별 try-catch가 제거되어 코드가 단순해진다.
- 관리자 컨트롤러는 기존 방식을 유지한다.
