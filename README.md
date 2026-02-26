# spring-gift-refactoring

## Docker 컨테이너 환경 구성

기존 프로젝트에 Docker 기반 실행 환경을 구성하고, Cucumber BDD 테스트 인프라를 도입하였다.

### 작업 배경

로컬 환경에 의존하지 않고 Docker 컨테이너만으로 애플리케이션이 동작하도록 구성할 필요가 있었다.
이미 구축된 `spring-gift-test-kakao` 프로젝트의 Docker 설정을 참고하여, 동일한 방식(포트 매핑 규칙, 멀티스테이지 빌드, Gradle 태스크 통합)을 현재 프로젝트의 기술 스택(MySQL, Flyway, Kotlin DSL)에 맞게 적용하였다.

### 변경 사항

#### 1. Docker 환경 구성

| 파일 | 역할 |
|------|------|
| `Dockerfile` | 멀티스테이지 빌드 (eclipse-temurin:21-jdk → 21-jre) |
| `docker-compose.yml` | MySQL 8.4 + Spring Boot 앱, 헬스체크 포함 |
| `.dockerignore` | 빌드 컨텍스트 최적화 |

**포트 매핑 규칙** — 참고 프로젝트와 동일한 패턴 적용:

| 서비스 | 호스트 | 컨테이너 |
|--------|--------|----------|
| MySQL | 13306 | 3306 |
| App | 28080 | 8080 |

```bash
./gradlew dockerUp    # 이미지 빌드 + 서비스 시작
./gradlew dockerDown  # 서비스 중지 + 볼륨 정리
```

#### 2. 기존 파일 수정

- **`build.gradle.kts`** — `spring-boot-starter-actuator` 추가 (헬스체크용), Docker/Cucumber Gradle 태스크 추가, Colima 소켓 자동 감지
- **`application.properties`** — 데이터소스 설정을 환경변수 플레이스홀더로 추가 (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)

#### 3. Cucumber BDD 테스트 도입

참고 프로젝트의 Cucumber 인프라를 마이그레이션하고, 현재 프로젝트의 도메인(주문)에 맞게 시나리오를 작성하였다.

| 파일 | 역할 |
|------|------|
| `src/test/java/gift/cucumber/CucumberSuite.java` | JUnit 5 Suite 진입점 |
| `src/test/java/gift/cucumber/CucumberSpringConfiguration.java` | Spring 컨텍스트 설정 (cucumber 프로필) |
| `src/test/java/gift/cucumber/steps/OrderStepDefinitions.java` | 주문 시나리오 스텝 정의 |
| `src/test/resources/features/order.feature` | 한국어 Gherkin 시나리오 7건 |
| `src/test/resources/application-cucumber.properties` | 테스트용 MySQL 접속 설정 |

```bash
./gradlew cucumberTest  # dockerUp → Cucumber 테스트 → dockerDown
```

**PostgreSQL → MySQL 적응 시 주의한 점:**
- TRUNCATE 시 `SET FOREIGN_KEY_CHECKS = 0/1` 필요 (PostgreSQL의 `CASCADE`와 다름)
- 드라이버 클래스: `com.mysql.cj.jdbc.Driver`
- Flyway가 이미 스키마를 생성하므로 테스트 프로필에서 `spring.flyway.enabled=false` 설정

### 학습 내용

#### Docker 멀티스테이지 빌드

빌드 스테이지(JDK)와 런타임 스테이지(JRE)를 분리하면 최종 이미지에서 빌드 도구와 소스코드가 제거되어 이미지 크기가 줄어든다. `bootJar -x test`로 테스트를 건너뛰어 빌드 시간도 단축할 수 있다.

#### Groovy → Kotlin DSL 변환

참고 프로젝트의 Groovy Gradle 스크립트를 Kotlin DSL로 변환하면서 차이를 확인하였다:
- `tasks.register('name', Exec)` → `tasks.register<Exec>("name")`
- `def` 클로저 → `val` 람다 (`() -> String?`)
- `tasks.named('test')` → `tasks.named<Test>("test")` — `tasks.withType<Test>`를 쓰면 `cucumberTest`를 포함한 모든 Test 태스크에 설정이 적용되므로 주의

#### Cucumber 테스트 아키텍처

테스트 JVM과 앱 JVM이 별도 프로세스로 동작하는 구조를 이해하였다:
- 테스트 JVM → MySQL (`localhost:13306`) 직접 접속하여 데이터 셋업/검증
- 테스트 JVM → 앱 (`localhost:28080`) HTTP 요청으로 기능 검증
- 같은 DB를 공유하므로 앱이 변경한 데이터를 테스트에서 즉시 확인 가능

JWT 인증이 필요한 API의 경우, 테스트 JVM에서 동일한 시크릿으로 `JwtProvider`를 주입받아 토큰을 직접 생성하는 방식으로 해결하였다.

#### 트랜잭션 경계의 중요성

Cucumber 테스트를 통해 `OrderController`의 트랜잭션 미적용 문제를 발견하였다. 참고 프로젝트는 Service 계층에 `@Transactional`을 적용하여 재고 차감과 포인트 차감이 하나의 트랜잭션으로 묶이지만, 현재 프로젝트는 Controller에서 Repository를 직접 호출하여 각 `save()`가 독립 커밋된다. 포인트 부족 시 재고만 차감되고 롤백되지 않는 문제가 테스트로 확인되었다.

## 1단계 — 인수 테스트 확장

### 작업 배경

리팩터링에 앞서 "현재 작동"을 보호하는 인수 테스트를 전 도메인으로 확장하였다.
기존에는 주문(order) 도메인만 Cucumber 시나리오가 존재했으나, 구조 변경(Service 계층 추출 등) 시 회귀를 잡으려면 모든 API 경계에 테스트가 필요했다.

### 변경 사항

#### 1. Feature 파일 스타일 통일

참고 프로젝트(`spring-gift-test-kakao`)의 feature 파일 스타일을 분석하여 기존 `order.feature`를 리라이팅하였다.

| 변경 전 | 변경 후 |
|---------|---------|
| `가격이 1000이고 재고가 10인 옵션이 존재한다` | `"M2 Silver" 옵션의 가격이 1000원이고 재고가 10개 있다` |
| `만일 3개를 주문한다` | `만약 "M2 Silver" 3개를 주문한다` |
| `응답 상태코드는 201이다` | `주문이 성공한다` |
| `재고는 7이다` | `"M2 Silver" 옵션의 재고가 7개이다` |

핵심 원칙: **비개발자도 시나리오를 읽고 의도를 이해할 수 있어야 한다.**

#### 2. 전 도메인 인수 테스트 추가 (34개 시나리오)

| 도메인 | 파일 | 시나리오 수 | 검증 내용 |
|--------|------|-----------|----------|
| 회원 | `member.feature` | 5 | 회원가입, 중복 이메일, 로그인, 미등록 이메일, 비밀번호 불일치 |
| 카테고리 | `category.feature` | 7 | 생성, 목록조회, 다건조회, 빈 배열, 수정, 수정실패, 삭제 |
| 상품 | `product.feature` | 7 | 생성, 목록조회, 다건조회, 빈 배열, 카테고리없음, 카카오이름, 15자초과 |
| 옵션 | `option.feature` | 4 | 추가+조회, 중복이름, 삭제, 마지막옵션 삭제불가 |
| 위시리스트 | `wish.feature` | 4 | 추가+조회, 삭제, 인증실패, 타인삭제 거부 |
| 주문 | `order.feature` | 7 | 재고차감, 누적차감, 재고부족, 재고소진, 미존재옵션, 정확한재고, 포인트부족 |

#### 3. Step Definitions 구조 개선

공통 초기화 로직(`@Before` TRUNCATE + RestAssured 설정)을 `CommonStepDefinitions`로 분리하여 중복을 제거하였다.

| 파일 | 역할 |
|------|------|
| `CommonStepDefinitions.java` | 매 시나리오마다 전체 테이블 TRUNCATE, RestAssured 설정 |
| `MemberStepDefinitions.java` | 회원가입/로그인 API 호출 및 검증 |
| `CategoryStepDefinitions.java` | 카테고리 CRUD API 호출 및 검증 |
| `ProductStepDefinitions.java` | 상품 CRUD API 호출 및 검증 (Page 응답 처리) |
| `OptionStepDefinitions.java` | 옵션 추가/삭제 API 호출 및 검증 |
| `WishStepDefinitions.java` | 위시리스트 API 호출 및 인증/인가 검증 |
| `OrderStepDefinitions.java` | 주문 API 호출 및 재고/포인트 검증 |

### 학습 내용

#### Cucumber Step 충돌 문제

Cucumber는 glue 패키지 내 모든 Step Definition 클래스를 하나의 글로벌 레지스트리로 관리한다. 서로 다른 클래스에 동일한 step 표현(`"응답에 id가 포함되어 있다"`)이 존재하면 ambiguous match 오류가 발생하고, 다른 클래스에 정의된 step을 실행하면 해당 클래스의 `lastResponse`가 null이어서 NPE가 발생한다. 각 도메인별로 step 표현을 구체적으로 분리(`"응답에 상품 id가 포함되어 있다"`)하여 해결하였다.

#### `@Before` 중복 실행

여러 Step Definition 클래스에 `@Before`를 정의하면 Cucumber가 모든 `@Before`를 매 시나리오마다 실행한다. TRUNCATE가 여러 번 실행되는 비효율과 충돌을 방지하기 위해 공통 초기화를 `CommonStepDefinitions` 한 곳으로 집중시켰다.

#### Page 응답 구조

Spring Data의 `Page` 응답은 `{ "content": [...], "pageable": {...}, ... }` 구조를 가진다. 상품/위시리스트처럼 `Pageable`을 사용하는 API는 목록을 `$.content`로 접근해야 하고, 카테고리/옵션처럼 `List`를 직접 반환하는 API는 `$`로 접근해야 한다.

### Claude Code 활용

이번 작업에서 Claude Code를 다음과 같이 활용하였다:

#### 이전 단계 (Docker + Cucumber 인프라)

- **참고 프로젝트 분석** — 기존 Docker 설정의 구조와 의도를 파악하여 단순 복사가 아닌 기술 스택에 맞는 적응 방향을 잡음
- **Gradle DSL 변환 검증** — Groovy에서 Kotlin DSL로 변환 시 `tasks.withType` vs `tasks.named` 스코핑 이슈를 빌드 실행으로 즉시 확인하고 수정
- **E2E 검증 자동화** — Docker 이미지 빌드 → 컨테이너 기동 → 헬스체크 → API 호출 → 정리까지 전 과정을 반복 실행하며 설정 오류를 조기에 잡음
- **테스트 기반 결함 발견** — Cucumber 시나리오 실행 결과를 분석하여 트랜잭션 미적용 문제의 근본 원인(아키텍처 차이)을 참고 프로젝트와의 비교를 통해 진단

#### 1단계 (인수 테스트 확장)

- **참고 프로젝트 Feature 스타일 분석** — `spring-gift-test-kakao`의 feature 파일 3개를 분석하여 "비개발자 가독성" 스타일 규칙(구체적 이름, 비즈니스 언어, 상태코드 배제)을 도출하고, 기존 `order.feature`를 해당 스타일로 리라이팅
- **도메인별 인수 테스트 생성** — 각 도메인의 Controller/Entity/DTO 코드를 분석한 뒤 feature 파일과 step definitions를 생성. 매 도메인마다 `./gradlew cucumberTest`를 실행하여 34개 시나리오 전체 통과를 확인
- **Step 충돌 디버깅** — 상품 테스트 추가 시 발생한 NPE(CategoryStepDefinitions의 `lastResponse` null 참조)를 분석하여 Cucumber 글로벌 step 레지스트리 구조를 이해하고, 도메인별 step 표현 분리로 해결
- **공통 초기화 추출** — `@Before` TRUNCATE 중복 문제를 인지하고 `CommonStepDefinitions`로 분리하는 리팩터링을 제안받아 적용

## 2단계 — 서비스 레이어 추출

### 작업 배경

모든 비즈니스 로직이 컨트롤러에 직접 구현되어 있어 `@Transactional` 없이 다중 엔티티 수정이 이루어지고 있었다. 특히 `OrderController`에서 재고 차감(`optionRepository.save()`)과 포인트 차감(`memberRepository.save()`)이 별도 커밋되어, 포인트 부족 시 재고만 차감되고 롤백되지 않는 버그가 존재했다. 서비스 레이어를 추출하여 관심사 분리와 트랜잭션 원자성을 확보하였다.

### 변경 사항

#### 1. 서비스 클래스 7개 생성

| 서비스 | 패키지 | 핵심 역할 |
|--------|--------|----------|
| `CategoryService` | `gift.category` | 카테고리 CRUD |
| `MemberService` | `gift.member` | 회원가입/로그인(JWT 발급), Admin 회원 관리, 포인트 충전 |
| `ProductService` | `gift.product` | 상품 CRUD, 이름 검증(API: 카카오 금지, Admin: 허용) |
| `OptionService` | `gift.option` | 옵션 추가/삭제, 이름 검증, 최소 1개 규칙 |
| `WishService` | `gift.wish` | 위시리스트 추가(중복 처리)/삭제(소유자 검증) |
| `KakaoAuthService` | `gift.auth` | 카카오 OAuth 인가 URL 생성, 콜백 처리(토큰 교환 + 회원 생성/조회 + JWT) |
| `OrderService` | `gift.order` | 주문 생성(재고 차감 + 포인트 차감 + 저장을 단일 트랜잭션으로 처리) |

#### 2. 컨트롤러 9개 수정

모든 컨트롤러에서 Repository 직접 주입을 Service 주입으로 변경하고, 각 메서드를 1~2줄 위임으로 축소하였다.

| 컨트롤러 | 변경 전 의존성 | 변경 후 의존성 |
|----------|---------------|---------------|
| `CategoryController` | `CategoryRepository` | `CategoryService` |
| `MemberController` | `MemberRepository`, `JwtProvider` | `MemberService` |
| `AdminMemberController` | `MemberRepository` | `MemberService` |
| `ProductController` | `ProductRepository`, `CategoryRepository` | `ProductService` |
| `AdminProductController` | `ProductRepository`, `CategoryRepository` | `ProductService`, `CategoryRepository`(폼 데이터용) |
| `OptionController` | `OptionRepository`, `ProductRepository` | `OptionService` |
| `WishController` | `WishRepository`, `ProductRepository`, `AuthenticationResolver` | `WishService`, `AuthenticationResolver` |
| `KakaoAuthController` | `KakaoLoginProperties`, `KakaoLoginClient`, `MemberRepository`, `JwtProvider` | `KakaoAuthService` |
| `OrderController` | `OrderRepository`, `OptionRepository`, `WishRepository`, `MemberRepository`, `AuthenticationResolver`, `KakaoMessageClient` | `OrderService`, `AuthenticationResolver` |

#### 3. 핵심 변경: OrderService 트랜잭션 도입

변경 전 (`OrderController`에서 직접 처리):
```java
/* 재고 차감 — 즉시 커밋 */
option.subtractQuantity(request.quantity());
optionRepository.save(option);

/* 포인트 차감 — 별도 커밋, 실패해도 재고는 이미 차감됨 */
member.deductPoint(price);
memberRepository.save(member);
```

변경 후 (`OrderService`에서 `@Transactional`로 처리):
```java
@Transactional
public OrderResponse createOrder(Long memberId, OrderRequest request) {
    Member member = memberRepository.findById(memberId).orElseThrow(...);
    Option option = optionRepository.findById(request.optionId()).orElseThrow(...);

    option.subtractQuantity(request.quantity());   // 재고 차감
    int price = option.getProduct().getPrice() * request.quantity();
    member.deductPoint(price);                     // 포인트 차감 — 실패 시 전체 롤백

    Order saved = orderRepository.save(request.toEntity(option, member.getId()));
    sendKakaoMessageIfPossible(member, saved, option);
    return OrderResponse.from(saved);
}
```

`@Transactional` 내에서 JPA managed entity의 dirty checking이 작동하므로 명시적 `save()` 호출 없이도 재고/포인트 변경이 트랜잭션 커밋 시 반영된다. 포인트 부족 예외 발생 시 트랜잭션 전체가 롤백되어 재고 차감도 취소된다.

#### 4. 테스트 기대값 수정

`order.feature`의 "포인트가 부족하면 주문이 실패한다" 시나리오에서 기대 재고를 변경하였다:

| | 변경 전 | 변경 후 |
|---|---------|---------|
| 기대 재고 | 9개 (재고 차감이 롤백되지 않음) | 10개 (트랜잭션 롤백으로 재고 원복) |
| 주석 | `@Transactional 미적용으로 재고 차감이 포인트 검증보다 먼저 커밋됨` | `@Transactional 적용으로 포인트 부족 시 재고 차감도 롤백됨` |

### 학습 내용

#### 서비스 레이어의 역할

컨트롤러는 HTTP 요청/응답 처리(인증 헤더 파싱, 상태코드 결정, URI 생성)에 집중하고, 서비스는 비즈니스 로직(검증, 엔티티 조회/수정, 트랜잭션 관리)을 담당한다. 이 분리로 같은 비즈니스 로직을 API 컨트롤러와 Admin 컨트롤러에서 재사용할 수 있게 되었다(예: `ProductService`를 `ProductController`와 `AdminProductController`가 공유).

#### @Transactional과 JPA Dirty Checking

`@Transactional` 메서드 내에서 `EntityManager`가 관리하는 엔티티의 필드를 변경하면, 트랜잭션 커밋 시점에 JPA가 자동으로 UPDATE SQL을 실행한다. 이 때문에 `OrderService.createOrder()`에서 `option.subtractQuantity()`와 `member.deductPoint()`만 호출하면 되고, 명시적 `save()` 호출이 불필요하다. 단, 컨트롤러에서 주입받은 `Member` 객체는 트랜잭션 바깥에서 조회된 detached 상태이므로, 서비스 메서드에서 `memberId`를 받아 트랜잭션 내에서 다시 조회하여 managed 상태의 엔티티를 사용해야 dirty checking이 정상 작동한다.

#### 트랜잭션 원자성의 실제 효과

서비스 레이어 추출 전에는 `optionRepository.save()`와 `memberRepository.save()`가 각각 독립된 트랜잭션으로 커밋되어, 포인트 차감 실패 시 재고만 차감되는 데이터 불일치가 발생했다. `@Transactional`로 두 연산을 하나의 트랜잭션으로 묶은 후, 포인트 부족 예외 시 재고 차감까지 롤백되는 것을 Cucumber 테스트(재고 기대값 9→10)로 검증하였다.

### Claude Code 활용

#### 활용 방식

- **리팩토링 계획 수립** — 현재 컨트롤러 9개의 코드를 분석하여 서비스 추출 순서(단순 CRUD → 복합 로직 순)와 각 서비스의 메서드 시그니처를 설계. 의존성이 적은 `CategoryService`부터 시작하여 패턴을 확립하고, 가장 복잡한 `OrderService`를 마지막에 구현하는 단계별 계획을 작성
- **서비스 클래스 생성 및 컨트롤러 리팩토링** — 각 단계마다 서비스 클래스를 생성하고 대응하는 컨트롤러를 수정. 기존 컨트롤러의 비즈니스 로직을 서비스로 이동하면서 `@Transactional`, `@Transactional(readOnly = true)` 어노테이션을 적절히 적용
- **테스트 기반 검증** — 7개 서비스 생성 + 9개 컨트롤러 수정 + 1개 테스트 수정 후 `./gradlew cucumberTest`를 실행하여 전체 시나리오 통과를 확인. 특히 `OrderService`의 `@Transactional` 적용 후 포인트 부족 시 재고 롤백이 정상 동작하는 것을 기존 테스트 기대값 수정(9개→10개)으로 검증

#### 코드 수정 내용

총 17개 파일을 수정/생성하였다:
- **새 파일 7개**: `CategoryService`, `MemberService`, `ProductService`, `OptionService`, `WishService`, `KakaoAuthService`, `OrderService`
- **수정 파일 9개**: `CategoryController`, `MemberController`, `AdminMemberController`, `ProductController`, `AdminProductController`, `OptionController`, `WishController`, `KakaoAuthController`, `OrderController`
- **테스트 수정 1개**: `order.feature` (트랜잭션 롤백 기대값 반영)
