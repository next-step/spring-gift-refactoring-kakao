# 📋 2단계 TODO - 작동 변경 실행 계획

> 작성일: 2026-03-05
> 목표: 작동 변경을 안전하게 수행하고, 그 결과를 증거(테스트)로 보여준다.

---

## 원칙

- **구조 변경 커밋**과 **작동 변경 커밋**을 분리한다.
- 작동 변경은 반드시 **상태를 재조회하여 검증하는 테스트**와 함께 제출한다.
- 각 작업 전에 "무엇을 바꾸는지 / 무엇을 바꾸지 않는지 / 무엇이 이를 증명하는지"를 확인한다.

---

## 작업 1: 트랜잭션 경계 세우기 ✅

### 현황
- `OrderService.createOrder()`가 3개의 독립적인 DB 저장을 수행:
  1. `optionRepository.save(option)` — 재고 차감
  2. `memberRepository.save(member)` — 포인트 차감
  3. `orderRepository.save(new Order(...))` — 주문 저장
- `@Transactional`이 없어 **중간 실패 시 부분 반영** 발생 가능
  - 예: 재고는 차감됐는데 포인트 차감에서 실패하면, 재고만 줄어든 채로 남음

### 변경 내용
- `OrderService.createOrder()`에 `@Transactional` 추가
- 읽기 전용 메서드(`findByMemberId`)에 `@Transactional(readOnly = true)` 추가

### 바꾸지 않는 것
- 카카오 알림(`sendOrderNotification`)은 트랜잭션 내부에서 best-effort로 유지 (예외 전파 없음)

### 증거
- 테스트: 포인트 부족 시 주문 실패 → **재고가 원래대로 유지됨**을 재조회로 확인 (`createOrderInsufficientPoints_stockRemainsUnchanged`)
- 기존 `OrderControllerTest`의 정상 주문 테스트 통과 확인

### 커밋 전략
- **작동 변경 커밋**: `@Transactional` 추가 + 트랜잭션 롤백 검증 테스트

### 결과물
- `docs/reports/step2/transaction-boundary.md`
- `docs/adr/001-transaction-kakao-notification.md`

---

## 작업 2: 누락된 작동 구현 — 주문 시 위시리스트 정리 ✅

### 현황
- 주문 흐름 7단계 중 **6단계 "cleanup wish"가 미구현**
- `WishRepository`가 `OrderController`(리팩토링 전)에 주입만 되어 있고 사용되지 않음
- 주문 시 해당 상품이 위시리스트에 있어도 그대로 남아있는 상태

### 변경 내용
- `OrderService.createOrder()`에 위시리스트 정리 로직 추가
- 주문한 상품이 해당 회원의 위시리스트에 있으면 자동 제거
- `WishService`에 `removeByMemberIdAndProductId(Long memberId, Long productId)` 메서드 추가
- `WishRepository`에 `deleteByMemberIdAndProductId` 추가
- 위시에 없는 경우에는 아무 일도 하지 않음 (예외 없이 정상 진행)

### 바꾸지 않는 것
- 위시리스트에 없는 상품을 주문하는 기존 흐름은 영향 없음
- 기존 위시 CRUD API 동작 유지

### 증거
- 테스트: 위시리스트에 상품 추가 → 해당 상품 주문 → **위시리스트에서 제거됨**을 재조회로 확인 (`createOrder_removesWish`)
- 테스트: 위시리스트에 없는 상품 주문 → **정상 처리됨** 확인

### 커밋 전략
- **작동 변경 커밋**: 위시 정리 구현 + 검증 테스트

### 결과물
- `docs/reports/step2/order-wish-cleanup.md`

---

## 작업 3: 누락된 작동 구현 — 상품 목록 카테고리별 필터링 ✅

### 현황
- API 명세: `GET /api/products?page=0&size=10&sort=name,asc&categoryId=1`
- 현재 구현: `categoryId` 파라미터를 **무시**하고 전체 상품을 반환
- `ProductRepository`에 카테고리 필터링 쿼리 메서드 없음

### 변경 내용
- `ProductRepository`에 `findByCategoryId(Long categoryId, Pageable pageable)` 추가
- `ProductService.findAll()`에 `categoryId` 파라미터 추가 (null이면 전체 조회, 값이 있으면 필터링)
- `ProductController.getProducts()`에 `@RequestParam(required = false) Long categoryId` 추가

### 바꾸지 않는 것
- `categoryId` 없이 호출하는 기존 동작은 그대로 유지 (전체 조회)
- 기존 상품 CRUD 동작 유지

### 증거
- 테스트: 카테고리 A, B 각각에 상품 등록 → `categoryId=A`로 조회 → **A 카테고리 상품만 반환됨** 확인 (`getProductsByCategoryId`)
- 테스트: `categoryId` 없이 조회 → **전체 상품 반환됨** 확인 (`getProductsWithoutCategoryId`)

### 커밋 전략
- **작동 변경 커밋**: 카테고리 필터 구현 + 검증 테스트

### 결과물
- `docs/reports/step2/category-product-filter.md`

---

## 작업 4: 도메인 책임 되찾기 (1) — 주문 금액 계산을 도메인으로 이동 ✅

### 현황 (개선 전)
```java
// OrderService.createOrder() 내부
var price = option.getProduct().getPrice() * quantity;
memberService.deductPoint(member, price);
```
- 가격 계산(`price * quantity`)이 서비스 레이어에 존재
- `option.getProduct().getPrice()`로 디미터 법칙 위반 (열차 충돌 호출)

### 변경 내용 (개선 후)
```java
// Option 도메인에 메서드 추가
public int calculateTotalPrice(int quantity) {
    return product.getPrice() * quantity;
}

// OrderService.createOrder()
var price = option.calculateTotalPrice(quantity);
memberService.deductPoint(member, price);
```
- 가격 계산 책임을 `Option` 도메인 객체로 이동
- 호출부 단순화: 디미터 법칙 위반 제거

### 바꾸지 않는 것
- 계산 결과(가격 × 수량)는 동일
- 기존 테스트 모두 통과

### 증거
- 기존 `OrderControllerTest` 전체 통과 (작동 유지 확인)
- `Option.calculateTotalPrice()` 단위 테스트 추가 (`calculateTotalPriceWithSingleQuantity`, `calculateTotalPriceWithMultipleQuantity`)

### 커밋 전략
- **구조 변경 커밋**: 도메인 메서드 추가 + 호출부 변경

### 결과물
- `docs/reports/step2/option-calculate-total-price.md`

---

## 작업 5: 도메인 책임 되찾기 (2) — validateName 중복 제거 ✅

### 현황 (개선 전)
```java
// ProductService (lines 16-20, 61-66) — 95% 동일
private static final NameValidator NAME_VALIDATOR = NameValidator.of("Product name", ...);
private void validateName(String name) {
    List<String> errors = NAME_VALIDATOR.validate(name);
    if (!errors.isEmpty()) throw new IllegalArgumentException(String.join(", ", errors));
}

// OptionService (lines 14-17, 69-74) — 95% 동일
private static final NameValidator NAME_VALIDATOR = NameValidator.of("Option name", ...);
private void validateName(String name) {
    List<String> errors = NAME_VALIDATOR.validate(name);
    if (!errors.isEmpty()) throw new IllegalArgumentException(String.join(", ", errors));
}
```
- 두 서비스에 validateName 메서드가 거의 동일하게 중복
- 에러 수집 → 예외 변환 패턴이 반복

### 변경 내용 (개선 후)
```java
// NameValidator에 validateOrThrow 메서드 추가
public void validateOrThrow(String name) {
    List<String> errors = validate(name);
    if (!errors.isEmpty()) {
        throw new IllegalArgumentException(String.join(", ", errors));
    }
}

// ProductService — 메서드 제거, 직접 호출
NAME_VALIDATOR.validateOrThrow(name);

// OptionService — 메서드 제거, 직접 호출
NAME_VALIDATOR.validateOrThrow(name);
```
- `NameValidator`에 검증+예외 변환을 통합하여 중복 제거
- 각 서비스의 `private void validateName()` 메서드 삭제

### 바꾸지 않는 것
- 검증 규칙 자체(maxLength, allowedCharacters, noKakao)는 변경 없음
- 예외 메시지 형식 동일

### 증거
- 기존 `NameValidatorTest` 전체 통과
- 기존 `ProductControllerTest`, `OptionControllerTest` 전체 통과

### 커밋 전략
- **구조 변경 커밋**: NameValidator에 메서드 추가 + 서비스 중복 제거

### 결과물
- `docs/reports/step2/validate-name-dedup.md`

---

## 작업 6: 버그 수정 — 위시 중복 추가 시 HTTP 상태코드 ✅

### 현황
- `WishController.addWish()`가 항상 `201 Created` 반환
- 이미 존재하는 위시를 다시 추가해도 `201` 반환 → 테스트 실패 (`addDuplicateWish`)
- `WishService.addWish()`는 중복 시 기존 항목을 반환하는 멱등 동작

### 변경 내용
- `WishService`에 `existsByMemberIdAndProductId()` 메서드 추가
- `WishController.addWish()`에서 중복 여부에 따라 상태코드 분기:
  - 새 위시 → `201 Created`
  - 기존 위시 → `200 OK`

### 바꾸지 않는 것
- `WishService.addWish()`의 멱등 동작 (기존 항목 반환)은 유지
- 기존 위시 추가/삭제/조회 동작 유지

### 증거
- `WishControllerTest.addDuplicateWish` 테스트 통과 (200 OK 검증)
- 전체 130개 테스트 통과

### 커밋 전략
- **작동 변경 커밋**: 위시 중복 추가 상태코드 수정

---

## 실행 순서 요약

| # | 작업 | 유형 | 상태 | 핵심 증거 |
|---|------|------|------|-----------|
| 1 | 트랜잭션 경계 추가 | 작동 변경 | ✅ | 실패 시 재고 롤백 재조회 테스트 |
| 2 | 위시리스트 정리 구현 | 작동 변경 | ✅ | 주문 후 위시 제거 재조회 테스트 |
| 3 | 카테고리별 상품 필터링 | 작동 변경 | ✅ | categoryId 필터 결과 검증 테스트 |
| 4 | 주문 금액 계산 도메인 이동 | 구조 변경 | ✅ | 기존 테스트 전체 통과 + 단위 테스트 |
| 5 | validateName 중복 제거 | 구조 변경 | ✅ | 기존 테스트 전체 통과 |
| 6 | 위시 중복 추가 상태코드 수정 | 작동 변경 | ✅ | addDuplicateWish 테스트 통과 |

### 커밋 순서
1. `feat(order): 트랜잭션 경계 추가로 주문 원자성 보장` — 작동 변경
2. `feat(order): 주문 시 위시리스트 자동 정리 구현` — 작동 변경
3. `feat(product): 카테고리별 상품 목록 필터링 구현` — 작동 변경
4. `refactor(option): 주문 금액 계산을 Option 도메인으로 이동` — 구조 변경
5. `refactor(validator): validateName 중복 제거` — 구조 변경
6. `fix(wish): 중복 위시 추가 시 200 OK 반환하도록 수정` — 작동 변경

---

## ADR 작성 대상

- **작업 1 (트랜잭션)**: 카카오 알림을 트랜잭션 내부에서 best-effort로 호출한 이유 → ✅ `docs/adr/001-transaction-kakao-notification.md`
- **작업 2 (위시 정리)**: 위시에 없는 경우 예외 없이 무시한 이유 → 트레이드오프가 명확하지 않으므로 ADR 불필요
- **작업 3 (카테고리 필터)**: 단순 구현이므로 ADR 불필요
