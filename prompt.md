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

### 프롬프트 5: 주석-코드 불일치 검토
> 소스 코드 전반을 살펴 보면서, 주석과 실제 코드가 일치하지 않는 부분은 없는지 검토합니다.

- 전체 Java 파일 51개 검토 수행
- 내용 불일치 없음 확인. 스타일 차원 불일치 4건(영어/한국어 혼용) + @author 태그 7건 보고

### 프롬프트 6: 외부 API 클라이언트 패키지 분리
> 외부 api 호출하는 관련 코드들을 패키지 분리

- `gift.auth.KakaoLoginClient`, `gift.auth.KakaoLoginProperties`, `gift.order.KakaoMessageClient` → `gift.infrastructure.kakao` 패키지로 이동
- `KakaoAuthService`, `OrderService`의 import 경로 업데이트
- `./gradlew spotlessApply build` — 테스트 9개 모두 통과 확인

## 세션: 2026-03-05 — 코드 리뷰 (미구현 의도 + 트랜잭션 경계)

### 프롬프트 1: 미구현 의도 탐색
> 기존 코드에 의도가 남아있지만 구현되지 않은 작동이 있는지 찾아봐

- 전체 소스 탐색 수행
- `Option.calculateTotalPrice()` + `Member.deductPoint()`로 가격 계산/차감은 하지만 `OrderResponse`에 가격 정보가 누락되어 있음을 발견

### 프롬프트 2: 트랜잭션 경계 점검
> 트랜잭션 경계는 잘 잡혀있는가?

- `OrderService.placeOrder()`에서 외부 HTTP 호출(`kakaoMessageClient.send()`)이 `@Transactional` 내부에 포함된 문제 발견
- 카카오 API 실패 시 전체 롤백 + `@Retryable` 3회 재시도로 최대 1.5초 DB 커넥션/락 점유 위험
- 해결 방향: 트랜잭션 커밋 후 메시지 전송 분리 필요

### 프롬프트 3: 카카오 메시지 전송을 트랜잭션 밖으로 분리
> kakaoMessageClient.send 이거를 트랜잭션 밖으로 빼줘. 트랜잭션이 성공한 경우에만 메시지가 나갈 수 있도록 수정해줘.

- `TransactionSynchronization.afterCommit()` 콜백으로 1차 구현 후 빌드 통과 확인

### 프롬프트 4: 가독성 개선
> 좀 더 가독성 좋게 작성할 수는 없나?

- `TransactionSynchronization` 익명 클래스 → Spring 이벤트 기반으로 리팩토링
- `OrderCompletedEvent` record 생성, `OrderCompletedEventListener`에서 `@TransactionalEventListener`(기본 AFTER_COMMIT)로 처리
- `OrderService`에서 `kakaoMessageClient` 의존성 제거, `ApplicationEventPublisher`로 이벤트 발행만 담당
- `./gradlew spotlessApply build` — 빌드 + 테스트 통과 확인

### 프롬프트 5: 개발자별 코드 스타일 불일치 검토 및 수정
> 사건의 전말(A/B/C/D 개발자 스토리)을 기반으로 코드 검토 후 수정

- 예외 타입 통일: `MemberService`의 엔티티 조회 실패를 `IllegalArgumentException` → `NoSuchElementException`으로 변경 (회원만 400이던 것을 404로 통일)
- 메서드명 통일: `MemberService.getById()` → `findById()` (다른 서비스와 동일 패턴)
- DI 방식 통일: `AuthenticationResolver`, `JwtProvider`에서 `@Autowired` 제거 (생성자 주입으로 통일)
- 주석 스타일 통일: Javadoc `@author`/`@since` 5개 파일 제거, 블록 주석 2개 파일 제거, 메서드 Javadoc 1건 제거 → 프로젝트 전체 "주석 없이 코드로 설명" 원칙으로 통일
- 인증 처리 통일: `AuthenticationResolver`가 null 반환 → 예외(`NoSuchElementException`) 던지도록 변경, 컨트롤러 5곳의 null 체크 중복 제거
- `GlobalExceptionHandler`에 `JwtException` → 401 핸들러 추가
- 201 응답 통일: `MemberController`의 `ResponseEntity.status(CREATED)` → `ResponseEntity.created(URI)` 패턴으로 통일
- 컨트롤러 반환 타입: `ResponseEntity<?>` → 구체 타입으로 변경
- `./gradlew spotlessApply build` — 빌드 + 테스트 통과 확인

### 프롬프트 6: 비밀번호 검증 로직을 Member 엔티티로 이동
> 비밀번호 검증로직을 Member 로 옮기면 어떨까?

- `MemberService.login()`의 비밀번호 비교 로직을 `Member.validatePassword(password)` 메서드로 추출
- 자기 상태 기반 검증(`chargePoint`, `deductPoint`와 동일 패턴)이 엔티티에 모이도록 통일
- `./gradlew spotlessApply build` — 빌드 + 테스트 통과 확인

### 프롬프트 7: 주문 시 위시리스트 자동 삭제
> wish 에 있던 상품을 구매하면 wish 에서 삭제하는 로직이 필요 할 것 같은데

- `OrderService.placeOrder()`에 `WishRepository` 의존성 추가
- 주문 저장 후 `wishRepository.findByMemberIdAndProductId()` → `ifPresent(delete)` 로직 추가
- 위시가 없는 경우 무시, 같은 트랜잭션 안에서 원자적 처리
- `./gradlew spotlessApply build` — 빌드 + 테스트 통과 확인

### 프롬프트 8: 위시 자동 삭제 테스트 코드 작성
> 방금 추가한거 테스트 코드 작성해줘

- `GiftAcceptanceTest`에 `위시에_담은_상품을_주문하면_위시에서_삭제된다()` 테스트 추가
- 위시 등록(201) → 주문(201) → 위시 목록 조회 → content가 비어있는지 검증
- `./gradlew test --tests "gift.GiftAcceptanceTest"` — 6개 테스트 모두 통과 확인

### 프롬프트 9: 가격 계산 중복 제거
> String.format 가격 계산이 Option.calculateTotalPrice 랑 중복되는데 합칠 수 있을까?

- `KakaoRestMessageClient.buildTemplate()`의 `product.getPrice() * order.getQuantity()` → `option.calculateTotalPrice(order.getQuantity())`로 교체
- 가격 계산 로직이 `Option.calculateTotalPrice()` 한 곳으로 통일
- `./gradlew spotlessApply build` — 빌드 + 테스트 통과 확인
