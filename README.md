# spring-gift-refactoring

Spring Boot 기반 선물 관리 e-commerce 애플리케이션의 리팩터링 프로젝트.

레거시 코드를 **외부 작동을 바꾸지 않으면서** 변경하기 쉬운 구조로 개선한다.

## 실행 방법

```bash
./gradlew bootRun              # 로컬 실행 (H2, 기본 포트 8080)
./gradlew test                 # 단위 테스트 실행
./gradlew acceptanceTest       # 인수 테스트 실행 (Cucumber)
./gradlew build                # 빌드 (단위 테스트 포함)
```

---

## 리팩터링 원칙

- **구조 변경과 작동 변경을 절대 섞지 않는다.** 한 커밋에는 둘 중 하나만 담는다.
- 기능을 추가하지 않는다. 외부 작동을 바꾸지 않는다.
- 코드의 의미를 더 분명하게 만든다. 다음 변경을 더 쉽게 만든다.

---

## Phase 1: 무엇을 했고, 왜 그렇게 했는가

Phase 1 목표: **변경하기 쉬운 상태 만들기.** 구조 변경만 수행한다.

### 0단계: 테스트 안전망 구축

리팩터링은 외부 작동을 유지해야 하므로, 코드를 바꾸기 전에 "현재 작동"을 기록하는 테스트가 필요했다.

기존 코드에는 테스트가 **0개**였다. 단위 테스트가 아닌 **인수 테스트**(API 수준)를 선택한 이유는, 리팩터링 안전망으로서 내부 클래스 구조가 바뀌어도 깨지지 않는 테스트가 필요했기 때문이다. 단위 테스트는 클래스명이나 메서드 시그니처가 바뀌면 함께 수정해야 하지만, API 테스트는 HTTP 계약(Method + URL + Status + Body)만 검증하므로 내부 리팩터링에 영향받지 않는다.

- Cucumber + RestAssured 기반 인수 테스트 인프라 구성
- 7개 Feature, **64개 Scenario** 작성 (정상 흐름 24개 + 예외 흐름 40개)
- `test` / `acceptanceTest` Gradle task 분리 — 일반 테스트와 인수 테스트가 서로 간섭하지 않도록 함
- 외부 의존성(`KakaoLoginClient`, `KakaoMessageClient`)은 `@MockBean`으로 격리

기존 64개 시나리오가 "요청 → 상태코드 확인"으로 끝나는 계약 테스트였기 때문에, 실제 데이터 변경이 올바른지 검증하지 못하는 한계가 있었다. 이를 보완하기 위해 **상태 변화 검증 시나리오 11개**를 추가했다:

- **상태 검증** (@state): 수정/삭제 후 재조회하여 실제 반영 확인 (product, category, wish, option)
- **경계값** (@boundary): 재고를 정확히 소진하는 주문 (order)
- **롤백 검증** (@rollback): 포인트 부족/재고 부족 시 `@Transactional` 롤백으로 재고 원복 확인 (order)

| Feature | 시나리오 수 | 정상 | 예외 | 상태검증 |
|---------|--------|----|----|------|
| 인증      | 1      | 1  | 0  | 0    |
| 카테고리    | 12     | 5  | 5  | 2    |
| 회원      | 8      | 2  | 6  | 0    |
| 옵션      | 13     | 3  | 8  | 2    |
| 주문      | 13     | 4  | 6  | 3    |
| 상품      | 16     | 5  | 9  | 2    |
| 위시      | 12     | 4  | 6  | 2    |

### 1단계: 스타일 정리

코드를 읽기 쉽게 만드는 것이 첫 번째 작업이었다. 프로젝트 전반에 스타일이 불일치하는 부분을 통일했다.

**왜 먼저 했는가**: 스타일이 불일치하면 이후 서비스 추출 시 diff를 읽기 어렵다. 스타일 변경과 구조 변경이 한 커밋에 섞이면 "이 변경이 의도적인가, 부수적인가"를 구분할 수 없다. 그래서 스타일을 먼저 통일한 뒤 구조 변경에 들어갔다.

수행한 작업:

- import 정리, 줄 간격 및 여백 통일
- Lombok 도입 → Entity의 수동 getter/protected 생성자 제거 (`@Getter`, `@NoArgsConstructor(access = PROTECTED)`)
- Entity에 `@Table(name = ...)` 명시, `@Builder` 패턴 적용
- Bean 주입 방식 통일: `@Autowired` 생성자 → `@RequiredArgsConstructor` + `private final` 필드
- 불필요한 `@Autowired` 제거
- 불필요한 `final` 로컬 변수 제거 (컨벤션: `var` 사용 금지, 불필요한 `final` 사용 금지)
- 오타 수정 (`imageUlr` → `imageUrl`)

### 2단계: 패키지 구조 재편

원본 코드는 도메인별로 패키지가 나뉘어 있었지만, 한 패키지 안에 Controller, Entity, Repository, DTO가 모두 평면적으로 놓여 있었다. 이 상태에서는 "이 클래스가 외부에 공개된 계약인가, 내부 구현인가"를 구분할 수 없었다.

**왜 `internal` 패키지를 도입했는가**: 도메인 간 의존성을 명확하게 하기 위해서다. 각 도메인에서 외부에 노출되는 것(Entity, Port 인터페이스)과 내부 구현(Controller, Service, Repository, DTO)을 분리했다. `internal` 패키지의 클래스는 다른 도메인이 직접 접근해서는 안 된다는 의도를 구조로 표현한 것이다.

**왜 `admin` 패키지를 분리했는가**: REST API Controller와 관리자 Thymeleaf Controller는 같은 도메인을 다루지만 역할이 다르다. API Controller는 JSON 응답, Admin Controller는 HTML 뷰 렌더링. 서로 다른 DTO와 Repository 쿼리를 사용하므로 패키지를 분리했다.

```
Before:                          After:
gift/category/                   gift/category/
├── Category.java                ├── Category.java          ← 외부 공개
├── CategoryController.java      └── internal/              ← 내부 구현
├── CategoryRepository.java          ├── CategoryController.java
├── CategoryRequest.java             ├── CategoryService.java
└── CategoryResponse.java            ├── CategoryRepository.java
                                     ├── CategoryRequest.java
                                     └── CategoryResponse.java
```

### 3단계: 커스텀 예외 계층 구성

원본 코드에는 글로벌 예외 처리가 없었다. 각 Controller에 `@ExceptionHandler(IllegalArgumentException.class)` → 400을 로컬로 선언하거나, 아예 없어서 500이 되는 상황이었다.

**왜 커스텀 예외 계층을 만들었는가**: 서비스 계층을 추출하면 예외가 Controller가 아닌 Service에서 발생한다. 로컬 `@ExceptionHandler`로는 다른 Controller를 경유한 예외를 잡을 수 없다. `CustomException` 기반의 예외 계층(`NotFoundException`, `BadRequestException`, `UnauthorizedException`, `ForbiddenException`)과 `@RestControllerAdvice` 글로벌 핸들러를 구성해, 어디서 예외가 발생하든 일관된 HTTP 응답을 반환하도록 했다.

### 4단계: 인증 계약 추출 (Port 패턴)

원본 코드에서는 인증이 필요한 Controller(`WishController`, `OrderController`)가 `AuthenticationResolver`를 직접 사용하고 있었다. 이 클래스는 `auth` 패키지의 내부 구현이므로, 다른 도메인이 직접 의존하면 `internal` 패키지 분리의 의미가 없어진다.

**왜 Port 인터페이스를 도입했는가**: `AuthenticationPort`와 `JwtPort`를 `auth` 패키지의 최상위에 인터페이스로 두고, 구현체는 `internal`에 배치했다. 다른 도메인은 인터페이스에만 의존하므로, 인증 구현이 바뀌어도(예: JWT → 세션) 다른 도메인 코드를 수정할 필요가 없다.

```
gift/auth/
├── AuthenticationPort.java     ← 인터페이스 (다른 도메인이 의존)
├── JwtPort.java                ← 인터페이스
└── internal/
    ├── AuthenticationPortImpl.java  ← 구현체
    ├── JwtPortImpl.java
    └── ...
```

### 5단계: 서비스 계층 추출

원본 코드의 가장 큰 문제는 **모든 비즈니스 로직이 Controller에 있다**는 것이었다. Controller가 Repository를 직접 호출하고, 검증 로직을 수행하고, 여러 Repository를 조합하는 오케스트레이션까지 담당했다.

**왜 서비스를 추출했는가**: Controller의 책임을 "HTTP 요청 검증 + Service 위임 + HTTP 응답 구성"으로 한정하기 위해서다. 비즈니스 로직이 Controller에 있으면, 같은 로직을 Admin Controller와 API Controller에서 중복 구현하게 되고, 테스트도 HTTP 레벨에서만 가능하다.

난이도 순으로 추출했다:

1. **Category** — 가장 단순한 CRUD. 서비스 추출 패턴을 확립하는 용도.
2. **Member** — register/login + Admin 분리. `admin/` 패키지 분리 패턴 확립.
3. **Option** — Bean Validation 적용으로 `OptionNameValidator` static 유틸리티를 제거.
4. **Order** — 가장 복잡. 주문 흐름(재고 차감 + 포인트 차감 + 저장)을 Service로, 메시지 빌드와 카카오 알림 발송을 별도 클래스(`OrderMessageBuilder`, `KakaoMessagingService`)로 분리.
5. **Product** — `ProductNameValidator`를 전략 패턴(`ProductNameRule` 인터페이스 + 4개 Rule 구현체)으로 재구성. REST API는 "카카오" 포함 금지 규칙 적용, Admin은 미적용 — 동일 인터페이스, 다른 Rule 조합.
6. **Wish** — `AuthenticationPort` 사용으로 재구성.

### 6단계: KakaoAuth 서비스 추출 + 트랜잭션 경계 개선

서비스 추출 이후에도 `KakaoAuthController.callback()`에는 OAuth 전체 흐름(토큰 교환 → 사용자 조회 → 회원 생성/갱신 → JWT 발급)이 남아 있었다. Controller가 `KakaoLoginClient`, `AuthMemberRepository`, `JwtProvider`를 직접 의존하고 있었다.

`KakaoAuthService`를 추출해 `loginWithKakao(code)` 메서드 하나로 전체 흐름을 캡슐화했다. Controller의 의존성은 4개 → 2개로 줄었다.

**트랜잭션 경계 문제**: 처음 추출할 때 메서드 전체에 `@Transactional`을 걸었으나, 카카오 외부 API 호출(수백ms~수초)이 트랜잭션 안에 포함되어 **API 응답을 기다리는 동안 DB 커넥션이 점유**되는 문제가 있었다. `@Transactional`을 제거하고, `repository.save()`가 `SimpleJpaRepository` 내부의 자체 트랜잭션으로 DB 저장을 처리하도록 개선했다.

```
Before (@Transactional 전체):
  트랜잭션 시작 (DB 커넥션 획득)
    ├── 카카오 API 호출 (수백ms~수초 대기)  ← 이 동안 DB 커넥션 점유
    ├── DB 조회 + 저장
    └── JWT 생성
  트랜잭션 종료 (DB 커넥션 반환)

After (@Transactional 제거):
  카카오 API 호출 (트랜잭션 없음)
  repository.save() 내부 트랜잭션 (짧은 DB 접근만)
  JWT 생성 (트랜잭션 없음)
```

---

## 리팩터링 전후 비교

**Before** (main 브랜치)

```
gift/
├── auth/          ← Controller + 인프라가 혼재, OAuth 전체 흐름이 Controller에
├── category/      ← Controller가 Repository 직접 호출
├── member/        ← Admin/API Controller 혼재
├── option/        ← Controller에 모든 비즈니스 로직
├── order/         ← Controller에 주문 + 메시지 + 알림 로직 전부
├── product/       ← Controller에 CRUD + 검증 + Admin 혼재
└── wish/          ← Controller가 Repository 직접 호출
```

- 서비스 계층 없음 (Controller → Repository 직접 호출)
- 글로벌 예외 처리 없음
- 테스트 0개

**After** (step1-refactor-package 브랜치)

```
gift/
├── auth/
│   ├── AuthenticationPort.java     ← 인증 계약 (public)
│   ├── JwtPort.java                ← JWT 계약 (public)
│   └── internal/                   ← KakaoAuthService, KakaoLoginClient, JwtProvider 등
├── category/
│   ├── Category.java               ← Entity (public)
│   └── internal/                   ← Controller, Service, Repository, DTO
├── member/
│   ├── Member.java
│   ├── internal/                   ← REST API 관련
│   └── admin/                      ← 관리자 페이지 관련
├── option/
│   ├── Option.java
│   └── internal/
├── order/
│   ├── Order.java
│   └── internal/                   ← Service, MessageBuilder, KakaoMessaging 분리
├── product/
│   ├── Product.java
│   ├── internal/                   ← REST API 관련
│   ├── admin/                      ← 관리자 페이지 관련
│   └── common/                     ← ProductNameRule 전략 패턴
├── wish/
│   ├── Wish.java
│   └── internal/
└── global/                         ← 공통 예외 계층 + 핸들러
```

- 도메인별 Service 계층 추출 완료
- Controller는 검증과 위임만 담당
- 인증 로직 Port 패턴으로 분리
- 커스텀 예외 계층 + 글로벌 핸들러
- 인수 테스트 75개 (전체 통과) — 상태 검증 + 경계값 + 롤백 시나리오 포함

---

## Phase 1 작업 체크리스트

#### 0단계: 준비

- [x] CLAUDE.md 작성 (미션 원칙, AI 협업 규율, 코드 컨벤션)
- [x] AI 도구 구성 (skills: verify, history, decision)

#### 1단계: 테스트 안전망 구축

- [x] Cucumber + RestAssured 의존성 추가
- [x] 테스트 인프라 구성 (profile, hooks, context, ApiClient, DataManipulator)
- [x] 인수 테스트 시나리오 작성 (7개 feature, **64개 시나리오**)
- [x] Step definitions 구현 (도메인별 분리)
- [x] Gradle task 분리 (`test` / `acceptanceTest`)
- [x] 상태 검증 시나리오 강화 (**+11개, 총 75개 시나리오**)

#### 2단계: 스타일 정리

- [x] import, 줄 간격 및 여백 정리
- [x] Lombok 의존성 추가 + Entity getter/constructor 간소화
- [x] Entity table 이름 명시, builder 패턴 적용
- [x] `@RequiredArgsConstructor` 통한 bean 주입 방식 통일
- [x] 불필요한 `@Autowired` 제거
- [x] 불필요한 `final` 로컬 변수 제거
- [x] 오타 수정

#### 3단계: 패키지 구조 재편 + 서비스 계층 추출

- [x] 도메인별 분리된 repository 구성
- [x] 커스텀 예외 계층 구성 (`global/` 패키지)
- [x] `AuthenticationPort`, `JwtPort` — 인증 계약 추출
- [x] entity, port 객체를 제외한 모든 class를 `internal` 패키지로 이동
- [x] **Category**: Service 추출, Controller → Service 위임
- [x] **Member**: Service 추출 + Admin Service/Controller 분리
- [x] **Option**: Service 추출, Bean Validation 적용
- [x] **Order**: Service 추출 + OrderMessageBuilder + KakaoMessagingService 분리
- [x] **Product**: Service 추출 + ProductNameRule 전략 패턴 + Admin 분리
- [x] **Wish**: Service 추출, AuthenticationPort 사용으로 재구성
- [x] **KakaoAuth**: Service 추출 + 트랜잭션 경계에서 외부 API 호출 분리
- [x] 불필요한 코드/메서드 제거

#### 다음 작업 (미완료)

- [ ] 크로스 도메인 중복 Repository 정리
- [ ] Controller에 남은 비즈니스 로직 Service로 이동 (Member JWT, Order 알림)
- [ ] Admin/Internal 서비스 로직 통합 (Product)
- [ ] Validator 중복 제거

---

## 커밋 히스토리 요약

총 **95개 커밋** (main 이후), 154개 파일 변경 (+5,040줄 / -1,314줄)

| 날짜    | 작업                                                                                 | 커밋 수 |
|-------|------------------------------------------------------------------------------------|------|
| 02-27 | 프로젝트 분석, CLAUDE.md 작성, 스타일 정리 시작                                                   | 2    |
| 02-28 | Lombok 적용, Entity builder 패턴, 테스트 인프라 + 64개 시나리오 구현                                | 45   |
| 03-01 | DI 통일, Repository 분리, 예외 계층, Auth Port, Category/Member Service 추출                 | 17   |
| 03-02 | Option/Order/Product/Wish/KakaoAuth Service 추출, Admin 분리, Validator 재구성, 스타일 세부 정리 | 31+  |

---

## AI 활용 내역

이 프로젝트에서 AI(Claude Code)는 **초안을 만드는 도구**로 활용했다.
설계와 검증 책임은 개발자에게 있다.

### 활용 범위

| 단계      | AI 역할                              | 개발자 역할             |
|---------|------------------------------------|--------------------|
| 계획 수립   | 코드 분석, 스타일 위반 목록 작성                | 작업 순서 결정, 범위 확정    |
| 테스트 작성  | 인수 테스트 시나리오 초안, step definition 구현 | 시나리오 검토, 테스트 전략 결정 |
| 스타일 정리  | Lombok 적용, import 정리, DI 통일        | 변경 범위 지정, diff 검토  |
| 서비스 추출  | Service 클래스 초안, Repository 분리      | 설계 결정, 계층 경계 확정    |
| 코드 분석   | 미흡 사항 식별, 리팩터링 포인트 도출              | 우선순위 결정, 실행 여부 판단  |
| 트랜잭션 개선 | 문제 식별 + 해결 방안 제시                   | 트랜잭션 경계 검토, 최종 판단  |

### 세션 기록

| 세션         | 파일                                              | 내용                               |
|------------|-------------------------------------------------|----------------------------------|
| planner-0  | `memory/session-planner-0-planning.md`          | 프로젝트 분석, 계획 수립, 도구 구성            |
| tester-1~6 | `memory/session-tester-*.md`                    | 테스트 규칙 수립 → 시나리오 작성 → 구현 → 리팩터링  |
| tester-7   | `memory/session-tester-7-state-verification.md` | 상태 검증 시나리오 11개 추가 (경계값, 롤백, 재조회) |

### 도구 구성

- **CLAUDE.md**: 미션 원칙, 코드 컨벤션, AI 협업 규율
- **/verify**: 코드 변경 후 검증 (테스트, diff, 구조/작동 분리, 30초 규칙, 컨벤션)
- **/decision**: 선택지 정리 → 토의 → 결정 기록
- **/history**: 세션 종료 시 작업 기록 저장
