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

## 6단계: 리팩터링 완성하기

- **활용 방식**: 리팩토링 전략을 먼저 수립하고, Phase별로 "테스트 먼저 → 구현 → 전체 테스트 확인 → 커밋" 사이클을 반복. 각 Phase마다 AI에게 "지금은 이것만" 범위를 명확히 지시하여 진행.
- **수정 내용**:
  - Phase 1: `OrderService.createOrder`에 `@Transactional` 적용 — 재고·포인트·주문 저장을 원자적으로 묶음
  - Phase 2-1: `GlobalExceptionHandler`의 `assignableTypes` 제거하여 전역 적용 — `IllegalArgumentException` 발생 시 500→400
  - Phase 2-2: 주문 완료 후 위시리스트 정리 구현 — `wishRepository.findByMemberIdAndProductId`로 조회 후 삭제
  - Phase 3-1: `AdminProductController` → `ProductService` 위임 — 이름 검증, 카테고리 조회, 상품 CRUD를 서비스로 이동
  - Phase 3-2: `AdminMemberController` → `MemberService` 위임 — 회원 CRUD, 포인트 충전을 서비스로 이동
  - Phase 3-3: `KakaoAuthController` → `KakaoAuthService` 추출 — 자동가입, 토큰 업데이트, JWT 발급 로직 이동
  - Phase 3-4: 가격 계산 중복 제거 — `Order.getTotalPrice()` 도메인 메서드 추가, `OrderService`와 `KakaoMessageClient`의 중복 계산 제거
  - Phase 3-5: 인증 체크 중복 제거 — `AuthenticationResolver`가 null 대신 `UnauthorizedException`을 던지도록 변경, `GlobalExceptionHandler`에서 401 처리, 컨트롤러 5곳의 null 체크 보일러플레이트 제거
- **학습 내용**:
  - "테스트 먼저" 접근법이 작동 변경의 안전망으로 효과적 — 실패하는 테스트가 버그를 증명하고, 구현 후 통과로 수정을 증명
  - 구조 변경(Phase 3)은 기존 인수 테스트가 안전망 역할을 충분히 함 — 새 테스트 없이 기존 테스트 통과만으로 검증 가능
  - `@Transactional` 범위 결정 시 외부 호출(카카오 API)의 위치가 중요 — try-catch로 감싸져 있으면 트랜잭션 롤백을 유발하지 않으므로 메서드 전체 적용이 안전
  - 인증 중복 제거 시 ArgumentResolver 대신 예외 기반 전환을 선택 — 변경 범위 최소화가 우선, Spring Security 도입 시 전체 교체될 부분이므로 과투자 방지
  - Admin 컨트롤러의 서비스 위임 시, API용 메서드(DTO 반환)와 Admin용 메서드(엔티티 반환)가 같은 서비스에 공존 — 현 규모에서는 수용하되, 규모가 커지면 분리 검토 필요

## 7단계: 누락 기능 추가 및 트랜잭션 개선

- **활용 방식**: 원본 코드와 현재 코드를 비교하여 누락된 의도를 분석하고, 테스트 먼저 작성 후 구현하는 사이클 적용
- **수정 내용**:
  - 옵션 수정 API 추가: `Option.update()` 도메인 메서드, `OptionService.updateOption()`, `OptionController` PUT 엔드포인트 구현. 자기 이름 유지 시 중복 체크 스킵 로직 포함
  - 카카오 알림 트랜잭션 분리: `OrderTransactionService` 추출하여 DB 작업만 `@Transactional`로 묶고, 카카오 알림은 트랜잭션 완료 후 호출
- **학습 내용**:
  - 옵션 수정 시 "자기 자신의 이름 유지"는 중복 체크에서 제외해야 함 — 이름 변경 없이 수량만 수정하는 케이스를 테스트로 먼저 증명
  - self-invocation 문제: 같은 클래스 내에서 `@Transactional` 메서드를 호출하면 프록시를 거치지 않아 트랜잭션이 적용되지 않음 — 별도 클래스(`OrderTransactionService`)로 분리하여 해결
  - ADR-1에서 "메서드 전체 @Transactional"로 결정했더라도, 이후 개선의 여지를 남겨두면 단계적으로 트랜잭션 범위를 좁힐 수 있음
