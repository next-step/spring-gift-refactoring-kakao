# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
./gradlew build            # 빌드 (단위 테스트 포함, 인수 테스트 제외)
./gradlew test             # 단위 테스트 실행 (acceptance 제외)
./gradlew acceptanceTest   # 인수 테스트 실행 (Cucumber, profile: acceptance-test)
./gradlew test --tests "gift.acceptance.suite.CategoryCucumberFeatureTest"  # 단일 feature 테스트
./gradlew bootRun          # 로컬 실행 (H2)
```

테스트는 Cucumber + RestAssured 기반 인수 테스트 (75개 시나리오). `src/test/resources/features/*.feature`에 Gherkin 시나리오, `src/test/java/gift/acceptance/steps/`에 step definitions. `test`와 `acceptanceTest`는 Gradle task가 분리되어 있음 (`test`는 `**/acceptance/**` 제외).

## Architecture

Spring Boot 3.5.9 / Java 21 선물 관리 e-commerce 애플리케이션.

### 패키지 구조 (`src/main/java/gift/`)

도메인별 패키지 안에서 **외부 공개**(Entity, Port 인터페이스)와 **내부 구현**(`internal/`)을 분리한다. 관리자 UI는 `admin/` 패키지에 별도 배치.

| 패키지              | 설명                                                                                                     |
|------------------|--------------------------------------------------------------------------------------------------------|
| `auth/`          | `AuthenticationPort`, `JwtPort` 인터페이스 (외부 공개)                                                          |
| `auth/internal/` | JWT 구현(`JwtProvider`, `JwtPortImpl`), Kakao OAuth(`KakaoAuthService`, `KakaoLoginClient`)              |
| `category/`      | `Category` Entity, `CategoryQueryPort` + `internal/`에 Controller, Service, Repository, DTO             |
| `product/`       | `Product` Entity, `ProductQueryPort` + `internal/`(REST API), `admin/`(관리자 UI), `common/`(전략 패턴)       |
| `member/`        | `Member` Entity, `MemberQueryPort`, `MemberCommandPort` + `internal/`(REST API), `admin/`(관리자 UI)      |
| `option/`        | `Option` Entity, `OptionQueryPort`, `OptionCommandPort` + `internal/`(Controller, Service, Repository) |
| `wish/`          | `Wish` Entity + `internal/`(Controller, Service, Repository, WishCleanupService)                       |
| `order/`         | `Order` Entity, `OrderCreatedEvent` + `internal/`(Service, KakaoNotificationEventListener)             |
| `global/`        | 커스텀 예외 계층(`CustomException`, `NotFoundException`, `BadRequestException` 등) + `CustomExceptionHandler`  |

### 핵심 설계 특성

- **계층 구조**: Controller → Service → Repository. Controller는 검증과 위임만 담당.
- **인증**: `Authorization: Bearer <JWT>` 헤더 → `AuthenticationPort` 인터페이스로 Member 조회. 구현체는 `auth/internal/`에 격리.
- **Port 패턴**: 도메인 간 접근은 Port 인터페이스(패키지 루트)를 통한다. QueryPort(조회/FK참조)와 CommandPort(상태변경)로 분리. 다른 도메인의 Repository를 직접 import하지 않는다.
- **이벤트 패턴**: 주문 부수 효과(위시 정리, 카카오 알림)는 `OrderCreatedEvent` + `@TransactionalEventListener(AFTER_COMMIT)`로 처리. order → wish 의존성이 이벤트로 디커플링.
- **Entity 관계**: Category(1) → Product(N) → Option(N) → Order(N). Wish와 Order의 memberId는 primitive FK(엔티티 참조 아님).
- **DTO**: 모든 요청/응답이 Java record. `from(Entity)` 팩토리 메서드로 변환.
- **Validator**: `ProductNameValidator`는 전략 패턴(`ProductNameRule` 인터페이스 + Rule 구현체). REST API는 "카카오" 포함 금지, Admin은 미적용. Option은 Bean Validation 사용.
- **예외 처리**: `CustomException` 기반 예외 계층 + `@RestControllerAdvice` 글로벌 핸들러(`CustomExceptionHandler`).
- **DB**: Flyway 마이그레이션 (`src/main/resources/db/migration/`). 로컬/테스트는 H2, 운영은 MySQL.

### 테스트 구조 (`src/test/java/gift/`)

**인수 테스트** (`acceptance/`)
- `context/` — `ScenarioContext`(시나리오 간 상태 전달), `CucumberSpringConfiguration`(테스트 부트), `CucumberHooks`(데이터 초기화)
- `steps/` — Given/When/Then step definitions (도메인별 분리)
- `suite/` — Cucumber runner 클래스 (feature별 1개)
- `support/` — `DataManipulator`(테스트 데이터 정리), test repository 인터페이스

**Port 통합 테스트** — `@DataJpaTest` + `@Transactional(NOT_SUPPORTED)` + `@Import(PortImpl)`. 실제 DB 조작 및 트랜잭션 전파 검증. 자동 트랜잭션 해제로 Port의 MANDATORY/REQUIRED가 실제 환경과 동일하게 작동.

**Service 단위 테스트** — `@ExtendWith(MockitoExtension.class)` + Mock Port. 오케스트레이션 순서와 호출 인자 검증.

외부 의존성(`KakaoLoginClient`, `KakaoMessageClient`)은 `@MockBean`으로 격리. 테스트 프로필은 `acceptance-test`.

---

# Refactoring Mission

## 구조 변경 vs 작동 변경

**구조 변경** — 외부 작동을 바꾸지 않고 내부 구조를 개선한다.

- 이름 바꾸기, 메서드 추출, 클래스 추출
- 패키지 이동, 의존성 정리, 계층 분리
- 목표: 읽기 쉽고 바꾸기 쉬운 구조

**작동 변경** — 사용자 관점에서 달라지는 결과를 만든다.

- 정책 변경, 예외 처리 방식 변경
- 트랜잭션 경계 변경, 누락된 기능 구현
- 목표: 사용자 관점에서 달라지는 결과

**구조 변경과 작동 변경을 절대 섞지 않는다. 한 커밋에는 둘 중 하나만 담는다.**

## Phase 1: 리팩터링 준비하기 (완료)

목표: 변경하기 쉬운 상태 만들기. 구조 변경만 수행.

1. 스타일 정리
2. 불필요한 코드 제거
3. 서비스 계층 추출
4. 인수 테스트 75개 구축 (Cucumber + RestAssured)

## Phase 2: 리팩터링 완성하기 (현재)

목표: **작동 변경을 안전하게 수행하고, 그 결과를 증거로 보여주기.**

### 핵심 원칙

- 작동 변경은 반드시 **증거**(테스트)와 함께 제출한다.
- 예외 발생 여부만 확인하는 것으로 충분하지 않다. **상태를 재조회하거나 결과를 관찰 가능한 방식으로 검증**해야 한다.
- 구조 변경 커밋과 작동 변경 커밋을 분리한다.
- 변경 전에 "무엇을 바꾸는지, 무엇을 바꾸지 않는지, 무엇이 이를 증명하는지"를 한 줄씩 적고 시작한다.
- 트레이드오프가 있는 결정은 ADR로 기록한다.

### 작업 범위

**1. 도메인 책임 되찾기 (구조 변경) — ADR-001**

크로스 도메인 Repository 8개를 QueryPort/CommandPort로 통합하여 도메인 경계를 강화한다.

| 도메인      | QueryPort | CommandPort | 대체 대상                                                         |
|----------|:---------:|:-----------:|---------------------------------------------------------------|
| Category |     O     |      X      | `ProductCategoryRepository`, `AdminProductCategoryRepository` |
| Product  |     O     |      X      | `WishProductRepository`, `OptionProductRepository`            |
| Option   |     O     |      O      | `OrderOptionRepository`                                       |
| Member   |     O     |      O      | `OrderMemberRepository`, `AuthMemberRepository`               |

트랜잭션 전파 전략:

- QueryPort `getReference()` → `MANDATORY` (detached entity 방지, fast-fail)
- QueryPort 기타 조회 → 트랜잭션 없음 (단순 값 반환)
- CommandPort 모든 메서드 → `REQUIRED` (비트랜잭셔널 호출자 수용)

개선 효과: 중복 Repository 제거 + Service 호출부 단순화 (`repo.findById().orElseThrow().메서드()` → `port.메서드(id)`)

**2. 누락된 작동 구현 + 트랜잭션 경계 (작동 변경)**

- **2-1. 주문 부수 효과** (ADR-003) — 위시 정리 구현 + 카카오 알림을 Spring ApplicationEvent 리스너로 분리. 위시 정리는 주문 트랜잭션에 포함하지 않음 (별도 트랜잭션). `@TransactionalEventListener(phase = AFTER_COMMIT)` 사용.
- **2-2. Option PUT** — 옵션 수정 엔드포인트 추가. `OptionRequest` 생성/수정 공유.
- **2-3. Category GET /{id}** — 카테고리 단건 조회 엔드포인트 추가.
- **2-4. Product 카테고리 필터** — `GET /api/products?categoryId={id}` 파라미터 추가.

**3. 테스트 구축 — 증거 (ADR-002)**

| 계층             | 대상       | 방식                                           | 검증 내용                                  |
|----------------|----------|----------------------------------------------|----------------------------------------|
| Port 통합 테스트    | Port 구현체 | `@DataJpaTest` + `NOT_SUPPORTED` + `@Import` | 실제 DB 조작, 트랜잭션 전파 (MANDATORY/REQUIRED) |
| Service 단위 테스트 | Service  | `@ExtendWith(MockitoExtension.class)` + Mock | 오케스트레이션 순서, 호출 인자, 분기 로직               |
| 인수 테스트         | API 전체   | Cucumber + RestAssured                       | 사용자 관점 계약, 트랜잭션 원자성, 상태 재조회            |

### 커밋 전략

```
[구조 변경] refactor: {도메인}Port 도입, 크로스 도메인 Repository 제거
[테스트]   test: Port 통합 테스트 / Service 단위 테스트
[작동 변경] feat: 누락 작동 구현 (각각 별도 커밋)
[테스트]   test: 작동 변경 인수 테스트 (상태 재조회 검증)
```

구조 변경 커밋은 기존 테스트가 모두 통과해야 한다. 작동 변경 커밋은 새 테스트와 함께 제출한다.

### ADR 현황

| 번호      | 결정 사항                                   | 문서                                        |
|---------|-----------------------------------------|-------------------------------------------|
| ADR-001 | Port 패턴 + Query/Command 분리 + 트랜잭션 전파 전략 | `docs/adr-001-cross-domain-repository.md` |
| ADR-002 | 테스트 전략 (Port 통합 + Service 단위 + 인수)      | `docs/adr-002-test-strategy.md`           |
| ADR-003 | 주문 부수 효과 (트랜잭션 분리 + 이벤트 + try-catch 로깅) | `docs/adr-003-order-side-effects.md`      |

상세 작업 계획: `plan-0.md`

## AI 협업 규율

이 프로젝트에서 AI는 초안을 만드는 도구이다. 설계와 검증 책임은 개발자에게 있다.

### MUST

- 코드 수정 전 README.md 체크리스트에 다음 작업이 적혀 있어야 한다.
- 변경 후 전체 테스트 통과를 확인한다. (`./gradlew test` + `./gradlew acceptanceTest`)
- 커밋은 목적 1개. git diff를 보고 30초 안에 의도를 설명할 수 없으면 더 쪼갠다.
- 작동 변경은 반드시 증거(테스트)와 함께 제출한다. 상태 재조회로 검증한다.
- AI 활용 내역을 README.md에 기록한다.

### MUST NOT

- AI가 요청하지 않은 작동을 추가하려 하면 즉시 중단한다.
- 테스트를 회피하거나 비활성화하지 않는다.
- 구조 변경과 작동 변경을 한 커밋에 섞지 않는다.
- "행동만 맞으면 된다"로 끝내지 않는다.
- 참조 Port에서 받은 Entity의 비즈니스 메서드를 직접 호출하지 않는다 (Port 컨벤션).

### 작업 지시 패턴

AI에게 요청할 때는 다음을 함께 제공한다:

- **목표**: 무엇을 바꾸는가
- **범위**: 어떤 파일을 수정하는가
- **제약**: 하지 말아야 할 것
- **검증**: 확인 방법

## 커밋 컨벤션

AngularJS Git Commit Message Conventions 사용.

```
<type>: <subject>

<body>
```

타입: `feat`, `style`, `refactor`, `test`, `fix`, `docs`, `chore`

## 코드 컨벤션

- Bean 주입: `@RequiredArgsConstructor` + `private final` 필드
- 변수 선언: `var` 사용 금지, 불필요한 `final` 사용 금지
- Response: `ResponseEntity<?>` 금지 → `ResponseEntity<Void>` 또는 명시적 타입
- Validation: Spring Validation 사용
- Entity: Lombok `@Getter`, `@NoArgsConstructor(access = PROTECTED)`
- Stream: `.toList()` 사용 (`.collect(Collectors.toList())` 금지)

## 세션 관리

- 세션 시작 시 `memory/` 디렉토리에서 이전 세션 기록을 확인한다.
- 코드 변경 후 커밋 전에 `/verify` 로 검증한다.
- 세션 종료 시 `/history` 로 작업 내용을 기록한다.
- 각 세션 파일은 `memory/session-{N}-{주제}.md` 형식으로 저장한다.

## Skills

- `/verify` — 변경 사항 검증 (테스트, diff, 구조/작동 분리, 30초 규칙, 컨벤션)
- `/decision` — 선택지 정리 → 토의 → 결정 기록 (`memory/decisions.md`)
- `/history` — 세션 종료 시 작업 기록 저장
