## 맥락

리팩토링 과정에서 Claude Code를 활용하여 코드 분석, 테스트 작성, 서비스 추출, 코드 스멜 수정을 진행했다.
각 단계별 활용 방식과 학습 내용을 기록한다.

## 1단계: 코드 분석

- **활용 방식**: 도메인별 코드 순회, Grep으로 패턴 탐색
- **결과물**: 5개의 분석 문서 (anti-pattern, style, unreferenced, etc, PROJECT_STRUCTURE)

## 2단계: 인수 테스트 작성

- **활용 방식**: TEST_STRATEGY.md 기반으로 RestAssured 인수 테스트 생성
- **수정 내용**:
  - FK 제약조건 순서에 맞게 테스트 데이터 삭제 순서 조정
  - 인증 헤더 누락 시 400 반환 (Spring MissingRequestHeaderException)
  - 옵션 삭제 테스트에서 옵션 개수 체크 로직 고려
- **학습 내용**:
  - Spring의 `@RequestHeader(required=true)`는 헤더 누락 시 400을 반환함 (401이 아님)
  - 테스트 데이터 정리 시 FK 순서 중요 (orders → wishes → options → products → categories → members)

## 3단계: 서비스 계층 추출

- **활용 방식**: Controller의 비즈니스 로직을 Service로 이동, 각 단위별로 테스트 실행 후 커밋
- **수정 내용**:
  - 6개 Service 클래스 생성 (Member, Category, Product, Option, Wish, Order)
  - Controller는 인증/HTTP 처리만 담당하도록 간소화
  - 인증 로직은 Controller에 유지 (AuthenticationResolver 사용)
- **학습 내용**:
  - 구조 변경 시 작동 변경을 섞지 않는 것이 중요
  - 각 단위별로 커밋하면 문제 발생 시 롤백이 용이

## 4단계: 코드 스멜 수정

- **활용 방식**: 분석 문서 기반으로 코드 스멜 수정, 각 패턴별 일괄 변경 후 테스트
- **수정 내용**:
  - `orElse(null)` + null 체크 → Optional chain (map/filter) 패턴으로 변경
  - HTTP 상태 코드 매직 넘버 → `HttpStatus.UNAUTHORIZED`, `HttpStatus.FORBIDDEN` 상수 사용
  - 불필요한 `@Autowired` 제거 (Spring 4.3+ 단일 생성자 자동 주입)
  - 에러 메시지 한국어로 통일
  - 미참조 getter 메서드 삭제 (`Order.getMemberId()`, `Product.getOptions()`)
  - Exception Swallowing에 최소 로깅 추가 (debug/warn 레벨)
  - `var` → 명시적 타입 선언으로 변경
  - `path=` 속성 생략으로 스타일 통일
  - 패키지 내부 클래스를 package-private으로 변경 (캡슐화 강화)
- **학습 내용**:
  - 에러 메시지 변경 시 해당 메시지를 검증하는 테스트도 함께 수정 필요
  - Optional chain 패턴은 orElse(null) + null 체크보다 의도가 명확함
  - 코드 스멜 수정 시 일부 항목은 이전 단계에서 이미 해결된 경우가 있음

## 5단계: 리뷰 피드백 반영

- **활용 방식**: 리뷰어 피드백을 기반으로 문서 구조 개선 및 코드 구조 변경
- **수정 내용**:
  - README에서 전략/AI 기록을 `docs/` 하위 문서로 분리하여 README 슬림화
  - 기존 분석 문서 6개를 프로젝트 루트에서 `docs/`로 이동
  - `WishService` 내부 중첩 타입(`AddWishResult`, `DeleteResult`)을 wish 패키지 내 별도 파일로 분리
- **학습 내용**:
  - 에러 메시지 변경은 API 응답 본문이 달라지므로 구조 변경이 아닌 작동 변경에 해당함 — 구조 변경 커밋에 섞지 말아야 함
  - 서비스 내부 중첩 타입이 컨트롤러에 `WishService.AddWishResult`로 노출되면 결합도가 높아짐 — 계층 간 계약은 별도 파일로 분리하는 것이 적절
  - `@RestControllerAdvice`를 전역 적용하면 기존에 `@ExceptionHandler`가 없던 컨트롤러의 작동이 바뀔 수 있음 (500→400) — `assignableTypes`로 범위를 한정하여 순수 구조 변경을 유지해야 함
