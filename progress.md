
# 진행 과정

아래 내용은 각 진행 방법에 대해 어떻게 진행한지 기록한 내용임.

---

## 일단 테스트를 만든다.

semi 가 claude 한테 인수 테스트코드를 만들어달라 요청함.


- 어떤 방식으로 테스트 만들지 논의하고 정리한다.

먼저 간단한 happy flow 에 해당하는 테스트코드를 구성한 다음 test coverage 로 보완 안된 부분을 추가함.

예를들어 CUD 요청에서 404, 401 같은 flow 들은 happy flow 에 안되있으니까 test coverage 로 보면 cover 안된 부분 보임. 그래서 claude 한테 이 비어있는 부분도 cover 하도록 인수 테스트 만들어달라 함.

---

## 규칙화된 스타일을 세운다. 여타 컨벤션? 도 세운다.

코딩 컨벤션 세워야 한다.
- 아래 링크 제공해서 간단한 convention 만들도록 했다.
- https://naver.github.io/hackday-conventions-java/

### 요청 검증 부분

요청 객체에 대한 validation 이 종종 `...Validator` 로 구성되어있는데, 이를 일괄 spring validation 을 이용한다.

> ### 문제상황 : ProductNameValidator
> 
> 현재 코드를 보니 ProductNameValidator 가 2 군데서 사용된다. 하나는 ProductController, 하나는 AdminProductController 이다.
> 
> 문제는 AdminProductController 에서 예외 처리이다. 여기선 validator 가 model 에 에러 메세지를 추가해 view 에서 보여주는 용도로 사용되고 있다. 즉, 여기선 exception 이 throw 되면 안된다.
> 
> 반면 ProductController 에서는 exception (IllegalArgumentException --> 400 CODE) 던지는데 사용된다.
> 
> product name 은 아래 규칙을 따라야 한다.
> - 길이 규칙, 특수문자 규칙, not blank 규칙
> - admin 에서는 이름 '카카오' 가 허용되는데 일반 product 에서는 안된다.
>
> **결론:** ProductNameValidator는 오류 리스트를 반환하는 순수 유틸로 유지. "예외를 던질지 / 모델에 담을지"는 호출 지점의 책임. 서비스 계층 추출 섹션에 상세 기록.

### 요청 응답 `ResponseEntity<...>`

코드를 보니 (개발자의 실수인 것 같지만) `ResponseEntity<?>` 처럼 되어 있는 부분이 있다.

wildcard 는 사용하지 않는다. `ResponseEntity<Void>`, `ResponseEntity<SomeResponse>` 처럼 명시한다.

### Bean 주입

모두 생성자 주입으로 되어있긴 한데, (이것도 아마 개발자의 실수) 생성자에 불필요한 `@Autowired` 가 있다.

여튼 생성자 주입을 사용하고 lombok 의 required argument constructor 를 이용한다. (+주입된 bean 은 모두 final 로 선언한다.)

### 변수, entity 선언

변수 선언에는 `var` 키워드는 사용하지 않는다. 불필요한 `final` 은 사용하지 않는다.

### Controller 메서드 이름

**결론: 현재 상태 유지**

- REST Controller는 `동사+도메인명` 패턴 (`createProduct`, `getCategories` 등)
- Admin Controller는 도메인명 없이 간결하게 (`create`, `update` 등) — URL에 이미 도메인이 포함되어 있으므로
- `addWish`/`removeWish`, `register`/`login`은 도메인 특성상 CRUD 네이밍보다 자연스러움

### `목록 (GET method)` 응답 타입

코드를 보니 몇몇 `목록 GET` endpoint 는 `List<...>` 로, 어떤건 `Page<...>` 로 되어 있다.

그리고 몇몇거는 `Pageable` param 을 받는다.

**결론: API 명세를 보니 의도적으로 나뉜 응답임. 그대로 유지.**

---

## 목록화된 스타일을 차례대로 진행한다.

- 요청 검증 부분 통일 일부 진행
  - OptionNameValidator 만 삭제
- 요청 응답 부분 진행
- Bean 주입 방식 통일
- 변수 구성 방식 통일

---

## 불필요한 코드를 제거한다.

### 불필요한 목록과 그 근거를 정리한다.

- entity 에 get methods --> lombok getter 이용

### 차례로 없앤다.

---

## 서비스 계층을 추출한다.

Controller의 비즈니스 로직을 Service로 이동한다. Controller는 요청 검증과 위임만 담당하도록 얇게 만든다. 신규 기능은 추가하지 않는다.

### 설계 원칙 (논의 후 결정)

| 항목 | 결정 | 이유 |
|------|------|------|
| Service 반환 타입 | Entity | DTO는 HTTP 계층 관심사, Service는 도메인 객체 반환 |
| Service 파라미터 | 원시 타입 (String, Long 등) | Service가 Request DTO에 의존하지 않도록 (HTTP 계층 분리) |
| 미존재 엔티티 처리 | `NoSuchElementException` throw | Controller에서 catch → 404 응답 |
| 다른 도메인 데이터 접근 | 해당 도메인의 Service를 통해 | Repository 직접 접근 금지 (예: ProductService → CategoryService) |
| 비즈니스 검증 | Service 내부에서 수행 | 이름 검증, 중복 체크 등은 비즈니스 규칙 |
| Request DTO의 toEntity() | 제거 | 엔티티 생성은 Service에서 직접 수행, Request는 순수 데이터 홀더 |

### ProductNameValidator 처리 방식

> **결론: 현재 구조 유지**
>
> - `ProductNameValidator`는 오류 리스트를 반환하는 순수 유틸로 유지
> - `ProductService`는 내부에서 `validateName()` 호출 → 실패 시 `IllegalArgumentException` throw
> - `AdminProductController`는 폼 에러 표시를 위해 서비스 호출 전에 `ProductNameValidator.validate(name, true)`를 직접 호출 (사전 검증)
> - "예외를 던질지 / 모델에 담을지"는 호출 지점의 책임
> - `allowKakao` 플래그로 Admin/API 간 규칙 차이만 분기

### 인증 처리 (AuthenticationResolver)

- `extractMember()`가 null 대신 `IllegalStateException`을 throw하도록 변경
- WishController, OrderController에서 중복되던 private `authenticate()` 헬퍼 제거
- 인증 실패 처리가 `AuthenticationResolver` 한 곳으로 통합됨
- 각 Controller에 `@ExceptionHandler(IllegalStateException.class)` → 401 응답

### 각 도메인별 진행 기록

#### 1. Category (완료)

- `CategoryService` 생성 — `findAll`, `findById`, `create`, `update`, `delete`
- `CategoryController` → `CategoryRepository` 의존성을 `CategoryService`로 교체

#### 2. Product (완료)

- `ProductService` 생성 — `findAll`, `findAll(Pageable)`, `findById`, `create`, `update`, `delete`
- 이름 검증(`ProductNameValidator`) 포함, `allowKakao` 파라미터로 Admin/API 분기
- `CategoryService.findById()` 추가하여 카테고리 조회 시 서비스 간 호출
- `ProductController`, `AdminProductController` 모두 `ProductService`로 교체
- `AdminProductController`의 카테고리 목록 조회는 `CategoryService.findAll()` 사용

#### 3. Wish (완료)

- `WishService` 생성 — `findByMemberId`, `findByMemberIdAndProductId`, `create`, `removeWish`
- `addWish`를 `findByMemberIdAndProductId` + `create`로 분리하여 Controller가 200(기존)/201(신규) 구분 가능
- 소유권 검증(403)은 Service에서 `IllegalStateException` throw
- `ProductRepository` 직접 접근 → `ProductService` 통해 접근

#### 4. Member (완료)

- `MemberService` 생성 — `findAll`, `findById`, `create`, `login`, `update`, `chargePoint`, `deductPoint`, `registerOrUpdateKakaoMember`, `delete`
- `register`(API)와 `create`(Admin) 둘 다 `MemberService.create()` 공용
- `login`의 비밀번호 검증은 Service로 이동
- JWT 토큰 생성은 Controller에 유지 (인증 인프라 관심사)
- `deductPoint` — OrderService에서 사용
- `registerOrUpdateKakaoMember` — KakaoAuthService에서 사용

#### 5. Option (완료)

- `OptionService` 생성 — `findByProductId`, `findById`, `subtractQuantity`, `create`, `delete`
- 이름 검증, 중복명 체크, 최소 1개 제약 모두 Service로 이동
- `subtractQuantity` — OrderService에서 사용
- `ProductRepository` 직접 접근 → `ProductService` 통해 접근

#### 6. Order (완료)

- `OrderService` 생성 — `findByMemberId`, `createOrder`
- 다른 도메인 Repository 직접 접근 없이 `OptionService.subtractQuantity`, `MemberService.deductPoint` 사용
- 카카오 메시지 발송(best-effort)도 Service로 이동
- 주의: `@ExceptionHandler(IllegalArgumentException.class)` 추가 시 포인트 부족(500)이 400으로 바뀌는 문제 발생 → ExceptionHandler 미사용으로 해결

#### 7. KakaoAuth (완료)

- `KakaoAuthService` 생성 — `loginWithKakao`
- 카카오 API 호출(KakaoLoginClient) + 회원 자동등록/토큰 갱신 오케스트레이션
- 회원 처리는 `MemberService.registerOrUpdateKakaoMember()`에 위임
- Controller에는 OAuth URL 구성과 JWT 발급만 유지

### 추가 정리

- `CategoryRequest.toEntity()`, `ProductRequest.toEntity()` 제거 — 서비스에서 직접 엔티티 생성하므로 미사용 코드
- 한국어/영어 혼재된 메시지, 주석을 한국어로 통일
