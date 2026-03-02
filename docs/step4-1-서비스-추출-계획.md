# 서비스 계층 추출 계획 분석 보고서

> 분석 범위: Controller 9개에 포함된 비즈니스 로직 식별 및 Service 계층 추출 대상 분석

## 1. 분석 목적

현재 2-layer 아키텍처(Controller → Repository)에서 Controller에 직접 포함된 비즈니스 로직을 식별하고, 3-layer(Controller → Service → Repository)로 전환하기 위한 추출 계획을 수립한다.

## 2. 분석 범위

| 패키지 | Controller | 파일 경로 |
|--------|-----------|----------|
| `gift.category` | `CategoryController` | `src/main/java/gift/category/CategoryController.java` |
| `gift.product` | `ProductController` | `src/main/java/gift/product/ProductController.java` |
| `gift.product` | `AdminProductController` | `src/main/java/gift/product/AdminProductController.java` |
| `gift.member` | `MemberController` | `src/main/java/gift/member/MemberController.java` |
| `gift.member` | `AdminMemberController` | `src/main/java/gift/member/AdminMemberController.java` |
| `gift.auth` | `KakaoAuthController` | `src/main/java/gift/auth/KakaoAuthController.java` |
| `gift.option` | `OptionController` | `src/main/java/gift/option/OptionController.java` |
| `gift.wish` | `WishController` | `src/main/java/gift/wish/WishController.java` |
| `gift.order` | `OrderController` | `src/main/java/gift/order/OrderController.java` |

## 3. 분석 결과

### 3-1. CategoryController

**엔드포인트 목록:**

| HTTP | URL | 메서드 |
|------|-----|--------|
| GET | `/api/categories` | `getCategories()` |
| POST | `/api/categories` | `createCategory()` |
| PUT | `/api/categories/{id}` | `updateCategory()` |
| DELETE | `/api/categories/{id}` | `deleteCategory()` |

**현재 비즈니스 로직:**
- `getCategories()`: Repository 조회 + DTO 변환 (L27-29)
- `createCategory()`: DTO → 엔티티 변환 + Repository 저장 (L35)
- `updateCategory()`: 엔티티 조회 + null 체크 + 필드 업데이트 + 저장 (L43-49)
- `deleteCategory()`: Repository 삭제 (L55)

**Service로 이동할 것:**
- `findAll()` — 전체 카테고리 목록 조회
- `findById(Long id)` — 단건 조회 + not found 처리
- `create(CategoryRequest)` — 엔티티 생성 + 저장
- `update(Long id, CategoryRequest)` — 조회 + 업데이트 + 저장
- `delete(Long id)` — 삭제

**Controller에 남길 것:**
- `@RequestBody`, `@PathVariable` 처리
- `ResponseEntity` 생성 (201 Created URI 포함)
- `CategoryResponse.from()` DTO 매핑

---

### 3-2. ProductController

**엔드포인트 목록:**

| HTTP | URL | 메서드 |
|------|-----|--------|
| GET | `/api/products` | `getProducts(Pageable)` |
| GET | `/api/products/{id}` | `getProduct()` |
| POST | `/api/products` | `createProduct()` |
| PUT | `/api/products/{id}` | `updateProduct()` |
| DELETE | `/api/products/{id}` | `deleteProduct()` |

**현재 비즈니스 로직:**
- `getProducts()`: 페이징 조회 (L34)
- `getProduct()`: 단건 조회 + null 체크 (L40-41)
- `createProduct()`: 이름 검증 + 카테고리 조회 + 엔티티 생성 + 저장 (L49-56)
- `updateProduct()`: 이름 검증 + 카테고리 조회 + 상품 조회 + 업데이트 + 저장 (L64-78)
- `deleteProduct()`: 삭제 (L83)
- `validateName()`: `ProductNameValidator` 호출 (L87-92)

**크로스 도메인 의존성:** `CategoryRepository` 직접 사용 (L51, L66)

**Service로 이동할 것:**
- `findAll(Pageable)` — 페이징 조회
- `findById(Long id)` — 단건 조회 + not found 처리
- `create(name, price, imageUrl, categoryId, allowKakao)` — 이름 검증 + 카테고리 조회 + 생성
- `update(id, name, price, imageUrl, categoryId, allowKakao)` — 이름 검증 + 카테고리 조회 + 수정
- `delete(Long id)` — 삭제

**Controller에 남길 것:**
- HTTP 요청/응답 처리
- `ProductResponse.from()` DTO 매핑
- `@ExceptionHandler(IllegalArgumentException.class)`

---

### 3-3. AdminProductController

**엔드포인트 목록:**

| HTTP | URL | 메서드 |
|------|-----|--------|
| GET | `/admin/products` | `list()` |
| GET | `/admin/products/new` | `newForm()` |
| POST | `/admin/products` | `create()` |
| GET | `/admin/products/{id}/edit` | `editForm()` |
| POST | `/admin/products/{id}/edit` | `update()` |
| POST | `/admin/products/{id}/delete` | `delete()` |

**현재 비즈니스 로직:**
- `list()`: 전체 상품 조회 (L28)
- `create()`: 이름 검증 + 카테고리 조회 + 상품 저장 (L45-54)
- `editForm()`: 상품 조회 + 카테고리 목록 조회 (L60-61)
- `update()`: 상품 조회 + 이름 검증 + 카테고리 조회 + 업데이트 (L75-89)
- `delete()`: 삭제 (L94)

**특이사항:** Admin 폼 검증은 `allowKakao=true`로 호출 (L45, L78). 검증 실패 시 에러와 함께 폼을 다시 보여주는 로직은 Controller에 남겨야 함.

**Service 공유:** `ProductController`와 동일한 `ProductService`를 공유. 추가로 `CategoryService.findAll()`을 사용하여 카테고리 목록 조회.

---

### 3-4. MemberController

**엔드포인트 목록:**

| HTTP | URL | 메서드 |
|------|-----|--------|
| POST | `/api/members/register` | `register()` |
| POST | `/api/members/login` | `login()` |

**현재 비즈니스 로직:**
- `register()`: 이메일 중복 확인 + 회원 저장 + JWT 생성 (L33-38)
- `login()`: 이메일로 회원 조회 + 비밀번호 검증 + JWT 생성 (L44-52)

**크로스 도메인 의존성:** `JwtProvider` 직접 사용

**Service로 이동할 것:**
- `register(email, password)` → JWT 토큰 문자열 반환
- `login(email, password)` → JWT 토큰 문자열 반환

**Controller에 남길 것:**
- `TokenResponse` 래핑
- `@ExceptionHandler`

---

### 3-5. AdminMemberController

**엔드포인트 목록:**

| HTTP | URL | 메서드 |
|------|-----|--------|
| GET | `/admin/members` | `list()` |
| GET | `/admin/members/new` | `newForm()` |
| POST | `/admin/members` | `create()` |
| GET | `/admin/members/{id}/edit` | `editForm()` |
| POST | `/admin/members/{id}/edit` | `update()` |
| POST | `/admin/members/{id}/charge-point` | `chargePoint()` |
| POST | `/admin/members/{id}/delete` | `delete()` |

**현재 비즈니스 로직:**
- `list()`: 전체 회원 조회 (L28)
- `create()`: 이메일 중복 확인 + 회원 저장 (L39-44)
- `editForm()`: 회원 조회 (L50-51)
- `update()`: 회원 조회 + 업데이트 + 저장 (L59-63)
- `chargePoint()`: 회원 조회 + 포인트 충전 + 저장 (L69-72)
- `delete()`: 삭제 (L79)

**Service 공유:** `MemberController`와 동일한 `MemberService`를 공유.

---

### 3-6. KakaoAuthController

**엔드포인트 목록:**

| HTTP | URL | 메서드 |
|------|-----|--------|
| GET | `/api/auth/kakao/login` | `login()` |
| GET | `/api/auth/kakao/callback` | `callback()` |

**현재 비즈니스 로직:**
- `login()`: 카카오 인가 URL 구성 (L41-47) — HTTP 리다이렉트 로직이므로 Controller에 유지
- `callback()`: 카카오 토큰 교환 + 사용자 정보 조회 + 회원 find-or-create + 토큰 갱신 + JWT 발급 (L56-65)

**Service로 이동할 것:**
- `loginWithKakao(email, kakaoAccessToken)` → JWT 토큰 문자열 반환 (회원 find-or-create + 토큰 갱신 + JWT 발급)

**Controller에 남길 것:**
- `login()` 전체 (HTTP 리다이렉트)
- `callback()` 중 카카오 API 호출 (`KakaoLoginClient` 사용)
- `TokenResponse` 래핑

---

### 3-7. OptionController

**엔드포인트 목록:**

| HTTP | URL | 메서드 |
|------|-----|--------|
| GET | `/api/products/{productId}/options` | `getOptions()` |
| POST | `/api/products/{productId}/options` | `createOption()` |
| DELETE | `/api/products/{productId}/options/{optionId}` | `deleteOption()` |

**현재 비즈니스 로직:**
- `getOptions()`: 상품 존재 확인 + 옵션 목록 조회 (L35-38)
- `createOption()`: 이름 검증 + 상품 조회 + 중복 확인 + 저장 (L48-59)
- `deleteOption()`: 상품 조회 + 최소 1개 제약 + 옵션 존재/소속 확인 + 삭제 (L66-81)

**크로스 도메인 의존성:** `ProductRepository` 직접 사용

**Service로 이동할 것:**
- `findByProductId(Long productId)` — 상품 존재 확인 + 옵션 목록 조회
- `create(productId, name, quantity)` — 이름 검증 + 상품 확인 + 중복 확인 + 저장
- `delete(productId, optionId)` — 상품 확인 + 최소 1개 제약 + 소속 확인 + 삭제

**Controller에 남길 것:**
- HTTP 요청/응답 처리
- `OptionResponse.from()` DTO 매핑
- `@ExceptionHandler`

---

### 3-8. WishController

**엔드포인트 목록:**

| HTTP | URL | 메서드 |
|------|-----|--------|
| GET | `/api/wishes` | `getWishes()` |
| POST | `/api/wishes` | `addWish()` |
| DELETE | `/api/wishes/{id}` | `removeWish()` |

**현재 비즈니스 로직:**
- `getWishes()`: 인증 확인 + 페이징 조회 (L39-43)
- `addWish()`: 인증 확인 + 상품 존재 확인 + 중복 확인 + 저장 (L52-71)
- `removeWish()`: 인증 확인 + 위시 존재 확인 + 소유권 확인 + 삭제 (L78-93)

**크로스 도메인 의존성:** `ProductRepository`, `AuthenticationResolver` 직접 사용

**특이사항:**
- `addWish()`: 중복 시 기존 위시 200 반환, 새 위시 201 반환 — 이 구분을 위해 `AddWishResult` record 필요
- `removeWish()`: 다른 회원의 위시 삭제 시 403 반환

**Service로 이동할 것:**
- `findByMemberId(memberId, pageable)` — 페이징 조회
- `addWish(memberId, productId)` → `AddWishResult(wish, created)` 반환
- `removeWish(memberId, wishId)` — 존재 확인 + 소유권 확인 + 삭제

**Controller에 남길 것:**
- 인증 확인 (`authenticationResolver.extractMember()` + 401)
- 200/201 구분
- `WishResponse.from()` DTO 매핑

---

### 3-9. OrderController (핵심 리팩토링 대상)

**엔드포인트 목록:**

| HTTP | URL | 메서드 |
|------|-----|--------|
| GET | `/api/orders` | `getOrders()` |
| POST | `/api/orders` | `createOrder()` |

**현재 비즈니스 로직:**
- `getOrders()`: 인증 확인 + 페이징 조회 (L44-48)
- `createOrder()`: 7단계 오케스트레이션 (L62-91)
  1. 인증 확인 (L64-67)
  2. 옵션 조회 + 검증 (L70-73)
  3. 재고 차감 `option.subtractQuantity()` + 저장 (L76-77)
  4. 포인트 차감 `member.deductPoint()` + 저장 (L80-82)
  5. 주문 저장 (L85)
  6. 카카오 알림 전송 (best-effort) (L88)
  7. 응답 반환 (L89-90)
- `sendKakaoMessageIfPossible()`: 카카오 메시지 전송 (L93-101)

**크로스 도메인 의존성:** `OptionRepository`, `MemberRepository`, `KakaoMessageClient`, `AuthenticationResolver`

**트랜잭션 문제:** 현재 재고 차감(3단계)과 포인트 차감(4단계)이 별도 `save()` 호출로 이루어져 있어, 중간 실패 시 부분 상태가 DB에 남을 수 있음. Service에서 `@Transactional`로 묶어야 함.

**Service로 이동할 것:**
- `findByMemberId(memberId, pageable)` — 페이징 조회
- `createOrder(memberId, optionId, quantity, message)` — 전체 오케스트레이션 (@Transactional)
- `sendKakaoMessageIfPossible()` — private 헬퍼

**Controller에 남길 것:**
- 인증 확인 + 401
- `OrderResponse.from()` DTO 매핑
- 201 Created 응답 생성

## 4. 대안 비교: 메서드 단위 추출 vs 도메인 단위 추출

### 대안 A: 메서드 단위 추출

각 Controller 메서드를 개별적으로 Service 메서드로 추출한다.

**장점:**
- 변경 범위가 작아 실수 위험 낮음
- 메서드 하나씩 테스트 가능

**단점:**
- Service 클래스 간 경계가 모호해질 수 있음
- 여러 Controller가 같은 Repository를 사용하는 경우 중복 Service 메서드 발생

### 대안 B: 도메인 단위 추출 (선택)

도메인(Entity)당 하나의 Service를 만들고, 해당 도메인과 관련된 모든 비즈니스 로직을 한 곳에 모은다.

**장점:**
- 도메인 응집도가 높아 유지보수 용이
- `ProductController`와 `AdminProductController`가 같은 `ProductService`를 공유하므로 중복 제거
- `MemberController`, `AdminMemberController`, `KakaoAuthController`가 같은 `MemberService`를 공유

**단점:**
- 초기 변경 범위가 다소 넓음

### 결정: 대안 B (도메인 단위 추출)

`ProductService`를 2개 Controller가 공유하고, `MemberService`를 3개 Controller가 공유하는 구조상 도메인 단위 추출이 자연스럽다.

## 5. 추출 계획 요약

| Service | 패키지 | 사용하는 Controller | 복잡도 |
|---------|--------|---------------------|--------|
| `CategoryService` | `gift.category` | `CategoryController` | 낮음 |
| `ProductService` | `gift.product` | `ProductController`, `AdminProductController` | 중간 |
| `MemberService` | `gift.member` | `MemberController`, `AdminMemberController`, `KakaoAuthController` | 중간 |
| `OptionService` | `gift.option` | `OptionController` | 중간 |
| `WishService` | `gift.wish` | `WishController` | 중간 |
| `OrderService` | `gift.order` | `OrderController` | 높음 |

### 추출 순서

단순한 것부터 복잡한 것 순서로 진행하여 패턴을 점진적으로 확립:

1. **CategoryService** — 가장 단순한 CRUD, 패턴 확립용
2. **ProductService** — 2개 Controller 공유, 크로스 도메인(Category) 의존성 포함
3. **MemberService** — 3개 Controller 공유, JWT 발급 포함
4. **OptionService** — 크로스 도메인(Product) 의존성 + 비즈니스 제약조건
5. **WishService** — 인증 연동 + 소유권 검증 + 200/201 구분
6. **OrderService** — 다중 도메인 오케스트레이션 + 트랜잭션 + 외부 API

### 예외 처리 전략

| 예외 | HTTP 상태 | 사용처 |
|------|----------|--------|
| `NoSuchElementException` | 404 | 엔티티 미발견 |
| `IllegalArgumentException` | 400 | 비즈니스 검증 실패 (이름 검증, 중복 확인, 재고 부족 등) |
| `IllegalStateException` | 403 | 위시 소유권 검증 실패 |

### 트랜잭션 전략

| 메서드 유형 | 어노테이션 |
|------------|-----------|
| 읽기 (조회) | `@Transactional(readOnly = true)` |
| 쓰기 (생성/수정/삭제) | `@Transactional` |
| `OrderService.createOrder()` | `@Transactional` (재고 차감 + 포인트 차감 + 주문 저장을 원자적으로 묶음) |

---

> 분석 일자: 2026-02-26
