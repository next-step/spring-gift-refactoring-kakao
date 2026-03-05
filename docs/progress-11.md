# Progress 11: Category GET /{id} — 카테고리 단건 조회 엔드포인트 추가

**기간**: 2026-03-05

---

## 이 문서의 목적

Phase 2 "누락된 작동 구현" — 2-3번 작업. 카테고리 단건 조회 엔드포인트(`GET /api/categories/{id}`)를 추가했다. Product의 `GET /api/products/{id}`와 동일한 패턴을 적용했다. CategoryServiceTest(9개)를 신규 생성하고, 인수 테스트 시나리오 4개를 추가했다.

**변경 유형**: 작동 변경 (feat) — 새 엔드포인트 추가.

---

## 1. 변경 내용

### 1-1. CategoryService — `getCategory(Long)` 추가

```java
public CategoryResponse getCategory(Long categoryId) {
    Category find = categoryRepo.findById(categoryId)
            .orElseThrow(NotFoundException::categoryNotFound);

    return CategoryResponse.from(find);
}
```

- `@Transactional` 불필요 — 읽기 전용, 단순 값 반환
- `ProductService.getProduct()`와 동일 패턴

### 1-2. CategoryController — `GET /{id}` 엔드포인트 추가

```java

@GetMapping("/{id}")
public ResponseEntity<CategoryResponse> getCategory(
        @PathVariable Long id
) {
    CategoryResponse response = categoryService.getCategory(id);

    return ResponseEntity
            .ok(response);
}
```

---

## 2. 단위 테스트

### CategoryServiceTest (9개) — 신규

파일: `src/test/java/gift/category/internal/CategoryServiceTest.java`

기존에 CategoryServiceTest가 없었으므로 `getCategory` 뿐 아니라 전체 Service 메서드를 함께 테스트했다.

| 테스트                          | 검증 내용                                               |
|------------------------------|-----------------------------------------------------|
| `testGetCategories`          | `findAll()` → Response 목록 매핑 (전체 필드 일치)             |
| `testGetCategoriesEmpty`     | `findAll()` → 빈 리스트                                 |
| `testGetCategory`            | `findById()` → Response 매핑 (전체 필드)                  |
| `testGetCategoryNotFound`    | `findById()` empty → NotFoundException              |
| `testCreateCategory`         | `save()` 호출 + Response 매핑                           |
| `testUpdateCategory`         | `findById()` → `update()` → Response + Entity 상태 검증 |
| `testUpdateCategoryNotFound` | `findById()` empty → NotFoundException              |
| `testDeleteCategory`         | `findById()` → `delete()` 호출 검증                     |
| `testDeleteCategoryNotFound` | `findById()` empty → NotFoundException              |

패턴: `@ExtendWith(MockitoExtension.class)` + `@Mock CategoryRepository` + Reflection setId

---

## 3. 인수 테스트

### 추가 시나리오 (4개)

파일: `src/test/resources/features/category.feature`

| ID   | 시나리오                     | 유형    | 검증 내용                     |
|------|--------------------------|-------|---------------------------|
| C-6  | 카테고리 단건 조회 성공            | happy | 생성 → ID로 조회 → 200 + 필드 일치 |
| C-S3 | 카테고리 수정 후 단건 재조회 시 변경 반영 | state | 수정 → 단건 조회 → 변경된 필드 확인    |
| C-S4 | 카테고리 삭제 후 단건 재조회 시 404   | state | 삭제 → 단건 조회 → 404          |
| C-E6 | 존재하지 않는 카테고리 단건 조회       | error | 없는 ID → 404               |

### Step Definition 추가 (2개)

파일: `src/test/java/gift/acceptance/steps/CategoryApiSteps.java`

| Step                               | 동작                                        |
|------------------------------------|-------------------------------------------|
| `@When("해당 카테고리 조회 요청을 보낸다")`      | `GET /api/categories/{currentCategoryId}` |
| `@When("존재하지 않는 카테고리 조회 요청을 보낸다")` | `GET /api/categories/{NON_EXISTENT_ID}`   |

---

## 4. 검증 결과

```bash
./gradlew test --tests "gift.category.internal.CategoryServiceTest"  # 9개 통과
./gradlew test                # BUILD SUCCESSFUL (전체 단위 테스트 통과)
./gradlew acceptanceTest      # BUILD SUCCESSFUL (기존 75개 + 신규 4개 = 79개 시나리오)
```

---

## 5. 수정/생성 파일 목록

| 파일                                                              | 변경                                 |
|-----------------------------------------------------------------|------------------------------------|
| `src/main/java/gift/category/internal/CategoryService.java`     | `getCategory(Long)` 메서드 추가         |
| `src/main/java/gift/category/internal/CategoryController.java`  | `GET /{id}` 엔드포인트 추가               |
| `src/test/java/gift/category/internal/CategoryServiceTest.java` | **신규** — 9개 테스트                    |
| `src/test/resources/features/category.feature`                  | 4개 시나리오 추가 (C-6, C-S3, C-E6, C-S4) |
| `src/test/java/gift/acceptance/steps/CategoryApiSteps.java`     | 2개 When step 추가                    |

---

## 6. Phase 2 누락 작동 구현 진행 현황

| 작업  | 설명                                   | 상태     |
|-----|--------------------------------------|--------|
| 2-1 | 주문 부수 효과 (위시 정리 + 카카오 알림 이벤트 분리)     | 미착수    |
| 2-2 | Option PUT (옵션 수정 엔드포인트)             | 미착수    |
| 2-3 | Category GET /{id} (카테고리 단건 조회)      | **완료** |
| 2-4 | Product 카테고리 필터 (`?categoryId={id}`) | 미착수    |
