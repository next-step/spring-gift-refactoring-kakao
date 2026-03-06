# Architecture Decision Records (ADR)

프로젝트의 주요 아키텍처 의사결정을 기록한다.

---

## ADR-1: 서비스 계층 도입

- **상태**: Accepted
- **관련 커밋**: `f42b234`
- **맥락(Context)**: 컨트롤러가 리포지토리를 직접 호출하며 비즈니스 로직(검증, 변환, 트랜잭션 조합)까지 담당하고 있었다. 컨트롤러가 비대해지고, 동일한 비즈니스 로직이 여러 컨트롤러에 중복되며, 단위 테스트가 어려운 구조였다.
- **결정(Decision)**: 도메인별 Service 클래스(`CategoryService`, `MemberService`, `OptionService`, `OrderService`, `ProductService`, `WishService`)를 도입하고, 컨트롤러에서 비즈니스 로직을 서비스 계층으로 이동시킨다.
- **근거(Consequences)**:
  - 컨트롤러는 HTTP 요청/응답 변환에만 집중하고, 비즈니스 로직은 서비스에서 관리한다.
  - 서비스 계층을 독립적으로 단위 테스트할 수 있다.
  - 계층이 하나 추가되어 간단한 CRUD 작업에도 서비스를 거쳐야 하는 간접 비용이 생긴다.

---

## ADR-2: 비즈니스 검증 로직을 도메인 객체로 이동

- **상태**: Accepted
- **관련 커밋**: `484dfd7`
- **맥락(Context)**: 상품명 특수문자 제한, 옵션명 규칙 등 비즈니스 검증 로직이 서비스 계층이나 별도 Validator 클래스(`ProductNameValidator`, `OptionNameValidator`)에 분산되어 있었다. 도메인 객체가 자체 불변식(invariant)을 보장하지 못하여, 서비스를 거치지 않으면 유효하지 않은 객체가 생성될 수 있었다.
- **결정(Decision)**: 비즈니스 검증 로직을 도메인 엔티티(`Product`, `Option`, `Member`, `Wish`, `Order`)의 생성자/세터로 이동시킨다. 별도 Validator 클래스는 삭제한다.
- **근거(Consequences)**:
  - 도메인 객체가 항상 유효한 상태를 유지한다 (자기 방어적 객체).
  - 검증 로직이 도메인에 응집되어 별도 Validator 클래스가 불필요해졌다.
  - 도메인 객체 생성 시 예외가 발생할 수 있어, 호출부에서 이를 고려해야 한다.

---

## ADR-3: 커스텀 예외 도입 및 GlobalExceptionHandler

- **상태**: Accepted
- **관련 커밋**: `57a0bd1`, `f23aaed`, `ac3337c`
- **맥락(Context)**: 각 컨트롤러에서 try-catch로 `NoSuchElementException`, `IllegalArgumentException`, `IllegalStateException` 등 Java 표준 예외를 직접 잡아 HTTP 상태 코드를 매핑하고 있었다. 예외 처리 코드가 모든 컨트롤러에 중복되었고, 표준 예외만으로는 비즈니스 의미(404 Not Found vs 409 Conflict vs 403 Forbidden)를 명확히 표현하기 어려웠다.
- **결정(Decision)**:
  - `GlobalExceptionHandler`(`@RestControllerAdvice`)를 도입하여 예외 처리를 중앙화한다.
  - 비즈니스 의미를 담은 커스텀 예외(`EntityNotFoundException`, `DuplicateException`, `ForbiddenException`, `UnauthorizedException`)를 도입한다.
  - 서비스 계층에서 Java 표준 예외 대신 커스텀 예외를 던지도록 전환한다.
- **근거(Consequences)**:
  - 컨트롤러에서 try-catch 보일러플레이트가 제거되어 코드가 간결해졌다.
  - 예외 클래스명만으로 비즈니스 의미와 HTTP 상태 코드를 명확히 파악할 수 있다.
  - 새로운 예외 유형이 필요할 때 커스텀 예외 클래스와 핸들러를 추가해야 한다.

---

## ADR-4: @Authenticated HandlerMethodArgumentResolver 인증 통합

- **상태**: Accepted
- **관련 커밋**: `8821fff`
- **맥락(Context)**: 인증이 필요한 컨트롤러 메서드마다 `@RequestHeader("Authorization") String token`을 받아 `memberService.extractMember(token)`을 호출하는 2줄짜리 보일러플레이트가 반복되고 있었다. 인증 로직 변경 시 모든 컨트롤러를 수정해야 했다.
- **결정(Decision)**: `@Authenticated` 커스텀 어노테이션과 `MemberArgumentResolver`(`HandlerMethodArgumentResolver`)를 구현한다. 컨트롤러 메서드 파라미터에 `@Authenticated Member member`만 선언하면 인증된 회원 객체가 자동 주입된다.
- **근거(Consequences)**:
  - 인증 보일러플레이트(`@RequestHeader` + `extractMember()`)가 제거되었다.
  - 인증 로직 변경 시 `MemberArgumentResolver` 한 곳만 수정하면 된다.
  - Spring MVC의 Argument Resolver 메커니즘에 대한 이해가 필요하다.

---

## ADR-5: Cucumber BDD 테스트 도입

- **상태**: Accepted
- **관련 커밋**: `5aa1c2b`, `aadf728`
- **맥락(Context)**: 기존 RestAssured 기반 인수 테스트는 HTTP 요청/응답 수준의 저수준 코드로 작성되어, 테스트가 어떤 비즈니스 시나리오를 검증하는지 파악하기 어려웠다.
- **결정(Decision)**: Cucumber BDD 프레임워크를 도입하여 `.feature` 파일에 Gherkin 문법(Given-When-Then)으로 시나리오를 정의하고, Step 클래스에서 이를 구현한다. 도메인별로 Feature 파일과 Step 클래스를 분리한다.
- **근거(Consequences)**:
  - 테스트 시나리오가 자연어에 가까운 형태로 기술되어 가독성이 높아졌다.
  - Cucumber 의존성(`cucumber-java`, `cucumber-spring`, `cucumber-junit-platform-engine`)이 추가되었다.
  - Step 클래스와 Feature 파일 간의 매핑 관리 비용이 있다.

---

## ADR-6: @DirtiesContext를 @Sql cleanup으로 대체

- **상태**: Accepted
- **관련 커밋**: `7d35e3a`
- **맥락(Context)**: 인수 테스트에서 `@DirtiesContext`를 사용하여 테스트 간 데이터 격리를 보장하고 있었다. `@DirtiesContext`는 매 테스트마다 Spring ApplicationContext를 완전히 재생성하므로 테스트 실행 속도가 크게 저하되었다.
- **결정(Decision)**: `@DirtiesContext` 대신 `@Sql` 어노테이션으로 테스트 후 cleanup SQL을 실행하여 데이터를 초기화한다.
- **근거(Consequences)**:
  - ApplicationContext를 재사용하므로 테스트 실행 속도가 크게 개선된다.
  - cleanup SQL이 실제 스키마와 동기화되어야 한다. 테이블이 추가/변경되면 cleanup SQL도 갱신해야 한다.

---

## ADR-7: wish 테이블 unique constraint 추가

- **상태**: Accepted
- **관련 커밋**: `dffbf01`
- **맥락(Context)**: 동일 회원이 같은 상품에 대해 위시를 중복 등록하는 것을 애플리케이션 레벨에서만 방지하고 있었다. 동시 요청 시 레이스 컨디션으로 중복 데이터가 삽입될 수 있었다.
- **결정(Decision)**: Flyway 마이그레이션으로 wish 테이블에 `(member_id, product_id)` 조합의 unique constraint를 추가한다.
- **근거(Consequences)**:
  - DB 레벨에서 중복 위시 삽입을 원천 차단하여 데이터 정합성을 보장한다.
  - 동시 요청 시 `DataIntegrityViolationException`이 발생하며, GlobalExceptionHandler에서 적절히 처리한다.

---

## ADR-8: @Column 매핑을 DDL과 일치하도록 추가

- **상태**: Accepted
- **관련 커밋**: `c0caf00`
- **맥락(Context)**: JPA 엔티티의 `@Column` 어노테이션이 누락되어 있어, DDL에 정의된 제약조건(nullable, unique, length)이 엔티티 코드에 반영되지 않았다. 코드만 보고는 실제 DB 스키마를 파악하기 어려웠다.
- **결정(Decision)**: 모든 엔티티(`Product`, `Category`, `Member`, `Order`, `Wish`)에 `@Column` 어노테이션을 추가하여 DDL과 일치시킨다. `nullable`, `unique`, `length` 속성을 명시한다.
- **근거(Consequences)**:
  - 엔티티 코드만으로 DB 스키마를 파악할 수 있다.
  - JPA 검증과 DB 제약조건이 일치하여 런타임 오류 가능성이 줄어든다.

---

## ADR-9: 기존 회원 비밀번호 BCrypt 마이그레이션

- **상태**: Accepted
- **관련 커밋**: `034a486`
- **맥락(Context)**: BCrypt 해싱을 도입하면서 기존에 평문으로 저장된 회원 비밀번호가 새로운 인증 로직과 호환되지 않는 문제가 발생했다. 기존 회원이 로그인할 수 없게 되는 상황이었다.
- **결정(Decision)**: Flyway 마이그레이션 스크립트(`V3__Encrypt_member_passwords.sql`)로 기존 평문 비밀번호를 BCrypt 해시값으로 일괄 변환한다.
- **근거(Consequences)**:
  - 기존 회원이 비밀번호 재설정 없이 그대로 로그인할 수 있다.
  - 마이그레이션 스크립트에 BCrypt 해시가 하드코딩되므로, 특정 평문에 대한 해시값이 고정된다.
  - 마이그레이션은 일회성이며, 이후 신규 가입은 애플리케이션에서 BCrypt 해싱을 수행한다.

---

## ADR-10: var 타입 추론을 명시적 타입 선언으로 변경

- **상태**: Accepted
- **관련 커밋**: `e96abcd`
- **맥락(Context)**: Java 10의 `var` 키워드로 지역 변수 타입을 추론하고 있었다. 코드 리뷰 시 변수의 실제 타입을 파악하려면 우변의 메서드 시그니처를 추적해야 하여 가독성이 떨어졌다.
- **결정(Decision)**: `var` 타입 추론을 제거하고, `HttpHeaders`, `MultiValueMap<String, String>` 등 명시적 타입 선언으로 변경한다.
- **근거(Consequences)**:
  - 변수 선언부만 보고도 타입을 즉시 파악할 수 있어 코드 리뷰와 유지보수가 용이하다.
  - 타입 선언이 길어져 코드가 다소 장황해질 수 있다.

---

## ADR-11: @RestControllerAdvice 적용 범위를 @RestController로 한정

- **상태**: Accepted
- **관련 커밋**: `a5e2369`
- **맥락(Context)**: `@RestControllerAdvice`가 기본적으로 모든 컨트롤러(`@Controller` 포함)에 적용되어, 뷰를 반환하는 컨트롤러의 예외까지 JSON 응답으로 처리하는 문제가 있었다.
- **결정(Decision)**: `@RestControllerAdvice(annotations = RestController.class)`로 적용 범위를 `@RestController`로 한정한다.
- **근거(Consequences)**:
  - API 컨트롤러(`@RestController`)와 뷰 컨트롤러(`@Controller`)의 예외 처리가 분리된다.
  - 뷰 컨트롤러에 별도 예외 처리가 필요하면 추가 `@ControllerAdvice`를 작성해야 한다.

---

## ADR-12: 엔티티 NOT NULL 제약을 @Column에서 @NotNull로 전환

- **상태**: Accepted
- **관련 커밋**: `9ac0fb9`
- **맥락(Context)**: `@Column(nullable = false)`는 DDL 생성 시에만 반영되고, 런타임에는 null 값이 SQL 실행 시점까지 도달하여 DB 예외(`DataIntegrityViolationException`)로 실패했다. 오류 시점이 늦고, 예외 메시지도 불명확했다.
- **결정(Decision)**: `@Column(nullable = false)`를 Bean Validation의 `@NotNull`로 전환한다. `@Column`에 다른 속성(`unique`, `length`)이 있는 경우 `nullable`만 제거한다.
- **근거(Consequences)**:
  - SQL 실행 전 Bean Validation 단계에서 null 검증이 이루어져 빠른 실패(fail-fast)가 가능하다.
  - `MethodArgumentNotValidException`으로 통합되어 `GlobalExceptionHandler`에서 일관된 400 응답을 반환한다.
  - DDL 자동 생성(`ddl-auto`)을 사용하는 경우, DB 컬럼에 NOT NULL 제약이 생성되지 않으므로 Flyway 등 별도 DDL 관리가 필요하다.
