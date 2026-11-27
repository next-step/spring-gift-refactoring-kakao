# 서식과 이름 정리 작업 목록

Java 포매터를 적용하고, 코드 이름을 용어 사전에 맞추는 작업이다. 모두 구조 변경이다.

- 작성일: 2026-11-26
- 기준 브랜치: `M32k`
- 용어는 [`docs/glossary.md`](../../glossary.md)를 따른다.

## 공통 규칙

- 한 항목 = 커밋 하나. 바뀌는 파일이 3개를 넘으면 더 쪼갠다. 예외는 S2(포매터 적용) 하나다.
- 확인 명령은 사용자가 실행한다. 결과가 너무 빨리 나오면 `--rerun-tasks`를 붙인다.
  - 인수 테스트: `./gradlew test --tests 'gift.acceptance.CucumberTest'`
- 작동이 바뀔 수 있는 일은 목록에 넣지 않고, 맨 아래 "작동 변경 후보" 표에 적는다. 4단계에서 다시 연다.
- git 명령은 사용자가 실행한다.
- 줄 번호는 이 문서를 쓴 시점 기준이다. S2 뒤에는 줄이 바뀌므로 작업할 때 다시 확인한다.

---

## 포매터: Spotless + Palantir Java Format

대상: `src/main/java/**/*.java`, `src/test/java/**/*.java`. Kotlin은 지금처럼 ktlint가 맡는다.

### S1. 포매터 설정 추가 (검사는 끈 채로)

- `build.gradle.kts`에 `id("com.diffplug.spotless") version "8.11.0"` 플러그인을 더한다.
  - `java { target("src/main/java/**/*.java", "src/test/java/**/*.java"); palantirJavaFormat("2.96.0"); removeUnusedImports() }`
  - `isEnforceCheck = false`
- 버전: Spotless 플러그인 8.11.0, Palantir Java Format 2.96.0
- 바꾸는 파일: `build.gradle.kts`, `docs/work/03-style/tasks.md`(버전 기록)
- 확인 명령: `./gradlew build` 통과. `./gradlew spotlessCheck`는 아직 실패하는 게 맞다(고칠 파일 목록이 나온다).
- 커밋 메시지 예: `style: Spotless와 Palantir Java Format 설정 추가`

### S2. 포매터 적용 ⚠️ 파일 3개 규칙의 예외

- `./gradlew spotlessApply`를 실행한 결과만 커밋한다. 손으로 고친 것은 넣지 않는다.
- 미사용 import 정리(`removeUnusedImports()`)도 이 커밋에 들어간다.
- 예외인 이유: 포매터 출력은 기계적인 변경이라 의미가 바뀌지 않는다. 강의에서도 포매터는 맨 처음 따로 커밋하라고 했다. 테스트 코드(`src/test/java`)도 서식만 바뀌고, 시나리오(feature 파일)와 기대값은 그대로다.
- 바꾸는 파일: `src/main/java`, `src/test/java`의 Java 파일 전부(약 55개)
- 확인 방법
  1. `./gradlew spotlessCheck` 통과
  2. `git diff -w --stat`으로 공백을 무시했을 때 남는 변경이 import 정리뿐인지 확인
     - 주의: Palantir가 줄을 합치거나 나누면 `-w`로도 그 줄이 남는다. 남은 파일이 있으면 `git diff --word-diff-regex='[^[:space:]]+'`로 다시 본다. 이렇게 보면 줄바꿈만 바뀐 곳은 사라지고, import 줄만 남아야 한다.
  3. 인수 테스트 통과
- 커밋 메시지 예: `style: Palantir Java Format 적용`

### S3. 빌드에서 포매터 검사 켜기

- `isEnforceCheck = false`를 지운다.
- 바꾸는 파일: `build.gradle.kts`
- 확인 명령: `./gradlew build`(`spotlessCheck` 포함) 통과
- 커밋 메시지 예: `style: 빌드에서 포매터 검사 켬`

---

## 이름을 용어 사전에 맞추기

기준: `docs/glossary.md`, `AGENTS.md`의 "코드가 오해를 부르는 곳". 내부 변수, 매개변수, 메서드 이름, 주석만 바꾼다. 오류 메시지 문구는 바꾸지 않는다.

### N1. 주문 금액 지역 변수

- `OrderController.createOrder`의 `price` → `totalPrice`(`OrderController.java:91-92`)
- 바꾸는 파일: `src/main/java/gift/order/OrderController.java`
- 확인 명령: 인수 테스트(16-1, 16-2)
- 커밋 메시지 예: `refactor: 주문 금액 변수 이름을 totalPrice로 바꿈`

### N2. 재고를 빼는 메서드

- `Option.subtractQuantity(int amount)` → `subtractStock(int orderQuantity)`, 생성자 매개변수 `quantity` → `stock`
- 호출하는 곳: `OrderController.java:87`
- 필드 `quantity`와 `getQuantity()`는 그대로 둔다(아래 "바꾸면 안 되는 이름").
- 바꾸는 파일: `src/main/java/gift/option/Option.java`, `src/main/java/gift/order/OrderController.java`
- 확인 명령: 인수 테스트(16-1~16-3)
- 커밋 메시지 예: `refactor: 재고 차감 메서드 이름을 subtractStock으로 바꿈`

### N3. 카카오 액세스 토큰 매개변수

- `KakaoLoginClient.requestUserInfo(accessToken)`, `KakaoMessageClient.sendToMe(accessToken, …)` → `kakaoAccessToken`
- 바꾸는 파일: `src/main/java/gift/auth/KakaoLoginClient.java`, `src/main/java/gift/order/KakaoMessageClient.java`
- 확인 명령: 인수 테스트, 코드 검토(카카오 연동은 인수 테스트에 없다)
- 커밋 메시지 예: `refactor: 카카오 액세스 토큰 매개변수 이름 정리`

### N4. 로그인 토큰 지역 변수

- `token` → `loginToken` (`MemberController.java:40,53`, `KakaoAuthController.java:66`, `AuthenticationResolver.java:27`)
- 바꾸는 파일: `src/main/java/gift/member/MemberController.java`, `src/main/java/gift/auth/KakaoAuthController.java`, `src/main/java/gift/auth/AuthenticationResolver.java`
- 확인 명령: 인수 테스트(1-1, 17-3)
- 커밋 메시지 예: `refactor: 로그인 토큰 변수 이름을 loginToken으로 바꿈`

### N5. 주문 알림 메서드

- `sendKakaoMessageIfPossible` → `sendOrderNotificationIfPossible`(`OrderController.java:99,104`)
- 바꾸는 파일: `src/main/java/gift/order/OrderController.java`
- 확인 명령: 인수 테스트, 코드 검토
- 커밋 메시지 예: `refactor: 주문 알림 메서드 이름 정리`

### N6. 주문 메시지 지역 변수

- `KakaoMessageClient.buildTemplate`의 `message` → `orderMessageLine`(`KakaoMessageClient.java:33`)
- 바꾸는 파일: `src/main/java/gift/order/KakaoMessageClient.java`
- 확인 명령: 인수 테스트, 코드 검토
- 커밋 메시지 예: `refactor: 주문 메시지 변수 이름 정리`

### N7. 주석의 토큰 용어

- `TokenResponse`의 "JWT access token"과 `KakaoAuthController`의 "issues a service JWT"를 "login token (JWT)"로 바꾼다. 주석은 영어로 둔다.
- `KakaoAuthController`의 "exchanges it for an access token"도 "exchanges it for a Kakao access token"으로 바꿨다. 카카오 액세스 토큰과 로그인 토큰을 구분하려고.
- 바꾸는 파일: `src/main/java/gift/auth/TokenResponse.java`, `src/main/java/gift/auth/KakaoAuthController.java`
- 확인 명령: `./gradlew build`
- 커밋 메시지 예: `style: 주석의 토큰 용어를 로그인 토큰으로 맞춤`

### N8. 문서 반영

- `AGENTS.md` "코드가 오해를 부르는 곳" 표의 이름과 `파일:줄`, `docs/open-questions.md` 10번의 `sendKakaoMessageIfPossible`을 새 이름으로 고친다.
- open-questions 10번 말고도 3, 6, 12번 항목의 옛 이름(`subtractQuantity`, `deductPoint(price)`)을 바꿨다.
- open-questions의 다른 줄 번호는 `05-service-layer`의 마지막 항목(D1)에서 한 번에 고친다.
- 바꾸는 파일: `AGENTS.md`, `docs/open-questions.md`
- 확인 명령: 문서의 `파일:줄`을 코드와 대조
- 커밋 메시지 예: `docs: 바뀐 코드 이름과 위치 반영`

---

## 바꾸면 안 되는 이름 (이번에 두는 이름)

| 이름 | 두는 이유 |
|---|---|
| `OptionResponse.quantity`, `OrderResponse.quantity`, `OrderResponse.message`, `TokenResponse.token`, `WishResponse.*`, `ProductResponse.*`, `CategoryResponse.*` | 응답 DTO 필드. JSON 키가 바뀐다 |
| `OrderRequest.quantity`, `OrderRequest.message`, `OptionRequest.quantity` 등 요청 DTO 필드 | 요청 JSON 키가 바뀌어 지금 요청이 실패한다 |
| `Option.quantity`, `Order.quantity`, `Order.message` (엔티티 필드) | 필드 이름이 곧 DB 열 이름이다. 바꾸려면 `@Column(name=…)`이 필요해서 매핑을 건드린다 |
| `Member.getPoint/getEmail/getPassword/getId`, `Product.get*`, `Category.getName/getId` | Thymeleaf 템플릿이 이름으로 읽는다(`member/list.html:27` 등) |
| `KakaoLoginClient.KakaoTokenResponse.accessToken` | 카카오 응답을 받는 DTO. 이름은 카카오 API를 따른다 |
| `Admin*Controller`, URL, 템플릿의 모델 속성 이름 | 이름은 관리자 화면을 가리키고 URL과 맞다. 권한 확인이 없는 것은 작동 문제다(open-questions 13, 22번) |
| `JwtProvider`, `KakaoMessageClient` (클래스 이름) | 바꿔도 작동은 같지만 이번에는 두었다. 바꾸고 싶으면 항목을 더한다 |

## 작동 변경 후보

| 후보 | 바뀌는 작동 | 깨지는 인수 테스트 | open-questions |
|---|---|---|---|
| 오류 메시지 언어 통일 | 응답 본문 문구가 바뀐다 | 1-2 | 6번 |
| 응답 DTO의 `quantity`를 `stock`으로 바꾸기 | 옵션 목록의 JSON 키가 바뀐다 | 16-1~16-3(재고 확인 단계) | 없음 |
| 엔티티 필드와 DB 열 이름을 `stock`으로 바꾸기 | 마이그레이션이 필요하다 | 없음(스키마가 바뀜) | 없음 |
