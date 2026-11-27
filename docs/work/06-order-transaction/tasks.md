# 주문 트랜잭션 작업 목록

[`tech-spec.md`](tech-spec.md)를 바탕으로 한 작업 목록이다. 주문하기의 재고 차감, 포인트 차감, 주문 저장을 한 트랜잭션으로 묶는다. 작동 변경이다.

- 작성일: 2026-11-27
- 기준 브랜치: `M32k`
- 용어는 [`docs/glossary.md`](../../glossary.md)를 따른다.

## 공통 규칙

- 한 항목 = 커밋 하나. 단, T1은 테스트가 실패한 상태라 따로 커밋하지 않고 T2와 함께 커밋한다.
- 확인 명령은 사용자가 실행한다. 결과가 너무 빨리 나오면 `--rerun-tasks`를 붙인다.
  - 인수 테스트: `./gradlew test --tests 'gift.acceptance.CucumberTest'`
  - ArchUnit: `./gradlew test --tests 'gift.architecture.LayerArchitectureTest'`
- git 명령은 사용자가 실행한다.
- `@Transactional`은 `org.springframework.transaction.annotation.Transactional`을 쓴다. 클래스가 아니라 `OrderService.createOrder` 메서드에만 붙인다. `findOrders`는 건드리지 않는다.
- 응답 코드는 바꾸지 않는다. `@ExceptionHandler`를 더하지 않는다.

---

### T1. 16-2를 바뀐 작동으로 고친다 (테스트만, 커밋하지 않음)
끝: 69a45bd fix: 포인트가 부족한 주문에서 재고도 되돌림 (ADR 0001) (T2와 함께 커밋)

- `order.feature` 16-2
  - 시나리오 이름: `16-2. 포인트가 부족하면 500이 오지만 재고는 줄어든 채 남는다 (open-questions 3번)` → `16-2. 포인트가 부족하면 500이 오고 아무것도 바뀌지 않는다 (open-questions 3번)`
  - `And 상품 1의 옵션 1 재고는 9다` → `And 상품 1의 옵션 1 재고는 그대로 10이다`
- 단계 정의는 바꾸지 않는다(`OrderSteps.java:24`가 "그대로"를 받는다).
- 코드는 바꾸지 않는다. 인수 테스트를 돌려 16-2가 재고 단계(기대 10, 실제 9)에서 실패하는지 본다. 다른 시나리오는 모두 통과해야 한다.
- 실패 출력을 아래 "T1 실패 출력"에 붙인다.
- 바꾸는 파일: `src/test/resources/features/order.feature`, `docs/work/06-order-transaction/tasks.md`
- 확인 명령: 인수 테스트
- 커밋: 하지 않는다. T2와 함께 커밋한다.

#### T1 실패 출력

- 날짜: 2026-11-27, 코드(`OrderService`)를 바꾸기 전
- 명령: `./gradlew test --tests 'gift.acceptance.CucumberTest' --rerun-tasks`

```
CucumberTest > Cucumber > 16. 회원은 포인트로 상품 옵션을 주문한다 > gift.acceptance.CucumberTest.16-2. 포인트가 부족하면 500이 오고 아무것도 바뀌지 않는다 (open-questions 3번) FAILED
    org.opentest4j.AssertionFailedError at OrderSteps.java:34

68 tests completed, 1 failed
```

테스트 결과 파일(`build/test-results/test/TEST-gift.acceptance.CucumberTest.xml`)의 실패 메시지:

```
org.opentest4j.AssertionFailedError: [상품 1의 옵션 1 재고]
expected: 10
 but was: 9
    at gift.acceptance.steps.OrderSteps.stockIs(OrderSteps.java:34)
```

해석: 16-2만 재고 단계에서 실패했다. 500 응답은 통과했고, 재고를 다시 조회하는 단계가 지금 작동(재고가 줄어든 채 남음)을 잡았다. 나머지 67개는 통과.

### T2. createOrder에 @Transactional을 붙여 16-2를 통과시킨다
끝: 69a45bd fix: 포인트가 부족한 주문에서 재고도 되돌림 (ADR 0001) (인수 테스트와 ArchUnit 71개 통과)

- `OrderService.createOrder`에 `@Transactional`을 붙인다. 바로 위에 주석 `// ADR 0001: 재고·포인트·주문 저장을 한 트랜잭션으로 묶는다. 응답 코드는 500 그대로.`를 단다.
- 주문 알림 호출과 `catch (Exception ignored)`는 그대로 둔다(07에서 바꾼다).
- 같은 커밋에 `docs/adr/0001-order-transaction-boundary.md`를 넣는다. 내용은 아래 "ADR 0001 내용"을 따른다.
- 통과해야 할 것: 16-1~16-7, 17-1~17-4, ArchUnit.
- 바꾸는 파일: `src/main/java/gift/order/OrderService.java`, `docs/adr/0001-order-transaction-boundary.md`(새 파일). T1의 `order.feature`, `tasks.md`도 함께 커밋한다(4개).
- 확인 명령: 인수 테스트, ArchUnit
- 커밋 메시지 예: `fix: 포인트가 부족한 주문에서 재고도 되돌림 (ADR 0001)`

#### ADR 0001 내용

- 제목: `0001. 주문의 재고·포인트·주문 저장을 한 트랜잭션으로 묶고 응답 코드는 500으로 유지한다`
- 맥락: 재고 차감, 포인트 차감, 주문 저장이 따로 커밋돼 포인트가 부족하면 재고가 줄어든 채 남았다(open-questions 3번). 재고 부족은 저장 전에 실패해 상태가 남지 않았다(12번).
- 결정: `OrderService.createOrder` 메서드에 `@Transactional`(옵션 확인부터 주문 알림 호출까지). 포인트 부족, 재고 부족 모두 응답 코드는 500 그대로.
- 고려한 선택지:

  | 선택지 | 고르지 않은 이유 |
  |---|---|
  | 포인트 부족, 재고 부족을 400으로 바꾸기 | 응답 코드까지 바꾸면 한 커밋에 작동 변경이 둘이 된다. 클라이언트가 보는 계약이 바뀌므로 따로 정한다 |
  | `@Transactional` 없이, 포인트 차감이 실패하면 재고를 직접 되돌리는 보상 코드 | 단계가 늘 때마다 되돌릴 코드도 늘고, 되돌리는 중에 실패하면 상태가 어긋난다 |

- 결과: 포인트가 부족한 주문은 500, 재고·포인트·주문 내역 모두 그대로(16-2). 생기는 문제:
  - 서버 오류와 주문 거절(포인트 부족, 재고 부족)을 응답 코드로 구분할 수 없다. 둘 다 500이다.
  - 주문 알림 호출이 07(아웃박스) 전까지 트랜잭션 안에 있다. 카카오 응답을 기다리는 동안 DB 연결을 잡고 있고, 커밋도 늦어진다.
  - 알림 예외는 지금처럼 잡혀서 롤백되지 않는다.

### T3. 문서 정리
끝: 6c540f4 docs: 주문 트랜잭션 결과를 문서에 반영 (ADR 0001)

- T2 뒤 같은 요청을 curl로 다시 보내 500, 재고 10 그대로를 확인한다(사용자가 실행). 앱을 새로 켠 초기 데이터에서 보낸다.

  ```
  # user2 로그인 토큰
  curl -s -X POST localhost:8080/api/members/login \
    -H 'Content-Type: application/json' \
    -d '{"email":"user2@example.com","password":"password2"}'

  # 맥북 옵션 1을 주문 수량 1로 주문 → 500
  curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:8080/api/orders \
    -H 'Authorization: Bearer <로그인 토큰>' -H 'Content-Type: application/json' \
    -d '{"optionId":1,"quantity":1}'

  # 옵션 1 재고 → 10
  curl -s localhost:8080/api/products/1/options
  ```

- `docs/open-questions.md` 3번: 상태를 `4단계에서 고침, ADR 0001`로 바꾼다. "인수 테스트" 줄의 `(500, 재고는 줄어든 채 남음)`을 `(500, 재고·포인트·주문 내역 그대로)`로 바꾼다.
- `docs/user-stories.md` 16번 "확인한 응답"
  - `포인트 부족: 500, 재고는 줄어듦(open-questions 3번)` → `포인트 부족: 500, 상태 그대로(open-questions 3번, ADR 0001)`
  - `포인트 3,000,000인 user2가 맥북 1개 주문: 500, 재고는 10에서 9로 줄어듦 (초기 데이터에서 직접 확인)` 줄은 지우지 않고 끝에 `(4단계 전, ADR 0001로 바뀜)`을 붙인다.
  - 그 바로 아래에 위 curl 결과 줄을 더한다. 예: `4단계 뒤 같은 요청: 500, 재고 10 그대로 (초기 데이터에서 직접 확인)`
- `docs/work/02-acceptance-test/test-strategy.md`: 16-2를 고정했던 이력은 남기고 "4단계에서 ADR 0001로 바뀜(재고도 그대로)"을 더한다.
  - 2장 표의 16-2 줄("실패했는데 재고가 줄어든 채 남는다 …")
  - 7장 표의 "버그처럼 보이는 작동" 줄("고치지 않고 지금 작동으로 고정 (16-2)")
- 이 파일 T1, T2, T3에 끝 표시를 한다(05-service-layer tasks.md에는 하지 않는다).
- 바꾸는 파일: `docs/open-questions.md`, `docs/user-stories.md`, `docs/work/02-acceptance-test/test-strategy.md`, `docs/work/06-order-transaction/tasks.md`, `AGENTS.md`(T2로 밀린 줄 번호)
- 확인 명령: 위 curl(사용자가 실행)
- 커밋 메시지 예: `docs: 주문 트랜잭션 결과를 문서에 반영`
