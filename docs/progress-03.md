# Progress 3: ProductQueryPort 구현

**기간**: 2026-03-05

---

## 이 문서의 목적

Phase 2 세 번째 구조 변경으로, 크로스 도메인 ProductRepository 2개(`WishProductRepository`, `OptionProductRepository`)를 대체할 `ProductQueryPort`를 구성했다. 이 단계에서는 Port + 테스트까지만 수행하고, 기존 Repository 교체는 이후 단계에서 진행한다.

---

## 1. 변경 대상 분석

### 크로스 도메인 ProductRepository 사용 현황

| Repository                | 소속 도메인          | 사용자                            | 호출 패턴                                                                            |
|---------------------------|-----------------|--------------------------------|----------------------------------------------------------------------------------|
| `WishProductRepository`   | wish/internal   | `WishService.addWish()`        | `findById(id).orElseThrow()` → `Wish.builder().product(product)`                 |
| `OptionProductRepository` | option/internal | `OptionService.createOption()` | `findById(id).orElseThrow()` → `Option.builder().product(product)`               |
| `OptionProductRepository` | option/internal | `OptionService.getOptions()`   | `findByIdLeftJoinFetchOptions(id).orElseThrow()` → `product.getOptions()`        |
| `OptionProductRepository` | option/internal | `OptionService.deleteOption()` | `findByIdLeftJoinFetchOptions(id).orElseThrow()` → `product.getOptions().size()` |

### CategoryPort와의 핵심 차이

Category는 `getReference()` + `findAll()` 두 메서드로 충분했지만, Product는 `OptionService`의 fetch join 패턴(`findByIdLeftJoinFetchOptions`) 처리가 설계 쟁점이었다.

---

## 2. 구현 결과

### 생성한 파일 (3개)

**Production (2개)**

| 파일                                               | 역할                                                       |
|--------------------------------------------------|----------------------------------------------------------|
| `gift/product/ProductQueryPort.java`             | 조회 Port 인터페이스 — `getReference(id)`, `validateExists(id)` |
| `gift/product/internal/ProductQueryAdaptor.java` | QueryPort 구현체 (`@Component`)                             |

**Test (1개)**

| 파일                                                   | 테스트 수 |
|------------------------------------------------------|-------|
| `gift/product/internal/ProductQueryAdaptorTest.java` | 5개    |

### 수정한 파일

없음. 기존 코드를 수정하지 않았다.

### Port 메서드 구성

| 메서드                       | 반환        | 트랜잭션        | 호출자                                                  | 근거                                                |
|---------------------------|-----------|-------------|------------------------------------------------------|---------------------------------------------------|
| `getReference(Long id)`   | `Product` | `MANDATORY` | WishService.addWish, OptionService.createOption      | `@ManyToOne` FK 참조. MANDATORY로 detached entity 방지 |
| `validateExists(Long id)` | `void`    | 없음          | OptionService.getOptions, OptionService.deleteOption | 상품 존재 검증. 트랜잭션 불필요 (비트랜잭셔널 호출자 수용)                |

---

## 3. 논의 사항

### 3-1. fetch join 메서드(getReferenceWithOptions) 제외 결정

`OptionProductRepository`에는 `findByIdLeftJoinFetchOptions()` 커스텀 쿼리가 있다. 이를 ProductQueryPort에 `getReferenceWithOptions(Long id)`로 포함할지 논의했다.

**결론: 포함하지 않는다.**

`getOptions()`와 `deleteOption()`에서 fetch join을 사용하는 실제 목적을 분석하면:

- `getOptions()` → productId로 옵션 목록 조회. Product를 경유할 필요 없음.
- `deleteOption()` → productId에 딸린 옵션 개수 확인. Product를 경유할 필요 없음.

둘 다 Option 도메인이 자체 `OptionRepository`로 직접 해결할 수 있는 작업이다. fetch join 관련 로직 변경은 이후 **작동 변경** 커밋에서 OptionService가 자체 OptionRepository를 사용하도록 수정할 때 처리한다.

### 3-2. validateExists 메서드 추가

초기 구현에서 ProductQueryPort는 `getReference(Long id)`만 가지고 있었다. 그러나 OptionService의 교체 시나리오를 검토하면서 문제를 발견했다.

| 메서드              | `@Transactional` | `getReference()` 사용 가능? |
|------------------|------------------|-------------------------|
| `createOption()` | O                | O — entity도 필요 (FK 참조)  |
| `deleteOption()` | O                | O — MANDATORY 충족        |
| `getOptions()`   | **X**            | **X — MANDATORY 위반**    |

`getOptions()`는 트랜잭션이 없으므로 `getReference(MANDATORY)`를 호출할 수 없다. 이후 작동 변경에서 `optionRepo.findByProductId(productId)`로 바꾸더라도, 존재하지 않는 상품에 대해 빈 목록이 반환되어 현재의 `NotFoundException` 동작이 달라진다.

**결론: `validateExists(Long id)` 추가.** void 반환, 트랜잭션 선언 없음. 상품이 없으면 NotFoundException. 구현체는 `productRepo.existsById(id)`를 사용한다.

---

## 4. 테스트 전략 (ADR-002 적용)

`@DataJpaTest` + `@Transactional(propagation = NOT_SUPPORTED)` + `@Import(ProductQueryAdaptor.class)`

### FK 체인 데이터 준비

Product는 Category에 `@ManyToOne` FK 의존이 있으므로, `TransactionTemplate` 내에서 managed Category를 참조하여 Product를 생성한다.

```java
private Product createProduct(String name, int price, String imageUrl, Long categoryId) {
    return transactionTemplate.execute(status -> {
        Category category = testCategoryRepo.findById(categoryId)
                .orElseThrow(AssertionError::new);
        return testProductRepo.save(Product.builder()
                .name(name).price(price)
                .imageUrl(imageUrl).category(category)
                .build());
    });
}
```

### tearDown (FK 순서 준수)

```java
testProductRepo.deleteAllInBatch();   // Product 먼저 (Category FK 참조)
testCategoryRepo.deleteAllInBatch();
```

### 테스트 목록 (5개)

| 테스트                                  | 검증 내용                                                      | 트랜잭션 방식             |
|--------------------------------------|------------------------------------------------------------|---------------------|
| `testGetReference`                   | 존재하는 상품 Entity 반환 (id, name, price, imageUrl, category 확인) | TransactionTemplate |
| `testGetReferenceNotFound`           | 없는 ID → NotFoundException                                  | TransactionTemplate |
| `testGetReferenceWithoutTransaction` | 트랜잭션 없이 호출 → IllegalTransactionStateException              | 직접 호출               |
| `testValidateExists`                 | 존재하는 상품 검증 → 예외 없음                                         | 불필요                 |
| `testValidateExistsNotFound`         | 없는 ID → NotFoundException                                  | 불필요                 |

---

## 5. 검증 결과

```bash
./gradlew test --tests "gift.product.internal.ProductQueryAdaptorTest"  # BUILD SUCCESSFUL (5개 통과)
./gradlew test                # BUILD SUCCESSFUL (전체 단위 테스트 통과)
./gradlew acceptanceTest      # BUILD SUCCESSFUL (기존 75개 시나리오 통과)
```

기존 코드를 수정하지 않았으므로 기존 테스트에 영향이 없다.

---

## 6. 다음 단계

같은 패턴으로 Option Port를 구성한다. 모든 Port 구성이 완료되면, 기존 크로스 도메인 Repository를 Port로 교체하면서 Service 단위 테스트(Mock Port)를 함께 추가한다.
