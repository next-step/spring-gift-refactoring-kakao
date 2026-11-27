# 주문 알림 아웃박스 기술 명세

카카오 주문 알림을 주문과 같은 트랜잭션에 저장(아웃박스)하고, 커밋 뒤 보내며, 실패하면 다시 보내는 작업이다. 구조 변경을 먼저 하고, 그다음 작동 변경을 한다.

- 작성일: 2026-11-27
- 기준 브랜치: `M32k`
- 용어는 [`docs/glossary.md`](../../glossary.md)를 따른다.
- 관련 확인 필요 목록: [`docs/open-questions.md`](../../open-questions.md) 10번(알림 실패 무시), 14번(본인에게 보냄)
- 관련 문서: [`docs/adr/0001-order-transaction-boundary.md`](../../adr/0001-order-transaction-boundary.md), [`docs/work/02-acceptance-test/test-strategy.md`](../02-acceptance-test/test-strategy.md)

## 정해진 결정

- 주문과 같은 트랜잭션에 "보낼 알림"을 저장한다.
- 발송 시점: 주문이 커밋된 직후 한 번 보내고, 실패한 것만 주기 작업이 다시 보낸다.
- 저장 내용: 주문 시점의 알림 문구(지금 `buildTemplate`이 만드는 것)와 회원 번호. 카카오 액세스 토큰은 저장하지 않고 보낼 때 회원에서 읽는다.
- 재시도: 5번까지. 다섯 번 실패하면 "실패"로 표시하고 더 보내지 않는다. 마지막 오류를 기록한다.
- 카카오 액세스 토큰이 없는 회원은 지금처럼 알림을 만들지 않는다.
- 응답 코드는 바꾸지 않는다. 알림이 실패해도 주문은 201이다.

## 1. 지금 작동

- `OrderService.createOrder`(`OrderService.java:45-67`)는 주문을 저장한 뒤, 같은 트랜잭션(ADR 0001) 안에서 `sendOrderNotificationIfPossible`(`:69-78`)을 부른다.
- 카카오 액세스 토큰이 없으면 보내지 않는다(`:70-72`). 있으면 `KakaoMessageClient.sendToMe`(`KakaoMessageClient.java:16-30`)로 바로 보낸다.
- 알림 문구는 보낼 때 `buildTemplate`(`KakaoMessageClient.java:32-45`)이 만든다.
- 예외는 모두 잡고 무시한다(`OrderService.java:76-77`). 기록도 재시도도 없다(open-questions 10번).
- 카카오 액세스 토큰은 카카오 로그인 콜백에서만 저장된다(`MemberService.java:37-40`). 초기 데이터의 회원에게는 토큰이 없다(`V2__Insert_default_data.sql`).

## 2. 바뀔 작동

| 경우 | 지금 | 바뀐 뒤 |
|---|---|---|
| 토큰 있는 회원의 주문 | 트랜잭션 안에서 바로 보내고, 결과를 남기지 않는다 | 주문과 같은 트랜잭션에 알림을 "보낼 것"으로 저장한다. 커밋 직후 보내고, "보냄"과 보낸 시각을 기록한다 |
| 발송 실패 | 예외를 무시하고 다시 보내지 않는다 | "보낼 것" 그대로 두고, 시도 횟수 +1과 마지막 오류를 기록한다. 주기 작업이 다시 보낸다 |
| 다섯 번 실패 | (해당 없음) | "실패"로 표시하고 더 보내지 않는다 |
| 알림 문구 | 보낼 때 만든다 | 주문 시점에 만들어 저장한 문구를 보낸다 |
| 발송 시점 | 커밋 전(트랜잭션 안) | 커밋 뒤(트랜잭션 밖) |

## 3. 바뀌지 않아야 할 작동

| 작동 | 지키는 인수 테스트 |
|---|---|
| 정상 주문 201, 응답 본문, 재고·포인트·주문 내역 변화 | `order.feature` 16-1, `order-history.feature` 17-2 |
| 포인트 부족 500, 상태 그대로 | `order.feature` 16-2 |
| 재고 부족 500, 상태 그대로 | `order.feature` 16-3 |
| 없는 옵션 404 / 틀린 로그인 토큰 401 / 헤더 없음 400 / 주문 수량 0 400 | `order.feature` 16-4~16-7 |
| 주문 내역 조회 | `order-history.feature` 17-1, 17-3, 17-4 |
| 알림이 실패해도 주문은 201 | 테스트 없음 → 8장 O-2로 새로 지킨다 |
| 토큰 없는 회원에게는 알림을 만들지 않는다 | 테스트 없음 → 8장 O-4로 새로 지킨다 |
| 알림 문구(내용과 모양) | 테스트 없음 → 8장 O-1로 새로 지킨다(가짜 발송기가 받은 문구 비교) |
| 알림은 주문한 본인에게 간다(open-questions 14번) | 테스트 없음 → 8장 O-1로 새로 지킨다(가짜 발송기가 받은 토큰이 주문한 회원의 것) |
| 201 응답의 `Location` 헤더 | 테스트 없음 |

## 4. 구조 변경과 작동 변경의 순서

구조 변경(`refactor:`)을 먼저 끝낸다. 구조 변경에서는 인수 테스트를 고치지 않는다.

| 순서 | 종류 | 내용 |
|---|---|---|
| R1 | refactor | 알림 문구 만들기를 `KakaoMessageClient.buildTemplate` 밖의 새 클래스(예: `gift.order.OrderNotificationMessage`)로 꺼낸다. `sendToMe`는 다 만든 문구를 받는다 |
| R2 | refactor | 발송을 인터페이스(예: `gift.order.OrderNotificationSender`, `send(kakaoAccessToken, text)`) 뒤로 옮긴다. `KakaoMessageClient`가 이 인터페이스를 구현하고, `OrderService`는 인터페이스에만 의존한다 |
| R3 | refactor | Flyway V3로 아웃박스 테이블을 만든다. 아직 쓰는 코드는 없다. 이 테이블이 있어야 B1의 테스트가 "테이블이 없음"이 아니라 상태 확인 단계에서 실패한다 |
| T1 | test | 테스트 설정에 가짜 발송기를 넣는다. 초기 데이터 회원에게는 토큰이 없어 지금 시나리오에서는 발송이 일어나지 않는다. 지금 68개 시나리오가 그대로 통과해야 한다 |
| B1 | fix/feat | 아웃박스 저장, 커밋 직후 발송, 실패 기록. 기존 `catch (Exception ignored)`를 없앤다. 8장 O-1, O-2(앞부분), O-4, O-5. ADR 0002를 같은 커밋에 넣는다 |
| B2 | feat | 주기 재시도와 5번 제한. 8장 O-2(재시도 부분), O-3 |

## 5. 테이블 설계 초안 (Flyway V3)

```sql
create table order_notification
(
    id            bigint auto_increment primary key,
    order_id      bigint        not null,
    member_id     bigint        not null,
    message       varchar(2000) not null,  -- 주문 시점의 알림 문구(template_object)
    status        varchar(20)   not null,  -- PENDING(보낼 것), SENT(보냄), FAILED(실패)
    attempt_count int           not null default 0,
    last_error    varchar(1000),
    created_at    timestamp     not null,
    sent_at       timestamp,
    foreign key (order_id) references orders (id),
    foreign key (member_id) references member (id)
);
```

- 카카오 액세스 토큰 열은 두지 않는다.
- 이름은 V1의 방식(`orders`, `options`, `snake_case`)을 따른다.
- `message` 길이는 주문 메시지(255) + 상품명(15) + 옵션명(50) + 틀 문자열을 넉넉히 넘게 잡았다.

## 6. 커밋 직후 발송: 선택지 비교

공통으로, `createOrder`는 아웃박스 행을 저장하고 이벤트를 발행한다. 리스너는 `@TransactionalEventListener(phase = AFTER_COMMIT)`이다. 리스너가 "보냄"이나 시도 횟수를 저장하려면 이미 커밋된 트랜잭션에 기댈 수 없다. 그래서 새 트랜잭션(`REQUIRES_NEW`)이 필요하다.

| | A. 같은 스레드 | B. `@Async` 다른 스레드 |
|---|---|---|
| 응답 시간 | 카카오 응답까지 기다린다. 지금(트랜잭션 안에서 보냄)과 비슷하다 | 카카오를 기다리지 않는다 |
| open-in-view와 DB 연결 | open-in-view가 켜져 있으면 요청의 영속성 컨텍스트가 열린 채 카카오를 기다린다. 이때 DB 연결도 잡고 있을 수 있다(확인할 것) | 리스너 스레드에는 요청의 영속성 컨텍스트가 없다. 자기 트랜잭션 동안만 연결을 쓴다 |
| 테스트 | 응답이 오면 발송과 상태 저장이 끝나 있다. 기다리지 않는다 | 응답 뒤에 끝나므로 기다려야 한다. 방법은 둘이다. (1) Awaitility 같은 대기 도구를 쓴다(새 테스트 라이브러리). (2) 테스트 프로필에서 실행기를 동기로 바꾼다(그러면 비동기 경로는 테스트하지 않는다) |
| 그 밖 | 더 할 설정이 없다 | `@EnableAsync`와 실행기 설정이 필요하다. 스레드 풀이 차면 어떻게 할지 정해야 한다 |

**결정: A.** 주문 응답이 카카오 응답을 기다리는 것은 지금과 같다. 테스트가 기다릴 필요가 없고, 새 라이브러리와 스레드 풀 설정이 없다. 응답 시간이 문제가 되면 `@Async`로 바꾸는 새 ADR을 쓴다.

## 7. 주기 재시도

- `@EnableScheduling`(설정 클래스)과 `@Scheduled(fixedDelay = …)`를 쓴다. 간격은 tasks.md에서 정한다.
- 다시 보낼 대상: `status = PENDING`이고, `attempt_count >= 1`이거나 `created_at`이 1분보다 오래된 행.
  - `attempt_count >= 1`: 커밋 직후 발송이 실패한 행이다.
  - `created_at`이 1분보다 오래됨: 커밋 직후 발송 중에 앱이 꺼져 시도 0번으로 남은 행이다. 1분보다 최근의 시도 0번 행은 커밋 직후 발송이 아직 진행 중일 수 있어서 건드리지 않는다.
- 한 번 돌 때 행마다:
  - 성공하면 SENT와 `sent_at`을 기록한다.
  - 실패하면 `attempt_count` +1과 `last_error`를 기록하고, 5가 되면 FAILED로 표시한다.
  - 한 행이 실패해도 다음 행으로 넘어간다.
- 테스트 프로필에서는 스케줄을 끈다. 예를 들어 `application-test.properties`에 끄는 속성을 두고 `@ConditionalOnProperty`로 건다. 시나리오는 재시도 메서드를 단계 정의에서 직접 한 번 부른다.

## 8. 인수 테스트 계획

**상태 확인**
- 알림 상태를 보는 API가 없어서, 포인트처럼 DB를 다시 조회한다: `select status, attempt_count, last_error, sent_at from order_notification where order_id = ?`
- 그러면 AGENTS.md "저장소에 없는 결정"과 test-strategy 5장의 "포인트만 DB" 예외, `MemberSteps`의 "DB를 읽는 유일한 곳" 주석이 넓어진다. 07 tasks.md의 문서 정리 항목에서 함께 고친다.

**가짜 발송기**
- 테스트 설정(`src/test/java/gift/acceptance/`)에서 `OrderNotificationSender`의 가짜를 `@Primary`로 넣는다.
- 받은 (토큰, 문구)를 기록한다.
- 시나리오에서 "다음 N번은 실패"나 "항상 실패"를 정할 수 있게 한다.
- 시나리오마다 초기화한다(`@Before`).

**테스트 회원에게 카카오 액세스 토큰 주기**
- 단계 정의에서 DB로 넣는다: `update member set kakao_access_token = ? where email = ?`
- 카카오 로그인 콜백은 카카오 서버에 기대서 쓰지 않는다.

**시나리오 후보** (`order-notification.feature`. 번호는 tasks.md에서 정한다)

| | 시나리오 | 확인 |
|---|---|---|
| O-1 | 토큰 있는 user1이 주문하면 알림이 "보냄"이 된다 | 201, 알림 1건, SENT, `sent_at` 있음. 가짜 발송기가 받은 문구가 지금 문구와 같고, 받은 토큰이 user1의 것이다 |
| O-2 | 발송이 실패해도 주문은 201이고, 재시도가 성공하면 "보냄"이 된다 | 201, 주문 내역 +1, 알림 PENDING·시도 1·마지막 오류 있음 → 재시도 한 번 → SENT |
| O-3 | 다섯 번 실패하면 "실패"가 되고 더 보내지 않는다 | 재시도 4번 뒤 FAILED·시도 5 → 재시도를 한 번 더 해도 가짜 발송기가 받은 횟수가 그대로다 |
| O-4 | 토큰 없는 회원이 주문하면 알림이 생기지 않는다 | 201, 알림 0건, 가짜 발송기가 받은 횟수 0 |
| O-5 | 포인트가 부족한 주문(500)에서는 알림도 생기지 않는다 | 토큰 있는 user2, 500, 알림 0건, 재고 그대로(ADR 0001 롤백) |

"지금 문구와 같다"의 기대값은 지금 `buildTemplate`의 결과로 정하고, 근거를 시나리오 위 Gherkin 주석으로 남긴다.

## 9. 고칠 테스트와 먼저 실패할 곳

R1~R3과 T1을 마친 상태에서, 코드를 바꾸기 전에 돌렸을 때:

| 시나리오 | 바꾸기 전 결과 | 실패하는 단계 |
|---|---|---|
| O-1 | 실패 | "알림은 1건이다"(실제 0건). 지금 코드도 가짜 발송기로 바로 보내므로 문구 비교 단계는 통과할 수 있다 |
| O-2 | 실패 | "알림은 1건이다"(실제 0건). 201은 통과한다 |
| O-3 | 실패 | 재시도 메서드가 아직 없으면 단계 정의가 컴파일되지 않는다. 그래서 B2에서 빈 재시도 메서드를 먼저 두고, 상태 확인 단계에서 실패하는 출력을 받는다 |
| O-4 | 통과 | 바뀌지 않아야 할 작동을 지키는 시나리오라 처음부터 통과한다 |
| O-5 | 통과 | 위와 같다 |

실패 출력은 tasks.md에 남긴다(4단계 규칙).

## 10. ArchUnit

규칙 B는 이름이 `Service`로 끝나는 클래스가 `org.springframework.http..`, `web..`, `ui..`에 직접 의존하지 못하게 한다(`LayerArchitectureTest.java:38-44`). 지키는 방법은 다음과 같다.
- `RestClient`(`org.springframework.web.client`)를 쓰는 곳은 `KakaoMessageClient` 하나로 둔다. 이 클래스 이름은 `…Service`로 짓지 않는다.
- 새 클래스 이름은 `…Service`로 끝나지 않게 한다. 예: `OrderNotificationSender`, `OrderNotificationRelay`, `OrderNotificationRetryJob`.
- `OrderService`는 인터페이스 `OrderNotificationSender`에만 의존한다.
- 실패를 기록할 때 `RestClientException`(web 패키지)을 잡는 코드를 `…Service`에 두지 않는다. `Exception`으로 받거나, `KakaoMessageClient` 안에서 자기 예외로 바꾼다.

규칙(`LayerArchitectureTest`)은 고치지 않는다.

## 11. 남길 ADR

- `0002. 카카오 주문 알림을 아웃박스에 저장하고 커밋 뒤 보내며 실패하면 5번까지 다시 보낸다` (본문은 아직 쓰지 않는다. B1과 같은 커밋에 넣는다)
- "결과"의 생기는 문제로 넘길 것: 커밋 직후 발송은 요청과 같은 스레드라(6장 결정 A) 주문 응답이 카카오 응답을 기다린다. 응답 시간이 문제가 되면 `@Async`로 바꾸는 새 ADR을 쓴다.
- 이 작업으로 ADR 0001 "결과"의 두 줄이 풀린다.
  - "주문 알림 호출이 07(아웃박스) 전까지 트랜잭션 안에 있다": 발송이 커밋 뒤로 나간다.
  - "알림 예외는 지금처럼 잡혀서 롤백되지 않는다": 예외를 무시하지 않고 시도 횟수와 마지막 오류로 남긴다.

## 12. 알려진 한계

- **같은 알림이 두 번 갈 수 있다.** 카카오에는 보냈는데 "보냄"으로 저장하기 전에 실패하면(앱 종료, DB 오류) 행이 PENDING으로 남고, 재시도가 다시 보낸다. 카카오 "나에게 보내기"에는 중복을 막는 키가 없다.
- **앱이 여러 대면 재시도가 겹친다.** 행을 잠그지 않아서, 두 대가 같은 PENDING 행을 동시에 보낼 수 있다. 지금은 한 대로 운영한다고 본다. 여러 대가 되면 잠금(`select … for update skip locked`) 등을 정해야 한다.
- **카카오가 느리면 주문 응답도 느리다.** 커밋 직후 발송은 요청과 같은 스레드에서 한다(6장 결정 A).
- **커밋 직후 발송과 재시도가 겹치지 않게 하는 방법**: 재시도는 시도 0번 행 가운데 `created_at`이 1분보다 오래된 것만 본다(7장). 커밋 직후 발송이 1분 넘게 걸리면 같은 행을 둘 다 보낼 수 있다.
