# Step 2 리팩토링 작업 계획

> 원칙: 각 요구사항에 대해 패키지 단위로 커밋을 분리한다.
> 구조 변경(외부 동작 불변)과 작동 변경(외부 동작 변경)은 같은 커밋에 섞지 않는다.

---

## 1. 도메인 책임 되찾기 (구조 변경 3건)

현재 서비스 레이어에 누수된 도메인 로직을 엔티티로 이동한다.

### 커밋 1-1. wish 패키지 — 소유권 검증을 도메인으로 이동

| 구분 | 내용 |
|------|------|
| 유형 | 구조 변경 |
| 변경 전 | `WishService.removeWish()`에서 `!wish.getMemberId().equals(memberId)` 직접 비교 |
| 변경 후 | `Wish.isOwnedBy(Long memberId)` 도메인 메서드 추가 → 서비스에서 호출 |

**대상 파일:**
- `gift/wish/Wish.java` — `isOwnedBy()` 메서드 추가
- `gift/wish/WishService.java` — 인라인 비교를 `wish.isOwnedBy(memberId)` 호출로 교체

**검증:** 기존 `WishControllerTest` 전체 통과 (외부 동작 동일)

---

### 커밋 1-2. option 패키지 — 주문 총액 계산을 도메인으로 이동

| 구분 | 내용 |
|------|------|
| 유형 | 구조 변경 |
| 변경 전 | `OrderService`에서 `option.getProduct().getPrice() * request.quantity()` 직접 계산 |
| 변경 후 | `Option.calculateTotalPrice(int quantity)` 도메인 메서드 추가 → 서비스에서 호출 |

**대상 파일:**
- `gift/option/Option.java` — `calculateTotalPrice(int quantity)` 메서드 추가
- `gift/order/OrderService.java` — `option.calculateTotalPrice(request.quantity())` 호출로 교체

**ADR 작성:** `docs/adr/001-price-calculation-location.md`
- 선택지: Option.calculateTotalPrice() vs Order.calculateTotalPrice()
- 결정: Option — Product 가격에 직접 접근 가능, Order 생성 전에도 계산 필요

**검증:** 기존 `OrderControllerTest` 전체 통과

---

### 커밋 1-3. product 패키지 — 카테고리 조회를 CategoryService로 이동

| 구분 | 내용 |
|------|------|
| 유형 | 구조 변경 |
| 변경 전 | `AdminProductController` → `ProductService.findAllCategories()` → `CategoryRepository` |
| 변경 후 | `AdminProductController` → `CategoryService.findAll()` (이미 존재하는 메서드) |

**대상 파일:**
- `gift/product/AdminProductController.java` — `CategoryService` 주입 추가, `productService.findAllCategories()` 3곳을 `categoryService.findAll()`로 교체
- `gift/product/ProductService.java` — `findAllCategories()` 메서드 삭제, `CategoryResponse` import 삭제

**검증:** 기존 `AdminProductControllerTest` 전체 통과

---

## 2. 누락된 작동 구현 (구조 1건 + 작동 1건)

OrderService의 null 반환 안티패턴과 OrderController의 ExceptionHandler 누락을 수정한다.

### 커밋 2-1. order 패키지 [구조] — null 반환을 예외로 전환

| 구분 | 내용 |
|------|------|
| 유형 | 구조 변경 |
| 변경 전 | `OrderService.createOrder()`에서 option 미존재 시 `null` 반환 → Controller에서 null 체크 후 404 |
| 변경 후 | `NoSuchElementException` throw → Controller의 `@ExceptionHandler`가 404 반환 |

**대상 파일:**
- `gift/order/OrderService.java` — `orElse(null)` + null 체크 → `orElseThrow(() -> new NoSuchElementException(...))`
- `gift/order/OrderController.java` — `response == null` 체크 제거 + `@ExceptionHandler(NoSuchElementException.class)` 추가

**검증:** 기존 `주문_생성_실패_옵션_미존재` 테스트가 여전히 404 반환 확인

---

### 커밋 2-2. order 패키지 [작동] — IllegalArgumentException 핸들러 추가 (500→400)

| 구분 | 내용 |
|------|------|
| 유형 | 작동 변경 |
| 변경 전 | 재고 부족 / 포인트 부족 시 `IllegalArgumentException` → Spring 기본 처리 → **500** |
| 변경 후 | `@ExceptionHandler(IllegalArgumentException.class)` 추가 → **400** |

**대상 파일:**
- `gift/order/OrderController.java` — `@ExceptionHandler(IllegalArgumentException.class)` 추가
- `gift/order/OrderControllerTest.java`:
  - `주문_생성_실패_재고_부족`: statusCode `500` → `400`
  - `주문_생성_실패_포인트_부족`: statusCode `500` → `400`

**상태 재조회 검증 추가 (재고 부족):**
```
1. POST /api/orders → 400 확인
2. GET /api/orders → content size 0 (주문 미생성)
3. GET /api/products/1/options → [0].quantity == 1 (재고 미변경)
```
> seed: option quantity=1, member point=1,000,000, 주문 수량 5

**상태 재조회 검증 추가 (포인트 부족):**
```
1. POST /api/orders → 400 확인
2. GET /api/orders → content size 0 (주문 미생성)
3. GET /api/products/1/options → [0].quantity == 10 (재고 미변경)
```
> seed: option quantity=10, member point=100, 상품 가격 10,000

---

## 3. 트랜잭션 경계 세우기 (구조 1건 + 작동 1건)

### 커밋 3-1. product 패키지 [구조] — 읽기 트랜잭션 기본값 정리

| 구분 | 내용 |
|------|------|
| 유형 | 구조 변경 |
| 변경 전 | `ProductService` 클래스 레벨 `@Transactional` (readOnly=false) — 읽기도 쓰기 트랜잭션 |
| 변경 후 | 클래스 레벨 `@Transactional(readOnly = true)` + 쓰기 메서드에만 `@Transactional` — 다른 서비스와 동일 패턴 |

**대상 파일:**
- `gift/product/ProductService.java`
  - 클래스: `@Transactional` → `@Transactional(readOnly = true)`
  - 쓰기 메서드 5개에 `@Transactional` 추가
  - 읽기 메서드에서 중복 `@Transactional(readOnly = true)` 제거

**검증:** 기존 `ProductControllerTest` + `AdminProductControllerTest` 전체 통과

---

### 커밋 3-2. order 패키지 [작동] — Member를 트랜잭션 내부에서 조회

| 구분 | 내용 |
|------|------|
| 유형 | 작동 변경 |
| 변경 전 | Controller에서 Member 엔티티 조회(트랜잭션 외부) → detached 상태로 Service에 전달 → stale point 읽기 가능 |
| 변경 후 | Service에서 memberId로 Member 조회(트랜잭션 내부) → managed 엔티티로 dirty checking 보장 |

**대상 파일:**
- `gift/order/OrderService.java`
  - `createOrder(Member member, ...)` → `createOrder(Long memberId, ...)`
  - 트랜잭션 내에서 `memberRepository.findById(memberId)` 호출
  - managed 엔티티이므로 `optionRepository.save()`, `memberRepository.save()` 명시 호출 제거
- `gift/order/OrderController.java`
  - `orderService.createOrder(member, request)` → `orderService.createOrder(member.getId(), request)`

**ADR 작성:** `docs/adr/002-order-transaction-boundary.md`
- 선택지 A: memberId(Long) 전달 후 서비스에서 조회 (선택)
- 선택지 B: email(String) 전달 후 findByEmail 사용
- 결정: A — ID 재사용으로 불필요한 쿼리 방지, managed 엔티티로 동시성 일관성 확보

**상태 재조회 검증 추가 (주문 성공):**
```
1. POST /api/orders → 201 확인
2. GET /api/orders → content size 1 (주문 생성 확인)
3. GET /api/products/1/options → [0].quantity == 8 (재고 차감: 10 - 2 = 8)
```
> seed: option quantity=10, member point=1,000,000, 상품 가격 10,000, 주문 수량 2

---

## 4. 외부 API 호출을 트랜잭션 밖으로 분리 (구조 2건)

외부 HTTP 호출이 `@Transactional` 내부에 있으면 네트워크 지연 동안 DB 커넥션을 점유한다.
Kakao API가 느려지면 커넥션 풀이 고갈되어 전체 서비스가 마비될 수 있다.

| 위치 | 외부 호출 | 영향 |
|------|----------|------|
| `KakaoAuthService.kakaoLogin()` | `requestAccessToken()` + `requestUserInfo()` 2회 | DB 커넥션을 2번의 HTTP 왕복 동안 점유 |
| `OrderService.createOrder()` | `sendKakaoMessageIfPossible()` → `kakaoMessageClient.sendToMe()` | 주문 트랜잭션이 메시지 전송 완료까지 유지 |

### 커밋 4-1. auth 패키지 [구조] — KakaoAuthService 외부 호출을 트랜잭션 밖으로 분리

| 구분 | 내용 |
|------|------|
| 유형 | 구조 변경 |
| 변경 전 | `kakaoLogin()`에 `@Transactional` → 2회 외부 HTTP 호출 동안 DB 커넥션 점유 |
| 변경 후 | 외부 호출 먼저 실행 → DB 작업만 `MemberService.findOrCreateByKakaoLogin()`에 위임 |





**대상 파일:**
- `gift/auth/KakaoAuthService.java` — `@Transactional` 제거, `MemberRepository` → `MemberService` 의존성 변경
- `gift/member/MemberService.java` — `findOrCreateByKakaoLogin()` 메서드 추가

**검증:** 기존 `KakaoAuthControllerTest` 전체 통과 (외부 동작 동일)

---

### 커밋 4-2. order 패키지 [구조] — 카카오 메시지 전송을 트랜잭션 밖으로 분리

> 의존: 커밋 3-2 이후 수행 (createOrder가 memberId를 받는 형태로 변경된 후)

| 구분 | 내용 |
|------|------|
| 유형 | 구조 변경 |
| 변경 전 | `createOrder()` 내부에서 `sendKakaoMessageIfPossible()` 호출 → 트랜잭션이 HTTP 호출까지 유지 |
| 변경 후 | Controller에서 `createOrder()` 호출 후 `sendKakaoMessageIfPossible()` 별도 호출 → 트랜잭션 밖에서 메시지 전송 |



**대상 파일:**
- `gift/order/OrderService.java` — `createOrder()`에서 `sendKakaoMessageIfPossible` 호출 제거, 메서드를 public으로 변경하고 시그니처를 `(String kakaoAccessToken, Long orderId)`로 변경
- `gift/order/OrderController.java` — `createOrder()` 호출 후 `sendKakaoMessageIfPossible()` 호출 추가

**검증:** 기존 `OrderControllerTest` 전체 통과 (외부 동작 동일)

---

## 실행 순서 요약

| 순서 | 커밋 | 요구사항 | 패키지 | 유형 |
|:---:|------|---------|--------|------|
| 1 | 1-1 | 도메인 책임 | wish | 구조 |
| 2 | 1-2 | 도메인 책임 | option | 구조 |
| 3 | 1-3 | 도메인 책임 | product | 구조 |
| 4 | 2-1 | 누락된 작동 | order | 구조 |
| 5 | 2-2 | 누락된 작동 | order | **작동** |
| 6 | 3-1 | 트랜잭션 경계 | product | 구조 |
| 7 | 3-2 | 트랜잭션 경계 | order | **작동** |
| 8 | 4-1 | 외부 API 분리 | auth | 구조 |
| 9 | 4-2 | 외부 API 분리 | order | 구조 |

> 4-2는 3-2에 의존 (createOrder의 memberId 시그니처 변경 후 수행)

## ADR 목록

| 파일 | 주제 | 해당 커밋 |
|------|------|----------|
| `docs/adr/001-price-calculation-location.md` | 가격 계산 로직 위치 결정 | 1-2 |
| `docs/adr/002-order-transaction-boundary.md` | 주문 생성 시 Member 조회 전략 | 3-2 |
| `docs/adr/003-external-api-outside-transaction.md` | 외부 API 호출을 트랜잭션 밖으로 분리 | 4-1, 4-2 |
