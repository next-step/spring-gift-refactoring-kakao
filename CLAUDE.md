# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
./gradlew build          # 빌드 (테스트 포함)
./gradlew test           # 전체 테스트 실행
./gradlew test --tests "gift.acceptance.suite.CategoryCucumberFeatureTest"  # 단일 feature 테스트
./gradlew bootRun        # 로컬 실행 (H2, profile: local)
./gradlew ktlintCheck    # Kotlin lint 검사
```

테스트는 Cucumber + RestAssured 기반 인수 테스트. `src/test/resources/features/*.feature`에 Gherkin 시나리오, `src/test/java/gift/acceptance/steps/`에 step definitions.

## Architecture

Spring Boot 3.5.9 / Java 21 선물 관리 e-commerce 애플리케이션.

### 패키지 구조 (`src/main/java/gift/`)

도메인별로 패키지를 분리. 각 패키지에 Controller, Entity, Repository, DTO(record)가 위치한다.

| 패키지         | 설명                                                                                                          |
|-------------|-------------------------------------------------------------------------------------------------------------|
| `auth/`     | JWT 인증 (`JwtProvider`, `AuthenticationResolver`) + Kakao OAuth2 (`KakaoAuthController`, `KakaoLoginClient`) |
| `category/` | 카테고리 CRUD                                                                                                   |
| `product/`  | 상품 CRUD + 관리자 Thymeleaf UI (`AdminProductController`)                                                       |
| `member/`   | 회원 등록/로그인 + 관리자 UI (`AdminMemberController`)                                                                |
| `option/`   | 상품 옵션 (Product 하위 리소스, `/api/products/{productId}/options`)                                                 |
| `wish/`     | 위시리스트 (인증 필요)                                                                                               |
| `order/`    | 주문 (인증 필요, 재고 차감 + 포인트 차감 + 카카오 알림)                                                                         |

### 핵심 설계 특성

- **서비스 계층 없음**: 현재 모든 비즈니스 로직이 Controller에 있다. Phase 1 리팩터링 대상.
- **인증**: `Authorization: Bearer <JWT>` 헤더 → `AuthenticationResolver`가 Member 조회. 실패 시 null 반환 → 컨트롤러에서 401.
- **Entity 관계**: Category(1) → Product(N) → Option(N) → Order(N). Wish와 Order의 memberId는 primitive FK(엔티티 참조 아님).
- **DTO**: 모든 요청/응답이 Java record. `from(Entity)` 팩토리 메서드로 변환.
- **커스텀 Validator**: `ProductNameValidator`, `OptionNameValidator` — static 유틸리티. 컨트롤러에서 직접 호출.
- **예외 처리**: 글로벌 핸들러 없음. 각 컨트롤러에 `@ExceptionHandler(IllegalArgumentException.class)` → 400.
- **DB**: Flyway 마이그레이션 (`src/main/resources/db/migration/`). 로컬/테스트는 H2, 운영은 MySQL.

### 테스트 구조 (`src/test/java/gift/acceptance/`)

- `context/` — `ScenarioContext`(시나리오 간 상태 전달), `CucumberSpringConfiguration`(테스트 부트), `CucumberHooks`(데이터 초기화)
- `steps/` — Given/When/Then step definitions (도메인별 분리)
- `suite/` — Cucumber runner 클래스 (feature별 1개)
- `support/` — `DataManipulator`(테스트 데이터 정리), test repository 인터페이스

외부 의존성(`KakaoLoginClient`, `KakaoMessageClient`)은 `@MockBean`으로 격리.

---

# Refactoring Mission

## 핵심 원칙

리팩터링은 **외부 작동을 바꾸지 않고 내부 구조를 개선하는 작업**이다.

- 기능을 추가하지 않는다.
- 요구 사항을 바꾸지 않는다.
- 코드의 의미를 더 분명하게 만든다.
- 다음 변경을 더 쉽게 만든다.

## 구조 변경 vs 작동 변경

**구조 변경** (이번 Phase 1 범위)

- 이름 바꾸기, 메서드 추출, 클래스 추출
- 패키지 이동, 의존성 정리, 계층 분리
- 목표: 읽기 쉽고 바꾸기 쉬운 구조

**작동 변경** (Phase 2 범위 - 지금은 하지 않는다)

- 정책 변경, 예외 처리 방식 변경
- 트랜잭션 경계 변경, 누락된 기능 구현
- 목표: 사용자 관점에서 달라지는 결과

**구조 변경과 작동 변경을 절대 섞지 않는다. 한 커밋에는 둘 중 하나만 담는다.**

## Phase 1: 리팩터링 준비하기

목표: 변경하기 쉬운 상태 만들기. 구조 변경만 수행한다.

### 작업 범위

1. **스타일 정리** - 프로젝트 전반의 스타일 불일치를 일관되게 정리. 작동 변경 없음.
2. **불필요한 코드 제거** - IDE/정적 분석 기준 미사용 항목 제거. 삭제 전 근거 확인 필수.
3. **서비스 계층 추출** - Controller → Service 로직 이동. Controller는 검증과 위임만. 신규 기능 추가 금지.

## AI 협업 규율

이 프로젝트에서 AI는 초안을 만드는 도구이다. 설계와 검증 책임은 개발자에게 있다.

### MUST

- 코드 수정 전 README.md 체크리스트에 다음 작업이 적혀 있어야 한다.
- 변경 후 전체 테스트 통과를 확인한다. (`./gradlew test`)
- 커밋은 목적 1개. git diff를 보고 30초 안에 의도를 설명할 수 없으면 더 쪼갠다.
- AI 활용 내역을 README.md에 기록한다.

### MUST NOT

- AI가 요청하지 않은 작동을 추가하려 하면 즉시 중단한다.
- 테스트를 회피하거나 비활성화하지 않는다.
- 구조 변경과 작동 변경을 한 커밋에 섞지 않는다.
- "행동만 맞으면 된다"로 끝내지 않는다.

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

타입: `style`, `refactor`, `test`, `fix`, `docs`, `chore`

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
