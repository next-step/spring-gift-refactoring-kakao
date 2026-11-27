# 서비스 분리 작업 목록

컨트롤러의 비즈니스 로직을 서비스로 옮기고, ArchUnit으로 그 구조를 고정하는 작업이다. 모두 구조 변경이다.
주문만 다루던 예전 목록(`docs/work/03-service-layer/tasks.md`)의 1~8번을 합쳤다.

- 작성일: 2026-11-26
- 기준 브랜치: `M32k`
- 용어는 [`docs/glossary.md`](../../glossary.md)를 따른다.

## 공통 규칙

- 한 항목 = 커밋 하나. 바뀌는 파일이 3개를 넘으면 더 쪼갠다.
- 확인 명령은 사용자가 실행한다. 결과가 너무 빨리 나오면 `--rerun-tasks`를 붙인다.
  - 인수 테스트: `./gradlew test --tests 'gift.acceptance.CucumberTest'`
  - ArchUnit(A1 뒤부터): `./gradlew test --tests 'gift.architecture.*'`
- 작동이 바뀔 수 있는 일은 목록에 넣지 않고, 맨 아래 "작동 변경 후보" 표에 적는다. 4단계에서 다시 연다.
- git 명령은 사용자가 실행한다.

## 목표

컨트롤러에는 요청 검증(로그인 확인과 401 포함), 서비스 호출, HTTP 응답 만들기(404, 201 등)만 남긴다. 비즈니스 로직은 서비스로 옮긴다.

## 옮길 때 지킬 것 (모든 항목에 공통)

- 서비스에 `@Transactional`을 붙이지 않는다. 붙이면 16-2가 깨진다(open-questions 3번).
- 확인 순서를 그대로 둔다. 예: 옵션 삭제는 상품 404 → 옵션 1개 400 → 옵션 404, 상품 수정은 이름 400 → 카테고리 404 → 상품 404.
- 404는 서비스가 `Optional`이나 결과 값으로 알리고, 컨트롤러가 `ResponseEntity.notFound()`로 응답한다. 예외로 바꾸지 않는다.
- 서비스는 지금 코드와 같은 종류의 예외를 같은 메시지로 던진다. 컨트롤러의 `@ExceptionHandler`는 컨트롤러에 그대로 둔다. 서비스에서 던져도 같은 처리기가 잡는다.
- 컨트롤러 하나에 항목 하나. 바꾸는 파일은 서비스(새 파일)와 컨트롤러, 2개다.

---

## 안전망과 ArchUnit

| 컨트롤러 | 지금 인수 테스트 | 옮기기 전 안전망 |
|---|---|---|
| OrderController | 16-1~16-3, 17-1~17-4 | 끝(L1): `order.feature`(16-4~16-7) |
| MemberController | 1-1~1-3 | 끝(L2): `login.feature`(2-1~2-3) |
| WishController | 15-1 | 끝(L3): `wish-list.feature`(13-1~13-3), `wish-add.feature`(14-1~14-3), `wish.feature`(15-2~15-3) |
| ProductController | 7-1~7-3 | 끝(L4): `product-view.feature`(4-1~4-4), `product-update.feature`(8-1~8-6), `product-delete.feature`(9-1~9-4) |
| OptionController | 없음 | 끝(L5): `option-list.feature`(6-1~6-2), `option-create.feature`(11-1~11-6), `option-delete.feature`(12-1~12-7) |
| CategoryController | 없음 | 끝(L6): `category-list.feature`(5-1), `category-manage.feature`(10-1~10-9) |
| AdminProductController | 없음 | 이번에는 옮기지 않는다(아래 "옮기지 않은 것") |
| AdminMemberController | 없음 | 이번에는 옮기지 않는다(아래 "옮기지 않은 것") |
| KakaoAuthController | 없음. 만들 수 없음(카카오 서버) | 코드 검토와, 사용자가 직접 카카오 로그인해 보기 |

### L1. 주문하기 오류 응답 인수 테스트 추가 (테스트만)
끝: `order.feature`(16-4~16-7)

- `/acceptance-test 16`으로 다음 시나리오를 더한다. 주문 흐름 분석에서 요청으로 확인한 작동이다(`docs/work/01-project-analysis/order-flow.md`).
  - 없는 옵션: 404
  - 틀린 로그인 토큰: 401
  - 로그인 토큰 헤더 없음: 400
  - 주문 수량 0: 400
- 바꾸는 파일: `src/test/resources/features/order.feature`, `src/test/java/gift/acceptance/steps/OrderSteps.java`, `docs/user-stories.md`(위치 줄)
- 확인 명령: 인수 테스트. 기대값 하나를 일부러 틀리게 바꿔 실패하는지 본다.
- 커밋 메시지 예: `test: 주문하기 오류 응답 인수 테스트 추가`

### L2~L6. 나머지 안전망 (테스트만)

- 위 표의 "필요" 줄마다 `/acceptance-test <스토리 번호>`로 하나씩 커밋한다.
  - L2: 스토리 2(로그인 실패). 끝: `login.feature`(2-1~2-3)
  - L3: 스토리 13, 14(위시 목록, 담기). 끝: `wish-list.feature`(13-1~13-3), `wish-add.feature`(14-1~14-3), `wish.feature`(15-2~15-3)
  - L4: 스토리 4, 8, 9(상품 목록, 수정, 삭제). 끝: `product-view.feature`(4-1~4-4), `product-update.feature`(8-1~8-6), `product-delete.feature`(9-1~9-4)
  - L5: 스토리 6, 11, 12(옵션 목록, 등록, 삭제). 끝: `option-list.feature`(6-1~6-2), `option-create.feature`(11-1~11-6), `option-delete.feature`(12-1~12-7)
  - L6: 스토리 5, 10(카테고리). 끝: `category-list.feature`(5-1), `category-manage.feature`(10-1~10-9)
- 관리자 화면 스토리 18~26은 넣지 않는다.
- 각 항목은 해당 컨트롤러를 옮기는 항목 바로 앞에 한다. 스킬은 스토리 하나씩 받으므로, 스토리가 여러 개인 항목은 더 쪼개도 된다.
- 바꾸는 파일: 해당 feature 파일, steps 파일, `docs/user-stories.md`
- 확인 명령: 인수 테스트. 기대값 하나를 일부러 틀리게 바꿔 실패하는지 본다.
- 커밋 메시지 예: `test: 옵션 등록·삭제 인수 테스트 추가`

### A1. ArchUnit 추가와 지금 통과하는 규칙 (테스트만)
끝: ef45559 test: ArchUnit 계층 규칙 추가

- 규칙 0: Repository는 Controller에 의존하지 않는다. 지금 코드에서 통과해야 한다.
- 바꾸는 파일
  - `build.gradle.kts`: `testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")`
  - `src/test/java/gift/architecture/LayerArchitectureTest.java`(새 파일)
- 확인 명령: ArchUnit, 인수 테스트
- 커밋 메시지 예: `test: ArchUnit 계층 규칙 추가`

### A2. 서비스 추출 전 커밋에서 규칙이 실패하는지 확인 (기록만)
끝: bbbd4ab docs: ArchUnit 컨트롤러 규칙의 추출 전 실패 결과 기록

- 규칙 A를 테스트 파일에 임시로 써서, A1 커밋 상태(서비스 추출 전)에서 돌린다.
  - 규칙 A: `@RestController`/`@Controller` 클래스는 `org.springframework.data.repository.Repository`에 의존하지 않는다. 단, `AdminProductController`와 `AdminMemberController`는 뺀다.
- 기대 결과: 실패. 컨트롤러 7개(Category, Member, Option, Order, Product, Wish, KakaoAuth)가 위반으로 나온다. 관리자 컨트롤러 두 개는 나오지 않아야 한다.
- 실패 출력 요약을 아래 "A2 결과"에 적는다. 테스트 파일 변경은 커밋하지 않는다(`git restore`는 사용자가 한다).
- 바꾸는 파일: `docs/work/05-service-layer/tasks.md`
- 확인 명령: ArchUnit(실패가 맞다)
- 커밋 메시지 예: `docs: ArchUnit 컨트롤러 규칙의 추출 전 실패 결과 기록`

**A2 결과**(2026-11-26, A1 커밋 `ef45559` 상태): 규칙 0은 통과, 규칙 A는 실패했다. 위반 62건, 위반한 컨트롤러 7개(CategoryController, MemberController, OptionController, OrderController, ProductController, WishController, KakaoAuthController). AdminProductController와 AdminMemberController는 규칙에서 빼서 나오지 않았다. 규칙 A가 실제로 검사하는 것을 확인했다.

---

## 서비스로 옮기기

### O1. 주문 내역 조회를 OrderService로
끝: 6ff1899 refactor: 주문 내역 조회를 OrderService로 옮김

- `OrderService`(새 파일)를 만들고 `findOrders(Long memberId, Pageable pageable)`로 `orderRepository.findByMemberId(...).map(OrderResponse::from)`을 옮긴다.
- 로그인 확인(`authenticationResolver.extractMember`)과 401 응답은 컨트롤러에 남긴다.
- 바꾸는 파일: `src/main/java/gift/order/OrderService.java`(새 파일), `src/main/java/gift/order/OrderController.java`
- 확인 명령: 인수 테스트(17-1~17-4), ArchUnit
- 커밋 메시지 예: `refactor: 주문 내역 조회를 OrderService로 옮김`

### O2. 주문하기(옵션 확인~주문 저장)를 OrderService로
끝: f55d1ac refactor: 주문하기 로직을 OrderService로 옮김

- `OrderService.createOrder(Member member, OrderRequest request)`로 옮긴다. 순서는 그대로 둔다.
  - 1) 옵션 확인, 2) 재고 차감과 저장, 3) 주문 금액 계산, 포인트 차감과 저장, 4) 주문 저장
- 포인트 부족과 재고 부족의 `IllegalArgumentException`은 잡지 않는다. 지금처럼 500이 되어야 한다(16-2, 16-3).
- 없는 옵션은 지금처럼 본문 없는 404가 되어야 한다.
- 흐름 주석(`// 6. cleanup wish` 포함)도 서비스로 함께 옮긴다.
- 바꾸는 파일: `src/main/java/gift/order/OrderService.java`, `src/main/java/gift/order/OrderController.java`
- 확인 명령: 인수 테스트(16-1~16-3, 17-2, L1), ArchUnit
- 커밋 메시지 예: `refactor: 주문하기 로직을 OrderService로 옮김`

### O3. 주문 알림 발송을 OrderService로
끝: ed27c9e refactor: 주문 알림 발송을 OrderService로 옮김 (K1 확인 때 카카오톡 주문 알림 도착 확인)

- 주문 알림 메서드(03-style N5에서 이름을 바꿈)를 `OrderService`로 옮긴다. 주문 저장 바로 뒤에 부른다.
- 카카오 액세스 토큰이 없으면 보내지 않는다. 모든 예외를 잡고 무시하며, 로그도 지금처럼 남기지 않는다(open-questions 10번).
- 주문 알림은 인수 테스트로 확인할 수 없다(초기 데이터 회원에게 카카오 액세스 토큰이 없다). 옮긴 뒤 코드를 읽어 위 두 가지를 확인한다. 필요하면 카카오 로그인 회원으로 직접 주문해 본다.
- 바꾸는 파일: `src/main/java/gift/order/OrderService.java`, `src/main/java/gift/order/OrderController.java`
- 확인 명령: 인수 테스트, ArchUnit, 코드 검토
- 커밋 메시지 예: `refactor: 주문 알림 발송을 OrderService로 옮김`

### O4. OrderController 의존성 정리
끝: 748532e refactor: OrderController 의존성 정리

- `OrderController`에는 `AuthenticationResolver`와 `OrderService`만 남긴다.
- `OrderRepository`, `OptionRepository`, `MemberRepository`, `KakaoMessageClient`는 서비스로 옮긴다. `WishRepository`는 04-unused-code의 U1에서 이미 지웠다.
- 바꾸는 파일: `src/main/java/gift/order/OrderController.java`, `src/main/java/gift/order/OrderService.java`
- 확인 명령: 인수 테스트, ArchUnit
- 커밋 메시지 예: `refactor: OrderController 의존성 정리`

### M1. MemberController를 MemberService로
끝: e32299f refactor: 회원 가입과 로그인을 MemberService로 옮김

- 회원 가입(중복 확인, 저장, 로그인 토큰 발급)과 로그인(비밀번호 비교)을 옮긴다.
- 메시지와 `@ExceptionHandler`는 그대로 둔다.
- 바꾸는 파일: `src/main/java/gift/member/MemberService.java`(새 파일), `src/main/java/gift/member/MemberController.java`
- 확인 명령: 인수 테스트(1-1~1-3, L2), ArchUnit
- 커밋 메시지 예: `refactor: 회원 가입과 로그인을 MemberService로 옮김`

### W1. WishController를 WishService로
끝: fbc3866 refactor: 위시 로직을 WishService로 옮김

- 담기는 "이미 있음(200)"과 "새로 담음(201)"을, 삭제는 "없음(404)", "남의 위시(403)", "삭제함(204)"을 결과 값으로 돌려준다. HTTP 코드는 컨트롤러가 정한다.
- 바꾸는 파일: `src/main/java/gift/wish/WishService.java`(새 파일), `src/main/java/gift/wish/WishController.java`
- 확인 명령: 인수 테스트(15-1, L3), ArchUnit
- 커밋 메시지 예: `refactor: 위시 로직을 WishService로 옮김`

### P1. ProductController를 ProductService로
끝: fcd59cc refactor: 상품 API 로직을 ProductService로 옮김

- 상품명 검증(`validate(name)`, "카카오" 금지)과 확인 순서를 그대로 둔다.
- 바꾸는 파일: `src/main/java/gift/product/ProductService.java`(새 파일), `src/main/java/gift/product/ProductController.java`
- 확인 명령: 인수 테스트(7-1~7-3, L4), ArchUnit
- 커밋 메시지 예: `refactor: 상품 API 로직을 ProductService로 옮김`

### Op1. OptionController를 OptionService로
끝: fc99c6d refactor: 옵션 로직을 OptionService로 옮김

- 옵션명 검증, 중복 확인, "옵션이 1개면 삭제 불가", 다른 상품의 옵션이면 404를 그대로 옮긴다.
- `Collectors.toList()`도 그대로 둔다.
- 바꾸는 파일: `src/main/java/gift/option/OptionService.java`(새 파일), `src/main/java/gift/option/OptionController.java`
- 확인 명령: 인수 테스트(L5), ArchUnit
- 커밋 메시지 예: `refactor: 옵션 로직을 OptionService로 옮김`

### C1. CategoryController를 CategoryService로
끝: fa6c010 refactor: 카테고리 로직을 CategoryService로 옮김

- 수정할 때 없는 카테고리는 지금처럼 404로 둔다.
- 바꾸는 파일: `src/main/java/gift/category/CategoryService.java`(새 파일), `src/main/java/gift/category/CategoryController.java`
- 확인 명령: 인수 테스트(L6), ArchUnit
- 커밋 메시지 예: `refactor: 카테고리 로직을 CategoryService로 옮김`

### K1. 카카오 로그인 콜백의 회원 처리를 MemberService로
끝: c4790fb refactor: 카카오 로그인 회원 처리를 MemberService로 옮김 (직접 카카오 로그인 후 주문 201, 카카오톡 주문 알림 도착 확인)

- 회원 찾기·만들기와 카카오 액세스 토큰 저장, 로그인 토큰 발급을 옮긴다.
- 카카오 로그인 주소 만들기(`/login`)는 컨트롤러에 남긴다.
- 인수 테스트가 없다. 옮긴 뒤 코드를 읽어 순서가 같은지 확인하고, 사용자가 직접 카카오 로그인을 해 본다.
- 바꾸는 파일: `src/main/java/gift/member/MemberService.java`, `src/main/java/gift/auth/KakaoAuthController.java`
- 확인 명령: 인수 테스트, ArchUnit, 코드 검토, 직접 카카오 로그인
- 커밋 메시지 예: `refactor: 카카오 로그인 회원 처리를 MemberService로 옮김`

### A3. 컨트롤러와 서비스 계층 규칙 고정 (테스트만)
끝: 0ab9356 test: 컨트롤러와 서비스 계층 규칙 추가 (Admin 제외 줄을 빼면 AdminProductController 위반 16건으로 실패하는 것 확인)

- 규칙 A: A2와 같다. `AdminProductController`, `AdminMemberController`는 이름으로 빼고, 테스트 코드의 그 자리에 아래 "옮기지 않은 것" 표를 가리키는 주석을 단다.
  - 실제 코드는 A2의 타입(`org.springframework.data.repository.Repository`) 대신 이름이 `Repository`로 끝나는 클래스로 검사한다. 지금 저장소의 저장소 인터페이스는 모두 이름이 `…Repository`라서 잡는 범위는 같다.
- 규칙 B: 이름이 `Service`로 끝나는 클래스는 `org.springframework.http..`, `org.springframework.web..`, `org.springframework.ui..`에 의존하지 않는다. 404와 `Model`은 컨트롤러가 맡는다.
- 바꾸는 파일: `src/test/java/gift/architecture/LayerArchitectureTest.java`
- 확인 명령: ArchUnit 통과, 전체 테스트 `./gradlew test`
- 커밋 메시지 예: `test: 컨트롤러와 서비스 계층 규칙 추가`

### D1. 문서 정리
끝(D1-a): 7bdff47 docs: 서비스 분리에 맞춰 코드 위치 갱신, 5df265e docs: 포매터 적용으로 밀린 코드 위치 갱신
끝(D1-b): 1cdb264 docs: ArchUnit 명령과 3단계 인수 테스트 범위 반영

- `docs/open-questions.md`, `docs/work/01-project-analysis/order-flow.md`, `AGENTS.md` 표의 `파일:줄`을 서비스 위치로 고친다. 작동 설명은 그대로 둔다.
- `AGENTS.md`와 `docs/work/02-acceptance-test/test-strategy.md`에 ArchUnit 실행 명령을 더한다.
- 파일이 3개를 넘으므로 두 커밋으로 나눈다.
  - D1-a: `docs/open-questions.md`, `docs/work/01-project-analysis/order-flow.md`
  - D1-b: `AGENTS.md`, `docs/work/02-acceptance-test/test-strategy.md`
- 확인 명령: 문서의 `파일:줄`을 코드와 대조
- 커밋 메시지 예: `docs: 서비스 분리에 맞춰 코드 위치 갱신`

---

## 옮기지 않은 것

| 컨트롤러 | 이유 | 다시 정할 때 |
|---|---|---|
| `AdminProductController` | 관리자 화면은 HTML과 302를 검사해야 해서 안전망을 만들기 어렵다. 안전망 없이 옮기면 응답이 그대로인지 확인할 수 없다 | 4단계 이후 |
| `AdminMemberController` | 위와 같다 | 4단계 이후 |

ArchUnit 규칙 A는 이 두 컨트롤러를 빼고 건다. 옮기게 되면 규칙에서 뺀 부분도 함께 지운다.

## 작동 변경 후보

서비스로 옮기다 보면 함께 고치고 싶어지지만, 모두 지금 작동을 바꾼다. 어느 것을 할지는 사람이 정한다. 하기로 하면 해당 인수 테스트의 기대값을 먼저 바꾼다.

| 후보 | 바뀌는 작동 | 깨지는 인수 테스트 | open-questions |
|---|---|---|---|
| 위시 정리 구현 | 주문한 상품이 위시에서 빠진다. 구현하면 WishRepository를 다시 주입해야 한다(04-unused-code U1에서 지움) | 없음 | 8번 |
| 재고·포인트 차감, 주문 저장을 `@Transactional`로 묶기 | 포인트가 부족하면 재고도 그대로 남는다 | 16-2 | 3번 |
| 포인트 부족, 재고 부족을 400으로(`@ExceptionHandler`나 `@ControllerAdvice`) | 500이 400이 된다 | 16-2, 16-3 | 3번, 12번 |
| `orElse(null)`과 404를 예외와 공통 처리기로 바꾸기 | 본문 없는 404에 본문이 생긴다 | L1의 404, 옵션·상품 시나리오 | 없음 |
| 관리자 화면에서 없는 회원·상품을 404로 | 500이 404가 된다 | 없음(관리자 화면 안전망 없음) | 21번 |
| 관리자 화면에서 포인트 0 이하 충전을 오류 화면으로 | 500이 바뀐다 | 없음(관리자 화면 안전망 없음) | 19번 |
| 주문 알림 실패를 로그로 남기기 | 실패가 기록된다(응답은 같음) | 없음 | 10번 |
| 주문 금액을 옵션 기준으로 계산 | 주문 금액이 바뀔 수 있다 | 16-1 | 2번 |
| 주문 알림 받는 사람 고르기 | 알림이 본인이 아닌 사람에게 간다 | 없음 | 14번 |
| 로그인 토큰 헤더가 없을 때 401로 응답 | 400이 401이 된다 | 17-4, L1의 헤더 없음 시나리오 | 없음(아직 항목이 아님) |
| 오류 메시지 언어 통일 | 응답 본문 문구가 바뀐다 | 1-2 | 6번 |
| API와 관리자 화면에 인증 붙이기 | 로그인 토큰 없이는 실패한다 | 7-1~7-3 | 1, 11, 13, 15~18, 22번 |
| 위시·주문이 있는 상품 삭제를 500 대신 다른 응답으로(예: 409) 또는 위시도 함께 지우기 | 500이 바뀐다 | 9-3 | 24번 |
| 주문이 있는 옵션 삭제를 500 대신 다른 응답으로 | 500이 바뀐다 | 12-7 | 25번 |
| 같은 이름의 카테고리 등록을 500 대신 400으로(옵션명 중복처럼 미리 확인) | 500이 400이 된다 | 10-7 | 26번 |
| 상품이 있는 카테고리 삭제를 500 대신 다른 응답으로(예: 409) | 500이 바뀐다 | 10-5 | 27번 |
| 없는 카테고리 삭제를 204 대신 404로 | 204가 404가 된다 | 10-6 | 28번 |
