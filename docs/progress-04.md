# Progress 4: OptionQueryPort + OptionCommandPort 구현

**기간**: 2026-03-05

---

## 이 문서의 목적

Phase 2 네 번째(마지막) 구조 변경으로, 크로스 도메인 `OrderOptionRepository`를 대체할 `OptionQueryPort` + `OptionCommandPort`를 구성했다. 이 단계에서는 Port + 테스트까지만 수행하고, 기존 Repository 교체는 이후 단계에서 진행한다.

---

## 1. 변경 대상 분석

### 크로스 도메인 OptionRepository 사용 현황

| Repository              | 소속 도메인         | 사용자                          | 호출 패턴                                                                                                                                                    |
|-------------------------|----------------|------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `OrderOptionRepository` | order/internal | `OrderService.createOrder()` | `findByIdInnerJoinFetchProduct(id).orElseThrow()` → `option.subtractQuantity(qty)` + `option.getProduct().getPrice()` + `Order.builder().option(option)` |

크로스 도메인 OptionRepository는 `OrderOptionRepository` **1개**뿐이다.

### OrderService.createOrder() 상세 분석

```java
@Transactional
public OrderResponse createOrder(Long memberId, OrderRequest request) {
    Option option = optionRepo.findByIdInnerJoinFetchProduct(optionId)
            .orElseThrow(NotFoundException::optionNotFound);

    option.subtractQuantity(quantity);           // (1) 상태 변경
    Product product = option.getProduct();       // (2) Product 접근
    int price = product.getPrice() * quantity;   // (3) 가격 계산

    memberRepo.findById(memberId)...deductPoint(price);

    Order build = Order.builder()
            .option(option)                      // (4) FK 참조
            .build();
}
```

용도 3가지:

1. **수량 차감** (`option.subtractQuantity`) → `OptionCommandPort.subtractQuantity()`
2. **상품 가격 접근** (`option.getProduct().getPrice()`) → `OptionQueryPort.getAssociatedProduct()`
3. **FK 참조** (`Order.builder().option(option)`) → `OptionQueryPort.getReference()`

---

## 2. 구현 결과

### 생성한 파일 (6개)

**Production (4개)**

| 파일                                               | 역할                                                                   |
|--------------------------------------------------|----------------------------------------------------------------------|
| `gift/option/OptionQueryPort.java`               | 조회 Port 인터페이스 — `getReference(id)`, `getAssociatedProduct(optionId)` |
| `gift/option/internal/OptionQueryAdaptor.java`   | QueryPort 구현체 (`@Component`)                                         |
| `gift/option/OptionCommandPort.java`             | 명령 Port 인터페이스 — `subtractQuantity(id, amount)`                       |
| `gift/option/internal/OptionCommandAdaptor.java` | CommandPort 구현체 (`@Component`)                                       |

**Test (2개)**

| 파일                                                   | 테스트 수 |
|------------------------------------------------------|-------|
| `gift/option/internal/OptionQueryAdaptorTest.java`   | 5개    |
| `gift/option/internal/OptionCommandAdaptorTest.java` | 3개    |

### 수정한 파일

| 파일                                           | 변경 내용                                          |
|----------------------------------------------|------------------------------------------------|
| `gift/option/internal/OptionRepository.java` | `findByIdInnerJoinFetchProduct(Long id)` 쿼리 추가 |

### 추가 생성한 파일

| 파일                             | 역할                                                           |
|--------------------------------|--------------------------------------------------------------|
| `gift/product/ProductDto.java` | Product 전체 필드 record (id, name, price, imageUrl, categoryId) |

---

## 3. Port 메서드 구성

### OptionQueryPort

| 메서드                                   | 반환           | 트랜잭션        | 호출자                      | 근거                                                |
|---------------------------------------|--------------|-------------|--------------------------|---------------------------------------------------|
| `getReference(Long id)`               | `Option`     | `MANDATORY` | OrderService.createOrder | `@ManyToOne` FK 참조. MANDATORY로 detached entity 방지 |
| `getAssociatedProduct(Long optionId)` | `ProductDto` | 없음          | OrderService.createOrder | 옵션에 연관된 상품 정보 제공. DTO 반환이므로 트랜잭션 불필요              |

### OptionCommandPort

| 메서드                                     | 반환     | 트랜잭션       | 호출자                      | 근거                                           |
|-----------------------------------------|--------|------------|--------------------------|----------------------------------------------|
| `subtractQuantity(Long id, int amount)` | `void` | `REQUIRED` | OrderService.createOrder | Entity 비즈니스 메서드 위임. Port 컨벤션에 따라 Port가 감싸야 함 |

---

## 4. 논의 사항

### 4-1. fetch join (findByIdInnerJoinFetchProduct) 처리

`OrderOptionRepository`는 Option + Product를 inner join fetch로 한 쿼리에 로드한다. Port 교체 시 이 fetch join을 어떻게 처리할지 논의했다.

**결론: `getAssociatedProduct(Long optionId)` 메서드를 추가하여 DTO로 상품 정보를 반환한다.**

lazy load 방식(`option.getProduct()`)도 기술적으로 동작하지만, OrderService가 Option entity의 내부 관계 구조(`@ManyToOne Product`)를 알아야 하는 문제가 있다. `getAssociatedProduct`는 도메인 경계를 명확히 하고, MemberQueryPort가 `getEmail(id)` 같은 값만 반환하는 Port 컨벤션과 일관된다.

구현체는 `OptionRepository.findByIdInnerJoinFetchProduct()`를 사용하여 Option + Product를 한 쿼리로 로드한 뒤, Product를 ProductDto로 변환한다. `product.getCategory().getId()`는 Hibernate 프록시에서 FK 값만 읽으므로 Category lazy load가 발생하지 않는다.

### 4-2. validateExists 불필요 결정

ProductQueryPort에는 `validateExists`가 있지만, OptionQueryPort에는 추가하지 않았다. 이유:

- ProductQueryPort.validateExists는 `OptionService.getOptions()`가 **트랜잭션 없이** 상품 존재만 확인해야 해서 필요했음
- OptionQueryPort의 경우, OrderService.createOrder()는 `@Transactional`이 있고, `subtractQuantity`와 `getAssociatedProduct`가 내부에서 이미 존재 검증을 수행함
- 별도 `validateExists`를 두면 동일 검증이 중복됨

### 4-3. JPA 1차 캐시와 중복 조회

OrderService.createOrder()는 하나의 `@Transactional` → 하나의 영속성 컨텍스트이다. Port 메서드들이 내부적으로 같은 optionId로 `findById`를 반복 호출하지만, JPA 1차 캐시 덕분에 실제 DB 쿼리는 최소화된다.

| 호출 순서 | Port 메서드                          | 내부 쿼리                               | DB 쿼리 여부     |
|-------|-----------------------------------|-------------------------------------|--------------|
| 1     | `getReference(optionId)`          | `findById(id)`                      | O (1차 캐시 저장) |
| 2     | `subtractQuantity(optionId, qty)` | `findById(id)`                      | X (1차 캐시 히트) |
| 3     | `getAssociatedProduct(optionId)`  | `findByIdInnerJoinFetchProduct(id)` | O (커스텀 JPQL) |

3번은 커스텀 JPQL이므로 1차 캐시를 먼저 확인하지 않고 DB에 쿼리를 보낸다. 다만 결과를 영속성 컨텍스트에 merge할 때, 이미 캐시에 있는 Option entity(2번에서 수량 차감된 상태)를 유지한다.

### 4-4. ProductDto 위치

`ProductDto`를 `gift.product` 패키지에 배치했다. `CategoryDto`가 `gift.category`에 있는 것과 동일한 패턴이다. OptionQueryPort가 `gift.product.ProductDto`를 import하지만, Option entity가 이미 `gift.product.Product`를 `@ManyToOne`으로 참조하고 있으므로 새로운 의존이 아니다.

---

## 5. OrderService Port 교체 예상 흐름

이후 크로스 도메인 Repository를 Port로 교체할 때, OrderService.createOrder()는 다음과 같이 변경될 예정이다.

### 현재 (OrderOptionRepository + OrderMemberRepository 직접 사용)

```java
@Transactional
public OrderResponse createOrder(Long memberId, OrderRequest request) {
    Option option = optionRepo.findByIdInnerJoinFetchProduct(optionId)
            .orElseThrow(NotFoundException::optionNotFound);

    option.subtractQuantity(quantity);

    Product product = option.getProduct();
    int price = product.getPrice() * quantity;

    memberRepo.findById(memberId)
            .orElseThrow(NotFoundException::memberNotFound)
            .deductPoint(price);

    Order build = Order.builder()
            .option(option)
            .memberId(memberId)
            .quantity(quantity)
            .message(message)
            .build();

    Order newEntity = orderRepo.save(build);
    return OrderResponse.from(newEntity);
}
```

### 교체 후 (Port 사용)

```java
@Transactional
public OrderResponse createOrder(Long memberId, OrderRequest request) {
    // FK 참조용 Option entity
    Option option = optionQueryPort.getReference(optionId);

    // 수량 차감 (Port 컨벤션: entity 비즈니스 메서드 직접 호출 금지)
    optionCommandPort.subtractQuantity(optionId, quantity);

    // 상품 가격 조회 (도메인 경계를 넘지 않고 DTO로 받음)
    ProductDto product = optionQueryPort.getAssociatedProduct(optionId);
    int price = product.price() * quantity;

    // 포인트 차감
    memberCommandPort.deductPoint(memberId, price);

    // 주문 저장
    Order build = Order.builder()
            .option(option)
            .memberId(memberId)
            .quantity(quantity)
            .message(message)
            .build();

    Order newEntity = orderRepo.save(build);
    return OrderResponse.from(newEntity);
}
```

---

## 6. 테스트 전략 (ADR-002 적용)

### FK 체인 데이터 준비

Option은 3단계 FK 체인: Category → Product → Option. `TransactionTemplate`으로 managed entity를 참조하여 생성한다.

```java
private Category createCategory(String name) {
    return testCategoryRepo.save(Category.builder()...build());
}

private Product createProduct(String name, int price, Long categoryId) {
    return transactionTemplate.execute(status -> {
        Category category = testCategoryRepo.findById(categoryId)
                .orElseThrow(AssertionError::new);
        return testProductRepo.save(Product.builder().category(category)...build());
    });
}

private Option createOption(String name, int quantity, Long productId) {
    return transactionTemplate.execute(status -> {
        Product product = testProductRepo.findById(productId)
                .orElseThrow(AssertionError::new);
        return testOptionRepo.save(Option.builder().product(product)...build());
    });
}
```

### tearDown (FK 역순)

```java
testOptionRepo.deleteAllInBatch();    // Option 먼저
testProductRepo.deleteAllInBatch();   // Product
testCategoryRepo.deleteAllInBatch();  // Category 마지막
```

### OptionQueryAdaptorTest (5개)

| 테스트                                  | 검증 내용                                                            | 트랜잭션 방식             |
|--------------------------------------|------------------------------------------------------------------|---------------------|
| `testGetReference`                   | 존재하는 옵션 Entity 반환 (id, name, quantity, product.id 확인)            | TransactionTemplate |
| `testGetReferenceNotFound`           | 없는 ID → NotFoundException                                        | TransactionTemplate |
| `testGetReferenceWithoutTransaction` | 트랜잭션 없이 호출 → IllegalTransactionStateException                    | 직접 호출               |
| `testGetAssociatedProduct`           | 옵션의 연관 상품 정보 반환 (id, name, price, imageUrl, categoryId 전체 필드 확인) | 불필요                 |
| `testGetAssociatedProductNotFound`   | 없는 ID → NotFoundException                                        | 불필요                 |

### OptionCommandAdaptorTest (3개)

| 테스트                                     | 검증 내용                                      | 트랜잭션 방식              |
|-----------------------------------------|--------------------------------------------|----------------------|
| `testSubtractQuantity`                  | 수량 차감 후 **상태 재조회** (quantity == 원래값 - 차감값) | 불필요 (REQUIRED 자체 tx) |
| `testSubtractQuantityInsufficientStock` | 재고 초과 차감 → IllegalArgumentException        | 불필요                  |
| `testSubtractQuantityNotFound`          | 없는 ID → NotFoundException                  | 불필요                  |

---

## 7. 검증 결과

```bash
./gradlew test --tests "gift.option.internal.OptionQueryAdaptorTest"   # BUILD SUCCESSFUL (5개 통과)
./gradlew test --tests "gift.option.internal.OptionCommandAdaptorTest" # BUILD SUCCESSFUL (3개 통과)
./gradlew test                # BUILD SUCCESSFUL (전체 단위 테스트 통과)
./gradlew acceptanceTest      # BUILD SUCCESSFUL (기존 75개 시나리오 통과)
```

기존 코드의 작동을 변경하지 않았으므로 기존 테스트에 영향이 없다.

---

## 8. 다음 단계

모든 Port 구성이 완료되었다. 이제 기존 크로스 도메인 Repository를 Port로 교체하면서 Service 단위 테스트(Mock Port)를 함께 추가한다.

| 도메인      | QueryPort           | CommandPort         | 상태 |
|----------|---------------------|---------------------|----|
| Member   | `MemberQueryPort`   | `MemberCommandPort` | 완료 |
| Category | `CategoryQueryPort` | —                   | 완료 |
| Product  | `ProductQueryPort`  | —                   | 완료 |
| Option   | `OptionQueryPort`   | `OptionCommandPort` | 완료 |
