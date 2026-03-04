# spring-gift-refactoring

## 구현할 기능 목록

### Phase 0: 테스트 코드 작성
- [x] test(category): 카테고리 CRUD API 인수테스트 작성
- [x] test(member): 회원 가입/로그인 API 인수테스트 작성
- [x] test(product): 상품 CRUD API 인수테스트 작성
- [x] test(option): 상품 옵션 API 인수테스트 작성
- [x] test(wish): 위시리스트 API 인수테스트 작성
- [x] test(order): 주문 API 인수테스트 작성

### Phase 1: 스타일 정리
- [x] chore: Checkstyle 플러그인 및 Google Java Style 설정 추가
- [x] style(all): Google Java Style 기반 코드 스타일 정리 (import 순서, @Autowired, Collectors 등)

### Phase 2: 미사용 코드 제거
- [x] refactor(order): 미사용 WishRepository 필드 및 import 제거
- [x] 미사용 private 메서드: 전체 검토 완료, 미사용 없음
- [x] 주석 처리된 레거시 코드: 전체 검토 완료, 해당 없음
- [x] refactor(product): ProductRequest의 미사용 toEntity 메서드 및 import 제거

### Phase 3: Controller 내에 있는 Service Layer 추출
- [x] refactor(category): CategoryService 생성 및 비즈니스 로직 이동
- [x] refactor(member): MemberService 생성 및 비즈니스 로직 이동
- [x] refactor(product): ProductService 생성 및 비즈니스 로직 이동
- [x] refactor(option): OptionService 생성 및 비즈니스 로직 이동
- [x] refactor(wish): WishService 생성 및 비즈니스 로직 이동
- [x] refactor(order): OrderService 생성 및 비즈니스 로직 이동

### Phase 4: 에러 처리 구조화
- [x] feat(error): 공통 에러 인프라 및 CommonErrorCode 생성
- [x] feat: 도메인별 ErrorCode + Exception 생성 (6개 도메인)
- [x] refactor(category): Service/Controller의 기존 예외를 도메인 예외로 교체
- [x] refactor(member): Service/Controller의 기존 예외를 도메인 예외로 교체
- [x] refactor(product): Service/Controller의 기존 예외를 도메인 예외로 교체
- [x] refactor(option): Service/Controller의 기존 예외를 도메인 예외로 교체
- [x] refactor(wish): Service/Controller의 기존 예외를 도메인 예외로 교체
- [x] refactor(order): Service/Controller의 기존 예외를 도메인 예외로 교체

### Phase 5: 인수테스트 강화
- [x] fix(member): EMAIL_ALREADY_REGISTERED 상태 코드 400 → 409 교정
- [x] fix(option): DUPLICATE_OPTION_NAME 상태 코드 400 → 409 교정
- [x] fix(option): Option.subtractQuantity의 IllegalArgumentException을 도메인 예외로 교체
- [x] fix(member): Member.deductPoint/chargePoint의 IllegalArgumentException을 도메인 예외로 교체
- [x] test(product): 에러 케이스 인수테스트 추가
- [x] test(category): 에러 케이스 인수테스트 추가
- [x] test(member): 에러 케이스 인수테스트 추가
- [x] test(option): 에러 케이스 인수테스트 추가
- [x] test(wish): 에러 케이스 인수테스트 추가
- [x] test(order): 에러 케이스 인수테스트 추가
- [x] test(member): 에러 응답 code 필드 검증 추가
- [x] test(product): 에러 응답 code 필드 검증 추가
- [x] test(category): 에러 응답 code 필드 검증 추가
- [x] test(option): 에러 응답 code 필드 검증 추가
- [x] test(wish): 에러 응답 code 필드 검증 추가
- [x] test(order): 에러 응답 code 필드 검증 추가

## 구현 전략

### Phase 0: 테스트 코드 작성
- **목적**: 리팩터링 전 현재 동작을 보호하는 안전망 확보
- **접근**: 각 도메인의 Controller API를 대상으로 인수테스트 작성
- **범위**: 정상 요청/응답 흐름(Happy Path) 위주
- **도구**: SpringBootTest + MockMvc
- **순서**: 의존성 적은 도메인부터 (category → member → product → option → wish → order)
- **검증**: `./gradlew test` 전체 통과

### Phase 1: 스타일 정리
- **도구**: Checkstyle (Google Java Style 기반)
- **접근**: `./gradlew checkstyleMain`으로 위반 사항 확인 후 수정
- **주의**: 포맷팅 중 로직 변경 절대 금지
- **검증**: `./gradlew checkstyleMain` 위반 0건 + `./gradlew test` 통과 확인

### Phase 2: 미사용 코드 제거
- **접근**: IDE 인스펙션 → git blame 확인 → 제거
- **주의**: TODO/주석 의도 확인 필수, 의도 불분명 시 보류
- **검증**: 빌드 + 테스트 통과

### Phase 3: Controller 내에 있는 Service Layer 추출
- **순서**: 의존성 적은 도메인부터 (category → member → product → option → wish → order)
- **원칙**: Controller는 요청 수신 + 응답 반환만 담당
- **주의**: 로직 이동 시 기능 추가/변경 금지
- **검증**: 각 도메인 추출 후 테스트 통과

### Phase 4: 에러 처리 구조화
- **목적**: 산재된 `@ExceptionHandler`와 `IllegalArgumentException`/`NoSuchElementException`을 도메인별 에러 코드 체계로 통합
- **구조**: `ErrorCode` 인터페이스 → 도메인별 `{Domain}ErrorCode` enum → `BusinessException` → `GlobalExceptionHandler`
- **순서**: (1) 공통 인프라 생성 → (2) 도메인별 ErrorCode+Exception 생성 → (3) Service/Controller에서 기존 예외를 도메인 예외로 교체 → (4) Controller별 @ExceptionHandler 제거
- **원칙**: 에러 응답 형식 통일 (`{"code": "...", "message": "..."}`)
- **주의**: Admin Controller(`@Controller`)는 `@RestControllerAdvice` 적용 대상이 아니므로 기존 방식 유지. 교체 커밋은 도메인별로 분리하여 테스트 통과 확인
- **검증**: 각 커밋마다 `./gradlew test` + `./gradlew checkstyleMain` 통과

## 진행 기록

### 2026-02-26
#### 작업 내용
- 프로젝트 구조 분석 및 리팩터링 준비 작업 완료

#### 의사결정
- **프로젝트 현황 파악**: 전체 41개 소스 파일, 9개 Controller 확인. Service 계층이 전혀 없으며 Controller에 비즈니스 로직(Repository 호출, 검증, 외부 API 호출 등)이 집중되어 있음을 확인
- **CLAUDE.md 작성**: AI 페어 프로그래밍 파트너의 행동 규약 정의. README.md 기반 선 계획 후 실행 원칙, Refactor/Feature 커밋 혼합 금지, 대량 변경 금지, 요청하지 않은 기능 추가 금지 규칙 수립
- **스킬 3종 구성**: readme-management(README 관리), acceptance-test(인수테스트 작성법), refactoring(리팩터링 절차) 스킬을 `.claude/skills/` 하위에 생성
- **Service 추출 순서 결정**: 도메인 간 의존성 분석 결과 category(의존성 없음) → member → product(category 의존) → option(product 의존) → wish(member+product 의존) → order(가장 복잡, 외부 API 의존) 순서로 진행하기로 결정
- **테스트 작성 순서**: Service 추출 순서와 동일하게 의존성 적은 도메인부터 작성

#### 이슈
- 테스트 코드가 전혀 없는 상태 (`src/test/` 하위에 `.gitkeep`만 존재). Phase 0에서 인수테스트를 먼저 확보해야 이후 리팩터링의 안전망이 보장됨
- Order, Auth 도메인은 Kakao 외부 API 의존성이 있어 Mock 처리가 필요

## AI 활용 기록

### 2026-02-26 — 리팩터링 준비 (CLAUDE.md, Skills, README 구성)
- **활용 방식**: 프로젝트 전체 구조 탐색 및 분석, CLAUDE.md 행동 규약 초안 생성, 스킬 문서 3종 작성, README.md 체크리스트 구성
- **AI 산출물 수정 내용**: CLAUDE.md에 "대량 변경 금지", "요청하지 않은 기능 추가 금지" 규칙을 사용자 요청에 따라 추가. readme-management 스킬에 Phase 0(인수테스트) 섹션을 사용자 요청에 따라 보강
- **학습한 내용**: 프로젝트가 Java 21 + Kotlin 1.9.25 혼용 환경이며, Flyway로 DB 마이그레이션을 관리하고, H2 인메모리 DB를 테스트에 활용할 수 있음을 파악

### 2026-02-26 — Phase 0: 테스트 코드 작성
- **활용 방식**: 6개 도메인(category, member, product, option, wish, order)의 Controller 코드를 분석하고 인수테스트 초안을 생성
- **AI 산출물 수정 내용**: 시드 데이터(V2__Insert_default_data.sql) 기반으로 테스트 데이터 ID를 매핑. 삭제 테스트에서 FK 제약 조건을 고려하여 새 엔티티를 생성 후 삭제하는 방식 적용. Order 도메인은 Kakao 외부 API 의존 없이 시드 데이터 사용자의 포인트 내에서 테스트 가능하도록 구성
- **학습한 내용**: AuthenticationResolver가 Authorization 헤더에서 JWT를 파싱하여 Member를 반환하는 구조. 인증 실패 시 null 반환 후 Controller에서 401 처리. @Transactional로 테스트 간 데이터 격리 가능

### 2026-02-26 — Phase 1: 스타일 정리 (Checkstyle 도입)
- **활용 방식**: 전체 Java 소스 파일의 import 순서, 불필요한 어노테이션, 미사용 import 탐색 및 수정
- **AI 산출물 수정 내용**: Google Java Style checkstyle.xml을 프로젝트에 맞게 커스터마이징 (인덴트 4spaces, 라인 120자, Javadoc 필수 비활성화). 12개 파일의 import 정렬, 4곳의 @Autowired 제거, OptionController의 Collectors.toList() → .toList() 변경, OrderController의 빈 catch 블록에 주석 추가
- **학습한 내용**: Google Java Style의 import 규칙은 static/non-static 두 그룹만 사용하며, non-static 내에서는 패키지 구분 없이 전체 알파벳순으로 정렬한다. Spring에서 단일 생성자는 @Autowired 불필요

### 2026-02-26 — Phase 2: 미사용 코드 제거
- **활용 방식**: 전체 41개 Java 소스 파일의 import, 필드, private 메서드, 주석 처리된 코드를 탐색
- **AI 산출물 수정 내용**: OrderController의 미사용 wishRepository 필드/import/생성자 파라미터 제거. 미구현 기능(cleanup wish)의 주석을 TODO로 변경하여 의도 보존
- **학습한 내용**: 코드베이스가 비교적 깔끔하여 미사용 코드가 거의 없었음. 유일한 미사용 코드는 OrderController에서 주문 시 위시리스트 정리를 위해 주입되었으나 미구현된 WishRepository 1건

### 2026-02-26 — Phase 3: Service Layer 추출
- **활용 방식**: 6개 도메인(category, member, product, option, wish, order)의 Controller에서 비즈니스 로직을 Service 클래스로 추출
- **AI 산출물 수정 내용**: 각 도메인별 Service 클래스 생성(CategoryService, MemberService, ProductService, OptionService, WishService, OrderService). Controller는 요청/응답 변환과 인증/인가만 담당하도록 변경. ProductController에 NoSuchElementException 핸들러 추가(Service에서 throw하는 경우). AdminProductController에서 CategoryRepository 대신 CategoryService 사용. OrderController의 5개 의존성을 OrderService+AuthenticationResolver 2개로 축소
- **학습한 내용**: Service가 엔티티를 반환하고 Controller에서 DTO 변환하는 패턴이 Admin/API Controller 공유에 효과적. 이름 검증은 Admin(allowKakao=true)과 API(allowKakao=false)의 규칙이 달라 Controller에 유지하는 것이 적절. 중복 위시 처리(200 OK vs 201 Created)처럼 HTTP 응답 코드 분기가 필요한 로직은 Controller에 유지해야 함

### 2026-02-26 — Phase 4: 에러 처리 구조화
- **활용 방식**: 사용자 제공 스킬 템플릿을 프로젝트에 맞게 커스터마이징하여 setup-error-handling 스킬 생성. 기존 예외 현황을 분석하고 도메인별 에러 코드를 도출하여 구조화된 예외 체계 구축
- **AI 산출물 수정 내용**: gift.error 패키지에 공통 인프라(ErrorCode, ErrorResponse, BusinessException, GlobalExceptionHandler, CommonErrorCode, CommonException) 6개 클래스 생성. 6개 도메인 패키지에 각각 ErrorCode enum + Exception 클래스 생성(12개 파일). 기존 NoSuchElementException/IllegalArgumentException을 도메인 예외로 교체하고 Controller-level @ExceptionHandler 3개 제거. 인증(401)/인가(403) 처리도 CommonException으로 통합
- **학습한 내용**: @RestControllerAdvice는 @RestController에만 적용되므로 Admin Controller(@Controller)의 예외 처리는 별도 고려 필요. 이름 검증처럼 여러 에러 메시지를 반환하는 경우 CommonException(INVALID_REQUEST)으로 단순화하면 기존 테스트(상태 코드만 검증)는 통과하지만 에러 메시지 상세도가 낮아지는 트레이드오프 존재
