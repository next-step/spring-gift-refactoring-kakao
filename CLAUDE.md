# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 필수 지침
- 모든 프롬프트 입력(원본)은 docs/PROMPT.md에 시간순으로 기록해야 한다.
- Java 코드를 변경한 경우, 커밋 전에 반드시 `./gradlew spotlessApply`를 실행하여 포매팅을 적용한다.

## Build & Test Commands

```bash
./gradlew build              # Full build (excludes cucumber tests)
./gradlew test               # Unit tests only
./gradlew cucumberTest       # Acceptance tests (auto starts/stops Docker containers)
./gradlew bootRun            # Run app locally
./gradlew spotlessCheck      # Java formatting check (palantir-java-format)
./gradlew spotlessApply      # Auto-fix Java formatting
```

Cucumber tests require Docker. The `cucumberTest` task handles `dockerUp`/`dockerDown` automatically.
To run containers manually: `./gradlew dockerUp` / `./gradlew dockerDown`.

## Tech Stack

- **Java 21**, Spring Boot 3.5.9, Gradle Kotlin DSL
- **Persistence**: Spring Data JPA, MySQL (production/E2E), H2 (local), Flyway migrations
- **Auth**: JWT (JJWT library), Kakao OAuth
- **Testing**: Cucumber 7.21.1 (Korean Gherkin), RestAssured 5.5.1, JUnit 5
- **Linting**: ktlint (Kotlin), Spotless + palantir-java-format (Java)

## Architecture

Gift/product ordering REST API with Kakao integration. Six domains: **Member**, **Category**, **Product**, **Option**, **Order**, **Wish**.

**Current state**: 2-layer architecture — Controllers contain business logic and call Repositories directly. **No Service layer exists yet** (extraction is the refactoring goal).

### Domain Relationships
- Category → Product (1:N)
- Product → Option (1:N, entity reference)
- Product → Wish (1:N)
- Option → Order (1:N)
- Member → Order, Wish (via primitive `memberId` FK, not entity reference)

### Auth Flow
- `JwtProvider`: creates/validates stateless JWT tokens (HMAC-SHA)
- `AuthenticationResolver`: extracts Member from `Authorization: Bearer {token}` header
- `KakaoAuthController` + `KakaoLoginClient`: Kakao OAuth callback flow

### Key Pattern: OrderController (primary refactoring target)
The order creation flow in `OrderController` orchestrates: auth check → option validation → stock subtraction → point deduction → order persistence → wish cleanup → Kakao notification. All of this should move to a service layer.

## Testing Infrastructure

Acceptance tests use **black-box E2E testing**: the app runs in a Docker container, tests hit HTTP endpoints via RestAssured.

- **Feature files** (`src/test/resources/features/`): Korean Gherkin (`# language: ko`) with `만일`/`그러면`/`그리고` keywords
- **Step definitions** (`src/test/java/gift/cucumber/steps/`): map Gherkin to RestAssured calls
- **ScenarioContext** (`@ScenarioScope`): shares state (response, IDs, token) across steps within a scenario
- **DatabaseCleaner**: truncates all tables before each scenario (MySQL `SET FOREIGN_KEY_CHECKS = 0`)
- **TestDataInitializer**: inserts test data via `SimpleJdbcInsert` (bypasses JPA)
- **Fixtures** (`src/test/java/gift/fixture/`): factory methods for domain objects
- **E2E profile** (`application-e2e.properties`): connects to Docker MySQL (`localhost:3306/gift_test`)

## Refactoring Roadmap (from README)

The project follows a 4-step refactoring plan:
1. **인수 테스트 작성** — Write acceptance tests as a safety net (completed)
2. **스타일 정리** — Consistent formatting (no logic changes)
3. **불필요한 코드 제거** — Remove unused code
4. **서비스 계층 추출** — Extract Service layer from Controllers

Critical rule: acceptance tests must pass before and after every refactoring step.