# Progress 15: 누락 단위 테스트 보완 — MemberService, AdminMemberService, ProductNameValidator/Rule, JwtProvider, OrderMessageBuilder

**기간**: 2026-03-05

---

## 이 문서의 목적

Phase 2 누락 테스트 보완. `temp.md`에 기록된 "feat 테스트 부재" 항목을 해소한다.

기존에 Service 단위 테스트가 없던 컴포넌트에 대해 테스트를 추가하여, 리팩터링 안전망을 강화한다.

**변경 유형**: 테스트만 추가 (구조 변경/작동 변경 없음)

---

## 1. 대상 컴포넌트와 테스트 현황

### 보완 전 상태

| 컴포넌트                              | 단위 테스트 | 비고                       |
|-----------------------------------|:------:|--------------------------|
| `MemberService`                   |   X    | 회원 가입/로그인 핵심 로직          |
| `AdminMemberService`              |   X    | 관리자 회원 CRUD + 포인트 충전     |
| `ProductNameValidator` (REST API) |   X    | 전략 패턴 Validator          |
| `ProductNameValidator` (Admin)    |   X    | 전략 패턴 Validator (카카오 허용) |
| `MaxLengthRule`                   |   X    | ProductNameRule 구현체      |
| `NotBlankRule`                    |   X    | ProductNameRule 구현체      |
| `NotContainsKakaoRule`            |   X    | ProductNameRule 구현체      |
| `SpecialCharacterRule`            |   X    | ProductNameRule 구현체      |
| `JwtProvider`                     |   X    | JWT 토큰 생성/검증 핵심 로직       |
| `OrderMessageBuilder`             |   X    | 주문 정보 → 카카오 메시지 DTO 변환   |

### 보완 후 상태

| 테스트 파일                            | 테스트 수  | 검증 대상                           |
|-----------------------------------|:------:|---------------------------------|
| `MemberServiceTest`               |   4    | register, login 성공/실패 분기        |
| `AdminMemberServiceTest`          |   10   | CRUD 전체 + 포인트 충전 + 조회 실패        |
| `RestApiProductNameValidatorTest` |   9    | 4개 Rule 통합 검증, 다중 위반            |
| `AdminProductNameValidatorTest`   |   6    | 3개 Rule 통합 검증 (카카오 허용 확인)       |
| `MaxLengthRuleTest`               |   5    | 경계값 (15자 이내/정확/초과), null/empty  |
| `NotBlankRuleTest`                |   3    | 정상/null/empty                   |
| `NotContainsKakaoRuleTest`        |   4    | 정상/시작/중간 위치, null               |
| `SpecialCharacterRuleTest`        |   6    | 허용 문자, 금지 문자, 한글 자모, null/empty |
| `JwtProviderTest`                 |   4    | 토큰 생성/추출, 만료, 잘못된 형식, 다른 키      |
| `OrderMessageBuilderTest`         |   2    | 메시지 DTO 생성, 주문 미존재              |
| **합계**                            | **53** |                                 |

---

## 2. MemberService 테스트

**파일**: `src/test/java/gift/member/internal/MemberServiceTest.java`

**의존성**: `@Mock MemberRepository memberRepo`

| 테스트                           | DisplayName                                | 핵심 검증                                             |
|-------------------------------|--------------------------------------------|---------------------------------------------------|
| `testRegister`                | 이메일이 미등록이면 회원을 저장하고 ID를 반환한다               | `existsByEmail` → false → `save` 호출 → 반환 ID 일치    |
| `testRegisterDuplicateEmail`  | 이미 등록된 이메일이면 RegisterFailedException을 던진다  | `existsByEmail` → true → `save` 미호출 (`never()`)   |
| `testLogin`                   | 이메일과 비밀번호가 일치하면 회원 ID를 반환한다                | `findByEmailAndPassword` → Optional.of → 반환 ID 일치 |
| `testLoginInvalidCredentials` | 이메일 또는 비밀번호가 틀리면 LoginFailedException을 던진다 | `findByEmailAndPassword` → Optional.empty         |

**테스트 설계 포인트**:

- `register` 성공 경로: `save` 호출 여부와 반환값 모두 검증
- `register` 실패 경로: `save(never())` — 중복 이메일 시 불필요한 저장이 발생하지 않음을 증명
- `login` 실패 경로: `LoginFailedException` 타입 검증 — `BadRequestException` 계층 확인

---

## 3. AdminMemberService 테스트

**파일**: `src/test/java/gift/member/admin/AdminMemberServiceTest.java`

**의존성**: `@Mock AdminMemberRepository memberRepo`

| 테스트                         | DisplayName                               | 핵심 검증                                      |
|-----------------------------|-------------------------------------------|--------------------------------------------|
| `testGetAllMembers`         | 전체 회원 목록을 MemberDto 리스트로 반환한다             | `findAll` → 2개 Member → DTO 리스트 크기 + 필드 매핑 |
| `testGetAllMembersEmpty`    | 회원이 없으면 빈 리스트를 반환한다                       | `findAll` → emptyList                      |
| `testHasEmailRegistered`    | 등록된 이메일이면 true를 반환한다                      | `existsByEmail` → true                     |
| `testHasEmailNotRegistered` | 미등록 이메일이면 false를 반환한다                     | `existsByEmail` → false                    |
| `testCreateMember`          | 회원을 생성하고 저장한다                             | `save(any(Member.class))` 호출               |
| `testFindMember`            | ID로 회원을 조회하고 MemberDto를 반환한다              | `findById` → Optional.of → DTO 4개 필드 일치    |
| `testFindMemberNotFound`    | 존재하지 않는 ID면 IllegalArgumentException을 던진다 | `findById` → Optional.empty                |
| `testUpdateMember`          | 회원 이메일과 비밀번호를 수정한다                        | Entity 상태 변경 확인 (dirty checking 시뮬레이션)     |
| `testChargePoint`           | 포인트를 충전한다                                 | Entity point 증가 확인 (1000 + 500 = 1500)     |
| `testDeleteMember`          | 회원을 삭제한다                                  | `delete(entity)` 호출 — 정확한 entity 객체 전달     |

**테스트 설계 포인트**:

- `updateMember`, `chargePoint`: JPA dirty checking에 의존하므로 `save` 호출이 아닌 **Entity 상태 변경**을 검증
- `findMemberOrThrowEx`는 private helper — not-found 경로는 `findMember`에서 대표 검증. 다른 메서드의 not-found는 동일 경로이므로 중복 테스트 생략
- `assertThatCode(() -> ...).doesNotThrowAnyException()`: void 반환 메서드의 성공 경로를 명시적으로 표현
- `getAllMembers`: `containsExactlyInAnyOrderElementsOf`로 MemberDto 리스트 매핑 정확도 검증

---

## 4. ProductNameValidator / Rule 테스트

별도 Claude 세션에서 작성. 전략 패턴(Strategy Pattern) 기반의 상품명 검증 로직을 단위 테스트한다.

### 4-1. Rule 단위 테스트 (4개 파일, 18개 테스트)

| 파일                         | 테스트 수 | 핵심 검증                                                     |
|----------------------------|:-----:|-----------------------------------------------------------|
| `MaxLengthRuleTest`        |   5   | 경계값: 15자 이내 통과, 정확히 15자 통과, 16자 위반. null/empty 안전         |
| `NotBlankRuleTest`         |   3   | 비어있지 않으면 통과, null과 empty 위반                               |
| `NotContainsKakaoRuleTest` |   4   | "카카오" 미포함 통과, 시작/중간 위치 위반, null 안전                        |
| `SpecialCharacterRuleTest` |   6   | 허용 특수문자 `()[]+-&/_` 통과, `!@#` 위반, 한글 자모 통과, null/empty 안전 |

**파일 위치**: `src/test/java/gift/product/common/`

### 4-2. Validator 통합 테스트 (2개 파일, 15개 테스트)

| 파일                                | 테스트 수 | 핵심 검증                                                       |
|-----------------------------------|:-----:|-------------------------------------------------------------|
| `RestApiProductNameValidatorTest` |   9   | 4개 Rule 전체 적용. "카카오" 포함 금지. 다중 위반 시 전체 위반 목록 반환             |
| `AdminProductNameValidatorTest`   |   6   | 3개 Rule 적용 (NotContainsKakaoRule **제외**). Admin에서는 "카카오" 허용 |

**파일 위치**:

- `src/test/java/gift/product/internal/RestApiProductNameValidatorTest.java`
- `src/test/java/gift/product/admin/AdminProductNameValidatorTest.java`

**REST API vs Admin의 차이**:

```
RestApiProductNameValidator: NotBlank → MaxLength(15) → SpecialCharacter → NotContainsKakao
AdminProductNameValidator:   NotBlank → MaxLength(15) → SpecialCharacter
```

Admin Validator는 `NotContainsKakaoRule`을 포함하지 않으므로, "카카오상품" 같은 이름이 Admin에서는 허용된다. 이 정책 차이가 테스트로 명시적으로 검증된다.

---

## 5. JwtProvider 테스트

**파일**: `src/test/java/gift/auth/internal/JwtProviderTest.java`

**특이점**: Mock 불필요. `JwtProvider`는 외부 의존성 없이 생성자에 `(String secret, long expiration)`만 받는 순수 로직 클래스. Spring Context 없이 직접 인스턴스를 생성하여 테스트한다.

```java
private static final String SECRET = "test-secret-key-at-least-32-chars!!"; // 36 chars ≥ 256 bits
private static final long EXPIRATION = 3_600_000L;

JwtProvider jwtProvider = new JwtProvider(SECRET, EXPIRATION);
```

| 테스트                          | DisplayName                      | 핵심 검증                                                         |
|------------------------------|----------------------------------|---------------------------------------------------------------|
| `testCreateTokenAndGetEmail` | 토큰을 생성하고 이메일을 추출한다               | `createToken(email)` → `getEmail(token)` == email             |
| `testExpiredToken`           | 만료된 토큰은 ExpiredJwtException을 던진다 | expiration=-1L로 생성 → 즉시 만료 → `ExpiredJwtException`            |
| `testMalformedToken`         | 잘못된 형식의 토큰은 JwtException을 던진다    | `getEmail("not.a.valid.jwt")` → `JwtException`                |
| `testWrongSecretToken`       | 다른 키로 서명된 토큰은 JwtException을 던진다  | 다른 secret의 JwtProvider로 서명 → 원래 provider로 검증 → `JwtException` |

**테스트 설계 포인트**:

- 만료 테스트: `expiration = -1L`로 별도 JwtProvider를 생성하면 토큰의 `expiryDate`가 과거 시점이 되어 즉시 만료됨
- 잘못된 키 테스트: 서로 다른 HMAC-SHA 키로 서명/검증하면 `SignatureException`(JwtException 하위)이 발생
- JJWT 라이브러리 요구사항: HMAC-SHA256 키는 최소 256비트(32바이트) 이상이어야 함

---

## 6. OrderMessageBuilder 테스트

**파일**: `src/test/java/gift/order/internal/OrderMessageBuilderTest.java`

**의존성**: `@Mock OrderRepository orderRepo`

`buildFrom(orderId)` 메서드의 동작:

1. `orderRepo.findByIdInnerJoinFetchOptionAndProduct(orderId)` — fetch join으로 Order → Option → Product 그래프 한 번에 로드
2. Product name, Option name, Order quantity, totalPrice(price × quantity), message 추출
3. JSON 텍스트 블록 템플릿에 포맷팅 (`%,d`로 천 단위 쉼표)
4. `new OrderMessageDto(templateMessage)` 반환

| 테스트                          | DisplayName                         | 핵심 검증                                                                                                               |
|------------------------------|-------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `testBuildFrom`              | 주문 정보로 카카오 메시지 DTO를 생성한다            | Product(아메리카노, 5000) → Option(TALL) → Order(quantity=2) → dto.message()에 "아메리카노", "TALL", "2", "10,000", "감사합니다" 포함 |
| `testBuildFromOrderNotFound` | 주문이 존재하지 않으면 NotFoundException을 던진다 | `findByIdInnerJoinFetchOptionAndProduct` → Optional.empty                                                           |

**테스트 설계 포인트**:

- fixture 체인: `Product` → `Option(product)` → `Order(option)` — 각 Entity에 reflection으로 id 설정
- 메시지 내용 검증: `contains()`로 핵심 값 포함 여부를 확인. JSON 구조 전체를 검증하지 않고 비즈니스 데이터만 검증하여 템플릿 변경에 유연하게 대응
- `%,d` 포맷: `5000 × 2 = 10,000` — 천 단위 쉼표가 올바르게 적용되는지 검증

---

## 7. 테스트 컨벤션 준수

모든 테스트가 프로젝트 기존 패턴을 따른다:

| 항목            | 적용                                                               |
|---------------|------------------------------------------------------------------|
| 프레임워크         | `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` + `@Mock` |
| Assertion 스타일 | BDD: `given()`, `then().should()`, `assertThat()`                |
| 네이밍           | 메서드명 영문, `@DisplayName` 한글                                       |
| Entity ID 설정  | reflection (`Field.setAccessible(true)`)                         |
| Fixture 배치    | `// -- fixtures --` 주석 아래 helper 메서드                             |
| Rule 테스트      | Spring Context 불필요 — 순수 단위 테스트                                   |

---

## 8. 수정/생성 파일 목록

| 파일                                                                         | 유형               |
|----------------------------------------------------------------------------|------------------|
| `src/test/java/gift/member/internal/MemberServiceTest.java`                | **신규** — 4개 테스트  |
| `src/test/java/gift/member/admin/AdminMemberServiceTest.java`              | **신규** — 10개 테스트 |
| `src/test/java/gift/product/common/MaxLengthRuleTest.java`                 | **신규** — 5개 테스트  |
| `src/test/java/gift/product/common/NotBlankRuleTest.java`                  | **신규** — 3개 테스트  |
| `src/test/java/gift/product/common/NotContainsKakaoRuleTest.java`          | **신규** — 4개 테스트  |
| `src/test/java/gift/product/common/SpecialCharacterRuleTest.java`          | **신규** — 6개 테스트  |
| `src/test/java/gift/product/internal/RestApiProductNameValidatorTest.java` | **신규** — 9개 테스트  |
| `src/test/java/gift/product/admin/AdminProductNameValidatorTest.java`      | **신규** — 6개 테스트  |
| `src/test/java/gift/auth/internal/JwtProviderTest.java`                    | **신규** — 4개 테스트  |
| `src/test/java/gift/order/internal/OrderMessageBuilderTest.java`           | **신규** — 2개 테스트  |

---

## 9. 남은 작업

`temp.md`에 기록된 "feat 테스트 부재" 항목이 모두 해소되었다. 추가로 전체 점검에서 발견한 `JwtProvider`와 `OrderMessageBuilder` 테스트 갭도 해소했다.

외부 HTTP 클라이언트(`KakaoLoginClient`, `KakaoMessageClient`)를 제외하면 모든 비즈니스 로직 클래스(40개 중 38개)에 단위/통합 테스트가 존재한다. 외부 클라이언트는 인수 테스트에서 `@MockBean`으로 격리되므로 별도 단위 테스트 대상이 아니다.

Phase 2 전체 작업 현황:

| 작업                                | 상태             |
|-----------------------------------|----------------|
| 1. 도메인 책임 되찾기 — Port 패턴 (ADR-001) | 완료             |
| 2-1. 주문 부수 효과 (ADR-003)           | 완료             |
| 2-2. Option PUT                   | 완료             |
| 2-3. Category GET /{id}           | 완료             |
| 2-4. Product 카테고리 필터              | 완료             |
| 3. 누락 단위 테스트 보완                   | **완료 (이번 작업)** |
