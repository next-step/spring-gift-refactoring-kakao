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

**(A) REST API만 적용** — `@RestControllerAdvice(annotations = RestController.class)`로 `@RestController`가 붙은 컨트롤러만 대상으로 한다.

### 근거

- REST API는 JSON 에러 응답이 필요하고, 관리자 MVC는 에러 페이지나 리다이렉트가 필요하다. 성격이 다르므로 같은 핸들러로 처리하면 안 된다.
- 처음에는 `basePackages = "gift"`로 구현했으나, 이 경우 같은 패키지의 `@Controller`(admin 컨트롤러)에도 적용되어 Thymeleaf 뷰 대신 JSON 에러가 반환되는 문제가 있었다.
- `annotations = RestController.class`로 변경하여 `@RestController`만 대상으로 제한했다.

### 영향

- `GlobalExceptionHandler` 클래스가 추가되어 `NoSuchElementException` → 404, `IllegalArgumentException` → 400, `IllegalStateException` → 403을 매핑한다.
- REST 컨트롤러의 개별 try-catch와 `@ExceptionHandler`가 제거되어 코드가 단순해진다.
- 관리자 컨트롤러(`@Controller`)는 `annotations` 필터에 해당하지 않아 기존 방식을 유지한다.

---

## ADR-004: 테스트 전략 — Mock 서비스 테스트 vs 통합 테스트

### 상태

결정됨

### 맥락

Step 2의 작동 변경을 검증할 테스트를 작성해야 한다. 서비스 계층을 어떤 방식으로 테스트할지 결정이 필요했다.

### 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| (A) Mock 기반 서비스 단위 테스트 (`@Mock` + `@InjectMocks`) | 빠른 실행, 서비스 로직 격리 | 호출 여부만 확인, 실제 DB 상태 변화 검증 불가 |
| **(B) 통합 테스트 (`@SpringBootTest` + `@Sql`) + 도메인 단위 테스트** | 실제 DB 상태를 재조회하여 검증 가능 | 실행 속도 느림, 테스트 데이터 관리 필요 |

### 결정

**(B) 통합 테스트 + 도메인 단위 테스트** — Mock 기반 서비스 단위 테스트는 사용하지 않는다.

### 근거

- 이 프로젝트의 검증 원칙은 **"상태를 재조회하여 검증"**이다. Mock 테스트는 `then(repository).should().deleteByMemberIdAndProductId(...)`처럼 메서드 호출 여부만 확인할 뿐, 실제로 DB에서 삭제되었는지는 알 수 없다.
- 통합 테스트는 API 호출 후 DB 상태를 재조회(`GET /api/wishes` → 0개)하여 실제 작동을 검증한다.
- 외부 API(`KakaoLoginClient`)처럼 테스트 환경에서 호출할 수 없는 의존성만 `@MockitoBean`으로 대체한다.
- 도메인 단위 테스트(`OptionTest`, `MemberTest`)는 외부 의존성 없이 순수 로직을 검증하므로 별도로 유지한다.

### 영향

- 컴파일 에러를 일으키는 Red 테스트(존재하지 않는 메서드 호출)는 0단계에서 선행 작성할 수 없다. 해당 메서드 구현 단계에서 함께 작성한다.
- 테스트 데이터는 `@Sql`로 관리하며, `setup-data.sql` / `cleanup.sql` 패턴을 따른다.

---

## ADR-005: 외부 HTTP 호출과 트랜잭션 경계 분리

### 상태

결정됨

### 맥락

`OrderService.createOrder()`와 `KakaoAuthService.loginWithKakao()`에 `@Transactional`을 적용했으나, 트랜잭션 내부에서 외부 HTTP 호출(카카오 API)이 실행되고 있었다. 카카오 API 응답이 느려지면 DB 커넥션을 그만큼 오래 점유하게 되어 커넥션 풀 고갈 위험이 있다.

### 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| (A) 현재 구조 유지 (`@Transactional` + 외부 호출) | 구현 단순 | DB 커넥션을 외부 호출 동안 점유 |
| **(B) `TransactionTemplate`으로 DB 작업만 감싸기** | 트랜잭션 경계가 명시적, DB 커넥션 즉시 반환 | `@Transactional` 대비 코드가 약간 복잡 |
| (C) `@TransactionalEventListener(AFTER_COMMIT)` | 관심사 분리가 깔끔 | 이벤트 클래스 + 리스너 도입으로 구조 복잡 |
| (D) 별도 서비스 클래스 분리 | Spring 프록시 문제 없음 | 클래스 증가 |

### 결정

**(B) `TransactionTemplate`** — 외부 HTTP 호출을 트랜잭션 밖에 두고, DB 작업만 `TransactionTemplate`으로 감싼다.

### 근거

- 새 클래스나 이벤트 인프라 없이 한 메서드 안에서 트랜잭션 경계를 명시적으로 제어할 수 있다.
- `@Transactional`의 self-invocation 문제(Spring 프록시 우회)를 고려할 필요가 없다.
- (C)는 이벤트 클래스 도입이 현재 규모에서 과잉이고, (D)는 클래스가 늘어난다.

### 영향

- `OrderService`: DB 작업(재고 차감·포인트 차감·주문 저장·위시 삭제)은 트랜잭션 내에서 실행되고, 카카오 알림은 커밋 후 전송된다.
- `KakaoAuthService`: 카카오 토큰 교환·사용자 조회는 트랜잭션 밖에서 실행되고, 회원 저장·토큰 갱신만 트랜잭션 내에서 실행된다.
- ADR-001의 결정(`@Transactional` 적용)을 `TransactionTemplate`으로 대체하지만, 트랜잭션 보장 범위는 동일하다.

---

## ADR-006: 동시 수정 시 Lost Update 방지 전략

### 상태

결정됨

### 맥락

`MemberService.update()`, `ProductService.update()`, `CategoryService.update()`, `Option.subtractQuantity()` 등 조회 → 상태변경 → 저장 패턴에서, 두 요청이 동시에 같은 엔티티를 조회한 뒤 저장하면 마지막 저장이 이전 변경을 덮어쓰는 lost update 문제가 발생할 수 있다.

### 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| **(A) 낙관적 락 (`@Version`)** | JPA 기본 지원, 추가 인프라 불필요, 충돌 드문 경우 성능 부담 없음 | 충돌 시 재시도 로직 필요 |
| (B) 비관적 락 (`@Lock(PESSIMISTIC_WRITE)`) | 충돌 자체를 원천 차단 | 데드락 위험, 조회 시점부터 row lock으로 성능 부담 |
| (C) 조건부 UPDATE (`WHERE point = ?`) | 특정 필드만 보호할 때 유효 | 범용적이지 않음, 엔티티마다 별도 쿼리 필요 |

### 결정

**(A) 낙관적 락 (`@Version`)** — Member, Product, Category, Option 4개 엔티티에 `@Version` 필드를 추가한다.

### 근거

- 현재 admin 관리 화면 + REST API 규모에서 동시 수정 충돌 빈도가 낮다. 낙관적 락은 충돌이 드문 경우 성능 부담 없이 데이터 정합성을 보장한다.
- JPA `@Version`만 추가하면 Hibernate가 UPDATE 시 `WHERE version = ?` 조건을 자동으로 붙여준다. 별도 쿼리나 인프라 변경이 필요 없다.
- 비관적 락(B)은 데드락 위험과 커넥션 점유 문제가 있어 현재 규모에서 과잉이다.
- 조건부 UPDATE(C)는 엔티티마다 별도 쿼리를 작성해야 하므로 범용적이지 않다.

### 영향

- 4개 엔티티에 `@Version private Long version` 필드가 추가된다.
- Flyway `V3__Add_version_columns.sql`로 기존 테이블에 `version` 컬럼이 추가된다.
- 동시 수정 충돌 시 `ObjectOptimisticLockingFailureException`이 발생하며, `GlobalExceptionHandler`가 409 Conflict를 반환한다.
- Order, Wish는 생성 후 수정이 없으므로 `@Version` 대상에서 제외한다.
