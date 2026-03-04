# spring-gift-refactoring-kakao

## Step 1: 구조 변경 (완료)

구조 변경을 통해 변경 난이도를 낮추되 작동은 유지한다.

상세 기록: [`docs/step1-plan.md`](docs/step1-plan.md) | [`docs/step1-adr.md`](docs/step1-adr.md) | [`docs/step1-prompt.md`](docs/step1-prompt.md)

### 계획

1. 프로그래밍 요구 사항을 바탕으로 claude.md 파일 생성하기
2. 기능 요구 사항 작성하기
3. 각 기능 요구 사항에 맞게 skill 만들기
4. 테스트 코드 작성 skill 만들기
5. 테스트 코드 작성하기
6. TDD 루프를 유지하며 기능 요구 사항 진행하기
7. README.md, prompt.md 정리
8. 카카오 API를 사용하기 위한 애플리케이션을 등록 진행

### 기능 요구 사항

#### 1단계: 스타일 정리

- [x] 프로젝트 전반의 스타일 불일치를 찾아 일관되게 정리한다.
- [x] 스타일 정리로 인해 작동이 바뀌지 않아야 한다.

#### 2단계: 불필요한 코드 제거 (작동 변경 없음)

- [x] IDE 또는 정적 분석 도구가 "미사용"으로 표시하는 항목을 제거한다.
- [x] 단, 삭제 전에 반드시 근거를 확인한다.
  - 주변 주석 또는 TODO에 의도가 있는가
  - git blame으로 누가 왜 추가했는가
  - 이후 단계(작동 변경)와 충돌하지 않는가

#### 3단계: 서비스 계층 추출 (구조 변경, 작동 변경 없음)

- [x] Controller의 비즈니스 로직을 Service로 이동한다.
- [x] Controller는 요청 검증과 위임만 담당하도록 얇게 만든다.

### AI 활용 방식

- **Claude Code** (CLI) — 코드 탐색, 편집, 빌드 검증까지 하나의 세션에서 수행
- **CLAUDE.md 작성** — 프로젝트 컨벤션, 아키텍처, 빌드 명령어를 정리하여 AI가 코드베이스를 이해할 수 있는 맥락을 제공했다.
- **Skill 정의** — 각 단계(스타일 정리, 불필요한 코드 제거, 서비스 추출)와 테스트 작성을 `.claude/skills/` 아래 SKILL.md로 정의했다. Skill에는 대상 범위, 정리 기준, 제약 조건을 명시하여 AI가 범위를 벗어나지 않도록 했다.
- **Skill 실행** — `/write-test`, `/style-fix`, `/dead-code`, `/extract-service` 명령으로 각 단계를 호출했다. AI는 항목별로 수정 → `./gradlew build` → 다음 항목 순서로 진행하며 빌드가 깨지지 않는지 매번 확인했다.
- **사람이 검토하고 커밋** — AI는 커밋 메시지만 제안하고, 실제 커밋은 직접 수행했다. 불필요한 변경(주석 삭제 등)은 되돌리도록 지시하여 의도하지 않은 변경을 방지했다.

### 코드 수정 내역

#### 테스트 코드 작성

리팩터링 전 현재 작동을 보호하는 테스트를 먼저 작성했다.

- **단위 테스트** — `Member.chargePoint/deductPoint`, `Option.subtractQuantity`, `ProductNameValidator`, `OptionNameValidator`의 정상/에러 케이스
- **통합 테스트** — RestAssured + `@Sql`(setup-data.sql, cleanup.sql) 기반 Given/When/Then 스타일로 6개 컨트롤러(Member, Category, Product, Option, Wish, Order) 엔드포인트 검증
- **경계 조건 테스트** — 재고 부족 주문, 포인트 부족 주문, "카카오" 포함 상품명, 존재하지 않는 리소스(상품/카테고리/옵션/위시) 접근 등 실패 케이스

#### 1단계: 스타일 정리

| 항목 | 내용 |
|------|------|
| `@Autowired` 제거 | Spring 4.3+ 단일 생성자 규칙에 따라 4개 파일에서 제거 |
| `var` → 명시적 타입 | WishController, OrderController, KakaoMessageClient에서 명시적 타입 선언으로 통일 |
| 로컬 `final` 제거 | 필드의 `private final`은 유지, 로컬 변수의 `final`은 제거하여 일관성 확보 |
| 에러 메시지 한글 통일 | 영어 예외 메시지(`"Email is already registered."` 등)를 한글로 변환 |
| 필드 빈 줄 정리 | Member 엔티티의 어노테이션 없는 필드 사이 불필요한 빈 줄 제거 |
| `/* */` → `/** */` | OptionController의 클래스 레벨 블록 주석을 JavaDoc으로 변환 |
| import 정렬 | `gift` → `jakarta` → `io/com` → `org` → `java` 순서로 그룹 간 빈 줄 추가 |

#### 2단계: 불필요한 코드 제거

| 항목 | 내용 |
|------|------|
| 미사용 메서드 제거 | `Product.getOptions()` — 프로젝트 어디에서도 호출되지 않음을 grep으로 확인 후 제거 |
| 자명한 주석 제거 | `// primitive FK`, `// point deduction for order payment` 등 필드명/메서드명으로 충분한 주석 제거 |
| `Collectors.toList()` 간소화 | Java 16+ 내장 `Stream.toList()`로 대체, 미사용 import 제거 |

#### 3단계: 서비스 계층 추출

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

Controller에는 HTTP 매핑, 요청 바인딩, `ResponseEntity` 생성만 남기고, Repository 호출과 비즈니스 검증은 모두 Service로 이동했다. `@Transactional`은 작동 변경에 해당하므로 이번 단계에서는 추가하지 않았다.

#### 코드 리뷰 반영

| 항목 | 내용 |
|------|------|
| `@Transactional` 제거 | 서비스 추출 시 임의로 추가한 `@Transactional`을 제거 — 작동 변경 없는 구조 정리 원칙 준수 |
| `@Valid` 복원 | WishController에서 서비스 추출 시 누락된 `@Valid` 애노테이션 복원 |
| 미사용 의존성 제거 | OrderService의 `WishRepository`, ProductService의 `validateNameWithKakao()` 제거 |
| 주석 복원 | 원본 컨트롤러의 주석(order flow, check product 등)을 서비스 계층에 복원 |
| 카카오 설정 기본값 | `${KAKAO_CLIENT_ID:}` 형태로 빈 기본값 추가 — `.env` 없는 환경에서도 부팅 보장 |
| 경계 조건 테스트 추가 | 재고 부족, 포인트 부족, 카카오 상품명, 존재하지 않는 리소스 등 실패 케이스 테스트 보강 |

### 학습한 점

#### Skill 기반 작업 분할의 효과

각 리팩터링 단계를 Skill 파일로 명확하게 정의한 뒤 AI에게 실행을 맡기는 방식이 효과적이었다. Skill에 **대상 범위**, **정리 기준**, **제약 조건**을 구체적으로 명시하면 AI가 범위를 벗어나는 변경을 하지 않았다. 반대로 기준이 모호하면 의도하지 않은 변경(예: 살려야 할 주석 삭제)이 발생했고, 이를 되돌리는 과정에서 Skill 정의의 구체성이 결과 품질을 좌우한다는 점을 체감했다.

#### 구조 변경과 작동 변경의 분리

스타일 정리 → 불필요한 코드 제거 → 서비스 추출 순서로 진행하면서, 각 단계가 **작동을 바꾸지 않는다**는 원칙을 지켰다. 매 변경 후 `./gradlew build`로 전체 테스트를 돌려 기존 작동이 유지됨을 확인했다. 코드 리뷰에서 `@Transactional` 추가가 작동 변경에 해당한다는 지적을 받고 제거했고, 에러 메시지 한글화도 스타일이 아닌 작동 변경이었음을 인지했다. 구조 변경과 작동 변경을 한 커밋에 섞지 않는 것이 중요하다는 점을 체감했다.

#### 테스트가 리팩터링의 전제 조건

RestAssured 통합 테스트를 SQL 기반 데이터 셋업으로 작성해두니, 서비스 계층 추출 과정에서 내부 구조가 크게 바뀌어도 테스트가 그대로 통과했다. 테스트가 API의 외부 동작만 검증하도록 작성했기 때문에 내부 리팩터링에 영향을 받지 않았다. 리팩터링 전에 테스트를 갖추는 것이 필수적이라는 점을 실감했다.

#### AI 산출물은 초안

AI가 생성한 코드를 그대로 받아들이지 않고, 변경 내역을 검토한 뒤 커밋하는 흐름이 중요했다. 실제로 불필요한 주석 삭제 건에서 AI의 판단과 사람의 판단이 달랐고, 되돌리기를 지시하여 바로잡았다. AI는 반복적이고 기계적인 작업(import 정렬, var 치환, Service 보일러플레이트 생성)에 강했고, 의도 판단이 필요한 부분에서는 사람의 개입이 필요했다.

---

## Step 2: 작동 변경 (완료)

Step 1에서 정리한 구조 위에 작동 변경을 수행한다. 모든 변경은 테스트로 증명하고, 상태 변화를 재조회하여 검증한다.

상세 계획: [`docs/step2-plan.md`](docs/step2-plan.md) | [`docs/step2-adr.md`](docs/step2-adr.md)

### 계획

1. 작동 변경 대상 파악 및 `step2-plan.md` 작성
2. 테스트 전략 결정 (Mock vs 통합 테스트)
3. 0단계: 모든 단계의 Red 테스트를 먼저 작성
4. 1~6단계: 테스트를 Green으로 전환하며 구현 진행
5. 7~9단계: 도메인 책임 개선 (구조 변경)
6. 매 단계 `./gradlew test` 전체 통과 확인
7. ADR 작성, README 정리

### 기능 요구 사항

#### 0단계: 테스트 코드 작성 (모든 단계 선행)

모든 단계의 기대 동작을 테스트로 먼저 정의한다. 테스트는 Red 상태에서 시작하고, 이후 단계에서 구현하면서 Green으로 전환한다.

**테스트 전략:** Mock 기반 서비스 단위 테스트(`@Mock` + `@InjectMocks`)는 사용하지 않는다. Mock 테스트는 "메서드가 호출됐는지"만 확인할 뿐, "실제로 DB 상태가 바뀌었는지"는 알 수 없다. 이 프로젝트의 검증 원칙은 **"상태를 재조회하여 검증"**이므로, 실제 DB를 사용하는 통합 테스트(`@SpringBootTest` + `@Sql`)와 외부 의존성 없는 도메인 단위 테스트로 구성한다. 단, 외부 API(`KakaoLoginClient`)처럼 테스트 환경에서 호출할 수 없는 의존성은 `@MockBean`으로 대체한다.

**통합 테스트:**
- [x] `OrderControllerTest` — 포인트 부족 시 재고 롤백 검증 (→ 1단계) — Red: `@Transactional` 없어 재고 롤백 안 됨
- [x] `KakaoAuthServiceTest` — 신규/기존 회원 카카오 로그인 검증, `KakaoLoginClient`만 `@MockitoBean` (→ 1단계) — Green: 기존 기능 검증 안전망
- [x] `OrderControllerTest` — 주문 생성 후 위시 자동 삭제 검증 (→ 2단계) — Red: `// TODO: cleanup wish` 미구현

**도메인 단위 테스트:**
- [x] `OptionTest` — `calculateTotalPrice` 단위 테스트 — 4단계에서 메서드와 함께 작성, Green

#### 1단계: @Transactional 적용

- [x] `OrderService.createOrder()`에 `@Transactional` 추가 → `createOrderInsufficientPointsRollsBackStock` Green
- [x] `KakaoAuthService.loginWithKakao()`에 `@Transactional` 추가 → `KakaoAuthServiceTest` Green 유지
- 나머지 서비스는 모두 단일 저장/삭제 작업이라 `@Transactional` 불필요 확인

#### 2단계: 주문 시 위시리스트 자동 삭제

- [x] `WishRepository.deleteByMemberIdAndProductId` 추가
- [x] `OrderService`에서 주문 저장 후 위시 삭제 호출 → `createOrderDeletesWish` Green

#### 3단계: 예외 삼킴(swallow) 로그 추가

- [x] `OrderService.sendKakaoMessageIfPossible` — `catch (Exception ignored)` → `catch (Exception e)` + `log.warn(...)` (회원 ID, 주문 ID 포함)
- [x] `AuthenticationResolver.extractMember` — `catch (Exception e) { return null; }` → 인증 실패 debug 로그 추가

#### 4단계: 가격 계산 도메인 메서드 추가

- [x] `Option.calculateTotalPrice(int quantity)` 메서드 추가 + `OptionTest` Green (작동 변경)
- [x] `OrderService`의 가격 계산을 `Option.calculateTotalPrice`로 위임 (구조 변경)

#### 5단계: 이메일 중복 검증 로직 통합

- [x] `MemberService`의 `register()`와 `create()`에 중복된 이메일 체크를 `validateEmailNotDuplicated`로 추출 (구조 변경)

#### 6단계: @RestControllerAdvice 도입

- [x] `GlobalExceptionHandler` 생성 — `@RestControllerAdvice(annotations = RestController.class)`로 REST API만 대상
  - `NoSuchElementException` → 404, `IllegalArgumentException` → 400, `IllegalStateException` → 403
- [x] REST 컨트롤러 6개에서 개별 try-catch, `@ExceptionHandler` 제거
- [x] View 컨트롤러(`/admin/...`)는 `@Controller`이므로 영향 없음 — 기존 try-catch 유지

#### 7단계: KakaoMessageClient가 Option을 받도록 변경

- [x] `sendToMe(accessToken, order, product)` → `sendToMe(accessToken, order, option)` 시그니처 변경
- [x] 가격 계산 중복 제거 — `product.getPrice() * order.getQuantity()` → `option.calculateTotalPrice(order.getQuantity())`
- [x] 디미터 법칙 개선 — `order.getOption().getName()` → `option.getName()`
- [x] `OrderService`에서 `Product` 추출 라인 제거, `option` 직접 전달

#### 8단계: 인증 null 체크 반복 제거

- [x] `AuthenticationResolver`를 `HandlerMethodArgumentResolver`로 전환 — 인증 실패 시 `ResponseStatusException(401)`
- [x] `WebMvcConfig` 생성 — resolver 등록
- [x] `OrderController`(2곳), `WishController`(3곳)에서 `@RequestHeader` + null 체크 제거, `Member` 직접 파라미터로 변경

#### 9단계: AdminProductController 상품명 검증 중복 제거

- [x] `AdminProductController`의 `ProductNameValidator` 직접 호출 제거, 서비스에 `allowKakao=true` 위임
- [x] `ProductService.create/update`에 `allowKakao` 오버로드 추가, `validateName` private으로 변경
- [x] `ProductServiceTest` — `allowKakao=true`이면 "카카오" 허용, `false`이면 거부 검증

### 코드 수정 내역

#### 0단계: 테스트 코드 작성

작동 변경 전 기대 동작을 테스트로 먼저 정의했다. Mock 기반 서비스 단위 테스트 대신 통합 테스트 + 도메인 단위 테스트 전략을 채택했다(ADR-004).

| 테스트 | 종류 | 초기 상태 | Red 이유 |
|--------|------|-----------|----------|
| `OrderControllerTest.createOrderInsufficientPointsRollsBackStock` | 통합 | Red | `@Transactional` 없어 재고 롤백 안 됨 |
| `KakaoAuthServiceTest` (신규/기존 회원) | 통합 | Green | 기존 기능 검증 안전망 |
| `OrderControllerTest.createOrderDeletesWish` | 통합 | Red | `// TODO: cleanup wish` 미구현 |
| `OptionTest.calculateTotalPrice` | 도메인 단위 | — | 메서드 미존재로 컴파일 에러, 4단계에서 함께 작성 |

#### 1단계: @Transactional 적용

| 항목 | 내용 |
|------|------|
| `OrderService.createOrder()` | `@Transactional` 추가 — 재고 차감·포인트 차감·주문 저장이 하나의 트랜잭션으로 묶임 |
| `KakaoAuthService.loginWithKakao()` | `@Transactional` 추가 — 회원 생성/토큰 갱신이 원자적으로 처리됨 |
| 나머지 5개 서비스 | 모두 단일 저장/삭제 작업이라 `@Transactional` 불필요 확인 (ADR-001) |

#### 2단계: 주문 시 위시리스트 자동 삭제

| 항목 | 내용 |
|------|------|
| `WishRepository` | `deleteByMemberIdAndProductId` 메서드 추가 |
| `OrderService` | `WishRepository` 의존성 추가, 주문 저장 후 `deleteByMemberIdAndProductId` 호출 |
| `// TODO: cleanup wish` | 구현 완료로 주석 제거 |

#### 3단계: 예외 삼킴 로그 추가

| 항목 | 내용 |
|------|------|
| `OrderService.sendKakaoMessageIfPossible` | `catch (Exception ignored)` → `catch (Exception e)` + `log.warn("카카오 메시지 전송 실패: memberId={}, orderId={}", ...)` |
| `AuthenticationResolver.extractMember` | `catch (Exception e) { return null; }` → `log.debug("인증 토큰 추출 실패: {}", e.getMessage())` 추가 (인증 실패는 정상 흐름이므로 debug 레벨) |

#### 4단계: 가격 계산 도메인 메서드 추가

| 항목 | 내용 |
|------|------|
| `Option.calculateTotalPrice(int quantity)` | 도메인 메서드 추가 — `product.getPrice() * quantity` (작동 변경) |
| `OrderService.createOrder()` | `option.getProduct().getPrice() * quantity` → `option.calculateTotalPrice(quantity)`로 위임 (구조 변경, ADR-002) |

#### 5단계: 이메일 중복 검증 로직 통합

| 항목 | 내용 |
|------|------|
| `MemberService` | `register()`와 `create()`에 중복된 `existsByEmail` 체크를 `validateEmailNotDuplicated` private 메서드로 추출 |

#### 6단계: @RestControllerAdvice 도입

| 항목 | 내용 |
|------|------|
| `GlobalExceptionHandler` 생성 | `@RestControllerAdvice(annotations = RestController.class)` — `@RestController`만 대상 (ADR-003) |
| 예외 매핑 | `NoSuchElementException` → 404, `IllegalArgumentException` → 400, `IllegalStateException` → 403 |
| REST 컨트롤러 정리 | 6개 컨트롤러에서 개별 try-catch, `@ExceptionHandler` 제거 |
| 테스트 상태코드 수정 | `IllegalArgumentException`이 500 → 400으로 변경되어 3개 테스트 기대값 수정 |
| admin 컨트롤러 보호 | `basePackages` 대신 `annotations`로 범위 제한 — `@Controller`인 admin 컨트롤러에 영향 없음 |

#### 7단계: KakaoMessageClient Option 시그니처 변경

| 항목 | 내용 |
|------|------|
| `KakaoMessageClient.sendToMe` | `Product` → `Option` 파라미터 변경, `calculateTotalPrice` 도메인 메서드 재사용 |
| `KakaoMessageClient.buildTemplate` | `order.getOption().getName()` → `option.getName()` 직접 접근 (디미터 법칙 개선) |
| `OrderService.sendKakaoMessageIfPossible` | `Product product = option.getProduct()` 제거, `option` 직접 전달 |

#### 8단계: 인증 null 체크 반복 제거

| 항목 | 내용 |
|------|------|
| `AuthenticationResolver` | `HandlerMethodArgumentResolver` 구현, 인증 실패 시 `ResponseStatusException(401)` |
| `WebMvcConfig` (신규) | `WebMvcConfigurer`로 resolver 등록 |
| `OrderController` | `AuthenticationResolver` 의존성 + null 체크 2곳 제거, `Member` 직접 파라미터 |
| `WishController` | 같은 방식으로 null 체크 3곳 제거 |

#### 9단계: AdminProductController 상품명 검증 중복 제거

| 항목 | 내용 |
|------|------|
| `AdminProductController` | `ProductNameValidator` 직접 호출 제거, `productService.create/update(... , true)` 위임 + try-catch로 폼 에러 UX 유지 |
| `ProductService` | `create/update`에 `allowKakao` 오버로드 추가, `validateName` private 변경 |
| `ProductServiceTest` (신규) | `allowKakao=true` → "카카오" 허용, `allowKakao=false` → 거부 검증 |

### 학습한 점

#### 테스트 전략은 검증 원칙에서 결정된다

처음에는 Mock 기반 서비스 단위 테스트(`@Mock` + `@InjectMocks`)를 계획했으나, "상태를 재조회하여 검증"이라는 원칙과 충돌했다. Mock 테스트는 `then(repository).should().delete(...)`처럼 호출 여부만 확인하고, 실제 DB 상태 변화는 알 수 없다. 통합 테스트(`@SpringBootTest` + `@Sql`)로 전환하니 주문 후 위시 목록을 재조회하여 0개임을 확인하는 식으로 실제 작동을 검증할 수 있었다.

#### Red 테스트 선행 작성의 한계

TDD의 "테스트 먼저" 원칙을 따라 0단계에서 모든 테스트를 선행 작성하려 했으나, 컴파일 에러를 일으키는 테스트(존재하지 않는 메서드 호출)는 전체 빌드를 깨뜨렸다. `OptionTest.calculateTotalPrice`는 `Option`에 메서드가 없어 컴파일 자체가 불가능했고, 4단계에서 메서드 구현과 함께 작성했다. 컴파일 언어에서 Red 테스트 선행 작성은 "실행 시 실패"와 "컴파일 에러"를 구분해야 한다.

#### @RestControllerAdvice 범위 지정의 함정

`@RestControllerAdvice(basePackages = "gift")`로 처음 구현했으나, 같은 패키지의 admin `@Controller`에도 적용되어 Thymeleaf 뷰 대신 JSON 에러가 반환될 수 있었다. `annotations = RestController.class`로 변경하여 `@RestController`만 대상으로 제한했다. 패키지 기반 필터링은 같은 패키지에 다른 성격의 컨트롤러가 공존할 때 위험하다.

#### 구조 변경과 작동 변경의 커밋 분리

4단계에서 `Option.calculateTotalPrice` 추가(작동 변경)와 `OrderService`에서의 위임(구조 변경)을 별도 커밋으로 분리했다. 한 커밋의 diff를 보고 30초 안에 의도를 설명할 수 있는지가 분리 기준이었다. 작동 변경 커밋에는 새 메서드와 테스트만, 구조 변경 커밋에는 호출부 교체만 담겼다.

#### 횡단 관심사는 프레임워크에 맡긴다

인증 null 체크가 컨트롤러 5곳에 반복되어 있었다. 직접 제거하려면 모든 메서드를 일일이 수정해야 했지만, Spring의 `HandlerMethodArgumentResolver`를 활용하니 인증 로직이 한 곳으로 모이고 컨트롤러는 `Member` 파라미터만 선언하면 됐다. 반복 코드를 발견했을 때 직접 추상화하기 전에 프레임워크가 제공하는 확장 포인트를 먼저 확인하는 것이 효과적이다.

#### 검증 중복은 불일치를 숨긴다

`AdminProductController`가 `allowKakao=true`로 검증하고, `ProductService`가 `allowKakao=false`로 다시 검증하고 있었다. 겉보기에는 "이중 안전장치"처럼 보이지만, 실제로는 admin에서 "카카오" 상품을 만들 수 없는 버그였다. 검증이 여러 레이어에 중복되면 파라미터 불일치가 숨겨지기 쉽다. 검증 책임을 한 곳(서비스)으로 모으고 호출부에서 정책(`allowKakao`)을 전달하는 방식이 안전하다.
