# Session tester-5: 인수 테스트 전체 구현 + step 자연어 리팩터링

**날짜**: 2026-02-27

## 이번 세션에서 한 일

### 1. 64개 인수 테스트 전체 구현

이전 세션에서 작성한 시나리오 명세(acceptance-scenarios.md)와 테스트 인프라를 기반으로, 7개 Feature에 대한 step definition + feature file + suite class를 모두 구현. 64 tests, 0 failures 달성.

### 2. 빌드/실행 이슈 4건 해결

- **Lombok annotation processor**: test source set에서 `@Getter`/`@Setter` 미인식 → `testAnnotationProcessor("org.projectlombok:lombok")` 추가
- **Suite가 모든 feature 실행**: `junit-platform.properties`의 `cucumber.features=classpath:features` 때문 → 제거하고 각 Suite에 `@ConfigurationParameter(FEATURES_PROPERTY_NAME)` 사용
- **DataTable 빈 값 NPE**: `Map.of()`에 null 불가 → `HashMap` + `nullToEmpty()` 헬퍼로 전환
- **한국어 조사 불일치**: `"orderDateTime"이 null이 아니다` vs `가 null이 아니다` → 별도 step definition 추가

### 3. Given step escaping 리팩터링

Cucumber Expression 특수문자(`\/`, `\(`, `\{`) 이스케이핑으로 IDE feature↔Java 링크 깨짐.
5개 Given step에서 이스케이핑 제거하고 자연스러운 한국어로 변경:

- `"email" / "password" 회원이 존재한다` → `이메일 "email" 비밀번호 "password" 회원이 존재한다`
- `회원이 존재하고 유효한 토큰을 가진다 (포인트 충분)` → `포인트가 충분한 회원이 존재하고 유효한 토큰을 가진다`
- `"카테고리"에 "상품"(가격 5000) 상품이 존재한다` → `"카테고리"에 가격 5000인 "상품" 상품이 존재한다`
- `해당 상품에 "옵션"(수량 100) 옵션이 존재한다` → `해당 상품에 수량 100인 "옵션" 옵션이 존재한다`
- 두 옵션 복합 step도 동일 패턴 적용

### 4. When step 자연어 리팩터링

API 경로 노출 방식(`POST /api/categories 요청`)에서 자연어(`카테고리 생성 요청을 보낸다`)로 35개 When step 전환:

- **인증 구분 패턴**: `인증된 사용자가 ...` vs `유효하지 않은 토큰으로 ...`
- **컨텍스트 vs 에러 패턴**: `해당 {도메인} ...` vs `존재하지 않는 {도메인} ...`
- **하드코딩 ID 내재화**: 7개 step에서 `{int}` 파라미터 제거, `NON_EXISTENT_ID = 99999` 상수 사용
- 수정 파일: Java 6파일(MemberApiSteps, CategoryApiSteps, ProductApiSteps, OptionApiSteps, WishApiSteps, OrderApiSteps) + Feature 6파일

## 커밋 목록

- 이번 세션에서 생성한 커밋 없음 (아직 커밋 대기 중)

## 변경된 파일

**신규**:

- `src/test/java/gift/acceptance/context/ScenarioContext.java` (secondAuthToken 추가)
- `src/test/java/gift/acceptance/steps/CommonDataSteps.java`
- `src/test/java/gift/acceptance/steps/AuthSteps.java`
- `src/test/java/gift/acceptance/steps/CommonAssertionSteps.java`
- `src/test/java/gift/acceptance/steps/MemberApiSteps.java`
- `src/test/java/gift/acceptance/steps/CategoryApiSteps.java`
- `src/test/java/gift/acceptance/steps/ProductApiSteps.java`
- `src/test/java/gift/acceptance/steps/OptionApiSteps.java`
- `src/test/java/gift/acceptance/steps/WishApiSteps.java`
- `src/test/java/gift/acceptance/steps/OrderApiSteps.java`
- `src/test/java/gift/acceptance/suite/AbstractCucumberFeatureTest.java`
- `src/test/java/gift/acceptance/suite/AuthCucumberFeatureTest.java`
- `src/test/java/gift/acceptance/suite/MemberCucumberFeatureTest.java`
- `src/test/java/gift/acceptance/suite/CategoryCucumberFeatureTest.java`
- `src/test/java/gift/acceptance/suite/ProductCucumberFeatureTest.java`
- `src/test/java/gift/acceptance/suite/OptionCucumberFeatureTest.java`
- `src/test/java/gift/acceptance/suite/WishCucumberFeatureTest.java`
- `src/test/java/gift/acceptance/suite/OrderCucumberFeatureTest.java`
- `src/test/resources/features/auth.feature`
- `src/test/resources/features/member.feature`
- `src/test/resources/features/category.feature`
- `src/test/resources/features/product.feature`
- `src/test/resources/features/option.feature`
- `src/test/resources/features/wish.feature`
- `src/test/resources/features/order.feature`

**수정**:

- `build.gradle.kts` (junit-platform-suite, testAnnotationProcessor 추가)
- `src/test/resources/junit-platform.properties` (cucumber.features 제거)
- `src/test/resources/application-test.yaml`

## 결정 사항

- **Suite 단위 feature 격리**: `@ConfigurationParameter(FEATURES_PROPERTY_NAME)` 방식으로 각 Suite가 자기 feature만 실행
- **Step 자연어 패턴**: `{동작} 요청을 보낸다` 통일, 인증/비인증/존재하지않는 패턴 구분
- **NON_EXISTENT_ID 상수 패턴**: 하드코딩 ID를 step 파라미터 대신 클래스 상수로 관리

## 발견한 문제

- Linter가 feature file의 DataTable 한글 열 정렬을 자동 변경 (기능에 영향 없음)
- Cucumber Expression 이스케이핑이 IDE 링크를 깨뜨림 → Given/When 모두 해결 완료

## 다음 세션 할 일

- 작업 내용을 적절한 단위로 커밋
- Phase 1 본작업 시작: 스타일 정리 → 불필요한 코드 제거 → 서비스 계층 추출
