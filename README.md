# spring-gift-refactoring

## 1단계 - 리팩터링 준비하기

> 핵심 목표: 작동을 바꾸기 쉬운 상태를 만든다. 구조 변경을 통해 변경 난이도를 낮추되 작동은 유지한다.

---

## 기능 목록

### 1. 스타일 정리 (작동 변경 없음)

- [x] `@Autowired` 제거 통일 — 단일 생성자 빈에서 불필요한 `@Autowired` 제거
  - 대상: `MemberController`, `AdminMemberController`, `AuthenticationResolver`, `JwtProvider`
  - 근거: Spring 4.3+부터 단일 생성자 빈은 `@Autowired` 없이도 자동 주입되므로 불필요한 어노테이션을 제거하여 코드 노이즈를 줄인다.
- [x] `@RequestMapping` 속성 스타일 통일 — `path = "/..."` 대신 `"/..."` 축약형으로 통일
  - 대상: `KakaoAuthController`, `OptionController`
  - 근거: `value`가 기본 속성이므로 `path = "..."` 명시는 불필요한 장식이며, 축약형으로 통일하여 일관성을 높인다.
- [x] 변수 선언 스타일 통일 — `var` 제거 후 명시적 타입으로 통일, 불필요한 `final` 제거
  - 대상: `OrderController`, `WishController`, `MemberController`, `AdminMemberController`, `AuthenticationResolver`
  - 근거: 명시적 타입 선언으로 가독성을 높이고, 지역 변수의 `final`은 재할당 방지 실익이 적어 제거하여 선언부를 간결하게 한다.
- [x] 주석/Javadoc 스타일 통일 — Javadoc(`/** */`) 또는 블록 주석(`/* */`) 중 하나로 통일
  - 대상: `WishController`, `OrderController`, `Order`, `Wish`, `Member`의 한 줄 주석(`//`)을 Javadoc(`/** */`)으로 변환
  - 근거: 기존 `member`·`auth` 패키지에서 이미 Javadoc 스타일을 사용하고 있으므로, 나머지 한 줄 주석도 Javadoc으로 통일하여 프로젝트 전체의 주석 형식을 일관되게 한다.
- [x] HTTP 상태 코드 표현 통일 — 매직 넘버(`401`, `403`) 대신 `HttpStatus` 열거형 사용
  - 대상: `WishController`, `OrderController`
  - 근거: 매직 넘버는 의미를 즉시 파악하기 어려우므로, `HttpStatus.UNAUTHORIZED`·`HttpStatus.FORBIDDEN` 열거형으로 대체하여 의도를 명확히 한다.
- [x] Stream 종결 연산 통일 — `.collect(Collectors.toList())` → `.toList()`로 통일
  - 대상: `OptionController`
  - 근거: `.toList()`는 불변 리스트를 반환하며 내부 최적화가 적용되어 더 간결하고 효율적이다. 수집한 리스트를 응답으로 반환만 하므로 가변성이 불필요하여 안전하게 전환할 수 있다.
- [x] 에러 메시지 언어 통일 — 영어 메시지를 한국어로 통일
  - 대상: `MemberController`, `AdminMemberController`, `Member`
  - 근거: Validator와 도메인 로직 대부분이 이미 한국어 메시지를 사용하므로, 나머지 영어 메시지도 한국어로 통일하여 사용자에게 일관된 오류 안내를 제공한다.
- [x] `ResponseEntity` 제네릭 타입 통일 — `ResponseEntity<?>` 대신 구체적 타입 사용
  - 대상: `OrderController`의 `getOrders`(`Page<OrderResponse>`), `createOrder`(`OrderResponse`)
  - 근거: 와일드카드(`?`)는 반환 타입 정보를 숨겨 API 문서 자동 생성이나 호출부에서 타입 추론이 불가능하므로, 구체적 제네릭 타입을 명시하여 컴파일 타임 타입 안전성과 가독성을 높인다.

### 2. 불필요한 코드 제거 (작동 변경 없음)

- [x] `OrderController`에서 미사용 `WishRepository` 의존성 제거
  - 필드, 생성자 파라미터, import 모두 제거 (주석 "6. cleanup wish"도 함께 제거)
  - 근거: 주석에 위시 정리 의도가 있으나 실제 구현이 없는 미사용 의존성이므로, 제거하여 불필요한 빈 주입과 혼란을 방지한다.
- [x] `OptionController`에서 `import java.util.stream.Collectors` 제거 (`.toList()` 전환 후)
  - 근거: `.toList()` 전환으로 `Collectors` 클래스를 더 이상 참조하지 않으므로, 미사용 import를 제거하여 불필요한 의존을 없앤다.
- [x] 각 Controller의 중복 `@ExceptionHandler` 제거
  - `ProductController`, `OptionController`, `MemberController`의 동일한 `IllegalArgumentException` 핸들러를 `GlobalExceptionHandler`(`@RestControllerAdvice`)로 통합
  - 근거: 동일한 예외 처리 로직이 3개 컨트롤러에 중복되어 있으므로, `@RestControllerAdvice`로 한 곳에서 관리하여 변경 시 누락 위험을 없앤다.

### 3. 서비스 계층 추출 (구조 변경, 작동 변경 없음)

- [x] **ProductService** 추출
  - `ProductController`의 상품 CRUD 로직 (이름 검증, 카테고리 조회, 생성/수정/삭제)
  - `AdminProductController`의 상품 CRUD 로직 (동일 로직 중복 제거)
  - 근거: 두 컨트롤러에 분산된 상품 비즈니스 로직을 `ProductService`로 통합하여 중복을 제거하고, 컨트롤러는 HTTP 요청/응답 처리만 담당하도록 역할을 분리한다.
- [x] **CategoryService** 추출
  - `CategoryController`의 카테고리 CRUD 로직 (조회, 생성, 수정, 삭제)
  - 근거: `CategoryController`가 `CategoryRepository`를 직접 사용하던 구조에서 `CategoryService`를 추출하여 비즈니스 로직을 분리하고, 컨트롤러는 HTTP 요청/응답 처리만 담당하도록 역할을 나눈다. `updateCategory`의 null 체크 패턴을 `orElseThrow`로 교체하여 `GlobalExceptionHandler`와 일관된 예외 처리를 적용한다.
- [x] **MemberService** 추출
  - `MemberController`의 회원 가입/로그인 로직
  - `AdminMemberController`의 회원 관리/포인트 충전 로직
  - 근거: 회원 가입·로그인·관리 로직을 `MemberService`로 통합하여, 컨트롤러는 HTTP/뷰 처리만 담당하고 비즈니스 로직은 서비스에서 일관되게 관리한다.
- [x] **OptionService** 추출
  - `OptionController`의 옵션 CRUD 로직 (이름 검증, 중복 확인, 최소 1개 보장 규칙)
  - 근거: `OptionController`가 `OptionRepository`·`ProductRepository`를 직접 사용하며 이름 검증, 중복 확인, 최소 1개 보장 등 비즈니스 로직을 수행하고 있으므로, `OptionService`로 추출하여 컨트롤러는 HTTP 요청/응답 처리만 담당하도록 역할을 분리한다.
- [x] **OrderService** 추출 (`@Transactional` 적용)
  - `OrderController`의 주문 생성 로직 (재고 차감 → 포인트 차감 → 주문 저장 → 카카오 메시지)
  - 근거: 주문 생성 시 재고 차감·포인트 차감·주문 저장이 원자적으로 처리되어야 하므로 `@Transactional`을 적용한 서비스로 추출하고, 컨트롤러는 인증 확인과 HTTP 응답 처리만 담당하도록 분리한다.
- [x] **WishService** 추출
  - `WishController`의 위시리스트 조회/추가/삭제 로직 (중복 확인, 소유권 검증)
  - 근거: `WishController`의 비즈니스 로직을 `WishService`로 추출하여 컨트롤러는 HTTP 요청/응답 처리만 담당하도록 역할을 분리한다. 소유권 검증 실패(403)를 예외 기반으로 처리하기 위해 `ForbiddenException`을 추가하고 `GlobalExceptionHandler`에 핸들러를 등록한다. `addWish`의 중복 확인·생성 분기는 HTTP 상태 코드 분기가 아닌 비즈니스 규칙이므로 서비스 내부에서 멱등하게 처리하고, 컨트롤러는 항상 200 OK를 반환하도록 단순화한다.
- [x] **KakaoAuthController** 리팩터링
  - `MemberRepository`를 직접 사용하여 회원 조회·저장·카카오 액세스 토큰 업데이트를 수행하고 있음
  - 근거: `KakaoAuthController`가 `MemberRepository`를 직접 사용하여 회원 조회·생성·카카오 토큰 업데이트를 수행하고 있으므로, `MemberService`에 `findOrCreateByKakaoLogin` 메서드를 추가하고 컨트롤러는 서비스에 위임하도록 변경하여 회원 관련 영속성 로직을 한 곳에서 관리한다.
- [x] **AdminProductController** 리팩터링
  - `CategoryRepository`를 직접 사용하여 카테고리 목록을 조회하고 있음
  - 근거: `AdminProductController`가 `CategoryRepository`를 직접 사용하여 카테고리 목록을 조회하고 있으므로, 이미 존재하는 `CategoryService.getCategories()`에 위임하도록 변경하여 컨트롤러가 Repository에 직접 의존하지 않도록 한다.
- [x] 각 Controller가 요청 검증 + Service 위임만 수행하는지 최종 확인

---

## 구현 전략

### 작업 순서

작동 변경 없이 구조만 개선하므로, 안전한 순서대로 진행한다.

```
스타일 정리 → 불필요한 코드 제거 → 서비스 계층 추출
```

### 단계별 전략

**1단계: 스타일 정리**

- KtLint/포매터를 먼저 적용하여 자동으로 잡을 수 있는 항목을 처리한다.
- 이후 수동으로 `@Autowired`, `var`/명시적 타입, 주석 스타일 등을 통일한다.
- 한 가지 스타일 항목씩 커밋한다 (예: "style: remove unnecessary @Autowired annotations").

**2단계: 불필요한 코드 제거**

- IDE 정적 분석(미사용 import, 미사용 필드)을 활용하여 제거 대상을 식별한다.
- `OrderController`의 `WishRepository`는 주석에 의도("cleanup wish")가 있으나 미구현 상태이므로, 이 단계에서는 미사용 코드로 판단하여 제거한다.
- 제거 전 `git blame`으로 추가 의도를 확인한다.

**3단계: 서비스 계층 추출**

- 도메인별로 하나씩 Service를 추출하며, 각 Service 추출마다 별도 커밋한다.
- 추출 순서: 의존이 적은 것부터 → 의존이 많은 것 순서로 진행한다.
  1. `CategoryService` (의존 없음, 가장 단순)
  2. `ProductService` (Category 의존)
  3. `MemberService` (독립적)
  4. `OptionService` (Product 의존)
  5. `WishService` (Member, Product 의존)
  6. `OrderService` (Member, Option, KakaoMessageClient 의존 — 가장 복잡, `@Transactional` 필수)
- API Controller와 Admin Controller가 동일 Service를 공유하도록 하여 중복 로직을 제거한다.
- 각 추출 후 전체 테스트를 실행하여 작동이 유지되는지 확인한다.

### 커밋 컨벤션

[AngularJS Git Commit Message Conventions](https://gist.github.com/stephenparish/9941e89d80e2bc58a153) 을 따른다.

| 접두사 | 용도 |
|---|---|
| `style` | 스타일 정리 (코드 포맷, 네이밍 통일 등) |
| `refactor` | 구조 변경 (서비스 계층 추출 등) |
| `chore` | 불필요한 코드 제거, 빌드 설정 등 |
| `docs` | README 등 문서 작성 |

### 검증 방법

- 매 커밋 전 `./gradlew test` 전체 테스트 통과 확인
- `./gradlew ktlintCheck`로 스타일 위반 확인
- 구조 변경 커밋에 작동 변경이 섞이지 않았는지 `git diff`로 확인

---

## 테스트 전략

### 목표

리팩터링된 코드의 **정확성을 보장**하고, 이후 변경에 대한 **안전망**을 구축한다.

### 테스트 피라미드

```
        ╱  인수 테스트  ╲          ← API 전체 흐름 (Cucumber + REST Assured)
       ╱               ╲
      ╱  서비스 단위 테스트  ╲       ← 비즈니스 로직 (Mockito)
     ╱                    ╲
    ╱  도메인 모델 단위 테스트   ╲    ← 핵심 규칙 (순수 Java, 의존성 없음)
```

### 1단계: 도메인 모델 단위 테스트 (우선순위 높음)

Spring 컨텍스트 없이 순수 Java로 작성한다. 실행 속도가 빠르고 핵심 비즈니스 규칙을 촘촘하게 검증한다.

| 대상 | 테스트 항목 |
|---|---|
| `Member.deductPoint()` | 정상 차감, 잔액 부족 예외, 음수/0 금액 예외, 잔액 정확히 일치 |
| `Member.chargePoint()` | 정상 충전, 음수/0 금액 예외 |
| `Option.subtractQuantity()` | 정상 차감, 재고 부족 예외, 재고 정확히 일치 |

### 2단계: 인수 테스트 확장 (우선순위 높음)

기존 Cucumber 인프라를 활용하여 Order 외 도메인으로 확장한다.

| Feature | 주요 시나리오 |
|---|---|
| `product.feature` | 상품 CRUD, 이름 검증 실패, 존재하지 않는 카테고리 |
| `option.feature` | 옵션 추가/삭제, 중복 이름 방지, 마지막 옵션 삭제 방지 |
| `wish.feature` | 위시 추가/삭제, 중복 추가 멱등성, 타인 위시 삭제 시 403 |
| `member.feature` | 회원가입, 로그인, 중복 이메일 거부 |
| `category.feature` | 카테고리 CRUD |

### 3단계: 서비스 단위 테스트 (우선순위 보통)

Mockito로 Repository를 모킹하여 서비스 로직만 격리 테스트한다. 복잡한 서비스부터 작성한다.

| 대상 | 테스트 항목 |
|---|---|
| `OrderService` | 정상 주문 생성, 재고 부족 시 실패, 포인트 부족 시 실패, 카카오 토큰 없을 때 메시지 미전송 |
| `OptionService` | 마지막 옵션 삭제 방지, 중복 이름 검증, 존재하지 않는 상품 예외 |
| `WishService` | 중복 위시 멱등 처리, 타인 위시 삭제 시 ForbiddenException |
| `MemberService` | 중복 이메일 가입 거부, 비밀번호 불일치 로그인 실패 |

> `CategoryService`는 단순 CRUD이므로 인수 테스트로 충분히 커버되어 단위 테스트를 생략한다.

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 단위 테스트만 (Spring 컨텍스트 불필요)
./gradlew test --tests "gift.domain.*"

# 인수 테스트만 (Cucumber)
./gradlew test --tests "gift.CucumberTest"
```

---

## 도메인 모델 단위 테스트 기능 목록

Spring 컨텍스트 없이 순수 Java로 작성한다. 엔티티의 비즈니스 메서드가 핵심 규칙을 올바르게 지키는지 검증한다.

### 1. `Member` 단위 테스트 (`MemberTest`) — 7개 케이스

| 테스트 케이스 | 검증 내용 | 상태 |
|-------------|----------|------|
| `chargePoint` — 정상 충전 | 1000 충전 → 포인트 1000 | [x] |
| `chargePoint` — 0 이하 금액 | 0 충전 → `IllegalArgumentException` | [x] |
| `chargePoint` — 음수 금액 | -100 충전 → `IllegalArgumentException` | [x] |
| `deductPoint` — 정상 차감 | 10000에서 3000 차감 → 포인트 7000 | [x] |
| `deductPoint` — 잔액 정확히 일치 | 5000에서 5000 차감 → 포인트 0 | [x] |
| `deductPoint` — 잔액 부족 | 1000에서 5000 차감 → `IllegalArgumentException` | [x] |
| `deductPoint` — 0 이하 금액 | 0 차감 → `IllegalArgumentException` | [x] |

### 2. `Option` 단위 테스트 (`OptionTest`) — 4개 케이스

| 테스트 케이스 | 검증 내용 | 상태 |
|-------------|----------|------|
| `subtractQuantity` — 정상 차감 | 재고 10에서 3 차감 → 재고 7 | [x] |
| `subtractQuantity` — 재고 정확히 일치 | 재고 10에서 10 차감 → 재고 0 | [x] |
| `subtractQuantity` — 재고 부족 | 재고 5에서 10 차감 → `IllegalArgumentException` | [x] |
| `subtractQuantity` — 0개 차감 | 재고 변동 없음 확인 | [x] |

---

## 도메인 모델 단위 테스트 파일 구조

```
src/test/java/gift/
├── member/
│   ├── MemberTest.java
│   └── PasswordTest.java
└── option/
    └── OptionTest.java
```

---

## 서비스 단위 테스트 기능 목록

Mockito로 Repository를 모킹하여 서비스 로직만 격리 테스트한다. `OrderService.createOrder`만 대상으로 한다.

### `OrderService.createOrder` 단위 테스트 (`OrderServiceTest`) — 6개 케이스

| 테스트 케이스 | 검증 내용 | 상태 |
|-------------|----------|------|
| 정상 주문 생성 | 옵션 재고 차감 + 회원 포인트 차감 + 주문 저장 확인 | [x] |
| 존재하지 않는 옵션 | 없는 optionId → `NoSuchElementException` | [x] |
| 재고 부족 | 재고보다 많은 수량 → `IllegalArgumentException` (옵션의 `subtractQuantity`에서 발생) | [x] |
| 포인트 부족 | 잔액보다 큰 금액 → `IllegalArgumentException` (회원의 `deductPoint`에서 발생) | [x] |
| 카카오 토큰이 없는 회원 | `kakaoAccessToken == null` → `kakaoMessageClient.sendToMe` 미호출 | [x] |
| 카카오 메시지 전송 실패 | `sendToMe`에서 예외 발생 → 주문은 정상 저장 | [x] |

---

## 서비스 단위 테스트 파일 구조

```
src/test/java/gift/
└── order/
    └── OrderServiceTest.java
```

---

## 인수 테스트 기능 목록

### 1. 회원 (`member.feature`) — 5개 시나리오

| 시나리오 | 검증 내용 | 상태 |
|---------|----------|------|
| 회원가입 성공 | POST `/api/members/register` → 201 + 토큰 발급 | [x] |
| 중복 이메일 회원가입 | 이미 등록된 이메일 → 400 | [x] |
| 로그인 성공 | POST `/api/members/login` → 200 + 토큰 발급 | [x] |
| 잘못된 비밀번호 로그인 | 비밀번호 불일치 → 400 | [x] |
| 존재하지 않는 이메일 로그인 | 미등록 이메일 → 400 | [x] |

### 2. 카테고리 (`category.feature`) — 5개 시나리오

| 시나리오 | 검증 내용 | 상태 |
|---------|----------|------|
| 카테고리 생성 | POST `/api/categories` → 201 + 카테고리 정보 | [x] |
| 카테고리 목록 조회 | GET `/api/categories` → 200 + 등록된 개수 확인 | [x] |
| 카테고리 수정 | PUT `/api/categories/{id}` → 200 + 변경된 이름 확인 | [x] |
| 카테고리 삭제 | DELETE `/api/categories/{id}` → 204 | [x] |
| 존재하지 않는 카테고리 수정 | 없는 ID로 수정 → 404 | [x] |

### 3. 상품 (`product.feature`) — 8개 시나리오

| 시나리오 | 검증 내용 | 상태 |
|---------|----------|------|
| 상품 생성 | POST `/api/products` → 201 + 상품 정보 | [x] |
| 상품 목록 페이징 조회 | GET `/api/products` → 200 + 등록된 개수 확인 | [x] |
| 상품 단건 조회 | GET `/api/products/{id}` → 200 + 이름 확인 | [x] |
| 상품 수정 | PUT `/api/products/{id}` → 200 + 변경된 이름/가격 확인 | [x] |
| 상품 삭제 | DELETE `/api/products/{id}` → 204 | [x] |
| 존재하지 않는 상품 조회 | 없는 ID로 조회 → 404 | [x] |
| 15자 초과 이름으로 생성 | 이름 길이 검증 → 400 | [x] |
| "카카오" 포함 이름으로 생성 | 금지어 검증 → 400 | [x] |

### 4. 옵션 (`option.feature`) — 7개 시나리오

| 시나리오 | 검증 내용 | 상태 |
|---------|----------|------|
| 옵션 추가 | POST `/api/products/{id}/options` → 201 + 옵션 정보 | [x] |
| 옵션 목록 조회 | GET `/api/products/{id}/options` → 200 + 등록된 개수 확인 | [x] |
| 중복 옵션명 추가 | 같은 이름의 옵션 → 400 | [x] |
| 옵션 삭제 (2개 이상) | DELETE → 204 | [x] |
| 마지막 옵션 삭제 | 옵션이 1개뿐일 때 삭제 → 400 | [x] |
| 50자 초과 이름으로 추가 | 이름 길이 검증 → 400 | [x] |
| 존재하지 않는 상품에 추가 | 없는 상품 ID → 404 | [x] |

### 5. 위시리스트 (`wish.feature`) — 7개 시나리오

| 시나리오 | 검증 내용 | 상태 |
|---------|----------|------|
| 위시 추가 | POST `/api/wishes` → 200 + 위시 정보 | [x] |
| 중복 위시 추가 | 이미 있는 상품 → 기존 위시 반환 (멱등) | [x] |
| 위시 목록 조회 | GET `/api/wishes` → 200 + 등록된 개수 확인 | [x] |
| 위시 삭제 | DELETE `/api/wishes/{id}` → 204 | [x] |
| 타인의 위시 삭제 | 다른 회원의 위시 삭제 → 403 | [x] |
| 인증 없이 조회 | Authorization 헤더 없이 → 400 | [x] |
| 존재하지 않는 상품 추가 | 없는 상품 ID → 404 | [x] |

### 6. 주문 (`order.feature`) — 7개 시나리오

| 시나리오 | 검증 내용 | 상태 |
|---------|----------|------|
| 유효한 주문 | 201 + 재고 차감 + 포인트 차감 | [x] |
| 존재하지 않는 옵션으로 주문 | 404 | [x] |
| 재고 초과 주문 | 400 | [x] |
| 포인트 부족 주문 | 400 | [x] |
| 인증 없이 주문 | 400 | [x] |
| 재고와 동일한 수량 주문 | 201 + 재고 0 | [x] |
| 주문 후 목록 조회 | 200 + 주문 내역 포함 | [x] |

---

## 인수 테스트 파일 구조

```
src/test/java/gift/
├── CucumberTest.java
├── CucumberSpringConfiguration.java
└── steps/
    ├── Hooks.java              # DB 초기화
    ├── SharedContext.java       # 상태 공유
    ├── MemberSteps.java
    ├── CategorySteps.java
    ├── ProductSteps.java
    ├── OptionSteps.java
    ├── WishSteps.java
    └── OrderSteps.java

src/test/resources/features/
├── member.feature               # 5개 시나리오
├── category.feature             # 5개 시나리오
├── product.feature              # 8개 시나리오
├── option.feature               # 7개 시나리오
├── wish.feature                 # 7개 시나리오
└── order.feature                # 7개 시나리오
```

---

## AI 활용 기록

### 활용 방식

- **Claude Code** (Anthropic CLI)를 페어 프로그래밍 도구로 활용함
- AI에게 한 번에 하나의 작업 단위만 지시하고, 매 결과를 직접 검토한 뒤 다음 단계로 진행함
- AI가 생성한 코드를 그대로 수용하지 않고, 불필요한 코드 제거·시나리오 문구 수정 등을 직접 수행함

### AI를 활용한 작업과 수정 내역

| 작업 | AI 활용 내용 | 직접 수정한 부분 |
|------|------------|----------------|
| 리팩터링 대상 식별 | 프로젝트 전체 코드를 분석하여 스타일 불일치, 미사용 코드, 서비스 추출 대상 목록을 도출 | 분석 결과를 검토하고 기능 목록과 구현 전략을 직접 정리, 작업 순서와 커밋 단위를 설계 |
| 서비스 계층 추출 | Controller에서 Service로 비즈니스 로직을 추출하는 코드 생성 | 추출된 코드의 메서드 시그니처, 예외 처리 방식, `@Transactional` 적용 범위를 검토·조정 |
| 테스트 계획 수립 | 테스트 피라미드 기반으로 도메인 모델·서비스·인수 테스트 시나리오 목록을 초안 작성 | 시나리오 우선순위와 범위를 조정 (예: 서비스 단위 테스트는 `OrderService.createOrder`만 대상으로 한정) |
| 테스트 코드 작성 | Cucumber Feature 파일과 Step 정의, JUnit/Mockito 단위 테스트 코드 생성 | 50자 경계값 테스트 문자열 길이 오류 수정, Steps 클래스의 미사용 필드 제거 등 직접 보정 |

### 학습한 점

- **리팩터링 순서의 중요성**: 스타일 정리 → 불필요한 코드 제거 → 구조 변경 순서로 진행해야 각 단계의 diff가 깨끗하게 유지되고, 구조 변경 시 노이즈가 줄어든다는 것을 체감함
- **테스트 피라미드 설계**: 도메인 모델 단위 테스트로 핵심 규칙을 빠르게 검증하고, 인수 테스트로 API 전체 흐름을 보장하는 계층 구조가 리팩터링의 안전망으로 효과적임을 확인함
- **AI 산출물 검증의 필요성**: AI가 생성한 경계값 테스트 문자열이 정확히 50자여서 테스트가 실패하는 사례를 통해, AI 산출물을 반드시 실행·검증해야 한다는 점을 재확인함
- **커밋 단위 분리**: 구조 변경과 작동 변경을 한 커밋에 섞지 않고, 목적 1개로 커밋을 구성하는 습관이 코드 리뷰와 롤백에 유리함을 실감함

### 1단계 페어(paul.an) 코드 리뷰 반영

- [x] **Password 일급객체 도입 및 BCrypt 해싱 적용**
- [x] **MemberService 중복 메서드 통일**
- [x] **영속 엔티티의 불필요한 `save()` 호출 제거 — JPA 더티 체킹 활용 (`MemberService`, `OrderService`)**
- [x] **OAuth 로그인 추상화** — `OAuthLoginClient` 인터페이스 도입, `OAuthLoginService`로 로그인 흐름 캡슐화, `Member` 필드명 일반화 (`kakaoAccessToken` → `oauthAccessToken`)
- [x] **주문 메시지 전송 전략 패턴 추상화** — `OrderMessageClient` 인터페이스 도입, `OrderService`의 `KakaoMessageClient` 직접 의존 제거
- [x] **ProductService 중복 메서드 통합 및 `@Transactional` 적용** — `saveProduct` overload 2개 제거 후 `createProduct`/`updateProduct`로 통합, 읽기 메서드에 `readOnly`, `updateProduct`에서 dirty checking 활용
- [x] **AuthenticationResolver의 MemberRepository 직접 참조를 MemberService로 교체** — 레이어드 아키텍처 원칙에 따라 Resolver가 Repository를 우회하지 않고 서비스 계층을 통해 회원을 조회하도록 변경
- [x] **`@Login` 애노테이션 + HandlerMethodArgumentResolver 도입** — `AuthenticationResolver`를 `HandlerMethodArgumentResolver`로 변환하고 `@Login Member member` 파라미터로 인증된 회원을 자동 주입하여 컨트롤러의 인증 보일러플레이트 제거
- [x] **OptionService `@Transactional` 적용** — 읽기 메서드에 `readOnly = true`, CUD 메서드에 `@Transactional`을 적용하여 트랜잭션 경계를 명시하고 DB 최적화 및 데이터 정합성 보장
- [x] **Option 엔티티에 `calculatePrice` 메서드 추가** — `option.getProduct().getPrice() * quantity` 계산을 `option.calculatePrice(quantity)`로 캡슐화하여 디미터 법칙을 준수하고 `OrderService.createOrder`의 추상화 레벨을 통일

---


## 2단계 - 리팩터링 완성하기

### 2단계 기능 요구 사항 분석

> 핵심 목표: 작동 변경을 안전하게 수행하고, 그 결과를 증거로 보여준다.

---

### 트랜잭션 경계 세우기

여러 저장 작업이 하나의 논리 작업이라면, 중간 실패에서 부분 반영이 발생하지 않도록 경계를 설정한다.

- [x] **OrderService** — `createOrder`에 `@Transactional` 적용 (재고 차감 → 포인트 차감 → 주문 저장이 원자적으로 처리)
- [x] **ProductService** — CUD 메서드에 `@Transactional`, 읽기 메서드에 `readOnly = true` 적용
- [x] **OptionService** — CUD 메서드에 `@Transactional`, 읽기 메서드에 `readOnly = true` 적용
- [x] **OAuthLoginService** — `login()`에 `@Transactional` 적용 (회원 조회/생성 + 토큰 업데이트가 원자적으로 처리)
- [x] **CategoryService** — CUD 메서드에 `@Transactional`, 읽기 메서드에 `readOnly = true` 적용. `updateCategory()`에서 `save()` 호출 제거하여 더티 체킹 활용 (기존 `ProductService.updateProduct` 패턴과 동일)
- [x] **MemberService** — `register()`, `deleteMember()`에 `@Transactional`, `login()`, `getAllMembers()`, `getMember()`, `getMemberByEmail()`에 `readOnly = true` 적용
- [x] **WishService** — `addWish()`, `removeWish()`에 `@Transactional`, `getWishes()`에 `readOnly = true` 적용

---

### 누락된 작동 구현

기존 코드에 의도가 남아 있었지만 구현되지 않은 작동을 완료한다.

- [x] **주문 완료 시 위시리스트 자동 제거** — `OrderService.createOrder()`에서 주문 저장 후 해당 상품이 회원의 위시리스트에 있으면 자동 제거. 1단계에서 `OrderController`에 남아 있던 미구현 의도를 서비스 계층에서 구현.
  - 변경 전: 주문 완료 후 위시리스트에 해당 상품이 그대로 남아 있음
  - 변경 후: `wishRepository.deleteByMemberIdAndProductId(memberId, productId)` 호출로 자동 정리

---

### 도메인 책임 되찾기

작동을 유지하면서도 책임과 계산, 판단을 올바른 위치로 이동해 누수와 중복을 줄인다. 변경 전후가 분명한 개선을 최소 2개 이상 수행한다.

- [x] **Option.calculatePrice() 도입** — `OrderService`에서 `option.getProduct().getPrice() * quantity`로 직접 계산하던 로직을 `option.calculatePrice(quantity)`로 캡슐화. 디미터 법칙 위반을 해소하고 가격 계산 책임을 도메인 엔티티로 이동.
  - 변경 전: `int price = option.getProduct().getPrice() * quantity;` (서비스에서 내부 구조 노출)
  - 변경 후: `int price = option.calculatePrice(quantity);` (도메인 메서드 호출)
- [x] **Password 일급객체 도입** — `MemberService`에서 비밀번호 값을 꺼내 평문 비교하던 로직을 `Password` 값 객체로 캡슐화. 암호화 전략이 도메인 내부에 숨겨지고, `Member.checkPassword()`로 비밀번호 검증 책임이 엔티티로 이동.
  - 변경 전: `member.getPassword() == null || !member.getPassword().equals(password)` (서비스에서 직접 처리)
  - 변경 후: `Password.of(rawPassword)` / `member.checkPassword(rawPassword)` (도메인 캡슐화)

---

## ADR

### 1. 트랜잭션 경계 세우기 — 메서드 단위 `@Transactional` 적용

**맥락**

Spring Data JPA의 `SimpleJpaRepository`는 개별 레포지토리 메서드에 `@Transactional`을 선언한다. 서비스 메서드에 별도 트랜잭션을 선언하지 않으면 레포지토리 호출마다 독립 트랜잭션이 열리므로, 하나의 비즈니스 작업 안에서 여러 레포지토리를 호출할 때 중간 실패 시 부분 반영이 발생할 수 있다.

**선택지**

| | 방식 | 장점 | 단점 |
|---|---|---|---|
| A | 클래스 레벨 `@Transactional` | 선언 한 줄로 전체 적용, 누락 위험 없음 | 읽기 메서드까지 쓰기 트랜잭션으로 실행되어 불필요한 더티 체킹 발생, `readOnly` 최적화 불가 |
| B | 메서드 레벨 `@Transactional` | CUD와 읽기를 구분하여 `readOnly = true` 적용 가능, 메서드별 의도 명시 | 메서드마다 어노테이션을 붙여야 하므로 누락 가능성 존재 |

**결정**: B안 — 메서드 레벨 적용

**근거**

- 읽기 메서드에 `readOnly = true`를 적용하면 Hibernate 플러시 모드가 `MANUAL`로 설정되어 더티 체킹을 건너뛰고, JDBC 드라이버에 읽기 전용 힌트를 전달하여 DB 수준 최적화(리플리카 라우팅 등)가 가능하다.
- 메서드 시그니처에 트랜잭션 속성이 명시되므로, 해당 메서드가 데이터를 변경하는지 조회만 하는지 코드만으로 판단할 수 있다.
- 이미 `ProductService`, `OptionService`, `OrderService`가 이 패턴을 사용하고 있으므로 프로젝트 전체의 일관성을 유지한다.

---

### 2. 인터페이스 사용 전략 — 외부 연동 경계에만 인터페이스 도입

**맥락**

Spring 프레임워크에서는 DI를 활용하기 위해 "모든 서비스에 인터페이스를 정의하고 구현체를 분리"하는 관행이 있었다. 그러나 구현체가 하나뿐인 서비스에 인터페이스를 만들면 클래스 수만 늘어나고, 이름 짓기가 어려워지며, 코드 탐색 시 한 단계가 추가되어 가독성이 떨어진다.
반면 외부 시스템 연동(OAuth 로그인, 메시지 발송 등)은 플랫폼 교체 가능성이 높아 구현을 추상화할 실질적 이유가 존재한다.

**선택지**

| | 방식 | 장점 | 단점 |
|---|---|---|---|
| A | 모든 서비스에 인터페이스 도입 | DIP 원칙 완전 준수, 테스트 시 mock 교체 용이 | 구현체가 하나뿐일 때 불필요한 간접 계층 증가, `*ServiceImpl` 네이밍 문제, 파일 수 2배 |
| B | 인터페이스 전면 생략 | 코드 최소화, 탐색 용이 | 외부 연동 교체 시 서비스 코드 직접 수정 필요, 전략 패턴 적용 불가 |
| C | 외부 연동 경계에만 인터페이스 도입 | 교체 가능성이 있는 곳만 추상화하여 실용적, 내부 서비스는 간결하게 유지 | 경계 판단 기준이 주관적일 수 있음 |

**결정**: C안 — 외부 연동 경계에만 인터페이스 도입

**근거**

- 현재 프로젝트에서 `ProductService`, `MemberService` 등 내부 서비스는 구현체가 하나뿐이며, 교체 시나리오가 존재하지 않는다. 인터페이스를 도입하면 클래스 수만 늘어나고 탐색 비용이 증가한다.
- `OAuthLoginClient`는 카카오 외에 네이버·구글 등 다른 OAuth 제공자로 교체될 수 있고, `OrderMessageClient`는 카카오 메시지 외에 SMS·이메일 등 다른 알림 채널로 교체될 수 있다. 이처럼 실질적 교체 가능성이 있는 외부 연동 지점에만 인터페이스를 두어 전략 패턴을 적용한다.
- Spring의 CGLIB 프록시는 구체 클래스도 프록시할 수 있으므로, `@Transactional` 등 AOP 적용을 위해 인터페이스가 필수적이지 않다.
- Mockito 등 테스트 프레임워크도 구체 클래스 모킹을 지원하므로, 테스트를 위해 인터페이스를 만들 필요가 없다.

---

### 3. 도메인 책임 되찾기 — 계산·판단 로직을 엔티티와 값 객체로 이동

**맥락**

서비스 계층 추출 후 비즈니스 로직이 서비스에 집중되면서, 도메인 엔티티가 데이터만 들고 있는 빈약한 도메인 모델에 가까워졌다. 구체적으로 두 가지 문제가 있었다:

1. `OrderService.createOrder`에서 `option.getProduct().getPrice() * quantity`로 가격을 직접 계산 — `Option`의 내부 구조(`Product`의 `price`)를 서비스가 알아야 하므로 디미터 법칙(Law of Demeter) 위반
2. `MemberService`에서 `BCryptPasswordEncoder`로 비밀번호 인코딩·매칭을 직접 수행 — 암호화 전략이 서비스에 노출되어 `Member`가 자신의 비밀번호 검증 책임을 갖지 못함

**선택지**

| | 방식 | 장점 | 단점 |
|---|---|---|---|
| A | 서비스에서 계산·판단 로직 유지 | 엔티티가 단순한 데이터 홀더로 유지되어 역할이 명확 | 서비스가 비대해지고, 동일 계산이 여러 서비스에 중복될 위험, 디미터 법칙 위반 |
| B | 엔티티·값 객체에 계산·판단 로직 이동 | 도메인 객체가 자신의 데이터에 대한 책임을 가짐, 서비스는 흐름 조율에 집중, 중복 제거 | 엔티티에 로직이 추가되어 JPA 매핑과의 경계 관리 필요 |

**결정**: B안 — 엔티티·값 객체에 계산·판단 로직 이동

**근거**

- 가격 계산은 `Option`이 자신의 연관 객체(`Product`)를 통해 수행하는 것이 자연스럽다. `option.calculatePrice(quantity)`로 캡슐화하면 서비스가 `Option`의 내부 구조를 알 필요가 없어지고, 가격 계산 정책이 변경되더라도 `Option` 한 곳만 수정하면 된다.
- 비밀번호 인코딩·매칭은 `Password` 값 객체(`@Embeddable`)로 캡슐화하면 암호화 전략(`BCryptPasswordEncoder`)이 도메인 내부에 숨겨진다. `Member.checkPassword(rawPassword)`로 비밀번호 검증 책임이 엔티티로 이동하여, 서비스는 인증 흐름만 조율한다.
- 두 변경 모두 서비스의 추상화 레벨을 통일하는 효과가 있다. `createOrder` 메서드가 "재고 차감 → 포인트 차감 → 주문 저장"이라는 비즈니스 흐름을 균일한 수준으로 표현하게 된다.

---