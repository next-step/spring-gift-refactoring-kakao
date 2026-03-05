# STEP 2: 리팩터링 완성 - AI 사용 기록

## 개요

이 문서는 리팩터링 완성 단계에서 AI를 어떻게 활용했는지를 기록한다.

Step 2의 핵심은 **작동 변경을 안전하게 수행하고, 테스트로 증명하는 것**이다.

1. **트랜잭션 경계 세우기** — 복합 쓰기 작업의 원자성 보장
2. **누락된 작동 구현** — TODO로 남아있던 위시리스트 자동 제거 완성
3. **도메인 책임 되찾기** — 검증/판단 로직을 올바른 위치로 이동

---

## 1단계: 프로젝트 현황 분석 및 Step 2 계획 수립

### 프롬프트

> Step 1 같이 진행한 상태가 현재 프로젝트 상태이고 이제 step-2 진행해야 하는 상태야. 프로젝트 전체적으로 한 번 다 둘러봐.

### AI 활용 방식

- 4개의 병렬 에이전트를 실행하여 전체 프로젝트를 동시에 분석:
  1. 프로젝트 디렉토리 구조 전체 탐색 (75+ 파일)
  2. 문서 체계 전문 읽기 (README, docs/, chatlog/)
  3. Git 히스토리 분석 (main vs step-2 브랜치 diff)
  4. 전체 소스 코드 읽기 (엔티티, 서비스, 컨트롤러, 설정 파일)

### 분석 결과

Step 1에서 완료된 상태:
- 7개 서비스 레이어 추출 완료 (Controller → Service → Repository 3계층)
- 25개 Cucumber BDD 시나리오 (gift 6, member 5, option 5, product 3, wish 6)
- `@Transactional`은 원본 동작 보존을 위해 의도적으로 제거된 상태 (commit 596884a)

Step 2에서 수행할 작업 식별:

| 작업 | 종류 | 증거 |
|------|------|------|
| `@Transactional` 추가 (특히 `OrderService.createOrder`) | 작동 변경 | 포인트 부족 시 재고 롤백 테스트 |
| 주문 후 위시리스트 자동 삭제 | 작동 변경 | 주문 → 위시 제거 확인 테스트 |
| 위시 중복 체크를 서비스로 이동 | 구조 변경 | 기존 테스트 재통과 |
| 상품 이름 검증을 서비스로 이동 | 구조 변경 | 기존 테스트 재통과 |

### 산출물

| 파일 | 내용 |
|------|------|
| `README.md` | Step 2 리팩터링 계획 추가 (작동 변경 2건 + 구조 변경 2건) |
| `chatlog/AI_USAGE_STEP_2.md` | 본 문서 (AI 활용 기록 시작) |

---

## 2단계: OrderService.createOrder 트랜잭션 경계 설정

### 프롬프트

> `OrderService.createOrder()` — 재고 차감 + 포인트 차감 + 주문 저장이 원자적으로 처리되어야 함

### 변경 전/후 정의

- **무엇을 바꾸는가**: `createOrder()`에 `@Transactional` 추가 → 포인트 부족 시 재고 차감이 롤백됨
- **무엇을 바꾸지 않는가**: 정상 주문 흐름 (재고 차감 + 포인트 차감 + 주문 저장 + 카카오 알림)
- **무엇이 이를 증명하는가**: `OrderServiceTest` 2개 테스트

### AI 활용 방식

1. **테스트 먼저 작성** (Red):
   - `OrderServiceTest.createOrder_insufficientPoints_rollbacksStock()` — 포인트 1000원, 상품 500원 × 3개 = 1500원 주문 시도 → 포인트 부족 예외 → 재고/포인트/주문 모두 원래 상태 확인
   - `OrderServiceTest.createOrder_success()` — 정상 주문 성공 시 재고/포인트 차감 확인
2. **테스트 실행으로 버그 확인**: `@Transactional` 없는 상태에서 롤백 테스트 실패 (재고 10 → 7로 차감됨, 포인트만 실패)
3. **최소 변경 적용** (Green): `OrderService.java`에 `@Transactional` import + 어노테이션 2줄만 추가
4. **git diff 확인**: 프로덕션 코드 변경이 2줄(import + 어노테이션)뿐인지 확인

### 산출물

| 파일 | 변경 | 종류 |
|------|------|------|
| `OrderServiceTest.java` | 신규 — 2개 테스트 (롤백 증명 + 정상 주문) | 테스트 |
| `OrderService.java` | `@Transactional` 추가 (import 1줄 + 어노테이션 1줄) | 작동 변경 |

### 테스트 결과

| 테스트 | @Transactional 전 | @Transactional 후 |
|--------|-------------------|-------------------|
| 포인트 부족 시 재고 롤백 | **FAIL** (재고 10→7) | **PASS** (재고 10 유지) |
| 정상 주문 성공 | PASS | PASS |

---

## 3단계: KakaoAuthService.processCallback 트랜잭션 경계 설정

### 프롬프트

> `KakaoAuthService.processCallback()` — 회원 조회/생성 + 카카오 토큰 저장

### 변경 전/후 정의

- **무엇을 바꾸는가**: `processCallback()`에 `@Transactional` 추가 → 회원 조회/생성 + 토큰 저장이 하나의 트랜잭션으로 처리
- **무엇을 바꾸지 않는가**: 카카오 OAuth 콜백 흐름 (토큰 교환 → 사용자 정보 조회 → 회원 처리 → JWT 발급)
- **무엇이 이를 증명하는가**: `KakaoAuthServiceTest` 2개 테스트

### AI 활용 방식

1. **테스트 먼저 작성**: `KakaoLoginClient`를 `@MockitoBean`으로 mock하여 외부 API 의존성 제거
   - `processCallback_newMember_createsWithToken()` — 신규 회원 생성 + 토큰 저장 + JWT 발급 검증 (DB 재조회)
   - `processCallback_existingMember_updatesToken()` — 기존 회원 토큰 갱신 + 기존 데이터(포인트) 유지 + 중복 생성 없음 검증
2. **@Transactional 없이 테스트 실행**: 단일 save 호출이라 기본 동작은 하지만, 논리적 단위로서 트랜잭션 경계가 필요
3. **@Transactional 추가 후 테스트 재통과 확인**

### 산출물

| 파일 | 변경 | 종류 |
|------|------|------|
| `KakaoAuthServiceTest.java` | 신규 — 2개 테스트 (신규 회원 + 기존 회원) | 테스트 |
| `KakaoAuthService.java` | `@Transactional` 추가 (import 1줄 + 어노테이션 1줄) | 작동 변경 |

### 참고

`OrderService.createOrder()`와 달리 이 메서드는 단일 `save()` 호출이므로 롤백 시나리오가 극적이지 않다. `@Transactional`을 추가하는 이유는 회원 조회 → 토큰 갱신 → 저장이 하나의 논리 단위임을 명시하고, JPA 영속성 컨텍스트를 적절히 관리하기 위함이다.

---

## 4단계: MemberService.update, chargePoint 트랜잭션 경계 설정

### 프롬프트

> `MemberService.update()`, `chargePoint()` — 조회 + 수정이 하나의 단위

### 변경 전/후 정의

- **무엇을 바꾸는가**: `update()`와 `chargePoint()`에 `@Transactional` 추가 → 조회 + 수정 + 저장이 하나의 트랜잭션
- **무엇을 바꾸지 않는가**: 회원 수정/포인트 충전의 비즈니스 로직
- **무엇이 이를 증명하는가**: `MemberServiceTest` 4개 테스트

### AI 활용 방식

1. **테스트 먼저 작성**:
   - `update_success()` — 수정 후 DB 재조회로 이메일/비밀번호 변경 확인
   - `update_notFound()` — 존재하지 않는 회원 수정 시 예외
   - `chargePoint_success()` — 충전 후 DB 재조회로 포인트 확인
   - `chargePoint_invalidAmount_noChange()` — 0 이하 금액 충전 시 예외 + 기존 포인트 유지 확인
2. **@Transactional 없이 테스트 통과 확인** (단일 save 패턴)
3. **@Transactional 추가 후 테스트 재통과 확인**

### 산출물

| 파일 | 변경 | 종류 |
|------|------|------|
| `MemberServiceTest.java` | 신규 — 4개 테스트 | 테스트 |
| `MemberService.java` | `@Transactional` 추가 (import 1줄 + 어노테이션 2줄) | 작동 변경 |

---

## 5단계: ProductService.update, CategoryService.update 트랜잭션 경계 설정

### 프롬프트

> `ProductService.update()`, `CategoryService.update()` — 조회 + 수정이 하나의 단위. 한 번에 진행.

### 변경 전/후 정의

- **무엇을 바꾸는가**: 두 서비스의 `update()`에 `@Transactional` 추가
- **무엇을 바꾸지 않는가**: 상품/카테고리 수정의 비즈니스 로직
- **무엇이 이를 증명하는가**: `ProductServiceTest` 3개 + `CategoryServiceTest` 2개 테스트

### AI 활용 방식

1. **테스트 먼저 작성**:
   - `ProductServiceTest`: 수정 성공(DB 재조회) + 상품 미존재 예외 + 카테고리 미존재 예외
   - `CategoryServiceTest`: 수정 성공(DB 재조회) + 카테고리 미존재 예외
2. **전체 테스트 실행 시 FK 제약 위반 발생**: 다른 테스트 클래스의 데이터가 남아있어 `categoryRepository.deleteAll()` 실패. `setUp`에 FK 역순 삭제(orders → wishes → options → products → categories) 추가하여 해결.
3. **@Transactional 추가 후 전체 테스트 통과 확인** (`./gradlew clean test` BUILD SUCCESSFUL)

### 산출물

| 파일 | 변경 | 종류 |
|------|------|------|
| `ProductServiceTest.java` | 신규 — 3개 테스트 | 테스트 |
| `CategoryServiceTest.java` | 신규 — 2개 테스트 | 테스트 |
| `ProductService.java` | `@Transactional` 추가 (import 1줄 + 어노테이션 1줄) | 작동 변경 |
| `CategoryService.java` | `@Transactional` 추가 (import 1줄 + 어노테이션 1줄) | 작동 변경 |

### 교훈

`@SpringBootTest`로 여러 테스트 클래스가 같은 H2 인스턴스를 공유할 때, `@BeforeEach`의 `deleteAll()` 순서가 FK 제약을 고려해야 한다. 삭제 순서: orders → wishes → options → products → categories → members (FK 역순).

---

## 6단계: 주문 완료 후 위시리스트 자동 제거 구현

### 프롬프트

> 주문 완료 후 위시리스트에서 해당 상품 자동 제거. 테스트 먼저 구성하고 구현.

### 변경 전/후 정의

- **무엇을 바꾸는가**: 주문 완료 후 해당 회원의 위시리스트에서 주문한 상품을 자동 제거
- **무엇을 바꾸지 않는가**: 기존 주문 흐름 (재고 차감 + 포인트 차감 + 주문 저장 + 카카오 알림)
- **무엇이 이를 증명하는가**: `OrderServiceTest.createOrder_removesWishForProduct()` 테스트

### AI 활용 방식

1. **테스트 먼저 작성** (Red):
   - `createOrder_removesWishForProduct()` — 위시에 상품 추가 → 주문 → DB 재조회로 위시 제거 확인
   - 불필요한 테스트(`noWish_stillSucceeds`) 제거 — 기존 `createOrder_success`가 이미 커버
2. **테스트 실패 확인**: 위시 삭제 로직이 없어서 주문 후에도 위시가 남아있음
3. **최소 구현** (Green):
   - `WishRepository` 의존성 추가
   - 주문 저장 후 `wishRepository.findByMemberIdAndProductId().ifPresent(delete)` 1줄 추가
   - TODO 주석 제거
4. **전체 테스트 통과 확인** (`./gradlew clean test` BUILD SUCCESSFUL)

### 산출물

| 파일 | 변경 | 종류 |
|------|------|------|
| `OrderServiceTest.java` | 테스트 1개 추가 (위시 자동 제거 검증) | 테스트 |
| `OrderService.java` | `WishRepository` 의존성 추가 + 위시 삭제 로직 + TODO 주석 제거 | 작동 변경 |

---

## 7단계: 위시 중복 체크를 서비스로 이동 (구조 변경)

### 프롬프트

> 위시 중복 체크를 서비스로 이동. 호출부 단순화.

### 변경 전/후 정의

- **무엇을 바꾸는가**: 중복 체크 책임을 컨트롤러에서 서비스로 이동
- **무엇을 바꾸지 않는가**: API 동작 (신규 201, 중복 200, 미존재 상품 404)
- **무엇이 이를 증명하는가**: `WishServiceTest` 3개 + 기존 Cucumber wish 시나리오 6개

### 변경 전 (컨트롤러에 분기)

```java
// WishController - 서비스를 두 번 호출, 컨트롤러가 도메인 규칙(멱등) 판단
var existing = wishService.findByMemberAndProduct(memberId, productId);
if (existing.isPresent()) {
    return ResponseEntity.ok(WishResponse.from(existing.get()));
}
var saved = wishService.addWish(memberId, productId);
return ResponseEntity.created(...).body(WishResponse.from(saved));
```

### 변경 후 (서비스가 책임)

```java
// WishService - 중복 체크 + 생성을 하나로, AddWishResult(wish, created) 반환
var result = wishService.addWish(memberId, productId);

// WishController - 서비스 한 번 호출, HTTP 상태만 결정
if (result.created()) {
    return ResponseEntity.created(...).body(...);
}
return ResponseEntity.ok(...);
```

### AI 활용 방식

1. **테스트 먼저 작성** (Red): `WishServiceTest` — 신규 추가(created=true) + 중복 추가(created=false, 동일 ID, count=1) + 미존재 상품 예외
2. **중복 테스트 실패 확인**: 기존 `addWish`에 중복 체크가 없어서 실패
3. **서비스 변경**: `addWish`가 `AddWishResult(Wish, boolean created)` 반환, 중복 시 기존 위시 반환
4. **컨트롤러 단순화**: `findByMemberAndProduct` 호출 제거, `addWish` 한 번만 호출
5. **`findByMemberAndProduct` 메서드 제거**: 외부 호출자 없음
6. **전체 테스트 통과 확인** (`./gradlew clean test` BUILD SUCCESSFUL)

### 산출물

| 파일 | 변경 | 종류 |
|------|------|------|
| `WishServiceTest.java` | 신규 — 3개 테스트 | 테스트 |
| `WishService.java` | `addWish` 반환 타입 변경 + 중복 체크 내재화 + `findByMemberAndProduct` 제거 | 구조 변경 |
| `WishController.java` | 분기 제거, 서비스 한 번 호출로 단순화 | 구조 변경 |

### 효과

- **컨트롤러 분기 제거**: 서비스 2회 호출 + if/else → 서비스 1회 호출 + result.created() 분기
- **도메인 규칙 캡슐화**: "위시 추가는 멱등" 규칙이 서비스 안에 위치
- **API 동작 유지**: 신규 201, 중복 200, 미존재 404 — Cucumber 시나리오 전체 통과

---

## 8단계: 상품 이름 검증을 서비스로 이동 (구조 변경)

### 프롬프트

> 상품 이름 검증을 서비스로 이동. 중복 제거.

### 변경 전/후 정의

- **무엇을 바꾸는가**: 이름 검증 책임을 컨트롤러에서 서비스로 이동
- **무엇을 바꾸지 않는가**: API 동작 (카카오 이름 400, 15자 초과 400, Admin은 카카오 허용)
- **무엇이 이를 증명하는가**: `ProductServiceTest` 5개 추가 + 기존 Cucumber product 시나리오 3개

### 변경 전 (컨트롤러에 검증 중복)

```java
// ProductController - create, update 양쪽에서 validateName 호출
validateName(request.name()); // allowKakao=false
productService.create(name, price, imageUrl, categoryId);

// AdminProductController - ProductNameValidator 직접 호출
List<String> errors = ProductNameValidator.validate(name, true); // allowKakao=true
productService.create(name, price, imageUrl, categoryId);
```

### 변경 후 (서비스가 책임)

```java
// ProductService - 검증을 내부에서 수행, allowKakao 오버로드
public Product create(name, price, imageUrl, categoryId) { ... }           // allowKakao=false
public Product create(name, price, imageUrl, categoryId, allowKakao) { ... } // 명시적 지정

// ProductController - validateName 제거, 서비스만 호출
productService.create(name, price, imageUrl, categoryId);

// AdminProductController - allowKakao=true 전달
productService.create(name, price, imageUrl, categoryId, true);
```

### AI 활용 방식

1. **테스트 먼저 작성** (Red): `ProductServiceTest`에 5개 추가
   - 카카오 이름 등록 거부 + DB 미생성 확인
   - 15자 초과 이름 등록 거부
   - Admin은 카카오 이름 허용 (`allowKakao=true`)
   - 카카오 이름 수정 거부 + 원래 이름 유지 확인
2. **서비스 변경**: `create`/`update`에 `allowKakao` 오버로드 추가, 내부에서 `validateName()` 호출
3. **컨트롤러 단순화**:
   - `ProductController`: `validateName()` 메서드 제거, 미사용 `List` import 제거
   - `AdminProductController`: `ProductNameValidator` 직접 호출 → `productService.create(..., true)` 위임
4. **전체 테스트 통과 확인** (`./gradlew clean test` BUILD SUCCESSFUL)

### 산출물

| 파일 | 변경 | 종류 |
|------|------|------|
| `ProductServiceTest.java` | 5개 테스트 추가 (카카오/길이/Admin/수정) | 테스트 |
| `ProductService.java` | `create`/`update` 오버로드 + `validateName()` private 메서드 | 구조 변경 |
| `ProductController.java` | `validateName()` 제거, 서비스만 호출 | 구조 변경 |
| `AdminProductController.java` | `ProductNameValidator` 직접 호출 → 서비스 `allowKakao=true` 위임 | 구조 변경 |

### 효과

- **검증 중복 제거**: 컨트롤러 2곳에서 각각 호출하던 검증이 서비스 1곳으로 통합
- **도메인 규칙 캡슐화**: "상품 이름 제약" 규칙이 서비스 안에 위치
- **Admin 정책 분리**: `allowKakao` 파라미터로 API/Admin 정책 차이를 명시적으로 표현

---

## 9단계: 카카오 이름 검증 책임 재설계 (8단계 개선)

### 프롬프트

> allowkakao 이거 넣는게 너무 어색한데 다른 방법 없을까

### 문제 인식

8단계에서 `allowKakao` boolean 오버로드 방식을 적용했으나, 다음 문제가 제기됨:

1. **호출부 가독성 부족**: `create(name, price, imageUrl, categoryId, true)` — `true`가 무엇을 의미하는지 코드만 보고 알 수 없음
2. **구조 중복**: `create(4인자)` / `create(5인자)`, `update(5인자)` / `update(6인자)` — 시그니처만 다른 거의 동일한 메서드 쌍
3. **관심사 혼재**: 서비스가 "누가 호출하는가"(API vs Admin)를 알아야 하는 구조

### 의사결정 과정

3가지 대안을 검토:

| 대안 | 설명 | 판단 |
|------|------|------|
| `create` / `createByAdmin` 분리 | 메서드명으로 구분 | 처음 선택했으나 로직 중복이 여전히 존재 |
| `allowKakao` boolean 유지 | 현행 유지 | 호출부 가독성 나쁨, 거부 |
| **DTO Bean Validation으로 분리** | 카카오 체크를 DTO 어노테이션으로 이동 | **최종 채택** |

**최종 설계**: 검증 책임을 계층별로 분리

- **"카카오" 제한** → DTO의 `@ValidProductName` Bean Validation 어노테이션이 담당
- **도메인 규칙 (길이, 특수문자)** → `ProductNameValidator`가 담당, 서비스에서 호출
- **Admin은 `@RequestParam`**으로 개별 파라미터를 받으므로 Bean Validation을 거치지 않음 → 카카오 이름 자연스럽게 허용

### 핵심 인사이트

서비스가 `ProductNameValidator.validate(name, true)`를 항상 호출하는 것은 설계 모순이었다.
"카카오" 검증은 **누가 호출하느냐**에 따라 달라지는 **접근 제어** 성격이므로, 서비스(도메인)가 아닌 **컨트롤러 계층(DTO)**에서 처리하는 것이 자연스럽다.

이를 통해:
- `ProductNameValidator`에서 `allowKakao` 파라미터 **완전 제거** — 도메인 규칙(길이, 특수문자)만 남김
- 서비스는 `ProductNameValidator.validate(name)` 한 가지만 호출
- 카카오 검증은 `@ValidProductName` 어노테이션이 전담

### 검증 흐름 최종 구조

```
[API 호출]
  ProductController → @Valid ProductRequest
    → @NotBlank        : null/빈값 체크
    → @ValidProductName : "카카오" 포함 여부 체크 (ProductNameConstraintValidator)
    → @Positive         : 가격 양수 체크
  → ProductService.create()
    → ProductNameValidator.validate(name) : 길이 15자, 특수문자만 체크

[Admin 호출]
  AdminProductController → @RequestParam (Bean Validation 미적용)
  → ProductService.create()
    → ProductNameValidator.validate(name) : 길이 15자, 특수문자만 체크
    → "카카오" 체크 없음 → Admin은 카카오 포함 상품명 등록 가능
```

### AI 활용 방식

1. `@ValidProductName` 커스텀 어노테이션 + `ProductNameConstraintValidator` 신규 작성
2. `ProductRequest`에 `@ValidProductName` 추가
3. `ProductNameValidator`에서 `allowKakao` 파라미터와 카카오 체크 로직 제거 → 도메인 규칙만 남김
4. `ProductService.validateName()`에서 `validate(name, true)` → `validate(name)` 변경
5. `AdminProductController`에서 5인자 호출(`true`) → 4인자 호출로 복원
6. `ProductServiceTest` 정리: 카카오 관련 서비스 테스트 제거, 서비스가 카카오를 안 막는 것 확인 테스트 추가
7. **전체 테스트 통과 확인** (`./gradlew test` BUILD SUCCESSFUL)

### 산출물

| 파일 | 변경 | 종류 |
|------|------|------|
| `ValidProductName.java` | 신규 — 커스텀 Bean Validation 어노테이션 | 구조 변경 |
| `ProductNameConstraintValidator.java` | 신규 — "카카오" 포함 여부 검증 | 구조 변경 |
| `ProductRequest.java` | `@ValidProductName` 추가 | 구조 변경 |
| `ProductNameValidator.java` | `allowKakao` 파라미터 및 카카오 체크 로직 제거 | 구조 변경 |
| `ProductService.java` | `validate(name, true)` → `validate(name)`, 오버로드 제거 | 구조 변경 |
| `AdminProductController.java` | 5인자 → 4인자 호출 복원 | 구조 변경 |
| `ProductServiceTest.java` | 카카오 서비스 테스트 정리, 서비스 카카오 미검증 테스트 추가 | 테스트 |

### 교훈

- **boolean 파라미터 지양**: `create(name, price, imageUrl, categoryId, true)`처럼 호출부만 보고 의미를 파악할 수 없는 구조는 피해야 한다
- **검증 책임은 성격에 따라 계층을 나눠야 한다**: "카카오" 제한처럼 호출자(API vs Admin)에 따라 달라지는 검증은 컨트롤러 계층(DTO + Bean Validation)에서, 길이/특수문자 같은 도메인 규칙은 서비스에서 처리하는 것이 자연스럽다

---

## 10단계: 전역 예외 처리로 컨트롤러 try-catch 제거 (구조 변경)

### 프롬프트

> 컨트롤러마다 try-catch 반복되는 거 @RestControllerAdvice로 전역 처리하자

### 변경 전/후 정의

- **무엇을 바꾸는가**: 5개 컨트롤러에 분산된 예외 처리를 `GlobalExceptionHandler` 한 곳으로 통합
- **무엇을 바꾸지 않는가**: HTTP 응답 (NoSuchElementException → 404, IllegalArgumentException → 400, IllegalStateException → 403)
- **무엇이 이를 증명하는가**: 기존 전체 테스트 + Cucumber 시나리오 통과

### 변경 전 (컨트롤러마다 try-catch 반복)

```java
// ProductController — 3개 메서드에서 동일 패턴
@GetMapping("/{id}")
public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
    try {
        return ResponseEntity.ok(ProductResponse.from(productService.findById(id)));
    } catch (NoSuchElementException e) {
        return ResponseEntity.notFound().build();
    }
}

@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
}

// OptionController, CategoryController, WishController, OrderController에서도 동일 패턴 반복
```

### 변경 후 (전역 한 곳에서 처리)

```java
// GlobalExceptionHandler — 모든 @RestController에 적용
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Void> handleForbidden(IllegalStateException e) {
        return ResponseEntity.status(403).build();
    }
}

// ProductController — try-catch 제거, 서비스 위임만 남음
@GetMapping("/{id}")
public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
    return ResponseEntity.ok(ProductResponse.from(productService.findById(id)));
}
```

### AI 활용 방식

1. 전체 컨트롤러 코드를 읽고 예외 처리 패턴 분류:
   - `NoSuchElementException → 404`: ProductController(3), OptionController(3), CategoryController(1), WishController(2), OrderController(1) = **10곳**
   - `IllegalArgumentException → 400`: ProductController(1), OptionController(1) = **2곳** (`@ExceptionHandler`)
   - `IllegalStateException → 403`: WishController(1) = **1곳**
2. `GlobalExceptionHandler.java` 신규 작성 — 3가지 예외 타입 전역 처리
3. 5개 컨트롤러에서 try-catch 블록과 `@ExceptionHandler` 메서드 제거
4. 미사용 `NoSuchElementException` import 정리
5. **빌드 + 전체 테스트 통과 확인** (`./gradlew clean build`, `./gradlew test` BUILD SUCCESSFUL)

### 산출물

| 파일 | 변경 | 종류 |
|------|------|------|
| `GlobalExceptionHandler.java` | 신규 — `@RestControllerAdvice` 전역 예외 핸들러 | 구조 변경 |
| `ProductController.java` | try-catch 3개 + `@ExceptionHandler` 1개 제거 | 구조 변경 |
| `OptionController.java` | try-catch 3개 + `@ExceptionHandler` 1개 제거 | 구조 변경 |
| `CategoryController.java` | try-catch 1개 제거 | 구조 변경 |
| `WishController.java` | try-catch 2개 제거 | 구조 변경 |
| `OrderController.java` | try-catch 1개 제거 | 구조 변경 |

### 효과

- **try-catch 10개 + @ExceptionHandler 2개 제거**: 컨트롤러가 비즈니스 위임에만 집중
- **예외 처리 정책 한 곳에서 관리**: 새 예외 타입 추가나 응답 형식 변경 시 `GlobalExceptionHandler`만 수정
- **일관성 보장**: 모든 컨트롤러에서 동일한 예외에 동일한 HTTP 상태 코드 반환

---

## 11단계: 주문 총액 계산을 도메인 객체로 이동 (구조 변경)

### 프롬프트

> 서비스에서 getter로 꺼내서 처리하는 부분, 도메인 객체에게 시키는 방향으로 개선하자

### 변경 전/후 정의

- **무엇을 바꾸는가**: 주문 총액 계산 책임을 `OrderService`에서 `Option` 도메인 객체로 이동
- **무엇을 바꾸지 않는가**: 주문 생성 흐름 및 가격 계산 결과
- **무엇이 이를 증명하는가**: 기존 `OrderServiceTest` 전체 통과 (롤백 + 성공 + 위시 제거)

### 변경 전 (서비스가 getter 체이닝으로 직접 계산)

```java
// OrderService — option에서 product를 꺼내고, product에서 price를 꺼내서 직접 곱셈
int price = option.getProduct().getPrice() * quantity;
member.deductPoint(price);
```

디미터 법칙(Law of Demeter) 위반: `option.getProduct().getPrice()` — 객체의 내부 구조를 서비스가 알아야 한다.

### 변경 후 (도메인 객체에게 위임)

```java
// Option — 자신의 상품 가격을 알고 있으므로 직접 계산
public int calculatePrice(int quantity) {
    return product.getPrice() * quantity;
}

// OrderService — 객체에게 물어보기만 함
member.deductPoint(option.calculatePrice(quantity));
```

### AI 활용 방식

1. 전체 서비스 코드를 읽고 "getter로 꺼내서 서비스에서 처리하는" 패턴 탐색
2. `OrderService` 48번째 줄 `option.getProduct().getPrice() * quantity` 식별
3. `Option.calculatePrice(int quantity)` 메서드 추가
4. `OrderService`에서 해당 라인을 `option.calculatePrice(quantity)` 호출로 교체
5. **전체 테스트 통과 확인** (`./gradlew test` BUILD SUCCESSFUL)

### 산출물

| 파일 | 변경 | 종류 |
|------|------|------|
| `Option.java` | `calculatePrice(int quantity)` 메서드 추가 | 구조 변경 |
| `OrderService.java` | getter 체이닝 → `option.calculatePrice(quantity)` 위임 | 구조 변경 |

### 효과

- **디미터 법칙 준수**: 서비스가 `option → product → price` 내부 구조를 몰라도 됨
- **가격 계산 캡슐화**: 계산 로직이 바뀌어도(할인, 세금 등) `Option` 한 곳만 수정하면 됨

---

## 12단계: 잔여 중복/불일치 정리 (코드 정리)

### 프롬프트

> 더 깔끔하게 만들 수 있는 부분 점검해줘

### 변경 내용

10단계에서 `GlobalExceptionHandler`를 도입했지만 미처 정리하지 못한 부분 2가지를 발견하여 정리:

1. **`MemberController`의 로컬 `@ExceptionHandler` 제거**
   - `GlobalExceptionHandler`에서 `IllegalArgumentException → 400`을 이미 전역 처리하는데, `MemberController`에 동일한 로컬 핸들러가 남아있었음
   - 로컬 핸들러와 미사용 `ExceptionHandler` import 제거

2. **`OptionController`의 `Collectors.toList()` → `.toList()` 통일**
   - `CategoryController` 등 다른 컨트롤러는 Java 16+의 `.toList()`를 사용하는데, `OptionController`만 `Collectors.toList()`를 사용
   - `.toList()`로 통일하고 미사용 `Collectors` import 제거

### 산출물

| 파일 | 변경 | 종류 |
|------|------|------|
| `MemberController.java` | `@ExceptionHandler` 메서드 + import 제거 | 코드 정리 |
| `OptionController.java` | `Collectors.toList()` → `.toList()` + import 제거 | 코드 정리 |

---
