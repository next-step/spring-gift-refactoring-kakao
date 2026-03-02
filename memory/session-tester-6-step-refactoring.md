# Session tester-6: Step Definition 구조 리팩터링

**날짜**: 2026-02-28

## 이번 세션에서 한 일

### 1. ContextKeys 상수 클래스 생성

`"__currentCategory"` 등 9개 매직 스트링을 `ContextKeys` 상수로 추출. 오타 방지 + IDE 네비게이션 지원.

### 2. ScenarioContext 개선

기존 `saveId`/`getId`/`hasId`는 유지하고 다음을 추가:

- `NON_EXISTENT_ID = 99999L` 상수 (4개 클래스 중복 제거)
- 타입 접근자 5개: `currentCategoryId()`, `currentProductId()`, `currentOptionId()`, `currentMemberId()`, `currentWishId()`
- `resolvePlaceHolderId()`: DataTable 플레이스홀더(`{optionId}`) vs 리터럴 숫자를 하나의 메서드로 해석
- `doesNotHaveId()`: `!hasId()` 가독성 개선 (사용자가 추가)

### 3. ApiClient 컴포넌트 생성

RestAssured 호출을 캡슐화하는 `@Component @ScenarioScope` 클래스.
12개 메서드: post/get/put/delete × 비인증/인증(authToken)/임의토큰.
응답 자동 저장으로 호출부에서 `context.setResponse()` 제거.

### 4. 6개 ApiSteps + AuthSteps 리팩터링

**공통 변경 패턴**:

- `Map<String, Object> body = new HashMap<>()` → 도메인별 Request record 사용
- `RestAssured.given().contentType().header().body().when().post()` → `apiClient.post(path, body)`
- `NON_EXISTENT_ID` 필드 제거 → `ScenarioContext.NON_EXISTENT_ID` static import
- `@RequiredArgsConstructor` 적용 (사용자가 수정)
- `BASE_URI` 상수 추출 (사용자가 수정)

**리팩터링 전후 라인 수**:
| 파일 | Before | After |
|------|--------|-------|
| CategoryApiSteps | 106줄 | 67줄 |
| ProductApiSteps | 118줄 | 95줄 |
| MemberApiSteps | 52줄 | 52줄 |
| OptionApiSteps | 113줄 | 114줄 |
| WishApiSteps | 133줄 | 85줄 |
| OrderApiSteps | 103줄 | 69줄 |

### 5. CommonDataSteps, CommonAssertionSteps에 ContextKeys 적용

- 매직 스트링 → `ContextKeys.CURRENT_XXX` 상수
- `context.getId("__currentXxx")` → `context.currentXxxId()` 타입 접근자
- `!context.hasId()` → `context.doesNotHaveId()` (사용자가 추가)

### 6. 사용자 검수 후 추가 수정

사용자가 직접 검수하며 다음을 변경:

- `ContextKeys`: 상수 그룹화 (관련 상수를 multi-declaration으로 묶음)
- `ScenarioContext`: `@Profile("acceptance-test")` 추가, `hasId()` → `doesNotHaveId()` 네이밍 변경, `resolveId` → `resolvePlaceHolderId` 네이밍 변경
- `ApiClient`: `@Profile("acceptance-test")` 추가, `import static io.restassured.RestAssured.given` 사용
- 각 Steps: `@RequiredArgsConstructor` 적용, `BASE_URI` 상수 추출, private record → src/main의 기존 Request record 활용
- `CommonAssertionSteps`: JUnit assertions → AssertJ `assertThat()` 전환
- `OptionApiSteps`: 하드코딩된 `"아메리카노"`, `"기본B"` → 파라미터화 (`{string} 상품에서 {string} 옵션 삭제 요청을 보낸다`)
- `CucumberSpringConfiguration`: `@ActiveProfiles("test")` → `@ActiveProfiles("acceptance-test")`

## 커밋 목록

- 이번 세션에서 생성한 커밋 없음 (아직 커밋 대기 중)

## 변경된 파일

**신규**:

- `src/test/java/gift/acceptance/context/ContextKeys.java`
- `src/test/java/gift/acceptance/client/ApiClient.java`

**수정**:

- `src/test/java/gift/acceptance/context/ScenarioContext.java` (NON_EXISTENT_ID, 타입 접근자, resolvePlaceHolderId, doesNotHaveId)
- `src/test/java/gift/acceptance/steps/CategoryApiSteps.java` (ApiClient + CategoryRequest record)
- `src/test/java/gift/acceptance/steps/ProductApiSteps.java` (ApiClient + ProductRequest record)
- `src/test/java/gift/acceptance/steps/MemberApiSteps.java` (ApiClient + MemberRequest record)
- `src/test/java/gift/acceptance/steps/OptionApiSteps.java` (ApiClient + OptionRequest record + URL 헬퍼)
- `src/test/java/gift/acceptance/steps/WishApiSteps.java` (ApiClient + WishRequest record)
- `src/test/java/gift/acceptance/steps/OrderApiSteps.java` (ApiClient + OrderRequest record)
- `src/test/java/gift/acceptance/steps/AuthSteps.java` (ApiClient 주입 + ContextKeys)
- `src/test/java/gift/acceptance/steps/CommonDataSteps.java` (ContextKeys 상수 적용)
- `src/test/java/gift/acceptance/steps/CommonAssertionSteps.java` (타입 접근자 + AssertJ)
- `src/test/java/gift/acceptance/CucumberSpringConfiguration.java` (profile 변경)

## 결정 사항

- **Request record 위치**: step 내 private record 대신 src/main의 기존 Request record를 직접 사용 (사용자 결정)
- **Profile 분리**: `test` → `acceptance-test`로 변경하여 인수 테스트 전용 profile 격리 (사용자 결정)
- **ApiClient 설계**: 응답 자동 저장 방식 채택. 호출부에서 `context.setResponse()` 제거.
- **OptionApiSteps 파라미터화**: 하드코딩된 상품/옵션명 → Cucumber Expression 파라미터 (사용자 결정)

## 발견한 문제

- 없음. 64 tests, 0 failures 유지.

## 다음 세션 할 일

- 작업 내용을 적절한 단위로 커밋
- Phase 1 본작업 시작: 스타일 정리 → 불필요한 코드 제거 → 서비스 계층 추출
