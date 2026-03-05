# Progress 12: Product 카테고리 필터 — `GET /api/products?categoryId={id}` 추가

**기간**: 2026-03-05

---

## 이 문서의 목적

Phase 2 "누락된 작동 구현" — 2-4번 작업. 기존 `GET /api/products` 엔드포인트에 optional `categoryId` 쿼리 파라미터를 추가하여 카테고리별 상품 필터링을 지원한다.

**변경 유형**: 작동 변경 (feat) — 기존 엔드포인트에 선택적 필터 파라미터 추가.

---

## 1. 설계 결정

| 항목                 | 결정                                | 이유                                                          |
|--------------------|-----------------------------------|-------------------------------------------------------------|
| categoryId 필수 여부   | **optional** (`required = false`) | 미지정 시 기존과 동일하게 전체 조회 → 하위 호환 보장                             |
| 존재하지 않는 categoryId | **빈 페이지 반환** (200)                | 404가 아닌 필터 시맨틱 — "해당 조건의 결과가 없음"                            |
| 페이지네이션             | 필터와 동시 지원                         | `Pageable` 파라미터 유지                                          |
| Repository 쿼리      | JPA 파생 쿼리                         | `findByCategoryId(Long, Pageable)` — Product.category.id 탐색 |

---

## 2. 변경 내용

### 2-1. ProductRepository — 파생 쿼리 추가

```java
Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
```

Spring Data JPA가 Product Entity의 `category.id`를 탐색하는 쿼리를 자동 생성한다.

### 2-2. ProductService — `getProducts` 시그니처 변경

```java
// Before
public PagedModel<ProductResponse> getProducts(Pageable pageable)

// After
public PagedModel<ProductResponse> getProducts(Long categoryId, Pageable pageable) {
    Page<Product> page = categoryId == null ?
            productRepo.findAll(pageable) :
            productRepo.findByCategoryId(categoryId, pageable);

    Page<ProductResponse> pageResponse = page.map(ProductResponse::from);

    return new PagedModel<>(pageResponse);
}
```

- `categoryId == null` → `findAll()` (기존 동작 유지)
- `categoryId != null` → `findByCategoryId()` (카테고리 필터)
- `@Transactional` 불필요 — 읽기 전용

### 2-3. ProductController — `@RequestParam` 추가

```java

@GetMapping
public ResponseEntity<PagedModel<ProductResponse>> getProducts(
        @RequestParam(required = false) Long categoryId,
        Pageable pageable
) {
    PagedModel<ProductResponse> response = productService.getProducts(categoryId, pageable);
    ...
}
```

---

## 3. 단위 테스트

### ProductServiceTest 변경

파일: `src/test/java/gift/product/internal/ProductServiceTest.java`

| 테스트                                 | 변경                                        | 검증 내용                                           |
|-------------------------------------|-------------------------------------------|-------------------------------------------------|
| `testGetProducts`                   | **수정** — `getProducts(null, pageable)` 호출 | categoryId null → `findAll()` 호출 (하위 호환)        |
| `testGetProductsWithCategoryFilter` | **추가**                                    | categoryId 지정 → `findByCategoryId()` 호출 + 응답 매핑 |

---

## 4. 인수 테스트

### 추가 시나리오 (3개)

파일: `src/test/resources/features/product.feature`

| ID   | 시나리오                     | 유형            | 검증 내용                                        |
|------|--------------------------|---------------|----------------------------------------------|
| P-F1 | 카테고리별 상품 필터링 성공          | happy         | 카테고리에 상품 2개 → 해당 카테고리로 필터 → content 크기 2     |
| P-F2 | 다른 카테고리 상품은 필터링되어 제외된다   | happy + state | 카테고리A(1개) + 카테고리B(1개) → A로 필터 → content 크기 1 |
| P-F3 | 존재하지 않는 카테고리로 필터링 시 빈 결과 | happy         | 없는 categoryId → 200 + content 크기 0           |

### Step Definition 추가 (3개)

파일: `src/test/java/gift/acceptance/steps/ProductApiSteps.java`

| Step                                       | 동작                                                 |
|--------------------------------------------|----------------------------------------------------|
| `@When("해당 카테고리의 상품 목록 조회 요청을 보낸다")`       | `GET /api/products?categoryId={currentCategoryId}` |
| `@When("{string} 카테고리의 상품 목록 조회 요청을 보낸다")` | `GET /api/products?categoryId={이름으로 조회한 ID}`       |
| `@When("존재하지 않는 카테고리의 상품 목록 조회 요청을 보낸다")`  | `GET /api/products?categoryId={NON_EXISTENT_ID}`   |

### 하위 호환 검증

기존 P-3 시나리오(페이지네이션 테스트)는 `categoryId` 없이 호출하므로 `null`로 전달되어 기존 동작 그대로 통과한다.

---

## 5. 검증 결과

```bash
./gradlew test --tests "gift.product.internal.ProductServiceTest"  # 11개 통과
./gradlew test                # BUILD SUCCESSFUL (전체 단위 테스트 통과)
./gradlew acceptanceTest      # BUILD SUCCESSFUL (기존 79개 + 신규 3개 = 82개 시나리오)
```

---

## 6. 수정/생성 파일 목록

| 파일                                                            | 변경                                                   |
|---------------------------------------------------------------|------------------------------------------------------|
| `src/main/java/gift/product/internal/ProductRepository.java`  | `findByCategoryId` 파생 쿼리 추가                          |
| `src/main/java/gift/product/internal/ProductService.java`     | `getProducts` 시그니처 변경 + categoryId 분기                |
| `src/main/java/gift/product/internal/ProductController.java`  | `@RequestParam(required = false) Long categoryId` 추가 |
| `src/test/java/gift/product/internal/ProductServiceTest.java` | 기존 테스트 수정 + 필터 테스트 추가                                |
| `src/test/resources/features/product.feature`                 | 3개 시나리오 추가 (P-F1, P-F2, P-F3)                        |
| `src/test/java/gift/acceptance/steps/ProductApiSteps.java`    | 3개 When step 추가                                      |

---

## 7. Phase 2 누락 작동 구현 진행 현황

| 작업  | 설명                                   | 상태     |
|-----|--------------------------------------|--------|
| 2-1 | 주문 부수 효과 (위시 정리 + 카카오 알림 이벤트 분리)     | 미착수    |
| 2-2 | Option PUT (옵션 수정 엔드포인트)             | 미착수    |
| 2-3 | Category GET /{id} (카테고리 단건 조회)      | **완료** |
| 2-4 | Product 카테고리 필터 (`?categoryId={id}`) | **완료** |
