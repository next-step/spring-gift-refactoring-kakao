## prompt

### 계획 1
- /init
- @CLAUDE.md 한글로 작성해줘.
- (프로그래밍 요구 사항, 힌트 복사 후) @CLAUDE.md 에 이 내용 반영해서 정리해줘.
- ktlintFormat는 코틀린 포매터인것 같은데, 우리는 자바로 진행할 거야. 자바에 맞게 수정해줘.

### 계획 2
- 이걸 참고해서 기능 요구 사항을 @README.md 에 작성해줘.
  1. 스타일 정리, 2. 불필요한 코드 제거(작동 변경 없음), 3. 서비스 계층 추출(구조 변경, 작동 변경 없음) 이걸 각 단계로 나누어 3단계로 작성해줘.

### 계획 3
- 1단계 스타일 정리를 하려고 해. claude skill을 만들어줘.
  https://code.claude.com/docs/ko/skills#claude-skills 이거 참고해줘.
- 2단계 불필요한 코드 제거를 하려고 해. claude skill을 만들어줘.
- 3단계 서비스 계층 추출을 하려고 해. claude skill을 만들어줘.

### 계획 4
- TDD 루프를 유지한다.
  가능하면 Red, Green, Refactor 순서로 진행한다.
  최소 요구 사항은 "변경 후 전체 테스트 통과"다.
  이걸 지키기 위한 테스트 코드를 작성하는 스킬을 만들어줘.

### 계획 5
- /write-test TDD 방식으로 테스트 코드 작성해줘.
- 통합테스트는 MockMVC 대신에 RestAssured를 이용해서 Given/When/Then으로 하고, 초기 데이터 셋업은 Repository 대신에 SQL로 해주도록 기존에 있는 write-test스킬을 수정해줘.
- /write-test 기존 통합 테스트도 RestAssured + SQL 방식으로 다시 작성해줘.

### 계획 6
- /style-fix 1단계 스타일 정리 진행해줘.
- 6번에서 삭제한 주석 다시 살려줘.
- /dead-code 2단계 불필요한 코드 제거 진행해줘.
- /extract-service 3단계 서비스 계층 추출 진행해줘.
