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
