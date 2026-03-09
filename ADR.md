# Architecture Decision Records

## ADR-001: Admin 컨트롤러에 서비스 계층 도입 방식

### 상태
채택됨

### 배경
`AdminMemberController`와 `AdminProductController`가 Repository를 직접 호출하고 있었다.
서비스 계층을 거치지 않아 트랜잭션 관리가 누락되고, 비즈니스 로직이 컨트롤러에 분산되는 문제가 있었다.

### 결정
별도 AdminService를 만들지 않고, 기존 `ProductService` / `MemberService` / `CategoryService`에 메서드를 추가한다.

### 근거
- Admin 작업은 별도 도메인이 아니라 같은 도메인의 CRUD이다. 서비스를 분리할 만큼 관심사가 다르지 않다.
- `AdminProductService`를 만들면 같은 Repository를 주입받는 서비스가 2개가 되고, 검증 로직이 중복되거나 서비스 간 호출이 필요하다.
- `AdminService`로 통합하면 Product·Member·Category를 한 클래스에 몰아넣게 되어 SRP를 위반하고, 패키지-바이-피처 구조와 맞지 않는다.

### 고려했던 대안

| 대안 | 기각 이유 |
|------|-----------|
| `AdminProductService` / `AdminMemberService` | 같은 Repository 중복 주입, 검증 로직 중복 |
| 통합 `AdminService` | SRP 위반, 패키지-바이-피처 구조와 불일치 |

---

## ADR-002: 서비스 계층 반환 타입 — 엔티티 vs DTO

### 상태
방향 확정, 점진적 전환 중

### 배경
기존 서비스 계층은 DTO를 반환하는 메서드가 대다수(13개)였고, 엔티티를 반환하는 메서드는 소수(4개)였다.
Admin 컨트롤러 리팩토링 과정에서 서비스가 DTO를 반환하면 같은 비즈니스 로직임에도 Admin용 메서드를 중복 생성해야 하는 문제가 드러났다.

### 결정
- 서비스는 엔티티를 반환하는 것이 바람직하다.
- 단, 전체 변경은 영향 범위가 크므로 우선 Admin 리팩토링에 필요한 엔티티 반환 메서드를 추가한다.
- 기존 DTO 반환 메서드는 그대로 유지하고, 추후 별도 작업으로 통일한다.

### 근거
- DTO 변환은 프레젠테이션 관심사이지 서비스의 역할이 아니다.
- 서비스가 DTO를 반환하면 두 번째 소비자(Admin 컨트롤러)가 생겼을 때 같은 로직인데 메서드를 중복 생성해야 한다.
- 엔티티를 반환하면 컨트롤러마다 `ProductResponse.from(entity)` 등 기존 팩토리 메서드로 자유롭게 변환할 수 있다.
- OSIV가 꺼져 있어도 서비스에 `@Transactional`이 있으므로 필요한 연관관계를 fetch join으로 로딩하면 된다.

### 기존 코드가 DTO를 반환했던 이유
REST API 컨트롤러만 있던 시점에서 서비스와 컨트롤러가 1:1이었다.
단일 소비자 상황에서는 서비스에서 DTO를 바로 반환하는 것이 편리하고 단점이 보이지 않았다.
Admin 컨트롤러(두 번째 소비자)가 생기면서 재사용 불가 문제가 드러났다.

---

## ADR-003: 컨트롤러의 다중 서비스 호출 시 트랜잭션 보장

### 상태
채택됨

### 배경
`WishController.addWish`에서 `isDuplicate`와 `add`를 별도로 호출하고 있었다.
두 메서드가 각각 독립된 트랜잭션으로 실행되어, 사이에 다른 요청이 끼어들면 응답 상태코드(201 vs 200)가 실제 결과와 불일치하는 race condition이 존재했다.

### 결정
컨트롤러에서 여러 서비스 메서드를 조합하지 않고, 서비스 메서드 하나에 로직을 통합하여 단일 트랜잭션으로 실행한다.
연산 결과의 부가 정보(중복 여부)가 필요하면 `WishResult`와 같은 값 객체를 도입하여 엔티티와 함께 반환한다.

### 근거
- 컨트롤러에서 서비스 메서드를 여러 번 호출하면 각 호출이 별도 트랜잭션으로 실행되어 원자성이 깨진다.
- 서비스 메서드 하나로 통합하면 `@Transactional` 안에서 중복 확인과 저장이 원자적으로 수행된다.
- `WishResult`는 컨트롤러용 DTO가 아니라 서비스 연산의 결과를 표현하는 값 객체이므로, 서비스의 엔티티 반환 원칙(ADR-002)과 충돌하지 않는다.

### 고려했던 대안

| 대안 | 기각 이유 |
|------|-----------|
| 컨트롤러에 `@Transactional` 적용 | 프레젠테이션 계층이 트랜잭션을 관리하게 되어 계층 책임이 혼재 |
| 201/200 구분 포기 | 기존 API 계약 변경, 클라이언트 영향 |

---

## ADR-004: 외부 API 호출을 트랜잭션 경계 밖으로 분리

### 상태
채택됨

### 배경
`KakaoAuthService.processCallback`에서 카카오 외부 API 호출(토큰 발급, 사용자 정보 조회)과 DB 작업(회원 조회/저장)이 하나의 `@Transactional` 안에 포함되어 있었다.
외부 API가 느리거나 타임아웃되면 DB 커넥션을 불필요하게 오래 점유하고, API 실패 시 DB 작업까지 롤백되는 문제가 있었다.

### 결정
컨트롤러가 `KakaoLoginClient`를 직접 호출하여 외부 API 작업을 수행하고, 서비스는 DB 작업(`loginOrRegister`)만 담당한다.
- `KakaoAuthController` — `KakaoLoginClient`로 토큰 발급·사용자 조회 후, `KakaoAuthService.loginOrRegister`로 DB 작업
- `KakaoAuthService` — `KakaoLoginClient` 의존 제거, `@Transactional` DB 작업만 보유

### 근거
- 외부 API 호출은 네트워크 I/O이므로 트랜잭션과 무관해야 한다. DB 커넥션 점유 시간을 최소화할 수 있다.
- `OrderController`에서 카카오 메시지 전송을 트랜잭션 밖에서 처리하는 기존 패턴과 일관성을 맞출 수 있다.
- 서비스에 위임 메서드(`getKakaoToken`, `getKakaoEmail`)를 두는 방안도 검토했으나, 비즈니스 로직 없이 클라이언트를 그대로 호출하는 것이므로 불필요한 간접 계층이다. 컨트롤러가 직접 클라이언트를 호출하면 서비스의 역할이 DB 작업으로 명확해진다.

### 고려했던 대안

| 대안 | 기각 이유 |
|------|-----------|
| 서비스에 위임 메서드로 외부 호출 분리 | 비즈니스 로직 없이 클라이언트를 그대로 호출하는 불필요한 간접 계층 |
| 같은 서비스 내 private 메서드 분리 + `@Transactional` | Spring AOP 프록시 미적용으로 트랜잭션 경계가 동작하지 않음 |
| `TransactionTemplate` 사용 | 선언적 트랜잭션과 혼용되어 코드 일관성 저하 |

---

## ADR-005: Admin/User API 서비스 메서드 네이밍 규칙 — `admin` 접두어

### 상태
채택됨

### 배경
`ProductService`와 `MemberService`에 동일한 조회 로직이지만 에러 메시지가 다른 메서드 쌍이 존재했다.
User API용은 내부 정보를 숨긴 메시지(`"상품이 존재하지 않습니다."`)를, Admin용은 디버깅을 위해 id를 포함한 메시지(`"상품이 존재하지 않습니다. id=123"`)를 사용한다.
초기에는 `findById` / `getById`로 구분했으나, 코드를 처음 보는 사람이 어느 쪽이 Admin용인지 알 수 없었다.

### 결정
Admin에서만 호출하는 서비스 메서드에는 `admin` 접두어를 붙인다.

| 호출처 | 네이밍 | 예시 |
|--------|--------|------|
| REST API (User) | `findById()`, `findCategory()` | `ProductService.findById(id)` |
| Admin | `adminFindById()`, `adminFindCategory()` | `ProductService.adminFindById(id)` |

### 근거
- 메서드명만으로 호출처가 즉시 구분된다. `find` vs `get` 같은 암묵적 컨벤션에 의존하지 않는다.
- 별도 AdminService를 만들지 않는 기존 결정(ADR-001)을 유지하면서도 Admin용 메서드를 명시적으로 식별할 수 있다.
- IDE에서 `admin`으로 검색하면 Admin 전용 메서드를 한 번에 찾을 수 있다.

### 고려했던 대안

| 대안 | 기각 이유 |
|------|-----------|
| `find` vs `get` 구분 | 암묵적이라 코드를 처음 보는 사람이 규칙을 모름 |
| `ControllerAdvice`에서 에러 메시지 분기 | 메서드는 하나로 줄지만 ControllerAdvice 복잡도 증가 |
| 별도 `AdminProductService` | ADR-001에서 이미 기각 — 중복 주입, 검증 로직 중복 |

---

## ADR-006: 서비스에 누수된 비즈니스 로직을 엔티티로 이동

### 상태
채택됨

### 배경
Service 계층을 도입(ADR-001)하면서 Controller에 있던 로직이 Service로 옮겨졌다.
그러나 엔티티의 상태를 getter로 꺼내 Service에서 판단·계산하는 패턴이 남아 있었다.
이는 캡슐화를 깨뜨리고, 동일한 판단 로직이 여러 Service에 중복될 위험이 있다.

### 결정
엔티티의 상태에 대한 판단·계산은 엔티티 메서드로 이동한다.

| 변경 전 (Service) | 변경 후 (Entity) |
|-------------------|-----------------|
| `member.getPassword() == null \|\| !member.getPassword().equals(pw)` | `Member.authenticate(password)` |
| `!wish.getMemberId().equals(memberId)` → 예외 | `Wish.validateOwnership(memberId)` |
| `option.getProduct().getPrice() * request.quantity()` | `Order.calculatePrice()` |

### 근거
- **캡슐화**: 엔티티 내부 상태(password, memberId, price)를 외부에 노출하지 않고 엔티티가 직접 판단한다.
- **중복 방지**: 같은 판단 로직이 여러 Service에 퍼지는 것을 원천 차단한다.
- **변경 지점 최소화**: 비밀번호 비교 방식 변경(예: BCrypt 도입), 가격 계산 변경(예: 할인 적용) 시 엔티티 한 곳만 수정하면 된다.

### 예외 처리 원칙
- 엔티티가 던지는 예외 타입은 기존과 동일하게 유지한다.
- `Wish.validateOwnership()`이 던지는 `IllegalAccessException`은 `WishService` 내부 클래스에서 wish 패키지의 독립 클래스로 분리하여, 엔티티→서비스 역방향 의존을 제거했다.

### 추가 적용: OrderService → WishService 위임
`OrderService`가 `WishRepository`를 직접 호출하여 wish를 삭제하던 것을 `WishService.removeByMemberAndProduct()`로 위임했다.
도메인 경계를 넘는 Repository 직접 조작을 제거하여, wish 삭제 로직이 변경되더라도 `WishService` 한 곳만 수정하면 된다.

### 보류 항목

| 항목 | 보류 이유 |
|------|-----------|
| `MemberService` 이메일 중복 검사 — `register()`와 `create()`에 동일 로직 반복 | 호출처가 2곳뿐이라 공통 메서드 추출의 실익이 작음. 호출처가 늘어나면 재검토 |

---

## ADR-007: Admin 접근 제어 — HandlerInterceptor 방식

### 상태
채택됨

### 배경
`/admin/**` 경로에 접근 제어가 없어 누구나 Admin 페이지에 접근 가능했다.
Admin 페이지는 Thymeleaf MVC 기반이므로 브라우저 세션 인증이 자연스럽다.

### 결정
- `Member`에 `Role` enum 필드(USER, ADMIN)를 추가한다.
- `AdminInterceptor`(`HandlerInterceptor`)가 `/admin/**` 요청의 세션에서 `adminMemberId`를 확인한다.
- `AdminAuthController`가 세션 기반 로그인/로그아웃을 처리한다.

### 근거
- **추가 의존성 없음**: 기존 `WebMvcConfig`에 인터셉터를 등록하면 되므로 Spring Security 같은 외부 라이브러리가 불필요하다.
- **경로 기반 적용**: `addPathPatterns("/admin/**")`으로 Admin 경로만 선택적으로 보호할 수 있다.
- **기존 인증과 분리**: REST API는 JWT(`AuthenticatedMemberArgumentResolver`), Admin은 세션으로 인증 방식이 다르므로 별도 메커니즘이 적절하다.

### 고려했던 대안

| 대안 | 기각 이유 |
|------|-----------|
| Spring Security | Admin 페이지 보호만을 위해 도입하기엔 설정 복잡도와 학습 비용이 과다 |
| AOP (`@Aspect`) | 경로 패턴 기반 적용이 불가능하고, 컨트롤러 메서드마다 어노테이션 필요 |
| Servlet Filter | Spring MVC의 `InterceptorRegistry`로 경로 패턴·제외 패턴을 선언적으로 관리할 수 없음 |
