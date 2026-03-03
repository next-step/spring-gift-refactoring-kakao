# Session tester-3: 테스트 인프라 구성

**날짜**: 2026-02-27

## 이번 세션에서 한 일

### 1. 테스트 담당 역할 확인 및 규칙 수립

- CLAUDE.md, skills, session-0 기록 전체 파악
- 테스트 구축 규칙 5개 확정 (R1~R5)
- `src/main/**` 수정 금지, 인수 테스트(API) 중심

### 2. 인수 테스트 시나리오 작성

- 33개 소스 파일 전체 코드 분석
- 6개 도메인별 Happy Flow + Error Flow 총 62개 시나리오 작성
- 코드 vs API 명세 불일치 4건 발견:
    - 옵션 PUT 미구현, OrderController @ExceptionHandler 없음 (500 반환),
    - 위시 자동 제거 미구현, @Transactional 없음

### 3. Cucumber + RestAssured 의존성 추가

- `build.gradle.kts`에 Cucumber BOM 7.20.1, RestAssured 5.5.0 추가

### 4. Cucumber Spring 설정 구성

- `CucumberSpringConfiguration`: @SpringBootTest(RANDOM_PORT) + @MockitoBean (Kakao 클라이언트 격리)
- `junit-platform.properties`: Cucumber 실행 설정
- `application-test.properties`: H2 인메모리 + Flyway

### 5. 테스트 데이터 인프라 구성

- `DataManipulator` 인터페이스에 `initAll()` 추가
- `AcceptanceTestDataManipulator` 구현: Builder 기반 엔티티 생성 + deleteAllInBatch 정리
- `CucumberHooks` 작성: @Before RestAssured 설정, @After 데이터 정리
- 기존 `DatabaseCleanup` 삭제 → CucumberHooks로 대체

## 커밋 목록

- 이번 세션에서 생성한 커밋 없음 (인프라 구성만 진행, 커밋 대기 중)

## 변경된 파일

- `build.gradle.kts` (수정) — Cucumber, RestAssured 의존성 추가
- `src/test/java/gift/acceptance/CucumberSpringConfiguration.java` (신규)
- `src/test/java/gift/acceptance/CucumberHooks.java` (신규)
- `src/test/java/gift/acceptance/AcceptanceTestDataManipulator.java` (신규)
- `src/test/java/gift/support/DataManipulator.java` (수정) — initAll() 추가
- `src/test/resources/application-test.properties` (신규)
- `src/test/resources/junit-platform.properties` (신규)
- `src/test/resources/features/.gitkeep` (신규)
- `src/test/java/gift/acceptance/DatabaseCleanup.java` (삭제)
- `memory/session-1-test-planning.md` (신규)
- `memory/session-2-test-scenarios.md` (신규)

## 결정 사항

- 테스트 규칙 R1~R5 확정 (MEMORY.md에 기록됨)
- 인수 테스트는 현재 실제 동작 기준으로 작성 (명세 불일치 시 코드 우선)
- JDBC 직접 truncate 대신 JPA deleteAllInBatch + DataManipulator 패턴 채택
- @MockBean → @MockitoBean 전환 (Spring Boot 3.5.x deprecated 경고 해소)

## 발견한 문제

- OrderController에 @ExceptionHandler 없음 → 재고/포인트 부족 시 500
- OrderController에 @Transactional 없음 → 재고/포인트 비일관 가능
- 옵션 PUT 엔드포인트 미구현
- 위시 자동 제거 미구현
- 이상 4건 모두 Phase 2(작동 변경) 범위

## 다음 세션 할 일

- Category API 인수 테스트 Feature 파일 + Step 구현
- Member API 인수 테스트 (인증 흐름 — 이후 도메인의 전제 조건)
- Product → Option → Wish → Order 순서로 진행
