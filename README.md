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
│   └── MemberTest.java
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

---

## 2단계 - 리팩터링 완성하기

> 원칙: 구조 변경과 작동 변경을 섞지 않는다. 각 단계는 하나의 커밋 단위이다.

---

### 트랜잭션 경계 세우기

#### Step 1. socialAccessToken 저장을 트랜잭션 안으로 이동 (작동 변경)

- **바꾸는 것**: `KakaoAuthController.callback()`의 `updateSocialAccessToken()` 호출이 실제로 DB에 반영되도록 수정
- **바꾸지 않는 것**: 로그인 흐름, JWT 발급 로직
- **증명**: 소셜 로그인 후 DB 재조회로 socialAccessToken 저장 확인하는 테스트

| # | 작업 |
|---|------|
| 1 | `MemberService`에 `@Transactional` 메서드 추가 — 회원 조회/등록 + socialAccessToken 업데이트를 하나의 트랜잭션으로 |
| 2 | `KakaoAuthController.callback()`에서 새 메서드 호출하도록 변경 |
| 3 | 테스트 작성: 소셜 로그인 후 member를 DB에서 재조회하여 socialAccessToken이 저장되었는지 검증 |
| 4 | `./gradlew test` 통과 확인 |

#### Step 2. 알림 전송을 트랜잭션 밖으로 분리 (구조 변경)

- **바꾸는 것**: `notificationSender.send()`를 `@Transactional` 바깥으로 이동
- **바꾸지 않는 것**: 알림이 전송되는 것 자체 (발송 여부 동일)
- **증명**: 기존 테스트 전체 통과

| # | 작업 |
|---|------|
| 1 | `OrderService.createOrder()`에서 주문 저장까지의 로직을 별도 `@Transactional` private 메서드로 추출 |
| 2 | `createOrder()`는 트랜잭션 없이 저장 메서드 호출 후 알림 전송 |
| 3 | `./gradlew test` 통과 확인 |

#### Step 3. WishService.addWish() Race Condition 방지 (작동 변경)

- **바꾸는 것**: `(member_id, product_id)` 조합에 DB 유니크 제약 추가
- **바꾸지 않는 것**: 정상적인 위시 추가/삭제 흐름
- **증명**: 동일한 조합으로 중복 등록 시도 시 중복 저장되지 않는지 확인
- **ADR**: [ADR-003 위시 중복 방지 전략](docs/adr/ADR-003-위시-중복-방지-전략.md)

| # | 작업 |
|---|------|
| 1 | Flyway `V3__Add_unique_constraint_wish.sql` 작성 (`ALTER TABLE wish ADD UNIQUE (member_id, product_id)`) |
| 2 | `WishService.addWish()`에서 `DataIntegrityViolationException` 발생 시 기존 위시 반환하도록 처리 |
| 3 | 테스트 작성: 동일 (memberId, productId) 중복 등록 시 위시가 1개만 존재하는지 검증 |
| 4 | `./gradlew test` 통과 확인 |

#### Step 4. 재고 차감에 비관적 락 적용 (작동 변경)

- **바꾸는 것**: Option 조회 시 `SELECT ... FOR UPDATE`로 행 락 획득
- **바꾸지 않는 것**: 단일 요청의 주문 흐름
- **증명**: 동시성 테스트로 재고 정합성 확인
- **ADR**: [ADR-001 재고 차감 동시성 제어](docs/adr/ADR-001-재고차감-동시성-제어.md)

| # | 작업 |
|---|------|
| 1 | `OptionRepository`에 `@Lock(PESSIMISTIC_WRITE)` + `@Query` 조회 메서드 추가 |
| 2 | `OrderService.findOption()`에서 새 메서드 호출 |
| 3 | 동시성 테스트 작성: ExecutorService로 N개 스레드 동시 주문 → 재고만큼 성공, 나머지 실패, 최종 재고 0 |
| 4 | `./gradlew test` 통과 확인 |

---

### 누락된 작동 구현

#### Step 5. 주문 완료 시 위시리스트 자동 삭제 (작동 변경)

- **바꾸는 것**: 주문 완료 시 해당 상품의 위시가 있으면 자동 삭제
- **바꾸지 않는 것**: 주문 생성 로직 자체 (재고, 포인트, 알림)
- **증명**: 주문 후 위시 목록 재조회 시 해당 항목 삭제 확인

| # | 작업 |
|---|------|
| 1 | `WishRepository`에 `deleteByMemberIdAndProductId(Long memberId, Long productId)` 추가 |
| 2 | `OrderService`에 `WishRepository` 주입, 주문 저장 후 위시 삭제 호출 |
| 3 | 테스트 작성: 위시 등록 → 해당 상품 주문 → 위시 목록 재조회 → 삭제 확인 |
| 4 | `./gradlew test` 통과 확인 |

---

### 도메인 책임 되찾기

#### Step 6. Wish 소유권 검증을 도메인으로 이동 (구조 변경)

- **바꾸는 것**: `WishService.removeWish()`의 소유권 비교 로직을 `Wish` 엔티티로 이동
- **바꾸지 않는 것**: 작동 (동일한 예외, 동일한 결과)
- **증명**: 기존 테스트 전체 통과

| # | 작업 |
|---|------|
| 1 | `Wish` 엔티티에 `isOwnedBy(Long memberId)` 메서드 추가 |
| 2 | `WishService.removeWish()`에서 `!wish.getMemberId().equals(memberId)` → `!wish.isOwnedBy(memberId)` 로 변경 |
| 3 | `./gradlew test` 통과 확인 |

#### Step 7. Order에 totalPrice 필드 추가 + 가격 계산 도메인 이동 (구조 변경)

- **바꾸는 것**: 가격 계산 책임을 `OrderService` → `Order` 생성자로 이동, totalPrice 저장
- **바꾸지 않는 것**: 주문 생성 흐름 (재고 차감, 포인트 차감, 알림)
- **증명**: 기존 테스트 전체 통과 + OrderResponse에 totalPrice 포함
- **ADR**: [ADR-002 주문 가격 저장 방식](docs/adr/ADR-002-주문-가격-저장-방식.md)

| # | 작업 |
|---|------|
| 1 | Flyway `V4__Add_total_price_to_orders.sql` 작성 (`ALTER TABLE orders ADD COLUMN total_price INT NOT NULL DEFAULT 0`) |
| 2 | `Order` 엔티티에 `totalPrice` 필드 추가 |
| 3 | `Order` 생성자에서 `price * quantity`를 받아 `totalPrice`를 계산하도록 변경 |
| 4 | `OrderService.createOrder()`에서 `calculatePrice()` 제거, `Order` 생성자에 가격 전달 |
| 5 | `OrderResponse`에 `totalPrice` 필드 추가 |
| 6 | 테스트 코드 수정 (Order 생성자 변경에 따른 컴파일 에러 해결) |
| 7 | `./gradlew test` 통과 확인 |

---

### ADR 목록

| ADR | 제목 | 관련 Step |
|-----|------|-----------|
| [ADR-001](docs/adr/ADR-001-재고차감-동시성-제어.md) | 재고 차감 동시성 제어 — 비관적 락 선택 | Step 4 |
| [ADR-002](docs/adr/ADR-002-주문-가격-저장-방식.md) | 주문 가격 저장 방식 — totalPrice 스냅샷 | Step 7 |
| [ADR-003](docs/adr/ADR-003-위시-중복-방지-전략.md) | 위시 중복 방지 — DB 유니크 제약 | Step 3 |

### 테스트 체크리스트

- [ ] `./gradlew test` 전체 통과
- [ ] 동시성 테스트: N개 스레드 동시 주문 → 재고 정합성 확인
- [ ] 위시 자동 삭제: 주문 후 위시 목록에서 삭제 확인
- [ ] 주문 응답에 totalPrice 포함 확인
- [ ] 소셜 로그인 후 socialAccessToken DB 저장 확인
