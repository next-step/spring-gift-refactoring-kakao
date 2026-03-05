# Progress 6: AdminProductService — CategoryQueryPort 교체 + 단위 테스트

**기간**: 2026-03-05

---

## 이 문서의 목적

Phase 2 "도메인 책임 되찾기" — 두 번째 Service 교체 작업. AdminProductService에서 `AdminProductCategoryRepository`를 `CategoryQueryPort`로 교체하고, Service 단위 테스트를 추가했다. 이로써 Category 도메인의 크로스 도메인 Repository가 모두 제거되었다.

---

## 1. 변경 내용

### AdminProductService 교체 (구조 변경)

| 항목                 | Before                                                          | After                                       |
|--------------------|-----------------------------------------------------------------|---------------------------------------------|
| 의존성                | `AdminProductCategoryRepository`                                | `CategoryQueryPort`                         |
| getAllCategories() | `categoryRepo.findAll()` + `CategoryDto::from`                  | `categoryQueryPort.findAll()` + DTO 매핑      |
| createProduct()    | `categoryRepo.findById(id).orElseThrow(NoSuchElementException)` | `getCategoryOrThrow(id)` (catch + re-throw) |
| updateProduct()    | `categoryRepo.findById(id).orElseThrow(NoSuchElementException)` | `getCategoryOrThrow(id)` (catch + re-throw) |

수정 파일: `src/main/java/gift/product/admin/AdminProductService.java`

### getAllCategories() DTO 매핑

`CategoryQueryPort.findAll()`은 `List<gift.category.CategoryDto>` (id, name, color, imageUrl, description)를 반환하지만, AdminProductService는 `List<ProductDto.CategoryDto>` (id, name)만 필요하다. 람다로 매핑:

```java
categoryQueryPort.findAll().stream()
        .map(c -> new CategoryDto(c.id(), c.name()))
        .toList();
```

### 예외 타입 유지 — getCategoryOrThrow()

`CategoryQueryPort.getReference()`는 `NotFoundException`을 던지지만, AdminProductService는 기존에 `NoSuchElementException`을 사용한다. 예외 타입 변경은 작동 변경이므로 catch + re-throw로 기존 작동을 유지하는 private 메서드를 추출했다:

```java
private Category getCategoryOrThrow(Long categoryId) {
    try {
        return categoryQueryPort.getReference(categoryId);
    } catch (NotFoundException e) {
        throw new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + categoryId);
    }
}
```

### 삭제 파일

| 파일                                    | 이유                                    |
|---------------------------------------|---------------------------------------|
| `AdminProductCategoryRepository.java` | 사용처 없음 (AdminProductService가 마지막 사용자) |

`ProductCategoryRepository.java`는 이전 단계에서 이미 삭제됨.

### AdminProductServiceTest 추가 (11개 테스트)

파일: `src/test/java/gift/product/admin/AdminProductServiceTest.java`

| 테스트                                 | 검증 내용                                                                                        |
|-------------------------------------|----------------------------------------------------------------------------------------------|
| `testGetAllProducts`                | `findAllInnerJoinFetchCategory()` 호출 + ProductDto 매핑                                         |
| `testGetAllCategories`              | `categoryQueryPort.findAll()` 호출 + `gift.category.CategoryDto` → `ProductDto.CategoryDto` 매핑 |
| `testCreateProduct`                 | `categoryQueryPort.getReference` 호출 + `productRepo.save` 호출                                  |
| `testCreateProductCategoryNotFound` | NotFoundException → **NoSuchElementException 변환**                                            |
| `testGetProduct`                    | `findByIdInnerJoinFetchCategory` 호출 + 응답 필드                                                  |
| `testGetProductNotFound`            | NoSuchElementException 발생                                                                    |
| `testUpdateProduct`                 | `categoryQueryPort.getReference` 호출 + entity 상태 변경                                           |
| `testUpdateProductNotFound`         | NoSuchElementException 발생                                                                    |
| `testUpdateProductCategoryNotFound` | NotFoundException → **NoSuchElementException 변환**                                            |
| `testDeleteProduct`                 | `productRepo.delete` 호출                                                                      |
| `testDeleteProductNotFound`         | NoSuchElementException 발생                                                                    |

---

## 2. 논의 사항

### 예외 타입 불일치 처리 방식 선택

AdminProductService는 `NoSuchElementException`, Port는 `NotFoundException`을 던진다. 세 가지 선택지를 검토했다:

| 선택지                       | 설명                                    |
|---------------------------|---------------------------------------|
| NotFoundException 그대로 전파  | 500 → 404 변경 (작동 변경)                  |
| **catch + re-throw (채택)** | 기존 NoSuchElementException 유지 (구조 변경만) |
| 별도 커밋으로 분리                | 이 커밋은 catch + re-throw, 다음 커밋에서 예외 통일 |

**결정: catch + re-throw.** 구조 변경과 작동 변경을 섞지 않는 원칙에 따라 기존 예외 타입을 유지했다.

---

## 3. 검증 결과

```bash
./gradlew test --tests "gift.product.admin.AdminProductServiceTest"  # BUILD SUCCESSFUL (11개 통과)
./gradlew test                # BUILD SUCCESSFUL (전체 단위 테스트 통과)
./gradlew acceptanceTest      # BUILD SUCCESSFUL (기존 75개 시나리오 통과)
```

---

## 4. Category 크로스 도메인 Repository 제거 현황

| Repository                       | 사용자                 | Port 교체         | Repository 삭제 |
|----------------------------------|---------------------|-----------------|---------------|
| `ProductCategoryRepository`      | ProductService      | 완료 (progress-5) | 완료            |
| `AdminProductCategoryRepository` | AdminProductService | **완료**          | **완료**        |

Category 도메인의 크로스 도메인 Repository가 모두 제거되었다.

---

## 5. 전체 Port 교체 대상 Service 현황

| Service             | 교체 대상 Repository                 | 교체할 Port                                | 상태              |
|---------------------|----------------------------------|-----------------------------------------|-----------------|
| ProductService      | `ProductCategoryRepository`      | `CategoryQueryPort`                     | 완료 (progress-5) |
| AdminProductService | `AdminProductCategoryRepository` | `CategoryQueryPort`                     | **완료**          |
| OrderService        | `OrderOptionRepository`          | `OptionQueryPort` + `OptionCommandPort` | 미착수             |
| OrderService        | `OrderMemberRepository`          | `MemberQueryPort` + `MemberCommandPort` | 미착수             |
| AuthService (Kakao) | `AuthMemberRepository`           | `MemberQueryPort` + `MemberCommandPort` | 미착수             |
