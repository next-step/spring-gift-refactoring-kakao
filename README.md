# spring-gift-refactoring

## 목표

작동을 바꾸기 쉬운 상태를 만든다. 구조 변경을 통해 변경 난이도를 낮추되 작동은 유지한다. 이 단계에서는 신규 기능을 추가하지 않는다.

## 계획

1. 프로그래밍 요구 사항을 바탕으로 claude.md 파일 생성하기
2. 기능 요구 사항 작성하기
3. 각 기능 요구 사항에 맞게 skill 만들기
4. 테스트 코드 작성 skill 만들기
5. 테스트 코드 작성하기
6. TDD 루프를 유지하며 기능 요구 사항 진행하기
7. README.md, prompt.md 정리
8. 카카오 API를 사용하기 위한 애플리케이션을 등록 진행

## 기능 요구 사항

### 1단계: 스타일 정리

- [x] 프로젝트 전반의 스타일 불일치를 찾아 일관되게 정리한다.
- [x] 스타일 정리로 인해 작동이 바뀌지 않아야 한다.

### 2단계: 불필요한 코드 제거 (작동 변경 없음)

- [x] IDE 또는 정적 분석 도구가 "미사용"으로 표시하는 항목을 제거한다.
- [x] 단, 삭제 전에 반드시 근거를 확인한다.
  - 주변 주석 또는 TODO에 의도가 있는가
  - git blame으로 누가 왜 추가했는가
  - 이후 단계(작동 변경)와 충돌하지 않는가

### 3단계: 서비스 계층 추출 (구조 변경, 작동 변경 없음)

- [x] Controller의 비즈니스 로직을 Service로 이동한다.
- [x] Controller는 요청 검증과 위임만 담당하도록 얇게 만든다.

---

## AI 활용 방식

### 사용 도구

- **Claude Code** (CLI) — 코드 탐색, 편집, 빌드 검증까지 하나의 세션에서 수행

### 활용 흐름

1. **CLAUDE.md 작성** — 프로젝트 컨벤션, 아키텍처, 빌드 명령어를 정리하여 AI가 코드베이스를 이해할 수 있는 맥락을 제공했다.
2. **Skill 정의** — 각 단계(스타일 정리, 불필요한 코드 제거, 서비스 추출)와 테스트 작성을 `.claude/skills/` 아래 SKILL.md로 정의했다. Skill에는 대상 범위, 정리 기준, 제약 조건을 명시하여 AI가 범위를 벗어나지 않도록 했다.
3. **Skill 실행** — `/write-test`, `/style-fix`, `/dead-code`, `/extract-service` 명령으로 각 단계를 호출했다. AI는 항목별로 수정 → `./gradlew build` → 다음 항목 순서로 진행하며 빌드가 깨지지 않는지 매번 확인했다.
4. **사람이 검토하고 커밋** — AI는 커밋 메시지만 제안하고, 실제 커밋은 직접 수행했다. 불필요한 변경(주석 삭제 등)은 되돌리도록 지시하여 의도하지 않은 변경을 방지했다.

## 코드 수정 내역

### 테스트 코드 작성

리팩터링 전 현재 작동을 보호하는 테스트를 먼저 작성했다.

- **단위 테스트** — `Member.chargePoint/deductPoint`, `Option.subtractQuantity`, `ProductNameValidator`, `OptionNameValidator`의 정상/에러 케이스
- **통합 테스트** — RestAssured + `@Sql`(setup-data.sql, cleanup.sql) 기반 Given/When/Then 스타일로 6개 컨트롤러(Member, Category, Product, Option, Wish, Order) 엔드포인트 검증

### 1단계: 스타일 정리

| 항목 | 내용 |
|------|------|
| `@Autowired` 제거 | Spring 4.3+ 단일 생성자 규칙에 따라 4개 파일에서 제거 |
| `var` → 명시적 타입 | WishController, OrderController, KakaoMessageClient에서 명시적 타입 선언으로 통일 |
| 로컬 `final` 제거 | 필드의 `private final`은 유지, 로컬 변수의 `final`은 제거하여 일관성 확보 |
| 에러 메시지 한글 통일 | 영어 예외 메시지(`"Email is already registered."` 등)를 한글로 변환 |
| 필드 빈 줄 정리 | Member 엔티티의 어노테이션 없는 필드 사이 불필요한 빈 줄 제거 |
| `/* */` → `/** */` | OptionController의 클래스 레벨 블록 주석을 JavaDoc으로 변환 |
| import 정렬 | `gift` → `jakarta` → `io/com` → `org` → `java` 순서로 그룹 간 빈 줄 추가 |

### 2단계: 불필요한 코드 제거

| 항목 | 내용 |
|------|------|
| 미사용 메서드 제거 | `Product.getOptions()` — 프로젝트 어디에서도 호출되지 않음을 grep으로 확인 후 제거 |
| 자명한 주석 제거 | `// primitive FK`, `// point deduction for order payment` 등 필드명/메서드명으로 충분한 주석 제거 |
| `Collectors.toList()` 간소화 | Java 16+ 내장 `Stream.toList()`로 대체, 미사용 import 제거 |

### 3단계: 서비스 계층 추출

7개 도메인별 Service 클래스를 생성하고 Controller의 비즈니스 로직을 이동했다.

| Service | 추출 원본 | 역할 |
|---------|----------|------|
| `MemberService` | MemberController, AdminMemberController | 회원 등록/로그인/CRUD, JWT 발급 |
| `KakaoAuthService` | KakaoAuthController | 카카오 OAuth 토큰 교환 + 회원 자동 등록 + JWT 발급 |
| `CategoryService` | CategoryController | 카테고리 CRUD |
| `ProductService` | ProductController, AdminProductController | 상품명 검증 + 카테고리 조회 + 상품 CRUD |
| `OptionService` | OptionController | 옵션명 검증 + 중복 검증 + 최소 1개 규칙 + CRUD |
| `WishService` | WishController | 위시 조회/추가(중복 검증)/삭제(소유권 확인) |
| `OrderService` | OrderController | 재고 차감 + 포인트 차감 + 주문 저장 + 카카오 알림 |

Controller에는 HTTP 매핑, 요청 바인딩, `ResponseEntity` 생성만 남기고, Repository 호출과 비즈니스 검증은 모두 Service로 이동했다. 변경 작업을 수행하는 Service 메서드에는 `@Transactional`을, 조회 메서드에는 `@Transactional(readOnly = true)`를 부여했다.

## 학습한 점

### Skill 기반 작업 분할의 효과

각 리팩터링 단계를 Skill 파일로 명확하게 정의한 뒤 AI에게 실행을 맡기는 방식이 효과적이었다. Skill에 **대상 범위**, **정리 기준**, **제약 조건**을 구체적으로 명시하면 AI가 범위를 벗어나는 변경을 하지 않았다. 반대로 기준이 모호하면 의도하지 않은 변경(예: 살려야 할 주석 삭제)이 발생했고, 이를 되돌리는 과정에서 Skill 정의의 구체성이 결과 품질을 좌우한다는 점을 체감했다.

### 구조 변경과 작동 변경의 분리

스타일 정리 → 불필요한 코드 제거 → 서비스 추출 순서로 진행하면서, 각 단계가 **작동을 바꾸지 않는다**는 원칙을 지켰다. 매 변경 후 `./gradlew build`로 전체 테스트를 돌려 기존 작동이 유지됨을 확인했다. 테스트를 먼저 작성해두었기 때문에 구조 변경 시 안전망 역할을 했다.

### 테스트가 리팩터링의 전제 조건

RestAssured 통합 테스트를 SQL 기반 데이터 셋업으로 작성해두니, 서비스 계층 추출 과정에서 내부 구조가 크게 바뀌어도 테스트가 그대로 통과했다. 테스트가 API의 외부 동작만 검증하도록 작성했기 때문에 내부 리팩터링에 영향을 받지 않았다. 리팩터링 전에 테스트를 갖추는 것이 필수적이라는 점을 실감했다.

### AI 산출물은 초안

AI가 생성한 코드를 그대로 받아들이지 않고, 변경 내역을 검토한 뒤 커밋하는 흐름이 중요했다. 실제로 불필요한 주석 삭제 건에서 AI의 판단과 사람의 판단이 달랐고, 되돌리기를 지시하여 바로잡았다. AI는 반복적이고 기계적인 작업(import 정렬, var 치환, Service 보일러플레이트 생성)에 강했고, 의도 판단이 필요한 부분에서는 사람의 개입이 필요했다.
