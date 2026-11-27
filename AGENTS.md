# AGENTS.md

이 저장소에서 일하는 AI 에이전트를 위한 안내다. 저장소를 읽어서 알 수 없는 것과 다시 찾는 데 오래 걸리는 것만 적는다.

## 작업을 확인하는 방법

- `./gradlew build`: 컴파일하고 인수 테스트도 함께 실행한다.
- `./gradlew bootRun`: `localhost:8080`에 서버가 뜬다.
- 요청으로 확인하는 일은 사용자가 한다. `http/*.http` 파일이나 curl을 쓴다. AI는 보낼 요청을 적어 줄 수만 있고 실행하지 않는다.
- 인수 테스트: `./gradlew test --tests 'gift.acceptance.CucumberTest'`. 결과 보고서는 `build/reports/tests/test/index.html`에 생긴다. 시나리오마다 초기 데이터로 되돌린 뒤 실행한다.
- ArchUnit: `./gradlew test --tests 'gift.architecture.LayerArchitectureTest'`. 규칙 0(Repository는 Controller에 의존하지 않는다), 규칙 A(컨트롤러는 Repository에 의존하지 않는다. 관리자 컨트롤러 둘은 뺀다), 규칙 B(Service는 http, web, ui 패키지에 의존하지 않는다)를 검사한다.

## 코드가 오해를 부르는 곳

같은 코드 이름이 서로 다른 뜻으로 쓰이거나, 용어 사전과 다르게 불린다. 문서와 대화에는 오른쪽의 용어 사전 이름을 쓴다.

| 코드 | 실제 뜻 | 위치 |
|---|---|---|
| `quantity` | `Option.quantity`는 **재고**이고, `OrderRequest.quantity`와 `Order.quantity`는 **주문 수량**이다. `subtractStock`은 재고에서 주문 수량을 뺀다. | `option/Option.java:28`, `order/OrderRequest.java:6`, `order/Order.java:25`, `order/OrderService.java:57` |
| `token`, `accessToken` | `TokenResponse.token`은 **로그인 토큰**이다(응답 JSON 키). `KakaoLoginClient.KakaoTokenResponse.accessToken`은 **카카오 액세스 토큰**이다(카카오 응답). | `auth/TokenResponse.java:9`, `auth/KakaoLoginClient.java:47` |
| `message` | `Order.message`와 `OrderRequest.message`는 **주문 메시지**다. `KakaoMessageClient`(클래스 이름)는 **주문 알림**을 보낸다. `e.getMessage()`는 오류 메시지다. | `order/Order.java:26`, `order/OrderRequest.java:6`, `order/KakaoMessageClient.java:8` |
| `admin`, `Admin*Controller` | 이름만 관리자용이다. `/admin/**`에는 권한 확인이 없다. 초기 데이터의 `admin@example.com`도 일반 회원이다. | `member/AdminMemberController.java`, `product/AdminProductController.java` |
| 회원 id와 `user` 번호 | 초기 데이터의 `user1@example.com`은 회원 id 2이고, `user2@example.com`은 회원 id 3이다. id 1은 admin이다. | `db/migration/V2__Insert_default_data.sql` |
| `// order flow:` 주석 | `OrderService`의 흐름 주석 "1. auth check …"는 컨트롤러에서 그대로 옮겨 온 것이라, 1단계 인증 확인은 `OrderController`에 있다. "7. send kakao notification"도 이제 알림을 같은 트랜잭션에 저장하는 것까지이고, 실제 발송은 커밋 뒤 `OrderNotificationRelay`가 한다(ADR 0002). 주석 자체는 고치지 않는다. | `order/OrderService.java:39-46`, `order/OrderController.java:40-44` |

## 3단계 작업 규칙 (구조 변경)

- 이 단계의 커밋은 모두 구조 변경이다. 같은 요청에 대한 응답(상태 코드, 응답 본문, 실패 뒤 상태)이 바뀌면 안 된다.
- 구조 변경과 작동 변경을 한 커밋에 섞지 않는다. 둘 다 필요하면 구조를 먼저 바꾼다.
- 인수 테스트를 고치거나 끄지 않는다. 바꿀 때마다 인수 테스트를 돌리고, 통과한 상태로만 커밋한다.
- 코드를 고치기 전에, 지금 할 한 가지가 그 작업의 `docs/work/<번호>-<작업>/tasks.md`에 적혀 있어야 한다. 한 번에 한 항목만 바꾼다.
- 버그나 작동 변경 후보를 찾으면 고치지 않고 tasks.md의 "작동 변경 후보"에 적는다. 4단계(작동 변경)에서 다시 연다.
- 작동을 바꾸기 쉬운 줄은 건드리지 않는다: `@Valid`, `@Transactional`, `@ExceptionHandler`를 더하거나 빼지 않고, `orElse(null)`과 404 응답을 예외로 바꾸지 않으며, 응답 DTO의 필드 이름을 바꾸지 않는다.
- 요청하지 않은 변경은 하지 않는다: Lombok 도입, 패키지 재편, 새 아키텍처 패턴, 라이브러리나 Java 버전 업그레이드. 필요해 보이면 tasks.md에 적고 묻는다.
- 커밋 메시지에 구조 변경임을 밝힌다(`refactor:`, `style:`). 한 줄로 설명할 수 없으면 더 쪼갠다.
- `/acceptance-test` 스킬은 단계와 상관없이 `src/main`을 고치지 않는다.

## 4단계 작업 규칙 (작동 변경)

3단계 규칙을 따르고, 아래만 다르다.

- 작업마다 `docs/work/<번호>-<작업>/tech-spec.md`(지금 작동, 바뀔 작동, 바뀌지 않아야 할 작동)를 먼저 쓰고, 그걸 바탕으로 tasks.md를 만든다. 끝난 작업의 계획(tasks.md, tech-spec.md)은 다음 작업에서 읽지 않는다. test-strategy.md, glossary.md, open-questions.md처럼 지금도 쓰는 기준 문서는 읽는다.
- 바뀌지 않아야 할 작동마다 그것을 지키는 인수 테스트를 붙인다.
- 작동의 증거는 상태를 다시 조회해 확인한다. 응답 코드만 보거나, 메서드를 불렀는지(verify)만 보는 테스트는 증거로 치지 않는다.
- 작동 변경 커밋은 "고친 테스트 + 바꾼 코드"를 함께 담는다. 커밋 전에, 고친 테스트가 코드를 바꾸기 전에 실패한 출력을 tasks.md에 남긴다.
- 작동 변경 커밋 메시지는 `feat:` 또는 `fix:`이고, 구조 변경은 계속 `refactor:`다.
- 3단계 규칙의 "`@Transactional`을 더하지 않는다"는 tech-spec에 적은 작업에서만 풀린다.
- ArchUnit 규칙(`LayerArchitectureTest`)은 고치지 않는다. 규칙을 새로 더하면 일부러 어기는 코드로 실패하는지 확인한다.
- 선택지가 둘 이상이고 트레이드오프가 있던 결정은 `docs/adr/`에 번호를 붙여 ADR로 남긴다. ADR의 "결과"에는 그 결정으로 생기는 문제도 적는다. ADR은 그 결정을 반영한 코드와 같은 커밋에 넣고, 커밋 메시지와 코드 주석에 ADR 번호를 적는다.
- 예외를 삼키는 코드(catch 후 아무것도 안 함)를 새로 만들지 않는다.

## 저장소에 없는 결정

- 현재 동작은 버그처럼 보여도 그대로 둔다. 이상한 동작은 `docs/open-questions.md`에 기록만 하고, 고칠지는 사람이 정한다.
- `docs/open-questions.md`의 항목 번호는 다시 매기지 않는다. 다른 문서가 번호로 항목을 가리킨다.
- 인수 테스트는 HTTP 요청과 응답으로만 검증한다. 상태 코드, 응답 본문, 바뀐 상태(다음 요청으로 확인)까지 본다. 포인트와 주문 알림 상태만 예외로 DB에서 확인한다(둘 다 조회 API가 없다). 이유는 `docs/work/02-acceptance-test/test-strategy.md`에 있다.
- 카카오 키는 `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET` 환경 변수로 넣는다. 저장소에 키를 넣지 않는다.
- PR은 Fork의 `M32k` 브랜치에서 원본 미션 저장소로 보낸다.

## 두 번 이상 고친 것

- AI가 curl을 직접 실행해서 그 결과가 "직접 확인"과 섞였다. 요청 확인은 사용자가 하고, AI는 실행하지 않는다.
- AI가 git 명령을 실행했다. git 명령은 사용자가 터미널에서 직접 한다.
- "토큰", "등록" 같은 표현을 넓게 써서 용어 사전에 맞춰 다시 고쳤다. 로그인 토큰과 카카오 액세스 토큰, 등록과 회원 가입을 구분해 쓴다.
- 연습 브랜치와 작업 브랜치가 섞였다. 작업 브랜치는 `M32k` 하나다. `practice-interface`는 쓰지 않는다.
- 새로 만든 상품의 번호를 미리 정할 뻔했다. 새로 만든 것은 응답에 나온 번호로 다음 요청을 보낸다.
- 인수 조건을 user-stories와 feature 파일 두 곳에 썼다. 인수 조건 본문은 feature 파일에만 쓰고, 수의 근거는 Gherkin 주석으로 남긴다.
- 기대값을 바꿨는데 테스트가 1초 안에 통과했다(Gradle이 이전 결과를 다시 씀). 결과가 너무 빨리 나오면 `--rerun-tasks`로 다시 실행한다.

## 문서 위치

- `docs/glossary.md`: 용어 사전. 문서와 대화에 쓰는 한글명과, 코드에 쓰는 영문명.
- `docs/user-stories.md`: 사용자 스토리. 누가 무엇을 왜 하는지와 확인한 응답.
- `docs/open-questions.md`: 확인 필요 목록. 의도인지 버그인지 결정이 필요한 동작.
- `src/test/resources/features/`: 인수 조건(feature 파일).
- `docs/work/02-acceptance-test/test-strategy.md`: 인수 테스트 전략. 고른 기준, 데이터 초기화, 포인트와 주문 알림 상태를 DB로 확인하는 이유.
- `.claude/skills/acceptance-test/SKILL.md`: 스토리 하나를 인수 테스트로 옮기는 스킬(`/acceptance-test [스토리 번호]`).

## 규칙

- 작업 중 세 문서와 다른 이름이나 작동을 발견하면 사용자에게 묻고, 답에 따라 문서를 고친다.
