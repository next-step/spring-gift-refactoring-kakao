# Progress 5: ProductService — CategoryQueryPort 교체 + 단위 테스트

**기간**: 2026-03-05

---

## 이 문서의 목적

Phase 2 "도메인 책임 되찾기" — 크로스 도메인 Repository를 Port로 교체하는 첫 번째 Service 교체 작업. ProductService에서 `ProductCategoryRepository`를 `CategoryQueryPort`로 교체하고, Service 단위 테스트(Mock Port)를 추가했다.

---

## 1. 변경 내용

### ProductService 교체 (구조 변경)

| 항목              | Before                                       | After                                |
|-----------------|----------------------------------------------|--------------------------------------|
| 의존성             | `ProductCategoryRepository`                  | `CategoryQueryPort`                  |
| createProduct() | `categoryRepo.findById(id).orElseThrow(...)` | `categoryQueryPort.getReference(id)` |
| updateProduct() | `categoryRepo.findById(id).orElseThrow(...)` | `categoryQueryPort.getReference(id)` |

수정 파일: `src/main/java/gift/product/internal/ProductService.java`

`ProductCategoryRepository`는 AdminProductService도 사용하므로 이 단계에서 삭제하지 않는다.

### ProductServiceTest 추가 (10개 테스트)

파일: `src/test/java/gift/product/internal/ProductServiceTest.java`

| 테스트                                 | 검증 내용                                                                     |
|-------------------------------------|---------------------------------------------------------------------------|
| `testGetProducts`                   | 페이징 조회 — content, metadata (size, number, totalElements, totalPages)      |
| `testGetProduct`                    | 단건 조회 — 전체 응답 필드 (id, name, price, imageUrl, categoryId)                  |
| `testGetProductNotFound`            | 없는 ID → NotFoundException                                                 |
| `testCreateProduct`                 | `categoryQueryPort.getReference(categoryId)` 호출 + save 호출 + 응답 필드         |
| `testCreateProductCategoryNotFound` | Port → NotFoundException 전파                                               |
| `testUpdateProduct`                 | `categoryQueryPort.getReference(newCategoryId)` 호출 + entity 상태 변경 + 응답 필드 |
| `testUpdateProductCategoryNotFound` | Port → NotFoundException 전파 (상품은 존재하는 상황, lenient stubbing)               |
| `testUpdateProductNotFound`         | 없는 상품 ID → NotFoundException                                              |
| `testDeleteProduct`                 | `productRepo.delete(product)` 호출                                          |
| `testDeleteProductNotFound`         | 없는 ID → NotFoundException                                                 |

---

## 2. 논의 사항

### 2-1. create/update 테스트에서 categoryQueryPort.getReference 호출 확인이 왜 필요한가

이번 리팩터링의 핵심 변경 포인트가 `ProductCategoryRepository` → `CategoryQueryPort` 교체이므로, 단위 테스트에서 `categoryQueryPort.getReference(categoryId)`가 정확한 인자로 호출되는지를 직접 검증한다. Port 교체가 올바르게 이루어졌다는 증거 역할.

### 2-2. updateProductCategoryNotFound에서 productId 문제

초기 구현에서는 `productService.updateProduct(1L, request)`처럼 임의의 productId를 넣었다. `updateProduct()`에서 category 조회가 product 조회보다 먼저 실행되므로 productId가 뭐든 결과는 같지만, **"상품은 존재하지만 카테고리가 없어서 실패하는 상황"** 이라는 테스트 의도를 명확히 표현하기 위해, 존재하는 상품을 `lenient()` stubbing으로 설정하는 방식으로 수정했다.

### 2-3. Mockito strict stubbing과 lenient()

`@ExtendWith(MockitoExtension.class)`는 기본적으로 strict stubbing을 사용한다. stubbing 해놓고 실제 호출되지 않으면 `UnnecessaryStubbingException`으로 테스트가 실패한다. "혹시 테스트가 의도와 다르게 동작하는 건 아닌가"를 잡아주는 안전장치.

이를 우회하는 방법:

- `lenient().when(mock.method()).thenReturn(value)` — 개별 stubbing을 lenient로
- `@MockitoSettings(strictness = Strictness.LENIENT)` — 클래스 전체를 lenient로

### 2-4. BDDMockito `then().should()` vs 전통 `verify()`

|              | 전통 Mockito                  | BDDMockito                     |
|--------------|-----------------------------|--------------------------------|
| stubbing     | `when(...).thenReturn(...)` | `given(...).willReturn(...)`   |
| verification | `verify(mock).method()`     | `then(mock).should().method()` |
| lenient      | `lenient().when(...)`       | (BDD 전용 lenient 없음 → 전통 방식 혼용) |

기능은 동일하고, BDD 용어(Given-When-Then)에 맞춘 API 래퍼.

---

## 3. 검증 결과

```bash
./gradlew test --tests "gift.product.internal.ProductServiceTest"  # BUILD SUCCESSFUL (10개 통과)
./gradlew test                # BUILD SUCCESSFUL (전체 단위 테스트 통과)
./gradlew acceptanceTest      # BUILD SUCCESSFUL (기존 75개 시나리오 통과)
```

구조 변경만 수행했으므로 기존 인수 테스트에 영향 없음.

---

## 4. 다음 단계

Port 교체 대상 Service 현황:

| Service             | 교체 대상 Repository                 | 교체할 Port                                | 상태     |
|---------------------|----------------------------------|-----------------------------------------|--------|
| ProductService      | `ProductCategoryRepository`      | `CategoryQueryPort`                     | **완료** |
| AdminProductService | `AdminProductCategoryRepository` | `CategoryQueryPort`                     | 미착수    |
| OrderService        | `OrderOptionRepository`          | `OptionQueryPort` + `OptionCommandPort` | 미착수    |
| OrderService        | `OrderMemberRepository`          | `MemberQueryPort` + `MemberCommandPort` | 미착수    |
| AuthService (Kakao) | `AuthMemberRepository`           | `MemberQueryPort` + `MemberCommandPort` | 미착수    |
