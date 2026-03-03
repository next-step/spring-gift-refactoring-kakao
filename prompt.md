# Prompt Log

## 세션: 2026-03-03 — Java 코드 포매터 적용

### 프롬프트 1: 포매터 라이브러리 탐색
> https://bestinu.tistory.com/64 이 자바 컨벤션에 대한 포매터를 이 프로젝트에 적용하고 싶어, 어떤 포매터 라이브러리를 사용하면 되니?

- URL을 제공하여 네이버 핵데이 Java 코딩 컨벤션을 참조하게 함
- Claude가 Spotless와 Checkstyle 두 가지 옵션을 비교 설명

### 프롬프트 2: Spotless 적용 + 글로벌 규칙 설정
> 글로벌 규칙으로 우리의 매번 대화 세션마다, 내가 어떻게 코딩에이전트를 이용했는지 프롬프트를 중심으로 root경로에 prompt.md를 작성합니다. 자동 포매팅이 목적이므로, Spotless를 사용합니다.

- 글로벌 규칙(prompt.md 작성)을 메모리에 저장하도록 지시
- Spotless 선택 의사결정을 전달
- Claude가 `build.gradle.kts`에 Spotless 플러그인 추가, `spotlessApply` 실행하여 전체 소스 포매팅 적용
- ktlint 위반도 함께 수정하여 빌드 성공 확인

### 프롬프트 3: 커밋 + Co-author 등록
> 이 작업 자를 코워커로 등록하고, 커밋을 한 번 합니다. Co-authored-by: koomin1227 <koomin1227@naver.com>

- Spotless 적용 작업 전체를 한 번에 커밋
- Co-author를 지정하여 커밋 메시지에 포함

### 프롬프트 4: 전역 규칙 고정
> 별도의 요청이 있기 전까지, 다음의 두가지를 전역 규칙으로 고정합니다. 1. 매 대화 세션 마다, 프롬프트위주로 나의 코딩 에이전트 사용에 대한 prompt.md파일 추가 작성 2. 커밋에 Co-authored-by: koomin1227 <koomin1227@naver.com> 코워커를 항상 등록하고, 한국어로 커밋메시지작성

- 두 가지 전역 규칙을 메모리에 저장: prompt.md 매 세션 추가 작성, 커밋 시 Co-author + 한국어 메시지

### 프롬프트 5: var → 명시적 타입 교체
> 모든 파일에서 var를 정상적인 타입으로 적용한 뒤, 테스트 해봅니다.

- 3개 파일(`OrderController`, `KakaoMessageClient`, `WishController`)에서 총 16개의 `var`를 명시적 타입으로 교체
- `Member`, `Option`, `Order`, `Product`, `Wish`, `Page<T>`, `int`, `String`, `LinkedMultiValueMap` 등 실제 타입 적용
- `spotlessApply` + `build` 성공 확인

### 프롬프트 6: 미사용 메서드 탐색 및 삭제
> 전체 소스 코드 돌면서, "사용되고 있지 않은" 메서드가 존재합니다. 예를 들면, Wish.java의 생성자 함수가 그 예시입니다. 모든 소스 코드를 대상으로 사용되고 있지 않은 메서드를 제거하고 싶습니다. 다만, 무조건 삭제하지 않고, 이전 작업자가 남긴, 주석이나 향후 변경 영향도 등을 검토하여 작업합니다. 그런 뒤 실행되는지 테스트합니다.

- Explore 에이전트 2개를 병렬로 실행하여 main/test 소스 전체 분석
- 미사용 확인된 항목 삭제: `Product.getOptions()`, `SeedMemberController`(전체), `MemberService`(전체), `CreateMemberRequest`(전체)
- JPA/Thymeleaf 등 프레임워크가 암묵적으로 사용하는 메서드는 보존 (`protected` 기본 생성자, `Member.getPoint()` 등)
- `./gradlew build` 성공 확인

### 프롬프트 7: protected 생성자 사용 여부 확인
> protected로 선언된 생성자는 안쓰이는게 아니니? 검토해줘

- JPA 스펙상 엔티티의 `protected` no-arg 생성자는 필수임을 확인
- 6개 엔티티(`Category`, `Product`, `Option`, `Order`, `Member`, `Wish`) 모두 유지

### 프롬프트 8: 삭제 코드의 출처 확인
> product의 option 메서드 삭제만, 체리픽 머지 이전의 코드야? 확인해 줘

- `git show`로 확인: `getOptions()`는 초기 커밋(`55ca9e4`, author: `wotjd243`)에서 생성된 코드
- 체리픽으로 가져온 코드가 아닌, 이전 작업자의 프로젝트 세팅 코드임을 확인

## 세션: 2026-03-03 — Lombok 적용 리팩토링

### 프롬프트 1: Lombok 적용 계획 수립 및 실행
> Lombok 적용 리팩토링 계획 (Plan 모드에서 수립 후 실행)

- JPA 엔티티 6개(Category, Product, Option, Member, Wish, Order)에 반복되는 getter 메서드(28개)와 protected 기본 생성자(6개)를 Lombok 어노테이션으로 대체
- `build.gradle.kts`에 `compileOnly("org.projectlombok:lombok")` + `annotationProcessor("org.projectlombok:lombok")` 추가
- 각 엔티티에 `@Getter` + `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 적용, 수동 getter/protected 생성자 삭제
- 비즈니스 메서드(`update()`, `subtractQuantity()`, `chargePoint()`, `deductPoint()` 등)와 public 생성자는 그대로 보존
- Record 클래스(Request/Response)는 이미 최적이므로 변경 불필요로 판단하여 제외
- `./gradlew spotlessApply build` — 포매팅 + 컴파일 + 테스트 9개 모두 통과 확인

### 프롬프트 2: 매직넘버/URL 등 의미 불명 리터럴 상수화
> url이나, 매직넘버 등 의미를 알 수 없는 값들이 있는지 모두 찾습니다. 있다면 상수 변수로 선언합니다.

- HTTP 상태 코드 `401`, `403` → `HttpStatus.UNAUTHORIZED`, `HttpStatus.FORBIDDEN` (WishController, OrderController)
- 카카오 API URL 4개 → 각 클래스에 `private static final` 상수 추출 (KakaoAuthController, KakaoLoginClient, KakaoMessageClient)
- `"Bearer "` 접두사 → `BEARER_PREFIX` 상수 (AuthenticationResolver, KakaoLoginClient, KakaoMessageClient)
- `"Content-Type"`, `"Authorization"` → Spring `HttpHeaders` 상수 사용
- `"application/x-www-form-urlencoded"` → `MediaType.APPLICATION_FORM_URLENCODED_VALUE`
- OAuth 스코프 `"account_email,talk_message"` → `OAUTH_SCOPE` 상수 (KakaoAuthController)
- 에러 응답 `"message"` 키 → `ERROR_MESSAGE_KEY` 상수 (GlobalExceptionHandler)
- 최소 옵션 수 `1` → `MIN_OPTION_COUNT` 상수 (OptionController)
- `./gradlew spotlessApply build` — 테스트 9개 모두 통과 확인

## 세션: 2026-03-03 — 서비스 레이어 추출 리팩토링

### 프롬프트 1: 서비스 레이어 추출 계획 수립
> 컨트롤러에 직접 구현된 비즈니스 로직을 서비스 레이어로 분리하는 계획 수립 (Plan 모드)

- 현재 모든 비즈니스 로직이 컨트롤러에 직접 구현되어 있어 서비스 클래스가 하나도 없는 상태를 분석
- 7개 서비스(CategoryService, MemberService, ProductService, OptionService, WishService, OrderService, KakaoAuthService) 추출 계획 수립
- 10개 컨트롤러 수정 범위 식별
- 설계 원칙 정의: 서비스는 엔티티 반환, 컨트롤러가 DTO 변환, Auth는 컨트롤러에 유지, @Transactional로 트랜잭션 보장

### 프롬프트 2: 서비스 레이어 추출 실행
> Implement the following plan: (서비스 레이어 추출 계획 전체 실행)

- 7개 서비스 클래스 신규 생성: `CategoryService`, `MemberService`, `ProductService`, `OptionService`, `WishService`, `OrderService`, `KakaoAuthService`
- 10개 컨트롤러에서 리포지토리 직접 호출을 서비스 호출로 교체
- `OrderController` 의존성 6개 → `OrderService` + `AuthenticationResolver` 2개로 축소, `@Transactional` 단일 트랜잭션으로 원자성 버그 수정
- `OrderService`에서 미사용이던 `WishRepository` 의존성 제거
- 허용된 미세 동작 변경: 존재하지 않는 optionId로 주문 시 404 → 400 (`NoSuchElementException` → `GlobalExceptionHandler`)
- 테스트 1개 업데이트: `GiftAcceptanceTest.존재하지_않는_옵션으로_선물하면_실패한다()` 기대값 404 → 400
- `./gradlew spotlessApply build` — 테스트 9개 모두 통과 확인

### 프롬프트 3: 공통 예외 처리 추가
> 공통 예외 처리를 추가합니다.

- `GlobalExceptionHandler`에 `IllegalArgumentException` 핸들러 추가 (→ 400)
- 3개 컨트롤러(`MemberController`, `ProductController`, `OptionController`)의 중복 로컬 `@ExceptionHandler` 제거
- `OrderController`에서 발생하던 `IllegalArgumentException`(재고 부족, 포인트 부족) 응답이 500 → 400으로 정상화
- 테스트 2개 기대값 업데이트: 재고 부족 시 500 → 400
- `./gradlew spotlessApply build` — 테스트 9개 모두 통과 확인

### 프롬프트 4: 요청 DTO 분리 (우발적 중복 제거)
> dto를 요청마다 분리하고 싶습니다. 타당성 검토를 거친 뒤 리팩터링을 진행합니다.

- 우발적 중복(accidental duplication) 관점에서 타당성 검토 수행
- 생성/수정이 동일 DTO를 공유하는 3개, 회원가입/로그인이 공유하는 1개를 분리 대상으로 선정
- `CategoryRequest` → `CreateCategoryRequest` + `UpdateCategoryRequest`
- `ProductRequest` → `CreateProductRequest` + `UpdateProductRequest`
- `MemberRequest` → `RegisterMemberRequest` + `LoginMemberRequest`
- 서비스 추출 이후 미사용 상태이던 `toEntity()` 메서드 함께 제거
- 단일 용도 DTO(`OptionRequest`, `OrderRequest`, `WishRequest`)는 이미 분리되어 있어 변경 불필요
- `./gradlew spotlessApply build` — 테스트 9개 모두 통과 확인
