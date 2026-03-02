# Session tester-4: 인수 테스트 시나리오 정밀 구성

**날짜**: 2026-02-27

## 이번 세션에서 한 일

### 1. 전체 소스 코드 정밀 분석

- 6개 도메인의 Controller, Entity, DTO, Validator 전체 코드 읽기
- 정확한 엔드포인트 시그니처, 응답 패턴, 에러 메시지 확인
- Validation 애노테이션(`@NotBlank`, `@Email`, `@Positive`, `@Min`, `@Max` 등) 매핑

### 2. acceptance-scenarios.md 작성

- 64개 시나리오 (Happy 24 + Error 40), 7개 Feature 구성
- 각 시나리오에 Gherkin 형식(Given/When/Then) + 코드 근거 명시
- DTO 필드명, 에러 메시지, 상태 코드를 코드에서 직접 확인하여 기재

### 3. 사용자 피드백 반영 (4건 수정)

- **M-3 → A-1**: 인증 시나리오를 `## 0. 인증 (공통)` 별도 섹션으로 분리
- **C-4**: `And 응답 body가 없다` 제거 (204만 검증)
- **O-1**: ⚠️ product builder 누락 경고 제거 (개발자가 코드 수정 완료)
- **OR-1**: ⚠️ orderDateTime null 경고 제거 (개발자가 코드 수정 완료), `orderDateTime != null` 검증 추가

### 4. Step Definition 재사용 전략 작성

- ScenarioContext 패턴 (시나리오 상태 공유)
- Step 클래스 책임별 분리 구조 (CommonDataSteps, AuthSteps, {Domain}ApiSteps, CommonAssertionSteps)
- 재사용 가능한 Given/Then 패턴 정의
- Feature 파일 구성 (도메인별 1파일)
- 태그 전략 (@api, @happy, @error, @domain)
- DataManipulator 활용 참고 테이블

### 5. 코드 vs 명세 불일치 테이블 업데이트

- 항목 5 (orderDateTime null), 항목 6 (옵션 생성 product 누락) 제거 (수정 완료됨)
- 잔존 불일치: 4건 (옵션 PUT 미구현, 주문 500, 위시 자동 제거 미구현, @Transactional 없음)

## 커밋 목록

- 이번 세션에서 생성한 커밋 없음 (문서 작성만 진행, 커밋 대기 중)

## 변경된 파일

- `acceptance-scenarios.md` (신규) — 64개 인수 테스트 시나리오 + step definition 재사용 전략

## 결정 사항

- **인증 시나리오 독립 분리**: 도메인 횡단 관심사인 인증은 별도 Feature(`auth.feature`)로 구성
- **DataManipulator 계약 변경 반영**: `addOrder`의 `orderDateTime`은 항상 `LocalDateTime.now()` (파라미터 불필요)
- **Step 클래스 구조**: 책임별 분리 — 공통 Given/Then은 재사용 극대화, 도메인별 When은 전용 클래스

## 발견한 문제

- 잔존 불일치 4건은 모두 Phase 2 범위 (변동 없음)

## 다음 세션 할 일

- `auth.feature` + `AuthSteps.java` 구현 (A-1 시나리오)
- `member.feature` + `MemberApiSteps.java` 구현 (M-1~M-E6)
- `ScenarioContext.java`, `CommonDataSteps.java`, `CommonAssertionSteps.java` 공통 인프라 구현
- 이후: category → product → option → wish → order 순서로 Feature + Step 구현
