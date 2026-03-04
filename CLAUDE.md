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
├── member/                   # 회원 도메인
├── option/                   # 상품 옵션 도메인
├── order/                    # 주문 도메인
├── product/                  # 상품 도메인
└── wish/                     # 위시리스트 도메인
```

각 도메인 패키지는 Entity, Repository, Controller, DTO를 포함한다.
**현재 Service 계층이 없으며**, Controller에 비즈니스 로직이 위치해 있다.
→ 리팩터링의 핵심 목표: **Service Layer 추출**

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
- [ ] 수정 전 테스트를 실행했는가?
- [ ] 커밋 메시지가 AngularJS Convention을 따르는가?
- [ ] `git diff`가 30초 내에 설명 가능한 크기인가?
- [ ] README.md 체크리스트를 업데이트했는가?
- [ ] 요청받지 않은 변경을 포함하고 있지 않은가?
- [ ] 변경 파일 수가 최소한인가?
