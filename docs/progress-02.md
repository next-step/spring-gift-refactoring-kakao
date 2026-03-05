# Progress 2: CategoryQueryPort 구현

**기간**: 2026-03-04

---

## 이 문서의 목적

Phase 2 두 번째 구조 변경으로, 크로스 도메인 CategoryRepository 2개(`ProductCategoryRepository`, `AdminProductCategoryRepository`)를 대체할 `CategoryQueryPort`를 구성했다. 이 단계에서는 Port + 테스트까지만 수행하고, 기존 Repository 교체는 다음 단계에서 진행한다.

---

## 1. 변경 대상 분석

### 크로스 도메인 CategoryRepository 사용 현황

| Repository                       | 소속 도메인           | 사용자                                      | 호출 패턴                                                                 |
|----------------------------------|------------------|------------------------------------------|-----------------------------------------------------------------------|
| `ProductCategoryRepository`      | product/internal | `ProductService.createProduct()`         | `findById(id).orElseThrow()` → `Product.builder().category(category)` |
| `ProductCategoryRepository`      | product/internal | `ProductService.updateProduct()`         | `findById(id).orElseThrow()` → `find.update(..., category)`           |
| `AdminProductCategoryRepository` | product/admin    | `AdminProductService.createProduct()`    | `findById(id).orElseThrow()` → `Product.builder().category(category)` |
| `AdminProductCategoryRepository` | product/admin    | `AdminProductService.updateProduct()`    | `findById(id).orElseThrow()` → `find.update(..., category)`           |
| `AdminProductCategoryRepository` | product/admin    | `AdminProductService.getAllCategories()` | `findAll()` → `CategoryDto::from` 매핑                                  |

### MemberPort와의 핵심 차이

Member는 primitive FK(`memberId`)를 사용해 단순 값만 반환하면 됐지만, Category는 Product의 `@ManyToOne` 관계이므로 **영속 Entity를 반환**해야 한다. ADR-001에 따라 `getReference()` + `MANDATORY` 트랜잭션 전파를 사용한다.

---

## 2. 구현 결과

### 생성한 파일 (4개)

**Production (3개)**

| 파일                                                 | 역할                                              |
|----------------------------------------------------|-------------------------------------------------|
| `gift/category/CategoryQueryPort.java`             | 조회 Port 인터페이스 — `getReference(id)`, `findAll()` |
| `gift/category/CategoryDto.java`                   | `findAll()` 반환용 record (Entity 노출 방지)           |
| `gift/category/internal/CategoryQueryAdaptor.java` | QueryPort 구현체 (`@Component`)                    |

**Test (1개)**

| 파일                                                     | 테스트 수 |
|--------------------------------------------------------|-------|
| `gift/category/internal/CategoryQueryAdaptorTest.java` | 5개    |

### 수정한 파일

없음. 기존 코드를 수정하지 않았다.

### Port 메서드 구성

| 메서드                     | 반환                  | 트랜잭션        | 호출자                                 | 근거                                                |
|-------------------------|---------------------|-------------|-------------------------------------|---------------------------------------------------|
| `getReference(Long id)` | `Category`          | `MANDATORY` | ProductService, AdminProductService | `@ManyToOne` FK 참조. MANDATORY로 detached entity 방지 |
| `findAll()`             | `List<CategoryDto>` | 없음          | AdminProductService                 | 단순 목록 반환. Entity 대신 record 반환 (Port 컨벤션)          |

`getReference()`는 `getReferenceById()` 대신 `findById().orElseThrow()`를 사용한다. 현재 호출부와 동일한 즉시 존재 검증 + 동일한 예외 타입을 유지하기 위함 (구조 변경이므로 작동을 바꾸지 않는다).

---

## 3. 논의 사항

### 3-1. TransactionTemplate 발견과 Port 테스트 활용

`getReference()`는 `MANDATORY` 전파를 사용하므로, 호출자의 트랜잭션이 필요하다. 테스트 클래스는 `@Transactional(NOT_SUPPORTED)`로 자동 트랜잭션을 해제했기 때문에, **`TransactionTemplate`으로 트랜잭션을 명시적으로 제공**하는 방식을 도입했다.

```java
// TransactionTemplate이 트랜잭션을 열어줌 → MANDATORY 조건 충족
Category result = transactionTemplate.execute(
                status -> categoryQueryPort.getReference(saved.getId())
        );
```

`TransactionTemplate`은 Spring이 제공하는 프로그래밍 방식 트랜잭션 관리 도구로, `@Transactional` 어노테이션이 메서드 전체를 감싸는 것과 달리 **코드 블록 단위**로 트랜잭션을 제어할 수 있다. `@DataJpaTest`가 구성하는 `PlatformTransactionManager`가 있으면 자동 등록되므로 `@Autowired`만으로 사용 가능하다.

이를 통해 MANDATORY 전파를 세 가지로 검증한다:

| 테스트                                  | 방식                           | 검증                               |
|--------------------------------------|------------------------------|----------------------------------|
| `testGetReference`                   | `TransactionTemplate` 안에서 호출 | 정상 동작                            |
| `testGetReferenceNotFound`           | `TransactionTemplate` 안에서 호출 | NotFoundException                |
| `testGetReferenceWithoutTransaction` | 직접 호출 (트랜잭션 없음)              | IllegalTransactionStateException |

### 3-2. findAll() 반환 타입: Entity → CategoryDto record

초기 구현에서 `findAll()`은 `List<Category>`를 반환했다. 호출자(`AdminProductService.getAllCategories()`)가 DTO 매핑만 수행하므로 JPA Entity를 반환할 이유가 없었고, MemberQueryPort가 primitive/record만 반환하는 컨벤션과도 맞지 않았다.

**결론: `findAll()`은 `List<CategoryDto>`를 반환하도록 변경했다.** `CategoryDto`는 Category 전체 필드를 담는 record로 `gift.category` 패키지에 배치. Entity → DTO 변환 책임은 Adaptor 구현체가 갖는다. `getReference()`는 JPA `@ManyToOne`에 영속 Entity가 필요하므로 Entity 반환을 유지한다.

### 3-3. Port 테스트용 DataManipulator 불필요 — TransactionTemplate으로 해결

Progress 1에서 "이후 Option, Product Port 테스트에서 FK 체인 데이터 준비를 위해 DataManipulator 도입을 검토한다"고 기록했다. `@Transactional(NOT_SUPPORTED)` 환경에서 detached entity를 `@ManyToOne` 필드에 넣으면 영속 상태 오류가 발생할 수 있기 때문이었다.

**결론: `TransactionTemplate`으로 해결되므로 별도 DataManipulator 구현체는 불필요하다.**

```java
// TransactionTemplate 안에서 FK 체인 데이터를 한 트랜잭션으로 준비
transactionTemplate.execute(status -> {
    Category category = testCategoryRepo.save(Category.builder()...build());
    Product product = testProductRepo.save(Product.builder().category(category)...build());
    testOptionRepo.save(Option.builder().product(product)...build());
    return null;
});
```

`TransactionTemplate`이 트랜잭션을 제공하므로, 별도 `@Component` + `@Transactional` 헬퍼 없이 테스트 메서드 안에서 직접 해결된다. 인수 테스트의 `CucumberTestDataManipulator`는 75개 시나리오의 데이터 정리 순서 관리라는 추가 책임이 있어 유지하지만, Port 통합 테스트에서는 `TransactionTemplate` + `deleteAllInBatch()`로 충분하다.

---

## 4. 테스트 전략 (ADR-002 적용)

`@DataJpaTest` + `@Transactional(propagation = NOT_SUPPORTED)` + `@Import(CategoryQueryAdaptor.class)`

- 자동 트랜잭션 해제 → Adaptor의 `@Transactional(MANDATORY)`만 작동 (실제 환경과 동일)
- `TransactionTemplate`으로 MANDATORY 메서드에 트랜잭션 제공
- `TestCategoryRepository`로 데이터 준비 및 정리
- `@AfterEach`에서 `deleteAllInBatch()`로 정리

### 테스트 목록 (5개)

| 테스트                                  | 검증 내용                                         | 트랜잭션 방식             |
|--------------------------------------|-----------------------------------------------|---------------------|
| `testGetReference`                   | 존재하는 카테고리 Entity 반환 (id, name 확인)             | TransactionTemplate |
| `testGetReferenceNotFound`           | 없는 ID → NotFoundException                     | TransactionTemplate |
| `testGetReferenceWithoutTransaction` | 트랜잭션 없이 호출 → IllegalTransactionStateException | 직접 호출               |
| `testFindAll`                        | 저장된 카테고리 2개 전체 반환                             | 불필요                 |
| `testFindAllEmpty`                   | 카테고리 없으면 빈 목록 반환                              | 불필요                 |

---

## 5. 검증 결과

```bash
./gradlew test --tests "gift.category.internal.CategoryQueryAdaptorTest"  # BUILD SUCCESSFUL (5개 통과)
./gradlew test                # BUILD SUCCESSFUL (Port 테스트 17개 포함)
./gradlew acceptanceTest      # BUILD SUCCESSFUL (기존 75개 시나리오 통과)
```

기존 코드를 수정하지 않았으므로 기존 테스트에 영향이 없다.

---

## 6. 다음 단계

같은 패턴으로 Product, Option Port를 순차 구성한다. 데이터 준비 시 FK 체인이 필요한 경우 `TransactionTemplate`을 활용한다. 모든 Port 구성이 완료되면, 기존 크로스 도메인 Repository를 Port로 교체하면서 Service 단위 테스트(Mock Port)를 함께 추가한다.
