---
name: acceptance-test
description: docs/user-stories.md의 스토리 하나에 인수 조건을 정해 Cucumber feature 파일과 단계 정의로 옮긴다. 아직 feature 파일 위치가 적혀 있지 않은 스토리를 인수 테스트로 고정할 때 쓴다.
argument-hint: "[스토리 번호]"
disable-model-invocation: true
---

# 인수 테스트 추가

인풋: `$ARGUMENTS`

## 인풋 처리

- 스토리 번호는 한 번에 하나만 받는다. 여러 개가 오면 하나만 고르라고 묻는다.
- 번호가 없으면 `docs/user-stories.md`에서 feature 파일 위치가 적혀 있지 않은 스토리를 찾는다. 범위가 작은 순서로 2~3개를 추천하고, 사용자가 고른 뒤 시작한다.
  - 범위가 작다는 것은 요청 수와 바뀌는 상태가 적고, 카카오 같은 외부 연동이 필요 없다는 뜻이다.
  - 추천마다 이유를 한 줄 붙인다.

## 순서

1. **읽기**: 다음을 읽는다.
   - 해당 스토리
   - `docs/glossary.md`
   - `src/main/resources/db/migration/V2__Insert_default_data.sql`
   - `docs/open-questions.md`에서 스토리 비고가 가리키는 항목
   - `docs/work/02-acceptance-test/test-strategy.md`
   - `src/test/resources/features/`의 기존 feature 파일
2. **인수 조건**: 인수 조건 초안을 보여 준다. 확인받은 뒤에만 쓴다.
   - feature 파일에 바로 쓴다. 기존 파일에 맞는 Feature가 있으면 그 파일에 추가한다.
   - 수의 근거(주문 금액 계산, 왜 이 수량인지 등)는 단계 문장에 넣지 않고 시나리오 위에 Gherkin 주석으로 남긴다.
   - `docs/user-stories.md`의 해당 스토리 아래에는 위치와 시나리오 번호만 적는다. 예: `인수 조건: src/test/resources/features/product.feature (7-1~7-3)`
3. **확인할 요청**: 사용자가 curl로 확인할 요청을 적어 준다. 직접 실행하지 않는다.
   - 스토리의 "확인한 응답"이나 open-questions에서 확인된 적 없는 시나리오에는 **(확인 안 함)**을 붙인다.
4. **표**: 시나리오, 입력, 기대 응답, 다음 요청으로 확인할 상태를 표로 보여 주고 확인받는다.
5. **단계 정의**
   - 먼저 `src/test/java/gift/acceptance/steps/`에서 재사용할 수 있는 것을 찾는다. 없는 것만 새로 쓴다.
   - 회원 별칭 뒤 조사(이/가, 을/를)는 기존 단계 정의처럼 정규식으로 받는다.
6. **실행 안내**: 실행 명령과 "일부러 실패시켜 볼 기대값 하나"를 알려 준다. 실행은 사용자가 한다.
   - 실행 명령: `./gradlew test --tests 'gift.acceptance.CucumberTest'`
7. **문서 반영 제안**: 바꿀 곳을 알려 준다. 고치는 것은 사용자가 확인한 뒤에 한다.
   - `docs/user-stories.md`의 "확인한 응답"
   - `docs/open-questions.md`
   - `test-strategy.md`

## 이 프로젝트의 기준

- 인수 조건 본문은 feature 파일에만 쓴다. `docs/user-stories.md`에는 feature 파일 위치와 시나리오 번호만 남긴다.
- 현재 작동 확인(curl), 테스트 실행, git 명령은 사용자가 한다. AI는 실행하지 않는다.
- 인수 조건의 수는 초기 데이터(V2)에서 가져온다. 회원 id와 user 번호가 어긋난다. user1은 회원 2번, user2는 회원 3번이다.
- 새로 만든 것(상품, 위시, 주문 등)은 응답에 나온 번호로 다음 요청을 보낸다. 번호를 미리 정하지 않는다.
- 포인트만 DB에서 확인한다(`MemberSteps`). 재고, 주문 내역, 위시, 상품은 API로 확인한다.
- 버그처럼 보여도 고치지 않는다.
  - 현재 작동을 테스트로 고정하고, open-questions에 남긴다.
  - open-questions의 번호는 다시 매기지 않는다. 새 항목은 지금 가장 큰 번호 다음 번호를 쓴다.
- 통과만 믿지 않는다.
  - 기대값 하나를 일부러 틀리게 바꿔 실패하는지 본다.
  - 실행이 너무 빨리 끝나면 Gradle이 이전 결과를 다시 쓴 것일 수 있다. 이때는 `--rerun-tasks`를 붙여 다시 실행한다.
- 시나리오마다 Flyway clean + migrate로 초기 데이터로 되돌린다(`DatabaseHook`). 그래서 각 시나리오의 Given은 초기 데이터 기준으로 쓴다.
- `src/main`은 고치지 않는다. 바꿀 수 있는 것은 `src/test`, 테스트용 설정 파일, 문서뿐이다.
