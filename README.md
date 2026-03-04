# spring-gift-refactoring

## 1단계 리팩터링 개요

**목표:** 구조 변경으로 변경 난이도를 낮추되, 기존 작동을 유지한다.

현재 Service 계층 없이 모든 비즈니스 로직이 Controller에 직접 구현되어 있다.
스타일 불일치(영/한 혼용, `var`/명시적 타입 혼용, `final` 사용 불일치, 불필요한 `@Autowired`, 하드코딩된 HTTP 상태 코드)와
미사용 코드, 보일러플레이트 주석이 존재한다.
이를 단계적으로 정리하여 이후 기능 추가·변경을 쉽게 만든다.

---

## 기능 목록

### Phase 1 — 도구 도입 및 포매팅

- [ ] 1. `build: add Spotless plugin with pre-commit hook`
  - **수정 파일:** build.gradle.kts
  - **변경 내용:**
    - `com.diffplug.spotless` 플러그인 추가 (Palantir Java Format, 미사용 import 자동 제거)
    - `spotlessCheck`를 실행하는 pre-commit Git hook 자동 설치 태스크 추가
  - **도입 근거:** 포맷 규칙은 도구로 강제하지 않으면 사람마다 다르게 적용된다.
    `spotlessApply`를 개발자가 매번 기억해서 실행하는 것은 현실적이지 않고,
    CI에서만 체크하면 PR을 올린 뒤에야 실패를 알게 되어 피드백 루프가 느리다.
    pre-commit hook으로 커밋 시점에 즉시 잡아주면 가장 빠르게 교정할 수 있다.

- [ ] 2. `style: apply Spotless formatting to all Java sources`
  - **수정 파일:** 전체 Java 소스
  - **변경 내용:** `./gradlew spotlessApply` 실행 결과 반영

### Phase 2 — 코드 스타일 통일

- [ ] 3. `style: remove redundant @Autowired on single-constructor beans`
  - **수정 파일:** JwtProvider, AuthenticationResolver, MemberController, AdminMemberController
  - **변경 내용:** Spring 4.3+ 단일 생성자 빈에서 불필요한 `@Autowired` 제거

- [ ] 4. `style: replace hard-coded HTTP status codes with HttpStatus constants`
  - **수정 파일:** OrderController, WishController
  - **변경 내용:** `status(401)` → `HttpStatus.UNAUTHORIZED`, `status(403)` → `HttpStatus.FORBIDDEN` (6곳)

- [ ] 5. `style: standardize error messages to Korean`
  - **수정 파일:** Member.java, MemberController, AdminMemberController
  - **변경 내용:** 영문 에러 메시지를 한국어로 통일 (예: "Amount must be greater than zero." → "충전 금액은 1 이상이어야 합니다.")

- [ ] 6. `style: replace var with explicit types`
  - **수정 파일:** WishController, OrderController, KakaoAuthController
  - **변경 내용:** `var` → 명시적 타입 선언으로 통일

- [ ] 7. `style: add final to local variables`
  - **수정 파일:** 전체 Controller, KakaoLoginClient, KakaoMessageClient
  - **변경 내용:** 재할당하지 않는 지역 변수에 `final` 추가

- [ ] 8. `style: standardize @RequestMapping to use value attribute`
  - **수정 파일:** OptionController, KakaoAuthController
  - **변경 내용:** `@RequestMapping(path = "...")` → `@RequestMapping("...")` 통일

- [ ] 9. `style: translate inline comments to Korean`
  - **수정 파일:** Member.java, Wish.java, Order.java, WishController, OrderController
  - **변경 내용:** 영문 인라인 주석을 한국어로 전환

- [ ] 10. `style: remove boilerplate Javadoc and translate meaningful comments`
  - **제거 대상:** `@author`/`@since` Javadoc (JwtProvider, AuthenticationResolver, TokenResponse, MemberController, AdminMemberController, Member, MemberRepository, MemberRequest), JwtProvider 메서드 Javadoc 2개, OptionNameValidator 블록 주석
  - **한국어 전환:** OptionController 블록 주석 (도메인 규칙), KakaoAuthController 블록 주석 (OAuth 흐름)

### Phase 3 — 불필요한 코드 제거

- [ ] 11. `refactor: remove unused Collectors import in OptionController`
  - **수정 파일:** OptionController
  - **변경 내용:** `.collect(Collectors.toList())` → `.toList()` (Java 16+)

- [ ] 12. `refactor: remove unused WishRepository from OrderController`
  - **수정 파일:** OrderController
  - **변경 내용:** 미구현 "cleanup wish" 단계의 미사용 의존성 제거

### Phase 4 — 서비스 계층 추출

- [ ] 13. `refactor: extract CategoryService from CategoryController`
  - **생성:** CategoryService | **수정:** CategoryController

- [ ] 14. `refactor: extract MemberService from controllers`
  - **생성:** MemberService | **수정:** MemberController, AdminMemberController

- [ ] 15. `refactor: extract ProductService from ProductController`
  - **생성:** ProductService | **수정:** ProductController

- [ ] 16. `refactor: extract OptionService from OptionController`
  - **생성:** OptionService | **수정:** OptionController

- [ ] 17. `refactor: extract WishService from WishController`
  - **생성:** WishService | **수정:** WishController

- [ ] 18. `refactor: extract KakaoAuthService from KakaoAuthController`
  - **생성:** KakaoAuthService | **수정:** KakaoAuthController

- [ ] 19. `refactor: extract OrderService from OrderController`
  - **생성:** OrderService | **수정:** OrderController

- [ ] 20. `refactor: delegate AdminProductController to ProductService`
  - **수정:** AdminProductController

---

## 서비스 추출 설계 원칙

- **Controller:** 요청 검증(auth 체크, `@Valid`)과 위임만 담당
- **Service:** 비즈니스 로직, `@Transactional` 관리, `.orElseThrow()` 사용
- Auth 체크(`AuthenticationResolver.extractMember`)는 HTTP 관심사이므로 Controller에 유지
- Service는 `Member` 객체나 `memberId`를 받고, Authorization 헤더를 직접 다루지 않음

---

## 코드 스타일 컨벤션

| 항목 | 규칙 |
|---|---|
| 포매터 | Palantir Java Format (Spotless) |
| 타입 선언 | 명시적 타입 (`var` 사용 금지) |
| 지역 변수 | 재할당하지 않으면 `final` |
| 에러 메시지 | 한국어 |
| 주석 | 한국어, 도메인 규칙·비자명한 흐름만 남김 |
| `@RequestMapping` | `value` 속성 사용 (`path` 사용 금지) |

---

## 검증 방법

매 커밋마다 아래 명령으로 E2E 테스트 전체 통과를 확인한다.

```bash
./gradlew cucumberE2eTest
```

44개 E2E 시나리오 전체 통과 시 해당 커밋의 리팩터링이 기존 동작을 깨뜨리지 않음을 보장한다.

---

## 2단계 리팩터링 개요

**목표:** 작동 변경을 안전하게 수행하고, 그 결과를 증거로 보여준다.

1단계에서 서비스 계층을 추출했지만, 비즈니스 로직 일부가 Controller에 남아 있다.
위시 중복 체크·소유권 검증은 Controller에서 직접 수행하고,
`OrderService`는 트랜잭션 밖에서 조회된 `Member`를 받아 포인트를 차감하므로 동시 요청에 취약하다.
주문 후 위시 삭제(`// TODO: cleanup wish`)는 의도만 남긴 채 구현되지 않았다.
예외 핸들러 8개가 6개 Controller에 동일하게 반복되고,
가격 계산(`price × quantity`)이 `OrderService`와 `KakaoMessageClient`에 중복된다.

이번 단계에서는 도메인 책임을 올바른 위치로 이동(구조 변경)한 뒤,
트랜잭션 경계를 바로잡고 누락된 기능을 완성(작동 변경)한다.
구조 변경 커밋과 작동 변경 커밋은 분리한다.

---

### Phase 5 — 도메인 책임 이동 (구조 변경)

- [ ] 21. `refactor: extract GlobalExceptionHandler via @ControllerAdvice`
  - **생성:** GlobalExceptionHandler
  - **수정:** ProductController, OptionController, CategoryController, WishController, OrderController, MemberController
  - **변경 내용:**
    - `@ExceptionHandler(NoSuchElementException.class)` 5곳 → 전역 핸들러로 통합
    - `@ExceptionHandler(IllegalArgumentException.class)` 3곳 → 전역 핸들러로 통합
    - 각 Controller에서 해당 메서드 제거

- [ ] 22. `refactor: move Kakao authorization URL building to KakaoLoginClient`
  - **수정:** KakaoLoginClient, KakaoAuthController
  - **변경 내용:**
    - `KakaoLoginClient.buildAuthorizationUrl()` 메서드 추가 (인가 URL 조립 책임 이동)
    - `KakaoAuthController`에서 URL 조립 코드 제거, `KakaoLoginClient` 호출로 대체
    - Controller에서 `KakaoLoginProperties` 의존성 제거

- [ ] 23. `refactor: add behavior methods to domain entities`
  - **수정:** Member, Order, Wish, MemberService, OrderService, KakaoMessageClient, WishController
  - **변경 내용:**
    - `Member.matchesPassword(String)` — 비밀번호 비교 캡슐화 (← `MemberService:30`의 getter 비교 대체)
    - `Member.isKakaoLinked()` — 카카오 토큰 존재 여부 (← `OrderService:53`의 null 체크 대체)
    - `Order.getTotalPrice()` — 가격 × 수량 (← `OrderService:46`, `KakaoMessageClient:33` 중복 계산 제거)
    - `Wish.isOwnedBy(Long memberId)` — 소유권 판단 (← `WishController:73`의 비교 대체)

- [ ] 24. `refactor: replace manual auth check with HandlerMethodArgumentResolver`
  - **생성:** `@AuthMember` 어노테이션, `AuthMemberArgumentResolver`
  - **수정:** WishController, OrderController, WebMvcConfigurer 구현체
  - **변경 내용:**
    - `AuthenticationResolver.extractMember()` + null 체크 보일러플레이트 5곳 제거
    - `@AuthMember Member member` 파라미터 선언만으로 인증된 회원 자동 주입
    - 미인증 시 `ArgumentResolver`에서 `UNAUTHORIZED` 응답 처리

- [ ] 25. `refactor: replace ResponseEntity<?> wildcards with concrete types`
  - **수정:** OrderController, WishController
  - **변경 내용:**
    - `ResponseEntity<?>` → `ResponseEntity<Page<OrderResponse>>`, `ResponseEntity<OrderResponse>` 등 구체 타입으로 변경
    - API 응답 계약 명확화

### Phase 6 — 작동 변경

- [ ] 26. `fix: add logging for Kakao message send failure in OrderEventListener`
  - **수정:** OrderEventListener
  - **변경 내용:**
    - `catch (Exception ignored)` → `catch (Exception e) { log.warn(..., e); }` 로 변경
    - 카카오 메시지 전송 실패 시 원인 추적 가능하도록 경고 로그 기록

- [ ] 27. `fix: move wish business logic to WishService and make atomic`
  - **수정:** WishService, WishController
  - **변경 내용:**
    - 중복 체크 + 생성 → `WishService.addWish(Long memberId, Long productId)`로 통합 (단일 `@Transactional`)
    - 소유권 체크 + 삭제 → `WishService.removeWish(Long wishId, Long memberId)`로 통합 (단일 `@Transactional`)
    - Controller는 Service에 위임만 수행
  - **검증:** 위시 중복 추가 시 기존 위시 반환, 타인 위시 삭제 시 403 응답 확인

- [ ] 28. `fix: reload member inside transaction in OrderService`
  - **수정:** OrderService, OrderController
  - **변경 내용:**
    - `createOrder(Member, OrderRequest)` → `createOrder(Long memberId, OrderRequest)`
    - 트랜잭션 내에서 `memberRepository.findById(memberId)`로 managed 엔티티 조회
    - detached 엔티티로 인한 포인트 덮어쓰기 방지
  - **검증:** 주문 후 회원 포인트 잔액 재조회로 정확한 차감 확인

- [ ] 29. `feat: remove wish on order placement`
  - **수정:** OrderService
  - **변경 내용:**
    - `OrderService`에 `WishRepository` 주입
    - 주문 저장 후 해당 회원·상품의 위시를 조회, 존재하면 삭제
  - **검증:** 위시 등록 → 주문 → 위시 목록 재조회 시 해당 위시 부재 확인

---

## 작동 변경 원칙

- **구조 변경과 작동 변경을 섞지 않는다:** Phase 5(구조)와 Phase 6(작동)의 커밋을 분리한다. 단, 코드 이동이 트랜잭션 경계를 필연적으로 변경하는 경우 작동 변경으로 분류한다.
- **작동 변경은 증거와 함께:** 예외 발생 여부가 아니라, 상태를 재조회하여 관찰 가능한 방식으로 검증한다.
- **트랜잭션 경계:** 하나의 논리 작업(조회 → 검증 → 저장)은 같은 `@Transactional` 안에서 수행한다.
- **엔티티 행위:** 판단과 계산은 엔티티 메서드로 캡슐화한다. Service가 getter로 상태를 꺼내 비교하지 않는다.
- **ADR:** 선택지가 2개 이상이고 트레이드오프가 있었거나, 팀이 따라야 할 규칙을 정한 경우 `docs/adr/`에 기록한다.

---

## 검증 방법 (2단계)

매 커밋마다 기존 E2E 테스트를 확인한다.

```bash
./gradlew cucumberE2eTest
```

작동 변경(Phase 6)은 추가로 E2E 시나리오를 작성하여 검증한다.
추가될 시나리오는 기존 시나리오의 톤앤매너 및 기조에 맞추어 설계한다.

```bash
./gradlew test
```
