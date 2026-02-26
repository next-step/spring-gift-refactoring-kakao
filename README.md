# spring-gift-refactoring

## Docker 컨테이너 환경 구성

기존 프로젝트에 Docker 기반 실행 환경을 구성하고, Cucumber BDD 테스트 인프라를 도입하였다.

### 작업 배경

로컬 환경에 의존하지 않고 Docker 컨테이너만으로 애플리케이션이 동작하도록 구성할 필요가 있었다.
이미 구축된 `spring-gift-test-kakao` 프로젝트의 Docker 설정을 참고하여, 동일한 방식(포트 매핑 규칙, 멀티스테이지 빌드, Gradle 태스크 통합)을 현재 프로젝트의 기술 스택(MySQL, Flyway, Kotlin DSL)에 맞게 적용하였다.

### 변경 사항

#### 1. Docker 환경 구성

| 파일 | 역할 |
|------|------|
| `Dockerfile` | 멀티스테이지 빌드 (eclipse-temurin:21-jdk → 21-jre) |
| `docker-compose.yml` | MySQL 8.4 + Spring Boot 앱, 헬스체크 포함 |
| `.dockerignore` | 빌드 컨텍스트 최적화 |

**포트 매핑 규칙** — 참고 프로젝트와 동일한 패턴 적용:

| 서비스 | 호스트 | 컨테이너 |
|--------|--------|----------|
| MySQL | 13306 | 3306 |
| App | 28080 | 8080 |

```bash
./gradlew dockerUp    # 이미지 빌드 + 서비스 시작
./gradlew dockerDown  # 서비스 중지 + 볼륨 정리
```

#### 2. 기존 파일 수정

- **`build.gradle.kts`** — `spring-boot-starter-actuator` 추가 (헬스체크용), Docker/Cucumber Gradle 태스크 추가, Colima 소켓 자동 감지
- **`application.properties`** — 데이터소스 설정을 환경변수 플레이스홀더로 추가 (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)

#### 3. Cucumber BDD 테스트 도입

참고 프로젝트의 Cucumber 인프라를 마이그레이션하고, 현재 프로젝트의 도메인(주문)에 맞게 시나리오를 작성하였다.

| 파일 | 역할 |
|------|------|
| `src/test/java/gift/cucumber/CucumberSuite.java` | JUnit 5 Suite 진입점 |
| `src/test/java/gift/cucumber/CucumberSpringConfiguration.java` | Spring 컨텍스트 설정 (cucumber 프로필) |
| `src/test/java/gift/cucumber/steps/OrderStepDefinitions.java` | 주문 시나리오 스텝 정의 |
| `src/test/resources/features/order.feature` | 한국어 Gherkin 시나리오 7건 |
| `src/test/resources/application-cucumber.properties` | 테스트용 MySQL 접속 설정 |

```bash
./gradlew cucumberTest  # dockerUp → Cucumber 테스트 → dockerDown
```

**PostgreSQL → MySQL 적응 시 주의한 점:**
- TRUNCATE 시 `SET FOREIGN_KEY_CHECKS = 0/1` 필요 (PostgreSQL의 `CASCADE`와 다름)
- 드라이버 클래스: `com.mysql.cj.jdbc.Driver`
- Flyway가 이미 스키마를 생성하므로 테스트 프로필에서 `spring.flyway.enabled=false` 설정

### 학습 내용

#### Docker 멀티스테이지 빌드

빌드 스테이지(JDK)와 런타임 스테이지(JRE)를 분리하면 최종 이미지에서 빌드 도구와 소스코드가 제거되어 이미지 크기가 줄어든다. `bootJar -x test`로 테스트를 건너뛰어 빌드 시간도 단축할 수 있다.

#### Groovy → Kotlin DSL 변환

참고 프로젝트의 Groovy Gradle 스크립트를 Kotlin DSL로 변환하면서 차이를 확인하였다:
- `tasks.register('name', Exec)` → `tasks.register<Exec>("name")`
- `def` 클로저 → `val` 람다 (`() -> String?`)
- `tasks.named('test')` → `tasks.named<Test>("test")` — `tasks.withType<Test>`를 쓰면 `cucumberTest`를 포함한 모든 Test 태스크에 설정이 적용되므로 주의

#### Cucumber 테스트 아키텍처

테스트 JVM과 앱 JVM이 별도 프로세스로 동작하는 구조를 이해하였다:
- 테스트 JVM → MySQL (`localhost:13306`) 직접 접속하여 데이터 셋업/검증
- 테스트 JVM → 앱 (`localhost:28080`) HTTP 요청으로 기능 검증
- 같은 DB를 공유하므로 앱이 변경한 데이터를 테스트에서 즉시 확인 가능

JWT 인증이 필요한 API의 경우, 테스트 JVM에서 동일한 시크릿으로 `JwtProvider`를 주입받아 토큰을 직접 생성하는 방식으로 해결하였다.

#### 트랜잭션 경계의 중요성

Cucumber 테스트를 통해 `OrderController`의 트랜잭션 미적용 문제를 발견하였다. 참고 프로젝트는 Service 계층에 `@Transactional`을 적용하여 재고 차감과 포인트 차감이 하나의 트랜잭션으로 묶이지만, 현재 프로젝트는 Controller에서 Repository를 직접 호출하여 각 `save()`가 독립 커밋된다. 포인트 부족 시 재고만 차감되고 롤백되지 않는 문제가 테스트로 확인되었다.

### Claude Code 활용

이번 작업에서 Claude Code를 다음과 같이 활용하였다:

- **참고 프로젝트 분석** — 기존 Docker 설정의 구조와 의도를 파악하여 단순 복사가 아닌 기술 스택에 맞는 적응 방향을 잡음
- **Gradle DSL 변환 검증** — Groovy에서 Kotlin DSL로 변환 시 `tasks.withType` vs `tasks.named` 스코핑 이슈를 빌드 실행으로 즉시 확인하고 수정
- **E2E 검증 자동화** — Docker 이미지 빌드 → 컨테이너 기동 → 헬스체크 → API 호출 → 정리까지 전 과정을 반복 실행하며 설정 오류를 조기에 잡음
- **테스트 기반 결함 발견** — Cucumber 시나리오 실행 결과를 분석하여 트랜잭션 미적용 문제의 근본 원인(아키텍처 차이)을 참고 프로젝트와의 비교를 통해 진단
