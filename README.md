# spring-gift-refactoring

## 리팩토링 목표

> 구조 변경을 통해 변경 난이도를 낮추되 작동은 유지한다.

## 명세 외 추가 API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `DELETE` | `/api/categories/{id}` | 카테고리 삭제 |

## User API / Admin API 네이밍 규칙

Service 메서드는 `admin` 접두어로 호출처를 구분한다.

| 네이밍 | 호출처 | 에러 메시지 | 이유 |
|--------|--------|-------------|------|
| `findById(id)` | REST API (`ProductController`) | `"상품이 존재하지 않습니다."` | 사용자에게 노출되므로 내부 정보를 숨김 |
| `adminFindById(id)` | Admin (`AdminProductController`) | `"상품이 존재하지 않습니다. id=123"` | 관리자 디버깅용으로 id를 포함 |

## 사용한 Claude Code 스킬

| 스킬 | 설명 |
|------|------|
| `acceptence-test-writer` | RestAssured 기반 E2E 인수 테스트를 작성한다. TEST_STRATEGY.md와 CLAUDE.md의 시나리오 매트릭스를 기반으로 성공/실패 케이스를 given-when-then 구조로 생성한다. |
| `junit5-test-reference` | JUnit 5 단위/통합 테스트를 생성한다. Assertions, Parameterized Tests, Mockito, Nested Tests 등 패턴 레퍼런스를 포함한다. |
| `logical-commit` | 변경 사항을 논리적 작업 단위로 분류하여 각각 별도의 커밋으로 생성한다. git status/diff를 분석하고, 관련 파일을 그룹핑하여 의미 있는 커밋 메시지와 함께 커밋한다. |

## 분석 작업 내역

| 작업 | 설명 | 결과 |
|------|------|------|
| 미사용 코드 탐지 | 전체 소스 파일을 읽고, 각 메서드/필드의 호출처를 검색하여 사용 여부 판별 | `Product.getOptions()`, `Order.getMemberId()`, `OrderController.wishRepository` 발견 |
| API 엔드포인트 목록 추출 | `@GetMapping`, `@PostMapping` 등 어노테이션을 grep하여 전체 API 목록 도출 | REST 18개 + Admin 13개 = 31개 엔드포인트 정리 |
| Controller 의존성 분석 | 각 Controller가 주입받는 Repository/Client를 파악하여 Service 계층 부재 확인 | 9개 Controller 모두 Repository 직접 호출 확인 |
| Repository 커스텀 메서드 사용 확인 | 각 Repository의 커스텀 메서드가 실제 호출되는지 codebase 전체 검색 | 미사용 커스텀 메서드 없음 확인 |
| Service 메서드 사용 확인 | Service 클래스 존재 여부 확인 | Service 클래스 자체가 0개 |

## 판단 근거 및 논의 기록

### 1. 현황 분석

#### 프로젝트 구조
- Spring Boot 3.5.9 / Java 21 / JPA / Flyway / Kakao OAuth2
- 도메인: auth, category, member, option, order, product, wish
- REST API 18개 + Admin 페이지 13개 = 총 31개 엔드포인트

#### 발견된 문제점

**미사용 코드**

| 항목 | 위치 | 이유 |
|------|------|------|
| `OrderService.wishRepository` | `OrderService.java:22` | 주입만 되고 사용 안 됨. 주석에 "6. cleanup wish"가 있으나 미구현 |

`Product.getOptions()`는 `open-in-view=false` 환경에서 LAZY 컬렉션의 트랜잭션 밖 접근 시 `LazyInitializationException` 발생을 검증하는 테스트(`OpenInViewTest`)를 위해 유지한다.

**Service 계층 부재**
- Service 클래스가 프로젝트에 하나도 없음
- 모든 Controller가 Repository를 직접 호출
- 특히 `OrderController`는 Repository 4개 + Client 1개를 직접 의존

**Controller에 비즈니스 로직 집중**
- 인증 확인, 검증, 재고 차감, 포인트 차감, 주문 저장, 외부 API 호출이 모두 Controller에 존재

### 2. 왜 Service 계층을 도입하는가?

| 문제 | Service 도입 후 |
|------|-----------------|
| 비즈니스 로직 변경 시 Controller를 직접 수정해야 함 | Service만 수정하면 됨 |
| REST / Admin 간 로직 재사용 불가 (복붙) | 같은 Service를 공유 |
| 비즈니스 로직만 단위 테스트하기 어려움 | Service 단위 테스트 가능 |
| 트랜잭션 경계가 불명확함 | `@Transactional`로 명확하게 제어 |

### 3. 왜 @Transactional을 Controller가 아닌 Service에 붙이는가?

Controller에 붙이면 동작은 하지만, 트랜잭션 범위가 불필요하게 넓어진다.

```
// Controller에 @Transactional 붙인 경우
@Transactional 시작
  1. 인증 확인          ← DB 불필요
  2. 옵션 검증          ← DB
  3. 재고 차감          ← DB
  4. 포인트 차감        ← DB
  5. 주문 저장          ← DB
  6. 카카오 알림 전송    ← 외부 API 호출 (느림)
@Transactional 끝
→ 카카오 API가 3초 걸리면 DB 커넥션을 3초간 점유. 트래픽 몰리면 커넥션 풀 고갈.

// Service에 @Transactional 붙인 경우
Controller:
  인증 확인
  orderService.createOrder()   ← @Transactional (DB 작업만)
  카카오 알림 전송              ← 트랜잭션 밖
→ 트랜잭션 범위를 필요한 만큼만 제어 가능.
```

**단, 성능에 크게 문제가 없는 단순 CRUD는 클래스 레벨에 `@Transactional`을 붙이고, 필요한 메서드만 override 한다.**

#### 카카오 알림을 Controller에서 호출하는 이유

`@TransactionalEventListener(phase = AFTER_COMMIT)`를 사용하면 `create()`를 어디서 호출하든 알림이 자동으로 붙는 장점이 있다. 하지만 다음 이유로 도입하지 않았다.

1. **알림 실패가 주문을 방해하면 안 된다.** Controller에서 best-effort로 호출하면 알림 실패가 주문 응답에 영향을 주지 않는다. 이벤트 리스너로 전환할 경우, 리스너 내 예외 처리·재시도·실패 큐 등 부수 인프라가 필요해진다.
2. **현재 `create()`를 호출하는 곳은 `OrderController` 한 곳뿐이다.** 이벤트 발행/구독 구조는 호출처가 여러 곳일 때 의미가 있으며, 지금은 복잡도만 높인다.
3. **코드 흐름이 명시적이다.** Controller를 읽으면 "주문 저장 → 알림 전송" 순서가 바로 보인다. 이벤트 기반은 흐름을 따라가려면 이벤트 클래스와 리스너를 찾아야 한다.

### 4. @ExceptionHandler를 언제 @ControllerAdvice로 합치는가?

**Controller 안에 두는 경우** — 도메인마다 예외 처리 로직이 다를 때 (다른 상태코드, 다른 응답 형식, 다른 후처리 등)

**`@ControllerAdvice`로 합치는 경우** — 예외 처리 로직이 모든 Controller에서 동일할 때. 복붙은 버그의 원인이 되고, 새 Controller 추가 시 핸들러를 빠뜨릴 위험이 있다.

`GlobalExceptionHandler`에서 처리하는 예외:
- `NoSuchElementException` → 404 — 전역 `@ControllerAdvice`로 처리. 리소스 미존재 시 500 대신 404를 반환하는 버그 수정.
- `IllegalArgumentException` → 400 — `assignableTypes`로 `MemberController`, `OptionController`, `ProductController`에만 한정 적용. 내부 static 클래스로 분리.

### 5. 리팩토링 순서 결정

1. **미사용 코드 제거** — 안 쓰는 메서드/필드를 조기에 찾아서 없애기
2. **인수 테스트 작성 (먼저)** — 유닛 테스트는 리팩토링 과정에서 깨지므로, 사용자 행동 기반의 인수 테스트를 먼저 작성하여 안전망 확보
3. **구조 변경 (Service 계층 도입)** — 작동 유지하면서 구조만 변경
4. **스타일 통일** — 구조 변경이 끝난 후에 진행

> 유닛 테스트를 지금 작성하면 리팩토링 과정에서 깨진다. 인수 테스트는 사용자 행동 주도이므로 구조가 바뀌어도 유지된다.

### 6. OSIV 비활성화(`open-in-view=false`) 결정

- 설정: `spring.jpa.open-in-view=false`
- 전제: 본 서비스는 서버사이드 View 렌더링이 아닌 API 응답 중심 구조다.

#### 6.1 문제 인식
OSIV가 활성화되면 트랜잭션 종료 이후(Controller/직렬화 시점)에도 지연 로딩이 발생할 수 있다.
그 결과 쿼리 발생 지점이 서비스 계층 밖으로 확산되어 성능 이슈 및 장애 원인 추적이 어려워진다.

#### 6.2 구조 변경으로 판단한 근거
이번 변경은 단순 옵션 조정이 아니라, 데이터 접근 경계를 서비스 트랜잭션 내부로 제한하는 구조적 선택이다.
즉, "응답 생성 단계에서 DB 접근이 일어나지 않게 한다"는 원칙을 도입하는 변경으로 판단했다.

#### 6.3 OSIV 선택 기준(장단점)

| 항목 | OSIV 활성화 (`true`) | OSIV 비활성화 (`false`) |
|---|---|---|
| 개발 편의성 | View/응답 단계에서 지연 로딩 가능해 구현이 단순함 | 트랜잭션 내부에서 필요한 데이터 조회를 미리 설계해야 함 |
| 쿼리 가시성 | View/직렬화 단계에서 추가 쿼리가 숨겨질 수 있음 | 트랜잭션 경계 내부에서 쿼리가 발생하도록 설계하기 쉬워 추적이 쉬움 |
| 성능 예측 가능성 | N+1, 지연 로딩으로 성능 변동 가능성이 큼 | 명시적 조회 중심이라 성능 예측/튜닝이 상대적으로 쉬움 |
| 계층 책임 | 프레젠테이션 계층이 영속성 동작에 일부 의존 가능 | 서비스 계층에 데이터 접근 책임이 명확히 모임 |
| 운영 안정성 | 초기 개발은 빠르지만 장애 원인 분석이 어려워질 수 있음 | 초기 설계 비용은 늘지만 운영 중 원인 파악과 통제가 용이함 |
| 적합한 상황 | 저트래픽, 단순 화면, 빠른 프로토타이핑 | API 중심, 고트래픽, 성능/관측 가능성 중시 서비스 |

## 리팩토링 진행 내역

### 1. Service 계층 추출
Controller에 몰려 있던 비즈니스 로직을 Service로 분리했다.

- `CategoryController` → `CategoryService`
- `MemberController` → `MemberService`
- `ProductController` → `ProductService`
- `OptionController` → `OptionService`
- `WishController` → `WishService`
- `OrderController` → `OrderService`

### 2. 주석 복원
원본 코드(`55ca9e4`)에 있던 주석들이 리팩토링 과정에서 누락되어 복원했다.

- `OrderController` — `// auth check`, order flow 개요 주석(1~7단계)
- `OrderService` — `// validate option`
- `WishController` — `// check auth`
- `WishService` — `// check product`, `// check duplicate`
- `MemberController` — Javadoc (`@author`, `@since`)
- `OptionController` — 블록 주석 (옵션 제약조건 설명)

### 3. 코드 스타일 통일
프로젝트 전체를 스캔하여 일관성이 깨진 부분을 정리했다.

| 항목 | 변경 내용 | 이유 |
|------|-----------|------|
| `var` → 명시적 타입 | 12곳에서 `var`를 `Member`, `Product`, `Optional<Wish>` 등 명시적 타입으로 변경 | 일관성이 없어서 제거 |
| `final` 로컬 변수 제거 | `AuthenticationResolver`, `JwtProvider`, `AdminMemberController`의 `final` 7곳 제거 | 일관성이 없어서 삭제 |
| `@Autowired` 제거 | 생성자가 하나인 클래스에서 `@Autowired` 어노테이션 및 미사용 import 제거 | Spring에서 권장하지 않아서 되도록 안 쓰려고 제거 |
| lint format | import 순서, 공백 등 포매팅 통일 | 통일성을 위해 |

### 4. E2E 인수 테스트 추가
RestAssured 기반 인수 테스트를 전 API에 대해 작성했다.

- `CategoryAcceptanceTest`
- `MemberAcceptanceTest`
- `ProductAcceptanceTest`
- `OptionAcceptanceTest`
- `WishAcceptanceTest`
- `OrderAcceptanceTest`

### 5. @ControllerAdvice 도입
`GlobalExceptionHandler`를 도입했다.
- `NoSuchElementException` → 404: 전역 적용. 기존에 핸들러가 없어 500으로 빠지던 리소스 미존재 응답을 404로 수정.
- `IllegalArgumentException` → 400: `assignableTypes`로 `MemberController`, `OptionController`, `ProductController`에만 한정 적용. 내부 static 클래스로 분리.

### 6. 카카오 알림 트랜잭션 밖 분리
`OrderService.create()` 안에 있던 `sendKakaoMessageIfPossible()`을 `OrderController`로 이동하여 트랜잭션 커밋 후 실행되도록 변경했다. `KakaoMessageClient` 의존성도 `OrderService`에서 `OrderController`로 이동.

### 7. 도메인 책임 되찾기
서비스에 누수되어 있던 비즈니스 로직을 엔티티로 이동했다.

| 변경 전 (Service) | 변경 후 (Entity) | 이유 |
|-------------------|-----------------|------|
| `MemberService`에서 `getPassword()` 꺼내 비교 | `Member.authenticate(password)` | 비밀번호 일치 판단은 Member 자신의 책임 |
| `WishService`에서 `getMemberId()` 꺼내 비교 | `Wish.validateOwnership(memberId)` | 소유권 판단은 Wish 자신의 책임 |
| `OrderService`에서 `getPrice() * quantity` 계산 | `Order.calculatePrice()` | 주문 총액 계산은 Order의 책임 |
| `OrderService`에서 `wishRepository` 직접 호출 | `WishService.removeByMemberAndProduct()` 위임 | 도메인 경계를 넘는 Repository 직접 조작 제거 |

### 8. open-in-view=false 검증 테스트 추가
`OpenInViewTest`를 추가하여 LAZY 컬렉션(`Product.options`)을 트랜잭션 밖에서 접근 시 `LazyInitializationException`이 발생하는 것을 검증했다.

### 9. Admin Role 체계 및 접근 제어 도입
`Member`에 `Role`(USER/ADMIN) 필드를 추가하고, `AdminInterceptor`로 `/admin/**` 경로를 세션 기반으로 보호한다.

## 작동 변경 증거 목록

> **원칙: 작동 변경은 반드시 증거와 함께 제출한다.**
> 예외가 발생하는지만 확인하는 것으로 충분하지 않다. 상태를 재조회하거나 결과를 관찰 가능한 방식으로 검증해야 한다.

### 새 기능 · 동작 변경

| 작동 변경 | 증거 (테스트) | 검증 방식 |
|-----------|--------------|-----------|
| 주문 생성 시 위시 자동 삭제 (`85fa303`) | `주문_생성_성공_위시_자동_삭제` | 주문 후 위시 목록 재조회 → `hasSize(0)` 확인 |
| 옵션 수정 API 구현 (`cb6683f`) | `옵션_수정_성공` 외 4개 | 수정 응답에서 변경된 name·quantity 값 확인, 이름 중복·존재하지 않는 옵션 등 실패 케이스 상태코드 확인 |
| 상품 카테고리 필터링 (`ed63978`) | `상품_목록_조회_카테고리_필터링` 외 1개 | 카테고리별 조회 결과 건수·totalElements 확인, 페이지네이션 조합 검증 |
| GlobalExceptionHandler 상태코드 수정 (`93f6f0e`) | `ad3ff0a` 테스트 기대값 보정 | 재고 부족 500→400, 포인트 부족 500→400, 타인 위시 삭제 500→403 상태코드 확인 |
| 에러 메시지 한글 통일 (`3603f3d`) | 기존 `MemberAcceptanceTest` 10개 케이스 | 로그인·회원가입 성공/실패 시나리오 전체 통과 확인 |
| Admin 접근 제어 (`b86bfb3`) | `AdminAuthAcceptanceTest` 7개 케이스 | 미인증 시 302 리다이렉트, 로그인 후 세션으로 200 접근, 일반 유저 거부, 로그아웃 후 재접근 302 차단 |

### 구조 변경 (작동 유지)

구조를 바꾸되 외부 동작은 유지한 변경. 기존 인수 테스트가 증거 역할을 한다.

| 구조 변경 | 증거 (기존 인수 테스트) | 검증 방식 |
|-----------|----------------------|-----------|
| Service 계층 추출 — AdminMemberController → MemberService (`7101646`, `31d95e9`) | `MemberAcceptanceTest` 10개 케이스 | 회원가입·로그인 성공/실패, 토큰 발급 확인 |
| Service 계층 추출 — AdminProductController → ProductService (`6a199ff`, `15a785c`) | `ProductAcceptanceTest` 14개 케이스 | 상품 CRUD 전체, 페이지네이션·정렬·필터링 결과 확인 |
| Service 반환 타입 DTO→엔티티 — CategoryService (`765106b`) | `CategoryAcceptanceTest` | 카테고리 CRUD 응답 필드 값 확인 |
| Service 반환 타입 DTO→엔티티 — MemberService (`d76a7dc`) | `MemberAcceptanceTest` | 로그인·회원가입 토큰 반환 확인 |
| Service 반환 타입 DTO→엔티티 — OptionService (`38764b4`) | `OptionAcceptanceTest` | 옵션 CRUD 응답 필드 값 확인 |
| Service 반환 타입 DTO→엔티티 — ProductService (`808aad4`) | `ProductAcceptanceTest` | 상품 조회 응답 id·name·price·categoryId 확인 |
| Service 반환 타입 DTO→엔티티 — WishService (`eb45a49`) | `WishAcceptanceTest` | 위시 추가·삭제·목록 조회 결과 확인 |
| Service 반환 타입 변경 — KakaoAuthService (`40b51f9`) | `KakaoAuthAcceptanceTest` | 카카오 로그인 리다이렉트 URL 확인 |
| HandlerMethodArgumentResolver 도입 (`480b492`) | `OrderAcceptanceTest`, `WishAcceptanceTest` | Bearer 토큰 인증 후 주문·위시 CRUD 전체 통과 확인 |
| 위시 트랜잭션 통합 (`8c76d00`) | `WishAcceptanceTest` | 위시 추가 시 201(신규)/200(중복) 상태코드 및 목록 재조회 확인 |
| KakaoAuth 외부 API 트랜잭션 분리 (`b264a1b`) | `KakaoAuthAcceptanceTest` | 카카오 로그인 흐름 정상 동작 확인 |
| AdminProduct 트랜잭션 분리 제거 (`0ce299c`) | `ProductAcceptanceTest` | 상품 수정 후 응답 값 확인 |
| 비밀번호 비교 → Member.authenticate() (`236fbe4`) | `MemberAcceptanceTest` | 로그인 성공·비밀번호 불일치 시나리오 확인 |
| 소유권 검증 → Wish.validateOwnership() (`ebbcb77`) | `WishAcceptanceTest` | 타인 위시 삭제 시 403 상태코드 확인 |
| 가격 계산 → Order.calculatePrice() (`bc704c5`) | `OrderAcceptanceTest` | 주문 후 포인트 차감 금액 재조회 확인 |
| wish 삭제 → WishService 위임 (`80d246d`) | `OrderAcceptanceTest` | 주문 생성 후 위시 자동 삭제 재조회 확인 |
| Admin 서비스 메서드 네이밍 (`d32ddbf`, `84964df`, `da9923a`) | `ProductAcceptanceTest`, `MemberAcceptanceTest` | 기존 API 응답 값 전체 통과 확인 |
