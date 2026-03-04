# CLAUDE.md — 페어 프로그래밍 파트너 행동 규약

이 문서는 AI가 본 프로젝트에서 **생각하고 행동하는 방식**을 정의한다.
모든 코드 수정은 이 규약을 따른다.

---

## 1. README.md 기반 작업 프로세스

README.md는 이 프로젝트의 **단일 진실 공급원(Single Source of Truth)**이다.

### 선 계획 후 실행
- 코드를 한 줄이라도 수정하기 **전에**, README.md의 `## 구현할 기능 목록`에 작업 단위를 정의한다.
- 체크리스트(`- [ ]` / `- [x]`)로 진행 상태를 추적한다.
- 체크리스트 항목 하나 = 커밋 하나.

### 작업 기록
- 코드 수정의 근거, 선택한 전략, 학습한 내용을 README.md에 실시간으로 기록한다.
- `## 진행 기록` 섹션에 날짜와 함께 의사결정 로그를 남긴다.

### 작업 흐름
```
1. README.md에 다음 작업 항목 정의
2. 테스트 확인 (Red/Green 상태 파악)
3. 코드 수정
4. 테스트 통과 확인
5. README.md 체크리스트 업데이트
6. 코드 커밋
7. Phase 완료 시 AI 활용 기록을 README.md에 추가
8. 문서 변경은 별도 docs 커밋으로 분리
```

---

## 2. 리팩터링 원칙

### 구조와 작동의 엄격한 분리
- **Refactor 커밋**: 동작 변경 없이 구조만 변경한다. (테스트 결과 동일)
- **Feature 커밋**: 새로운 동작을 추가한다.
- 하나의 커밋에 Refactor과 Feature를 **절대 섞지 않는다**.

### 구조 변경과 작동 변경의 분리
- **구조 변경 커밋** (`refactor`): 외부 동작이 동일함을 기존 테스트로 증명한다.
- **작동 변경 커밋** (`feat`, `fix`): 반드시 **상태 재조회 테스트**를 동반한다.
- 작동 변경 시 예외 발생 확인만으로는 불충분하다. 변경된 상태를 조회 API로 재확인해야 한다.

### 단계적 접근 (순서 엄수)
```
Phase 1: 스타일 정리
  - 코드 포맷팅, 네이밍 컨벤션 통일
  - ktlint / checkstyle 적용

Phase 2: 미사용 코드 제거
  - 제거 전 git blame, 주변 주석, TODO 확인
  - 의도가 불분명하면 제거하지 않고 TODO로 남김

Phase 3: Service Layer 추출
  - Controller → 얇게(Thin) 유지 (요청 수신 + 응답 반환만)
  - 비즈니스 로직 → Service 클래스로 이동
  - Repository 호출 → Service에서만 수행

Phase 6: 트랜잭션 경계 세우기
  - 트랜잭션 변경은 구조 변경과 작동 변경을 반드시 분리하여 커밋
  - 구조 변경(refactor): 선언 위치 이동, 단일 Repository 호출에 명시적 추가
  - 작동 변경(feat): 복합 쓰기에 원자성 부여, propagation/isolation 변경

Phase 7: 누락된 작동 구현
  - TODO/FIXME 탐색으로 미구현 기능 식별
  - 테스트를 먼저 작성한 후 기능 구현 (feat 커밋)
  - 작동 변경이므로 상태 재조회 테스트 필수

Phase 8: 도메인 책임 되찾기
  - Service에 있는 도메인 로직을 Entity로 이동
  - 검증 로직, 상태 변경 로직 등 Entity가 스스로 판단해야 할 책임 식별
  - 최소 2개 이상의 책임 이동 개선
```

### 작동 유지 보장
- 각 단계가 끝날 때마다 `./gradlew test`로 기존 동작이 유지됨을 확인한다.
- 테스트가 없는 경우, 리팩터링 전에 현재 동작을 검증하는 테스트를 먼저 작성한다.

---

## 3. 품질 및 검증

### TDD 루프
```
수정 전: ./gradlew test → 현재 상태 확인
수정 중: 실패하는 테스트 먼저 작성 (Feature일 경우)
수정 후: ./gradlew test → 전체 통과 확인
```

### 커밋 컨벤션 (AngularJS Commit Convention)
```
<type>(<scope>): <subject>

<body>

<footer>
```

**type 종류:**
| type | 용도 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 코드 구조 변경 |
| `style` | 포맷팅, 세미콜론 등 코드 의미 변경 없음 |
| `test` | 테스트 추가/수정 |
| `docs` | 문서 변경 |
| `chore` | 빌드, 설정 등 기타 변경 |

**scope 예시:** `product`, `order`, `auth`, `member`, `category`, `option`, `wish`

**커밋 크기 기준:** `git diff`를 30초 내에 설명할 수 있어야 한다.

**금지 항목:** 커밋 메시지에 `Co-Authored-By` 줄을 포함하지 않는다.

### 작동 변경 증거 (Evidence)

작동을 변경하는 커밋은 반드시 **변경 전 3줄 명세**를 먼저 정의한다:

```
1. 무엇을 바꾸는가: (예: 주문 시 옵션 재고를 차감한다)
2. 무엇을 안 바꾸는가: (예: 주문 생성 자체의 흐름은 동일)
3. 어떻게 증명하는가: (예: 주문 후 옵션 조회 API로 재고 감소 확인)
```

**증거 테스트 원칙:**
- 예외 발생 확인만으로는 **불충분**하다.
- 반드시 **상태 재조회**(조회 API 호출)로 변경 결과를 검증한다.
- Given-When-Then에서 **Then은 상태 조회**여야 한다.

### ADR (Architecture Decision Record)

아키텍처 결정은 `docs/adr/` 디렉토리에 ADR로 기록한다.

**ADR 작성 조건** (아래 중 하나라도 해당하면 작성):
1. **트레이드오프가 존재**: 선택지가 2개 이상이고 각각 장단점이 있는 경우
2. **팀 규칙/경계 결정**: 계층 간 호출 규칙, 패키지 구조 등 팀 전체에 영향
3. **테스트 전략 결정**: 어떤 수준에서 무엇을 테스트할지 결정

**파일 규칙:**
- 위치: `docs/adr/NNNN-제목.md`
- 상태: `proposed` → `accepted` / `rejected` / `superseded`
- 템플릿은 `adr` 스킬 참조

### 미사용 코드 제거 체크리스트
- [ ] `git blame`으로 코드 작성 맥락 확인
- [ ] 주변 주석/TODO 확인
- [ ] 다른 파일에서 참조 여부 확인 (Grep)
- [ ] 제거 후 빌드/테스트 통과 확인

---

## 4. 빌드 및 테스트 명령어

```bash
# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 애플리케이션 실행
./gradlew bootRun

# Java 코드 스타일 검사 (Checkstyle, Google Java Style)
./gradlew checkstyleMain

# Kotlin 코드 포맷팅 (ktlint)
./gradlew ktlintFormat

# Kotlin 코드 스타일 검사
./gradlew ktlintCheck

# 클린 빌드
./gradlew clean build
```

---

## 5. 프로젝트 기술 스택

| 항목 | 버전/기술 |
|------|-----------|
| Framework | Spring Boot 3.5.9 |
| Language | Java 21 + Kotlin 1.9.25 |
| Build Tool | Gradle 8.14 (Kotlin DSL) |
| DB Migration | Flyway 12.0.1 |
| Database | MySQL (prod) / H2 (dev/test) |
| Auth | JWT (JJWT 0.13.0) + Kakao OAuth |
| Template | Thymeleaf |
| Linter | Checkstyle 10.21.4 (Java, Google Style) + ktlint 14.0.1 (Kotlin) |
| Test | JUnit 5 |

---

## 6. 프로젝트 구조

```
src/main/java/gift/
├── Application.java          # Spring Boot 진입점
├── auth/                     # 인증 (JWT, Kakao OAuth)
├── category/                 # 카테고리 도메인
├── error/                    # 공통 에러 처리 인프라
├── member/                   # 회원 도메인
├── option/                   # 상품 옵션 도메인
├── order/                    # 주문 도메인
├── product/                  # 상품 도메인
└── wish/                     # 위시리스트 도메인
```

각 도메인 패키지는 Entity, Repository, Service, Controller, DTO를 포함한다.
Service 계층이 추출되어 비즈니스 로직을 담당하고, Controller는 요청/응답만 처리한다.
→ 다음 목표: **트랜잭션 경계, 누락 작동, 도메인 책임 이동**

---

## 7. AI 행동 금지 사항

### 대량 변경 금지
- 한 번에 여러 파일을 동시에 대량 변경하지 않는다.
- 한 커밋에서 변경하는 파일 수를 최소화하고, 관련된 파일만 함께 수정한다.
- 변경 범위가 넓어질 경우 반드시 작업을 쪼개어 순차적으로 진행한다.

### 요청하지 않은 기능 추가 금지
- 사용자가 명시적으로 요청한 작업**만** 수행한다.
- "이왕 하는 김에" 식의 추가 개선, 리팩터링, 주석 추가, 타입 어노테이션 보강 등을 임의로 하지 않는다.
- 필요하다고 판단되는 추가 작업이 있으면 코드를 수정하기 전에 사용자에게 먼저 제안하고 승인을 받는다.

---

## 8. AI 행동 체크리스트

코드를 수정하기 전, 매번 아래를 확인한다:

- [ ] README.md에 이번 작업이 정의되어 있는가?
- [ ] 이 커밋은 Refactor인가, Feature인가? (혼합 금지)
- [ ] 이 커밋은 구조 변경인가, 작동 변경인가? (혼합 금지)
- [ ] 작동 변경이면 상태 검증 테스트가 있는가?
- [ ] 변경 전에 '무엇을 바꾸는지 / 안 바꾸는지 / 증명 방법'을 정의했는가?
- [ ] ADR이 필요한 결정인가?
- [ ] 수정 전 테스트를 실행했는가?
- [ ] 커밋 메시지가 AngularJS Convention을 따르는가?
- [ ] `git diff`가 30초 내에 설명 가능한 크기인가?
- [ ] README.md 체크리스트를 업데이트했는가?
- [ ] 요청받지 않은 변경을 포함하고 있지 않은가?
- [ ] 변경 파일 수가 최소한인가?
