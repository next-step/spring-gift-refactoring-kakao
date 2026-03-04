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

### (deprecated no co-worker)프롬프트 3: 커밋 + Co-author 등록

- Spotless 적용 작업 전체를 한 번에 커밋

### 프롬프트 4: 전역 규칙 고정
> 별도의 요청이 있기 전까지, 다음의 두가지를 전역 규칙으로 고정합니다. 1. 매 대화 세션 마다, 프롬프트위주로 나의 코딩 에이전트 사용에 대한 prompt.md파일 추가 작성

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

### 프롬프트 5: 주석-코드 불일치 검토
> 소스 코드 전반을 살펴 보면서, 주석과 실제 코드가 일치하지 않는 부분은 없는지 검토합니다.

- 전체 Java 파일 51개 검토 수행
- 내용 불일치 없음 확인. 스타일 차원 불일치 4건(영어/한국어 혼용) + @author 태그 7건 보고

### 프롬프트 6: 외부 API 클라이언트 패키지 분리
> 외부 api 호출하는 관련 코드들을 패키지 분리

- `gift.auth.KakaoLoginClient`, `gift.auth.KakaoLoginProperties`, `gift.order.KakaoMessageClient` → `gift.infrastructure.kakao` 패키지로 이동
- `KakaoAuthService`, `OrderService`의 import 경로 업데이트
- `./gradlew spotlessApply build` — 테스트 9개 모두 통과 확인

## 세션: 2026-03-04 — Step1 검토 및 리뷰어 피드백 반영

### 프롬프트 1: Step1 전체 검토
> step1 과제 요구사항 대비 커밋 이력 검토

- 10개 커밋 중 8개는 순수 구조 변경으로 원칙을 잘 지킴
- **문제 커밋 2개 식별**:
  - `b926ddc` (서비스 레이어 추출): 구조 변경에 작동 변경 혼재 (404→400 응답 변경, 트랜잭션 경계 추가)
  - `20f2ad4` (예외 처리 통합): `refactor:` 라벨이나 실제로는 버그 수정 (500→400), 응답 body 형식 변경
- 기타 코드 우려: `OptionService.createRaw()` 검증 우회, `BEARER_PREFIX` 3곳 중복, `OrderService`의 `MemberRepository` 직접 의존

### 프롬프트 2: 리뷰어(wooobo) 피드백 확인 (PR #41)

**피드백 3건:**

1. **커밋 분할 제안** — 7개 서비스를 한 커밋에 추출한 것이 너무 큼. 도메인 단위로 커밋 분리 권장
2. **응답 상태 코드 변경 주의** — 구조 변경 시 404→400 같은 클라이언트 의존 부분이 달라지지 않는지 확인 필요
3. **Claude Skills 문서 포맷** — SKILL.md에 name, description 등 메타데이터 프로퍼티 활용 제안

**자체 검토와 리뷰어 피드백 일치점:**
- 피드백 1, 2번은 자체 검토에서도 동일하게 식별한 문제 (커밋 6의 구조+작동 혼재)
- 리뷰어는 승인했으나, 다음 단계에서 개선할 포인트로 인식

### 프롬프트 3: 리뷰어(catsbi) 피드백 확인 (PR #40)

**코드 품질 12건 상세 리뷰:**

1. `validateNameOrThrow` public → private 변경 제안
2. `orElse(null)` + null 체크 → Optional 체이닝 권장
3. 예외 메시지 한글/영어 혼용 지적
4. public API 주석 → javadoc 스타일 변경 제안
5. 외부 URL 상수 → properties 외부화 제안
6. `option.getProduct().getPrice()` 디미터 법칙 위반
7. `catch (Exception ignored)` 로깅 없이 무시 지적
8. `sendKakaoMessageIfPossible` 전략 패턴 추상화 제안
9. 비밀번호 평문 저장/비교 지적
10. `NoSuchElementException` → 400은 부적절, 404 권장
11. `AdminMemberController` 인증 로직 분리 제안
12. `Order`에서 Option/Member 참조 방식 불일치 지적

### 프롬프트 4: 피드백 통합 및 반영 계획 수립 (Plan 모드)

- 자체 검토 + 리뷰어1(wooobo) + 리뷰어2(catsbi) 피드백을 통합하여 분류
- **구조 변경 7건 (S1~S7)**: step2 전에 즉시 반영 (작동 변경 없음)
- **작동 변경 8건 (B1~B8)**: step2에서 테스트와 함께 반영
- **설계 판단 1건 (D1)**: step2에서 검토
- **프로세스 4건 (P1~P4)**: 습관 개선으로 인지
- 구조 변경을 독립 커밋으로 분리하여 "한 커밋 = 하나의 의도" 원칙 준수

### 프롬프트 5: 구조 변경 반영 실행

- **커밋 1**: `validateNameOrThrow` public → private (OptionService, ProductService)
- **커밋 2**: WishService `orElse(null)` → Optional 체이닝
- **커밋 3**: Member.java, MemberService.java 예외 메시지 6건 한국어 통일
- **커밋 4**: Member.deductPoint() 주석 javadoc 스타일로 변경
- **커밋 5**: 카카오 API URL 4개를 application.properties로 외부화, KakaoMessageProperties 신규 생성
- **커밋 6**: BEARER_PREFIX 3곳 중복 → AuthConstants 공통 상수 클래스로 통합
- **커밋 7**: OptionService.createRaw() 제거, SeedOptionController에서 create() 사용
- 매 커밋마다 `./gradlew spotlessApply build` 성공 확인

## 세션: 2026-03-04 — Step2 리팩터링 완성하기

### 프롬프트 1: Step2 구현 계획 수립 (Plan 모드)
> Step2 과제 요구사항(트랜잭션 경계, 누락 기능, 도메인 책임)에 대한 구현 계획 수립

- 구조 변경 4건(S8~S11) + 작동 변경 3건(B1~B3) + ADR 2건 계획
- 각 커밋의 파일, 코드 변경, 검증 방법을 상세 설계
- "구조 변경은 refactor:, 작동 변경은 fix:/feat:/test:" 라벨 분리 원칙 적용

### 프롬프트 2: Step2 구현 실행
> Implement the following plan: (Step2 전체 실행)

**구조 변경 (작동 불변):**
- **S8**: `Option.calculateTotalPrice(quantity)` 추가, `OrderService`에서 `option.getProduct().getPrice() * quantity` → `option.calculateTotalPrice(quantity)` (디미터 법칙 해소)
- **S9**: `WishService.removeByMemberAndProduct(memberId, productId)` 추가 (B2 준비)
- **S10**: `OrderService`의 `MemberRepository` → `MemberService` 전환 (계층 의존 정리)
- **S11**: `catch (Exception ignored)` → `log.warn()` 경고 로깅 추가 (운영 가시성)

**작동 변경 (증거 포함):**
- **B1**: `NoSuchElementException` → 404 변경, G4 테스트 기대값 수정, ADR-001 작성
- **B2**: 주문 시 위시 자동 정리 (`OrderService`에 `WishService` 연동), G6 테스트 + Cucumber 시나리오 + GiftSteps 3개 추가, ADR-002 작성
- **B3**: 트랜잭션 경계 검증 (포인트 부족 시 재고 rollback), G7 테스트 + Cucumber 시나리오 + GiftSteps 2개 추가, test-data.sql에 poor 회원 추가

**문서:**
- `PLAN.md` 생성 및 완료 상태 업데이트
- 매 커밋마다 `./gradlew spotlessApply build` 성공 확인
