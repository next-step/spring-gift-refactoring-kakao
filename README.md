# spring-gift-refactoring

## 2단계 - 리팩터링 완성하기

> 목표: 작동 변경을 안전하게 수행하고, 그 결과를 증거(테스트)로 보여준다.

---

### 1. 트랜잭션 경계 세우기

**문제:** `OrderService.createOrder()`가 재고 차감 → 포인트 차감 → 주문 저장을 각각 독립적으로 수행하여, 중간 실패 시 부분 반영(재고만 차감되고 주문은 생성되지 않는 상태)이 발생할 수 있었다.

**해결:**
- `createOrder()`에 `@Transactional` 추가 — 3개 DB 작업이 하나의 원자적 단위로 실행
- `findByMemberId()`에 `@Transactional(readOnly = true)` 추가
- 카카오 알림은 트랜잭션 내부에서 best-effort로 유지 (예외 전파 없음)

**증거:**
- `createOrderInsufficientPoints_stockRemainsUnchanged` — 포인트 부족 시 주문 실패 후 재고가 원래대로 유지됨을 DB 재조회로 검증

**ADR:** 카카오 알림을 트랜잭션 밖으로 분리하지 않은 이유 → `docs/adr/001-transaction-kakao-notification.md`

---

### 2. 누락된 작동 구현 — 주문 시 위시리스트 자동 정리

**문제:** 주문 흐름 7단계 중 6단계 "cleanup wish"가 미구현. `WishRepository`가 주입만 되고 사용되지 않았다. 주문 완료된 상품이 위시리스트에 그대로 남아있었다.

**해결:**
- `WishRepository`에 `deleteByMemberIdAndProductId()` 추가
- `WishService`에 `removeByMemberIdAndProductId()` 추가 — 위시에 없으면 예외 없이 0건 삭제
- `OrderService.createOrder()`에서 주문 저장 후 위시 정리 호출
- 기존 `@Transactional` 안에서 실행되어 원자성 보장

**증거:**
- `createOrder_removesWish` — 위시리스트에 상품 추가 → 주문 → 위시리스트에서 제거됨을 DB 재조회로 검증

---

### 3. 누락된 작동 구현 — 카테고리별 상품 목록 필터링

**문제:** API 명세에 `GET /api/products?categoryId=1` 필터링이 정의되어 있으나, `categoryId` 파라미터를 무시하고 전체 상품을 반환하고 있었다.

**해결:**
- `ProductRepository`에 `findByCategoryId(Long categoryId, Pageable pageable)` 추가
- `ProductService.findAll(Long categoryId, Pageable pageable)` — categoryId가 null이면 전체 조회, 있으면 필터링
- `ProductController.getProducts()`에 `@RequestParam(required = false) Long categoryId` 추가

**증거:**
- `getProductsByCategoryId` — 카테고리 A, B에 상품 등록 후 categoryId=A로 조회 → A 카테고리 상품만 반환됨 확인
- `getProductsWithoutCategoryId` — categoryId 없이 조회 → 전체 상품 반환됨 확인

---

### 4. 도메인 책임 되찾기 (1) — 주문 금액 계산을 Option 도메인으로 이동

**문제:** `OrderService`에서 `option.getProduct().getPrice() * quantity`로 가격 계산. 디미터 법칙 위반(열차 충돌 호출)이며 계산 책임이 서비스에 누수되어 있었다.

**해결:**
```java
// Before: 서비스에서 직접 계산 (디미터 법칙 위반)
var price = option.getProduct().getPrice() * quantity;

// After: 도메인 객체에 위임
var price = option.calculateTotalPrice(quantity);
```

**증거:**
- `calculateTotalPriceWithSingleQuantity`, `calculateTotalPriceWithMultipleQuantity` — Option 단위 테스트
- 기존 `OrderControllerTest` 전체 통과 (작동 유지 확인)

---

### 5. 도메인 책임 되찾기 (2) — validateName 중복 제거

**문제:** `ProductService`와 `OptionService`에 동일한 `validateName()` private 메서드가 95% 중복 존재. 에러 수집 → 예외 변환 패턴이 반복.

**해결:**
```java
// Before: 두 서비스에 동일한 메서드 반복
private void validateName(String name) {
    List<String> errors = NAME_VALIDATOR.validate(name);
    if (!errors.isEmpty()) throw new IllegalArgumentException(String.join(", ", errors));
}

// After: NameValidator에 통합, 서비스에서 메서드 삭제
NAME_VALIDATOR.validateOrThrow(name);
```

**증거:**
- 기존 `NameValidatorTest`, `ProductControllerTest`, `OptionControllerTest` 전체 통과

---

### 6. 버그 수정 — 위시 중복 추가 시 HTTP 상태코드

**문제:** `WishController.addWish()`가 이미 존재하는 위시를 다시 추가해도 항상 `201 Created`를 반환. 멱등 동작이지만 상태코드가 이를 반영하지 않았다.

**해결:**
- `WishService.existsByMemberIdAndProductId()` 추가
- 새 위시 → `201 Created`, 기존 위시 → `200 OK` 분기

**증거:**
- `addDuplicateWish` — 기존 위시 재추가 시 200 OK 반환 검증
- 전체 130개 테스트 통과

---

### Claude Code Agent Team 활용

이번 단계에서는 Claude Code의 Agent Team 기능을 활용하여 5개 작업을 병렬로 진행했다.

**팀 구성:** 팀 리더 1명 + 작업 에이전트 5명

| 에이전트 | 담당 작업 |
|---------|----------|
| agent-1-transaction | 트랜잭션 경계 세우기 |
| agent-2-wish-cleanup | 주문 시 위시리스트 정리 |
| agent-3-category-filter | 카테고리별 상품 필터링 |
| agent-4-domain-price | 주문 금액 계산 도메인 이동 |
| agent-5-validate-dedup | validateName 중복 제거 |

**워크플로우:** 각 에이전트가 2단계로 작업을 진행했다.
1. **Phase 1 (계획):** 변경 대상 파일, 테스트 케이스, 바꾸지 않는 것을 명시한 실행 계획 보고서 작성 → 사용자 검토/승인
2. **Phase 2 (실행):** 승인된 계획대로 코드 변경 → 테스트 통과 확인 → 완료 보고서 작성

**병렬 실행:** 작업 1, 3, 4, 5는 서로 다른 파일을 수정하므로 동시 진행. 작업 2는 작업 1(트랜잭션)에 의존하여 순차 진행.

**팀 프롬프트:** `docs/plan/AGENT_TEAM_PROMPTS.md`

---

### 문서

| 분류 | 경로 |
|------|------|
| 요구사항 | `docs/plan/REQUIREMENT_STEP2.md` |
| 실행 계획 | `docs/plan/TODO_STEP2.md` |
| 팀 프롬프트 | `docs/plan/AGENT_TEAM_PROMPTS.md` |
| ADR | `docs/adr/001-transaction-kakao-notification.md` |
| 작업 보고서 | `docs/reports/step2/` |
| 1단계 정리 | `docs/plan/OVERVIEW_STEP1.md` |
