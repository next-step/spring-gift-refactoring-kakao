# 미사용 코드 정리 작업 목록

IDE나 컴파일러가 "미사용"으로 보는 코드를 지우는 작업이다. 모두 구조 변경이다.

- 작성일: 2026-11-26
- 기준 브랜치: `M32k`
- 용어는 [`docs/glossary.md`](../../glossary.md)를 따른다.

## 공통 규칙

- 한 항목 = 커밋 하나. 바뀌는 파일이 3개를 넘으면 더 쪼갠다.
- 확인 명령은 사용자가 실행한다. 결과가 너무 빨리 나오면 `--rerun-tasks`를 붙인다.
  - 인수 테스트: `./gradlew test --tests 'gift.acceptance.CucumberTest'`
- 작동이 바뀔 수 있는 일은 목록에 넣지 않고, 맨 아래 "작동 변경 후보" 표에 적는다. 4단계에서 다시 연다.
- git 명령은 사용자가 실행한다. 항목마다 `git blame`으로 볼 파일과 줄만 적는다.
- 줄 번호는 이 문서를 쓴 시점(03-style의 S2 전) 기준이다. 작업할 때 다시 확인한다.

후보는 IntelliJ "Inspect Code > Unused declaration"과 grep으로 모았다. 미사용 import는 03-style의 S2(`removeUnusedImports()`)에서 지우므로 이 목록에는 없다.

---

## 작업 목록

### U1. OrderController의 WishRepository 지우기

- 지우는 것: import(`OrderController.java:8`), 필드(`:26`), 생성자 매개변수(`:34`), 대입(`:41`)
- 지워도 되는 근거: 선언, 매개변수, 대입에만 나오고 쓰는 곳이 없다(open-questions 9번). 주석 `// 6. cleanup wish`(`:67`)에 쓰려던 것으로 보이지만 구현되지 않았다(8번).
- 4단계와 충돌하는지: 충돌한다. 위시 정리를 구현하면 다시 주입해야 한다. 그래서 [`05-service-layer/tasks.md`](../05-service-layer/tasks.md)의 "작동 변경 후보"에 적어 두었다.
- 주석 `// 6. cleanup wish`는 지우지 않는다. 05에서 서비스로 함께 옮긴다.
- 같은 커밋에서 `docs/open-questions.md` 9번의 상태 줄을 다음으로 고친다. 번호는 그대로 둔다.
  - `- **상태**: 3단계에서 지움, 근거는 05-service-layer/tasks.md 작동 변경 후보`
- blame: `src/main/java/gift/order/OrderController.java:8,26,34,41,61-68`
- blame 결과: 다섯 줄(import, 필드, 매개변수, 대입, 주석 `// 6. cleanup wish`) 모두 `55ca9e4` 'feat: set up the project'에서 함께 들어왔다. 프로젝트 초기 세팅 커밋(파일 62개)이고 본문이 없어 넣은 이유는 알 수 없다. 같은 커밋의 `// 6. cleanup wish` 주석만이 단서다.
- 바꾸는 파일: `src/main/java/gift/order/OrderController.java`, `docs/open-questions.md`
- 확인 명령: 인수 테스트(16, 17)
- 커밋 메시지 예: `refactor: OrderController에서 쓰지 않는 WishRepository 제거`

### U2. `Product.getOptions()` 지우기

- 지워도 되는 근거: Java 코드에서도 템플릿(`product/*.html`)에서도 쓰지 않는다. `Product`는 JSON으로 바로 나가지 않는다(API는 `ProductResponse`를 쓴다).
- 주의: 필드 `options`(`Product.java:30-31`)는 남긴다. `cascade = ALL, orphanRemoval = true`가 상품을 지울 때 옵션도 지우기 때문이다. 필드를 지우는 일은 "작동 변경 후보"에 있다.
- 주석, TODO: 없음
- 4단계와 충돌하는지: 상품 상세에 옵션을 함께 보여 주게 되면 다시 필요하다. 다시 만들기는 쉽다.
- blame: `src/main/java/gift/product/Product.java:30-31,70-72`
- blame 결과: 필드 `options`와 `getOptions()` 모두 `55ca9e4` 'feat: set up the project'에서 들어왔다. 넣은 이유는 커밋에 없다.
- 바꾸는 파일: `src/main/java/gift/product/Product.java`
- 확인 명령: 인수 테스트(7-1)
- 커밋 메시지 예: `refactor: 쓰지 않는 Product.getOptions 제거`

### U3. `Order.getMemberId()` 지우기

- 지워도 되는 근거: 부르는 곳이 없다(쓰이는 것은 `Wish.getMemberId`다). `Order`는 JSON으로 바로 나가지 않는다(`OrderResponse`를 쓴다). 필드 `memberId`는 DB 열이라 남긴다.
- 주석: `// primitive FK`(`Order.java:24`). 필드에 붙은 주석이라 그대로 둔다.
- 4단계와 충돌하는지: 주문 상세에서 소유자를 확인하게 되면 다시 필요하다.
- blame: `src/main/java/gift/order/Order.java:24-25,49-51`
- blame 결과: 필드 `memberId`와 `getMemberId()` 모두 `55ca9e4` 'feat: set up the project'에서 들어왔다. 넣은 이유는 커밋에 없다. `OrderRepository.findByMemberId`는 필드 이름으로 조회하므로 getter와 상관없다.
- 바꾸는 파일: `src/main/java/gift/order/Order.java`
- 확인 명령: 인수 테스트(17-1)
- 커밋 메시지 예: `refactor: 쓰지 않는 Order.getMemberId 제거`

### U4. 생성자가 하나뿐인 클래스의 `@Autowired` 지우기 (auth)

- 지워도 되는 근거: 생성자가 하나뿐이면 Spring이 `@Autowired` 없이도 주입한다. 함께 쓰지 않게 되는 `Autowired` import도 지운다.
- 대상: `AuthenticationResolver.java:19`, `JwtProvider.java:23`
- 주석, TODO: 없음. 4단계와 충돌: 없음.
- blame: `src/main/java/gift/auth/AuthenticationResolver.java:19`, `src/main/java/gift/auth/JwtProvider.java:23`
- blame 결과: 두 파일의 `@Autowired`와 import 모두 `55ca9e4` 'feat: set up the project'에서 들어왔다. 넣은 이유는 커밋에 없다.
- 바꾸는 파일: `src/main/java/gift/auth/AuthenticationResolver.java`, `src/main/java/gift/auth/JwtProvider.java`
- 확인 명령: 인수 테스트 전체(로그인 토큰을 쓰는 시나리오 모두)
- 커밋 메시지 예: `refactor: auth 패키지에서 불필요한 @Autowired 제거`

### U5. 생성자가 하나뿐인 클래스의 `@Autowired` 지우기 (member)

- 대상: `MemberController.java:27`, `AdminMemberController.java:23`. 근거는 U4와 같다.
- `AdminMemberController`에는 인수 테스트가 없다. 인수 테스트가 돌면 앱이 떴다는 뜻이고, 앱이 떴으면 주입도 된 것이다.
- blame: `src/main/java/gift/member/MemberController.java:27`, `src/main/java/gift/member/AdminMemberController.java:23`
- blame 결과: 두 파일의 `@Autowired`와 import 모두 `55ca9e4` 'feat: set up the project'에서 들어왔다. 넣은 이유는 커밋에 없다. 이 커밋으로 `src/main`에 `@Autowired`가 남지 않는다.
- 바꾸는 파일: `src/main/java/gift/member/MemberController.java`, `src/main/java/gift/member/AdminMemberController.java`
- 확인 명령: 인수 테스트(1-1~1-3)
- 커밋 메시지 예: `refactor: member 패키지에서 불필요한 @Autowired 제거`

---

## IDE는 미사용으로 보지만 지우면 안 되는 것

| 코드 | 실제로 쓰는 곳 |
|---|---|
| `Member.getPoint()`, `getPassword()`, `getEmail()` | Thymeleaf `member/list.html:27`, `member/edit.html:15,19,23` |
| `Product`, `Category` getter | Thymeleaf `product/*.html` |
| 엔티티의 `protected` 기본 생성자 | JPA |
| 요청·응답 record 구성 요소 | Jackson |
| Repository 인터페이스 메서드 | Spring Data가 구현을 만든다 |
| `KakaoLoginProperties` | `@ConfigurationProperties` |
| `Product.options` 필드 | 상품을 지울 때 옵션도 지우는 cascade |

## 요청 밖이라 묻고 정할 것 (이번 목록에 넣지 않음)

- Kotlin 플러그인, ktlint, `src/main/kotlin`, `src/test/kotlin`의 `.gitkeep`: Kotlin 코드가 없다. 지우면 빌드 설정이 바뀐다.
- `src/test/java/gift/.gitkeep`: 이제 그 폴더에 파일이 있다.
- `src/main/.omc/`, `src/main/java/gift/.omc/`: 플러그인 상태 파일이다. 저장소에 들어 있는지 `git ls-files src/main/.omc src/main/java/gift/.omc`로 확인한다.

## 작동 변경 후보

| 후보 | 바뀌는 작동 | 깨지는 인수 테스트 | open-questions |
|---|---|---|---|
| `Product.options` 필드(cascade) 지우기 | 옵션이 있는 상품을 지우면 FK 오류가 나거나 옵션이 남는다 | 없음(상품 삭제 시나리오가 없음) | 없음 |
| `catch (Exception ignored)`에 로그 남기기 | 실패가 기록된다 | 없음 | 10번 |
| `JWT_SECRET` 기본값 지우기 | 환경 변수가 없으면 앱이 뜨지 않는다 | 테스트 설정에 따라 다름 | 23번 |
