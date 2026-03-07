# spring-gift-refactoring

## 구현할 기능 목록 및 구현 전략

---

### 1. 스타일 정리

#### 1-1. 에러 메시지 언어 통일

**현재 문제:** 에러 메시지가 영어와 한국어로 혼재되어 있다.
- `Member.chargePoint()` → 영어: `"Amount must be greater than zero."`
- `Member.deductPoint()` → 한국어: `"차감 금액은 1 이상이어야 합니다."`
- `MemberController` → 영어: `"Email is already registered."`, `"Invalid email or password."`
- `OptionController` → 한국어: `"이미 존재하는 옵션명입니다."`
- `ProductNameValidator` → 한국어: `"상품 이름은 필수입니다."`


**모든 메시지를 영어로 통일**
- Member, MemberController 쪽 메시지는 유지하고, Validator와 나머지를 영어로 변경.
- 변경 대상: `ProductNameValidator`, `OptionNameValidator`, `OptionController`, `Option.subtractQuantity()`, `Member.deductPoint()` 등.
- 장점: 개발자 간 소통 시 일관성. 글로벌 확장에 유리.
- 단점: 변경 범위가 크다. 사용자에게 영어 메시지가 노출될 수 있다.

---

#### 1-2. 예외 처리 패턴 통일

**현재 문제:** `@ExceptionHandler(IllegalArgumentException.class)`가 `MemberController`, `ProductController`, `OptionController`, `OrderController`에 각각 중복 정의되어 있다. 반환 형식도 `ResponseEntity<String>`으로 단순 문자열이다.

**@ControllerAdvice + ErrorResponse DTO 도입**
- GlobalExceptionHandler를 만들면서, 응답 형식도 `ErrorResponse(String message)` 같은 DTO로 통일한다.
- 장점: 클라이언트가 에러 응답을 파싱하기 쉽다. JSON 구조가 일관된다.
- 단점: 대안 A보다 변경 범위가 넓다. DTO 클래스가 하나 추가된다.

---

#### 1-3. Null 처리 패턴 통일

**현재 문제:** 엔티티 조회 시 처리 방식이 Controller마다 다르다.
- `ProductController`, `OptionController`, `WishController`, `OrderController` → `.orElse(null)` 후 if-null 체크
- `AdminMemberController`, `AdminProductController` → `.orElseThrow()`
- `AuthenticationResolver` → try-catch 후 null 반환

**orElseThrow()로 통일 + 예외로 404 처리**
- 모든 `.orElse(null)` + if-null 패턴을 `.orElseThrow(() -> new NoSuchElementException(...))`로 변경.
- GlobalExceptionHandler에서 `NoSuchElementException`을 404로 매핑.
- 장점: null 체크 분기문이 사라져 코드가 간결해진다. 일관된 흐름.
- 단점: 예외를 흐름 제어에 사용하게 된다.

---

### 2. 불필요한 코드 제거

#### 2-1. 미사용 @Autowired 제거

**현재 문제:** `MemberController`, `AdminMemberController` 등에서 생성자 주입을 사용하면서도 `@Autowired` 어노테이션이 남아있다. Spring 4.3+ 부터 생성자가 하나뿐이면 `@Autowired`는 불필요하다.

**@Autowired 어노테이션만 제거 (필드는 이미 final)**
- 4개 파일(`JwtProvider`, `AuthenticationResolver`, `MemberController`, `AdminMemberController`)에서 `@Autowired` 어노테이션과 import를 제거한다.
- 모든 파일이 이미 `final` 필드를 사용하고 있으므로 어노테이션 제거만으로 충분하다.
- Spring 4.3+ 에서 생성자가 1개뿐이면 `@Autowired`는 불필요하다.

---

#### 2-2. Validator 중복 코드 정리

**현재 문제:** `ProductNameValidator`와 `OptionNameValidator`가 거의 동일한 코드이다.
- 동일한 정규식: `^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ ()\\[\\]+\\-&/_]*$`
- 차이점: MAX_LENGTH (Product=15, Option=50), Product만 "카카오" 포함 여부 검증

**하나의 NameValidator로 통합 (파라미터로 분기)**
- `NameValidator.validate(String name, int maxLength, boolean checkKakao)` 같은 메서드로 통합.
- 기존 `ProductNameValidator`, `OptionNameValidator`는 삭제.
- 장점: 중복 완전 제거. 정규식 수정 시 한 곳만 변경.
- 단점: 파라미터가 많아질 수 있다. Product/Option 고유 검증이 추가될 때 분기가 복잡해질 수 있다.

---

#### 2-3. 미사용 Kotlin 의존성 정리

**현재 문제:** `build.gradle.kts`에 Kotlin 플러그인(`kotlin("jvm")`, `kotlin("plugin.spring")`)과 Kotlin 의존성이 포함되어 있으나, 실제 Kotlin 소스 파일이 없다. `ktlint` 플러그인도 설정되어 있다.

**Kotlin 관련 설정 전부 제거**
- `build.gradle.kts`에서 Kotlin 플러그인, 의존성, ktlint 설정을 모두 제거.
- `src/main/kotlin`, `src/test/kotlin` 디렉토리도 제거.
- 장점: 빌드 시간 단축. 불필요한 의존성 제거로 프로젝트가 깔끔해진다.
- 단점: 향후 Kotlin 도입 시 다시 추가해야 한다.

---

### 3. 서비스 계층 추출

#### 3-1.

**현재 문제:** `MemberController`에 회원가입(중복 확인 → 저장 → JWT 발급), 로그인(조회 → 비밀번호 검증 → JWT 발급) 비즈니스 로직이 직접 구현되어 있다. `AdminMemberController`에도 회원 생성, 포인트 충전 로직이 있다.

**MemberService + AuthService 분리**
- 인증 관련(register, login, JWT 발급)은 `AuthService`로, 회원 관리(조회, 포인트)는 `MemberService`로 분리.
- 장점: 단일 책임 원칙. 인증과 회원 관리의 관심사가 명확히 나뉜다.
- 단점: 클래스가 2개 추가된다. 경계가 모호한 경우가 있을 수 있다.


---

#### 3-2. CategoryService 추출

**현재 문제:** `CategoryController`에 CRUD 로직이 직접 구현되어 있다. 비교적 단순하지만 Repository 직접 호출, 엔티티 변환 등이 Controller에 있다.

**CategoryService 생성 (전체 CRUD 추출)**
- `findAll()`, `create()`, `update()`, `delete()` 메서드를 가진 `CategoryService` 생성.
- Controller는 요청 파싱과 응답 변환만 담당.
- 장점: 일관된 계층 구조. 다른 도메인과 동일한 패턴.
- 단점: 단순 위임만 하는 Service가 될 수 있다 (pass-through).

---

#### 3-3. ProductService 추출

**현재 문제:** `ProductController`에 상품명 검증(`validateName`), 카테고리 조회, 상품 CRUD 로직이 있다. `AdminProductController`에도 동일한 검증 + Thymeleaf 렌더링 로직이 있다.

**ProductService + 검증은 Validator에 위임**
- `ProductService`는 CRUD만 담당. 검증은 기존 `NameValidator`를 Service 내부에서 호출.
- 장점: 각 클래스의 책임이 명확. Validator 재사용 가능.
- 단점: 대안 A와 큰 차이 없음. 호출 체인이 한 단계 깊어진다.

---

#### 3-4. OptionService 추출

**현재 문제:** `OptionController`에 옵션명 검증, 중복 확인, 최소 옵션 개수 검증, CRUD 로직이 모두 있다.

**OptionService 생성 (전체 비즈니스 로직 추출)**
- `createOption()`: 이름 검증 + 중복 확인 + 저장.
- `deleteOption()`: 최소 개수 검증 + 소속 확인 + 삭제.
- Controller는 pathVariable 파싱과 응답 반환만 담당.
- 장점: 비즈니스 규칙(중복 확인, 최소 개수)이 Service에 모인다.
- 단점: 없음. 가장 자연스러운 구조.

---

#### 3-5. WishService 추출

**현재 문제:** `WishController`에 인증 확인, 상품 조회, 위시 중복 확인, 소유권 검증 로직이 모두 있다. 인증 코드(`extractMember` + null 체크)가 매 메서드마다 반복된다.

**WishService 생성 + HandlerMethodArgumentResolver로 인증 처리**
- `@LoginMember Member member` 같은 커스텀 어노테이션 + ArgumentResolver를 만들어 인증 보일러플레이트를 제거.
- Service에는 순수 비즈니스 로직만 남긴다.
- 장점: Controller가 가장 얇아진다. 인증 코드 중복 완전 제거.
- 단점: ArgumentResolver 클래스 추가 필요. 구조 변경 범위가 넓다.

---

#### 3-6. OrderService 추출

**현재 문제:** `OrderController.createOrder()`가 프로젝트에서 가장 복잡한 메서드다. 인증 확인 → 옵션 조회 → 재고 차감 → 포인트 차감 → 주문 저장 → 카카오 알림 발송까지 6단계를 하나의 Controller 메서드에서 처리한다.

**OrderService + 카카오 알림은 별도 분리**
- 주문 처리(조회, 차감, 저장)는 `OrderService`에, 카카오 알림은 `KakaoNotificationService`로 분리.
- `OrderService`가 `KakaoNotificationService`를 호출.
- 장점: 알림 로직 변경이 주문에 영향을 주지 않는다. 단일 책임 원칙.
- 단점: 클래스가 2개 추가된다. 현재 `KakaoMessageClient`가 이미 분리되어 있으므로 중간 계층이 하나 더 늘어난다.


---

#### 3-7. KakaoAuthService 추출

**현재 문제:** `KakaoAuthController.callback()`에 Kakao OAuth 흐름(토큰 요청 → 사용자 정보 조회 → 회원 자동 등록/조회 → kakaoAccessToken 저장 → JWT 발급)이 직접 구현되어 있다.

**KakaoAuthService 생성**
- OAuth 콜백 처리 전체를 `KakaoAuthService.processCallback(String code)` 메서드로 추출.
- 반환값은 JWT 토큰 문자열 또는 `TokenResponse`.
- 장점: Controller가 위임만 담당. OAuth 흐름이 Service에 캡슐화된다.
- 단점: 없음. 가장 자연스러운 구조.
