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
- **Service:** 비즈니스 로직, `.orElseThrow()` 사용
- Auth 체크(`AuthenticationResolver.extractMember`)는 HTTP 관심사이므로 Controller에 유지
- Service는 `Member` 객체나 `memberId`를 받고, Authorization 헤더를 직접 다루지 않음

---

## 서비스 추출 순서 및 의사 결정

### 변경 전략

1단계는 **구조 변경만** 수행하고 작동 변경은 하지 않는다.
Controller에 있던 로직을 Service로 이동하되, `@Transactional` 추가나 새로운 동작 구현은 별도 단계에서 진행한다.

### 추출 순서: 의존 그래프의 리프 노드부터

도메인 간 의존성이 적은 것부터 추출하여, 매 커밋마다 컴파일·테스트가 통과하고
이전에 만든 Service를 다음 커밋에서 재사용할 수 있도록 했다.

| 순서 | Service                | 의존하는 도메인          | 이 순서인 이유              |
|----|------------------------|-------------------|-----------------------|
| 1  | CategoryService        | 없음                | 의존 없는 리프 노드           |
| 2  | MemberService          | 없음                | 의존 없는 리프 노드           |
| 3  | ProductService         | Category          | Category 추출 완료 후 진행   |
| 4  | OptionService          | Product           | Product 추출 완료 후 진행    |
| 5  | WishService            | Product           | Product 추출 완료 후 진행    |
| 6  | KakaoAuthService       | Member            | Member 추출 완료 후 진행     |
| 7  | OrderService           | Option, Member    | 의존 대상 모두 추출 완료 후 진행   |
| 8  | AdminProductController | Product, Category | 이미 만든 Service를 재사용만 함 |

### 의존 그래프

```
Category ← Product ← Option ← Order
Member   ← KakaoAuth          ↑
         ← AdminMember    Order
Product  ← Wish
```

---

## 2단계 리팩터링 개요

**목표:** 작동 변경을 안전하게 수행하고, 그 결과를 검증 가능한 증거로 보여준다.

**원칙:**
- 구조 변경 커밋과 작동 변경 커밋을 분리한다
- 작동 변경은 반드시 상태를 재조회하여 검증하는 테스트와 함께 제출한다
- 트레이드오프가 있는 결정은 ADR로 남긴다

---

## 2단계 기능 목록

### Phase 5 — 도메인 책임 되찾기 (구조 변경)

- [ ] 21. `refactor(member): 비밀번호 검증을 Member 엔티티로 이동`
  - **수정 파일:** Member.java, MemberService.java
  - **변경 내용:**
    - `Member.checkPassword(String password)` 메서드 추가
    - `MemberService.login()`의 비밀번호 비교 로직(`password == null || !equals`)을 `member.checkPassword()` 호출로 교체
  - **이득:** 비밀번호 비교의 null 처리가 엔티티에 캡슐화됨. 향후 비밀번호 해싱 도입 시 Member만 수정하면 됨
  - **검증:** E2E 테스트 전체 통과

- [ ] 22. `refactor(product): 주문 금액 계산을 Product 도메인으로 이동`
  - **수정 파일:** Product.java, OrderService.java
  - **변경 내용:**
    - `Product.calculateTotalPrice(int quantity)` 메서드 추가
    - `OrderService.createOrder()`의 `option.getProduct().getPrice() * request.quantity()`를 `option.getProduct().calculateTotalPrice(request.quantity())` 호출로 교체
  - **이득:** 가격 정책 변경 시(할인, 세금 등) Product만 수정. 호출부에서 계산 로직 제거
  - **검증:** E2E 테스트 전체 통과

### Phase 6 — @Transactional 정리 (구조 변경)

- [ ] 23. `refactor: 단일 저장 메서드에서 불필요한 @Transactional 제거`
  - **수정 파일:** CategoryService, ProductService, MemberService, WishService
  - **변경 내용:**
    - 단일 `save()` 또는 `deleteById()`만 호출하는 메서드에서 `@Transactional` 제거
    - **제거 대상:**
      - `CategoryService`: `createCategory`, `deleteCategory`
      - `ProductService`: `deleteProduct`
      - `MemberService`: `deleteMember`
      - `WishService`: `removeWish`
    - **유지 대상 (read-then-write 또는 multi-save):**
      - `CategoryService.updateCategory` (find → update → save)
      - `MemberService.register` (existsByEmail → save)
      - `MemberService.updateMember` (find → update → save)
      - `MemberService.chargePoint` (find → charge → save)
      - `ProductService.createProduct` (validate → findCategory → save)
      - `ProductService.updateProduct` (find → find → update → save)
      - `OptionService.createOption` (find → check → save)
      - `OptionService.deleteOption` (find → check → find → delete)
      - `OrderService.createOrder` (find → subtract → deduct → save × 3)
      - `KakaoAuthService.handleCallback` (findOrCreate → update → save)
  - **근거:** `save()` 하나만 호출하는 메서드는 JPA가 이미 트랜잭션을 보장. 불필요한 `@Transactional`은 의도를 흐림
  - **검증:** E2E 테스트 전체 통과

### Phase 7 — 외부 API 트랜잭션 분리 (구조 변경)

- [ ] 24. `refactor(auth): KakaoAuthService 외부 API 호출을 트랜잭션 밖으로 분리`
  - **수정 파일:** KakaoAuthService.java
  - **변경 내용:**
    - 외부 API 호출(`requestAccessToken`, `requestUserInfo`)을 `@Transactional` 밖으로 이동
    - DB 작업(find-or-create member, update token, save)만 `@Transactional` 범위에 유지
    - 방법: `handleCallback()`에서 `@Transactional` 제거 → 외부 호출 먼저 수행 → `@Transactional` 내부 메서드 호출
  - **근거:** 1단계에서 OrderService에 동일 문제를 `@TransactionalEventListener`로 해결함. KakaoAuthService도 외부 API 실패 시 불필요한 DB 커넥션 점유와 롤백 위험이 있음
  - **검증:** E2E 테스트 전체 통과

### Phase 8 — 트랜잭션 경계 증거 (작동 변경)

- [ ] 25. `test(order): 주문 실패 시 재고 원복 검증 테스트 추가`
  - **수정 파일:** order.feature, E2eOrderStepDefinitions.java
  - **변경 내용:**
    - 기존 "포인트 부족 시 주문 실패" 시나리오에 `그리고 해당 옵션의 재고는 10개이다` 검증 추가
    - 포인트 차감 실패 시 재고 차감도 롤백되는지 상태를 재조회하여 확인
  - **증거:** 트랜잭션 롤백으로 재고가 원복됨을 관찰
  - **ADR:** `docs/adr/001-order-transaction-boundary.md` 작성
    - `@Transactional` 경계를 `OrderService.createOrder()`에 설정한 근거
    - 재고 차감 + 포인트 차감 + 주문 저장이 하나의 논리 작업인 이유

### Phase 9 — 누락된 작동 구현 (작동 변경)

- [ ] 26. `feat(order): 주문 시 위시 자동 삭제 구현`
  - **수정 파일:** OrderService.java
  - **변경 내용:**
    - `WishRepository` 의존성 추가
    - 주문 완료 후 해당 회원+상품의 위시가 존재하면 삭제
  - **근거:** 초기 코드에 `// 6. cleanup wish` 주석과 `WishRepository` 의존성이 있었으나 구현되지 않았음
  - **검증:** 별도 E2E 시나리오 추가

- [ ] 27. `test(order): 주문 시 위시 삭제 검증 테스트 추가`
  - **수정 파일:** order.feature, E2eOrderStepDefinitions.java
  - **변경 내용:**
    ```gherkin
    시나리오: 주문하면 해당 상품의 위시가 삭제된다
      먼저 재고가 10개인 옵션이 존재한다
      그리고 해당 상품이 위시리스트에 존재한다
      만일 해당 옵션을 1개 주문하면
      그러면 주문에 성공한다
      그리고 위시리스트에서 해당 상품이 제거되었다
    ```
  - **증거:** 위시를 재조회하여 삭제 상태를 확인
  - **ADR:** `docs/adr/002-wish-cleanup-strategy.md` 작성
    - 위시 삭제를 트랜잭션 내 동기 처리로 결정한 근거
    - 주문과 위시 삭제는 하나의 논리 작업인 이유

---

### 2단계 커밋 순서 요약

| 순서 | 유형 | 커밋 | 증거 |
|------|------|------|------|
| 21 | 구조 변경 | 비밀번호 검증 → Member 엔티티 | E2E 전체 통과 |
| 22 | 구조 변경 | 가격 계산 → Product 도메인 | E2E 전체 통과 |
| 23 | 구조 변경 | 불필요한 @Transactional 제거 | E2E 전체 통과 |
| 24 | 구조 변경 | KakaoAuthService 외부 API 분리 | E2E 전체 통과 |
| 25 | 작동 변경 | 트랜잭션 롤백 검증 테스트 + ADR | 재고 원복 시나리오 |
| 26 | 작동 변경 | 주문 시 위시 삭제 구현 | — |
| 27 | 작동 변경 | 위시 삭제 검증 테스트 + ADR | 위시 삭제 시나리오 |

### 2단계 커밋 순서 근거

구조 변경(21–24)을 작동 변경(25–27)보다 먼저 배치한다.
구조 변경은 기존 작동을 깨뜨리지 않으므로 E2E 테스트만으로 검증 가능하고,
작동 변경은 새로운 시나리오 테스트와 ADR이 필요하므로 나중에 배치하여
검증 기반을 먼저 확보한다.

| 순서 | 이유 |
|------|------|
| 21 먼저 | Member는 의존이 없는 리프 노드. 가장 단순한 도메인 캡슐화 |
| 22 다음 | Product도 리프 노드. 21과 독립적이지만 같은 유형이므로 연속 배치 |
| 23 다음 | @Transactional 정리는 전체 서비스에 걸치므로, 도메인 변경(21-22) 완료 후 일괄 수행 |
| 24 다음 | KakaoAuthService 분리는 23의 유지 목록 확정 후 진행해야 정합성 유지 |
| 25 다음 | OrderService.createOrder의 트랜잭션 경계를 테스트로 증명. 구조가 확정된 상태에서 수행 |
| 26→27 | 위시 삭제 구현(26)과 검증(27)은 한 쌍. 구현 후 즉시 테스트로 증거 제시 |

---

## 코드 스타일 컨벤션

| 항목                | 규칙                              |
|-------------------|---------------------------------|
| 포매터               | Palantir Java Format (Spotless) |
| 타입 선언             | 명시적 타입 (`var` 사용 금지)            |
| 지역 변수             | 재할당하지 않으면 `final`               |
| 에러 메시지            | 한국어                             |
| 주석                | 한국어, 도메인 규칙·비자명한 흐름만 남김         |
| `@RequestMapping` | `value` 속성 사용 (`path` 사용 금지)    |

---

## 검증 방법

매 커밋마다 아래 명령으로 E2E 테스트 전체 통과를 확인한다.

```bash
./gradlew cucumberE2eTest
```

44개 E2E 시나리오 전체 통과 시 해당 커밋의 리팩터링이 기존 동작을 깨뜨리지 않음을 보장한다.
