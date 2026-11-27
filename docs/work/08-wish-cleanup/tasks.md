# 위시 정리 작업 목록

[`tech-spec.md`](tech-spec.md)를 바탕으로 한 작업 목록이다. 구조 변경(R1, R2, T1)을 먼저 끝내고 작동 변경(B1)을 한다.

- 작성일: 2026-11-27
- 기준 브랜치: `M32k`
- 용어는 [`docs/glossary.md`](../../glossary.md)를 따른다.

## 공통 규칙

- 한 항목 = 커밋 하나. B1은 고친 테스트, 바꾼 코드, 실패 출력(이 파일), ADR 0003을 한 커밋에 담는다.
- 확인 명령은 사용자가 실행한다. 결과가 너무 빨리 나오면 `--rerun-tasks`를 붙인다.
  - 전체: `./gradlew build` (spotless, 인수 테스트, ArchUnit 포함)
  - 인수 테스트: `./gradlew test --tests 'gift.acceptance.CucumberTest'`
  - ArchUnit: `./gradlew test --tests 'gift.architecture.LayerArchitectureTest'`
- git 명령은 사용자가 실행한다.
- 응답 코드는 바꾸지 않는다. 위시 정리가 실패해도 주문은 201이다.
- 새 클래스 이름은 `…Service`로 끝나지 않게 한다(tech-spec 9장).
- 트랜잭션은 B1 리스너의 `REQUIRES_NEW`에서만 연다(tech-spec 4장). R1, R2에서는 `@Transactional`을 붙이지 않는다.

---

### R1. 위시 정리 인터페이스와 구현 (부르는 곳 없음)

- `gift.wish.WishCleaner` 인터페이스: `void clean(Long memberId, Long productId)`
- `gift.wish.RepositoryWishCleaner`(`@Component`): `WishRepository`로 그 회원 + 상품의 위시를 모두 지운다. 하나만 찾거나 `Optional`로 받는 방식은 쓰지 않는다(tech-spec 2장).
- `WishRepository`에 회원 번호 + 상품 번호로 모두 지우는 메서드를 더한다.
- 트랜잭션은 B1 리스너의 REQUIRES_NEW에서 연다(파생 delete는 트랜잭션 필요).
- 아직 부르는 곳은 없다. `OrderService`, 테스트, feature 파일은 건드리지 않는다.
- 바꾸는 파일: `WishCleaner.java`(새 파일), `RepositoryWishCleaner.java`(새 파일), `WishRepository.java`, 이 파일
- 확인 명령: 전체
- 커밋 메시지 예: `refactor: 위시 정리 인터페이스와 저장소 구현 추가`

#### R1 한 일

- `WishRepository`에 파생 delete `long deleteByMemberIdAndProductId(Long memberId, Long productId)`를 더했다.
  - `Wish`는 `memberId` 필드 + `product` 연관이라, 기존 `findByMemberIdAndProductId`처럼 `memberId`와 `product.id`로 찾는다.
  - 반환값(지운 개수)은 쓰지 않는다. 확인은 위시 목록 API로 한다.
- `WishCleaner`(인터페이스, `void clean(Long memberId, Long productId)`)와 `RepositoryWishCleaner`(`@Component`)를 만들었다. `@Transactional`은 붙이지 않았다.
- 부르는 곳은 없다. `OrderService`, 테스트, feature 파일은 그대로다.
- `./gradlew build` 성공(2026-11-27): spotlessCheck 통과. 인수 테스트 75개, ArchUnit 3개, KakaoMessageClientTest 3개 모두 통과.

### R2. 주문마다 OrderPlacedEvent 발행 (받는 곳 없음)

- `gift.order.OrderPlacedEvent(orderId, memberId, productId)`를 만든다.
- `createOrder`가 주문 저장 뒤 이 이벤트를 발행한다. 받는 리스너가 아직 없어서 작동은 같다.
- 바꾸는 파일: `OrderPlacedEvent.java`(새 파일), `OrderService.java`
- 확인 명령: 전체
- 커밋 메시지 예: `refactor: 주문마다 OrderPlacedEvent 발행`

#### R2 한 일

- `gift.order.OrderPlacedEvent(Long orderId, Long memberId, Long productId)` record를 만들었다.
- `OrderService.createOrder`에서 주문 저장 바로 뒤, 주문 알림 저장(`saveOrderNotificationIfPossible`)보다 앞에 `eventPublisher.publishEvent(new OrderPlacedEvent(..))`를 넣었다.
  - 카카오 액세스 토큰과 상관없이 성공한 주문마다 발행된다.
  - 주석은 tech-spec 4장을 가리킨다. ADR 0003은 B1에서 생기므로 적지 않았다.
- 받는 리스너는 없다. `gift.wish`, 테스트, feature 파일은 그대로다. `@Transactional`은 새로 붙이지 않았다.
- 처음 쓴 발행 줄이 121자라 spotlessCheck가 실패했다. 포매터가 보여 준 모양(인자를 다음 줄로)으로 고쳤다.
- `./gradlew build` 성공(2026-11-27): spotlessCheck 통과. 인수 테스트 75개, ArchUnit 3개, KakaoMessageClientTest 3개 모두 통과.

### T1. 위시 정리 감싸개와 단계 정의 (테스트만)

- `src/test/java/gift/acceptance/FailableWishCleaner.java`(`@Component @Primary`): 평소에는 진짜 `RepositoryWishCleaner`에 맡기고, 정해 두면 예외를 던진다. 시나리오마다 초기화한다.
- 단계 정의: "위시 정리가 실패한다". 위시 확인은 기존 `WishSteps`의 `위시 번호는 …다`를 쓴다.
- 시나리오는 아직 더하지 않는다. 지금 시나리오가 그대로 통과해야 한다.
- 바꾸는 파일: `FailableWishCleaner.java`(새 파일), 단계 정의 파일
- 확인 명령: 전체
- 커밋 메시지 예: `test: 위시 정리 감싸개와 단계 정의 추가`

#### T1 한 일

- `src/test/java/gift/acceptance/FailableWishCleaner.java`(`@Component @Primary`, `WishCleaner` 구현)를 만들었다.
  - 진짜 `RepositoryWishCleaner`를 생성자로 받아 평소에는 그대로 맡긴다.
  - `failNext()`로 정해 두면 다음 `clean` 한 번은 맡기지 않고 `IllegalStateException(FAILURE_MESSAGE)`를 던진다. `FAILURE_MESSAGE = "가짜 위시 정리 실패"`.
  - `reset()`이 있다.
- 시나리오마다 초기화하는 방식은 07 `FakeOrderNotificationSender`와 같다. 단계 정의 클래스의 `@Before`가 `reset()`을 부른다.
- 단계 정의는 새 파일 `src/test/java/gift/acceptance/steps/WishCleanupSteps.java`에 두었다. `@Before` 초기화와 `위시 정리가 실패한다`(Given)가 있다.
  - 07 `OrderNotificationSteps`처럼 감싸개를 다루는 단계를 한곳에 모았다.
  - `WishSteps`는 위시 API(목록, 담기, 삭제) 단계만 두어 13~15번 feature가 쓰는 파일을 건드리지 않았다.
  - 위시 확인은 기존 `WishSteps`의 `위시 번호는 …다`를 그대로 쓴다.
- feature 파일과 시나리오는 더하지 않았다. `src/main`은 그대로다.
- `./gradlew build` 성공(2026-11-27): spotlessCheck 통과. 인수 테스트 75개, ArchUnit 3개, KakaoMessageClientTest 3개 모두 통과.

### B1. 주문 커밋 뒤 위시 정리

- 테스트 먼저: `src/test/resources/features/wish-cleanup.feature`(새 파일). 16-15~16-19 = tech-spec 7장 W-1~W-5.
- 코드를 바꾸기 전에 W-1(16-15), W-3(16-17)이 위시 번호 단계에서 실패하는 출력을 "B1 바꾸기 전 실패 출력"에 붙인다. W-2, W-4, W-5는 처음부터 통과한다.
- 코드
  - `gift.wish`에 `OrderPlacedEvent`를 받는 `@TransactionalEventListener(AFTER_COMMIT)` 리스너를 둔다(예: `OrderWishCleanupListener`).
  - `REQUIRES_NEW` 트랜잭션에서 `WishCleaner.clean`을 부른다.
  - 실패는 리스너의 catch가 잡아 warn 로그로 남긴다. 빈 catch는 만들지 않는다.
- 코드를 바꾼 뒤 W-5에서 "위시 정리가 실패한다"를 빼서 위시 단계에서 실패하는지 확인하고, "W-5 일부러 틀린 결과"에 붙인다. 확인한 뒤 되돌린다.
- 코드 주석과 커밋 메시지에 ADR 0003을 적는다. `docs/adr/0003-wish-cleanup-after-order-commit.md`를 같은 커밋에 넣는다.
- 바꾸는 파일: `wish-cleanup.feature`, 이 파일, `OrderWishCleanupListener.java`(새 파일), `OrderWishCleanupExecutor.java`(새 파일), ADR 0003, tech-spec 4장(리스너와 트랜잭션 빈을 나눈 이유)
- 확인 명령: 전체
- 커밋 메시지 예: `feat: 주문 커밋 뒤 주문한 상품을 위시에서 뺌 (ADR 0003)`

#### B1 한 일

- 빈을 둘로 나눴다(07의 `OrderNotificationRelay`/`OrderNotificationDispatcher`와 같은 구조).
  - `gift.wish.OrderWishCleanupListener`: `@TransactionalEventListener(AFTER_COMMIT)`로 `OrderPlacedEvent`를 받는다. 트랜잭션은 없다. `OrderWishCleanupExecutor.cleanUp`을 `try/catch`로 감싸고, 실패하면 warn 로그(주문 번호, 회원 번호, 상품 번호, 예외 메시지)를 남긴다.
  - `gift.wish.OrderWishCleanupExecutor`: `@Transactional(REQUIRES_NEW) cleanUp(event)`가 `WishCleaner.clean(memberId, productId)`를 부른다.
- REQUIRES_NEW를 리스너와 다른 빈에 둔 이유를 정정했다.
  - 처음에는 "리스너 메서드에 붙이면 커밋 때 예외로 주문 응답이 500이 될 수 있다"고 설명했다. 이것은 틀렸다. AFTER_COMMIT 리스너는 afterCompletion에서 불리고, 여기서 난 예외는 Spring이 error 로그만 남기고 삼켜서 응답은 201 그대로다(spring-tx 6.2 `TransactionSynchronizationUtils.invokeAfterCompletion`).
  - 나누는 이유는 둘이다. 커밋 때 나는 예외까지 우리 catch가 잡아 warn 로그를 남기려는 것이고, 같은 빈 안의 호출은 프록시를 거치지 않아 `@Transactional`이 걸리지 않기 때문이다. tech-spec 4장과 ADR 0003 "결정"에 이 이유로 적었다.
- 증거(같은 테스트 출력)
  - 우리 warn 로그가 남았다: `위시 정리 실패: orderId=5, memberId=2, productId=3, message=가짜 위시 정리 실패`
  - Spring의 `afterCompletion threw exception` error 로그는 0건이다. 예외를 Spring이 아니라 우리 catch가 잡았다는 뜻이다.
- `./gradlew build` 성공(2026-11-27): spotlessCheck 통과. 인수 테스트 80개, ArchUnit 3개, KakaoMessageClientTest 3개 모두 통과.

#### B1 바꾸기 전 실패 출력

```
$ ./gradlew test --tests 'gift.acceptance.CucumberTest' --rerun-tasks
80 tests completed, 2 failed

16-15. 위시에 담은 상품을 주문하면 그 위시가 빠진다 FAILED
java.lang.AssertionError:
Expecting actual:
  [1, 2]
to contain exactly in any order:
  [1]
but the following elements were unexpected:
  [2]
    at gift.acceptance.steps.WishSteps.wishIdsAre(WishSteps.java:38)
    at ✽.user1의 위시 번호는 1이다(classpath:features/wish-cleanup.feature:12)

16-17. 다른 회원의 같은 상품 위시는 남는다 FAILED
java.lang.AssertionError:
Expecting actual:
  [1, 2, 5]
to contain exactly in any order:
  [1, 2]
but the following elements were unexpected:
  [5]
    at gift.acceptance.steps.WishSteps.wishIdsAre(WishSteps.java:38)
    at ✽.user1의 위시 번호는 1, 2다(classpath:features/wish-cleanup.feature:31)
```

해석: 리스너가 없어서 위시 2(상품 3), 새 위시 5(상품 5)가 남는다. 16-16, 16-18, 16-19는 통과.

#### W-5 일부러 틀린 결과

```
# 16-19에서 "And 위시 정리가 실패한다" 줄을 빼고 돌림
$ ./gradlew test --tests 'gift.acceptance.CucumberTest' --rerun-tasks
80 tests completed, 1 failed

16-19. 위시 정리가 실패해도 주문은 201이고 위시는 남는다 FAILED
java.lang.AssertionError:
Expecting actual:
  [1]
to contain exactly in any order:
  [1, 2]
but could not find the following elements:
  [2]
    at gift.acceptance.steps.WishSteps.wishIdsAre(WishSteps.java:38)
    at ✽.user1의 위시 번호는 그대로 1, 2다(classpath:features/wish-cleanup.feature:49)
```

해석:
- 실패 설정을 빼면 정리가 정상으로 돌아 위시 2가 빠진다. 16-19는 "실패 설정이 있을 때 위시가 남는다"를 실제로 확인한다. (:49는 한 줄을 뺀 파일 기준)
- 이 출력에는 "위시 정리 실패" warn 로그가 0건이다. B1 빌드의 warn 로그 1건은 실패 설정 때문에 생긴 것이다.

확인한 뒤 feature를 되돌렸고, `./gradlew build`로 80개 통과를 다시 확인했다.

### D. 문서 정리

tech-spec 11장대로 한다.
- `AGENTS.md` "코드가 오해를 부르는 곳"의 `// order flow:` 줄: "6. cleanup wish"가 이제 커밋 뒤 `gift.wish`의 리스너에서 일어난다고 적고 줄 번호를 맞춘다.
- `open-questions.md` 8번: 상태를 "4단계에서 고침, ADR 0003"으로 바꾸고 인수 테스트 줄을 더한다. 06부터 밀린 줄 번호(`:58`, `:61`)를 맞추거나 "4단계 전 코드"로 표시한다.
- `open-questions.md` 9번: 상태 끝에 "위시 정리는 08에서 OrderController가 아닌 gift.wish 리스너로 구현함(ADR 0003)"을 더한다.
- `docs/work/05-service-layer/tasks.md` "작동 변경 후보"의 "위시 정리 구현" 줄: 지우지 않고 끝에 "(08에서 구현, ADR 0003)"을 붙인다.
- `user-stories.md` 16번: 비고와 인수 조건(`wish-cleanup.feature` 16-15~16-19)을 더한다.
- R2로 밀린 `OrderService.java` 줄 번호를 맞춘다.
- 07 OrderNotificationRelay의 catch 주석('주문 응답(201)에는 새어 나가지 않게 한다')의 이유를 ADR 0003 정정에 맞춘다.
- 이 파일에 끝 표시를 한다.
- 바꾸는 파일: `AGENTS.md`, `docs/open-questions.md`, `docs/work/05-service-layer/tasks.md`, `docs/user-stories.md`, `src/main/java/gift/order/OrderNotificationRelay.java`(주석만), 이 파일
- 확인 명령: 없음(문서만)
- 커밋 메시지 예: `docs: 위시 정리 결과를 문서에 반영 (ADR 0003)`

---

## 작동 변경 후보

이번 작업에서는 고치지 않는다. 하기로 하면 해당 테스트의 기대값을 먼저 바꾼다.

| 후보 | 바뀌는 작동 | 깨지는 테스트 | open-questions |
|---|---|---|---|
| (아직 없음) | | | |
