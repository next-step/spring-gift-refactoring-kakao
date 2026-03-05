# Progress 8: OptionService — ProductQueryPort 교체 + 쿼리 책임 재배치 + 단위 테스트

**기간**: 2026-03-05

---

## 이 문서의 목적

Phase 2 "도메인 책임 되찾기" — 네 번째 Service 교체 작업. OptionService에서 `OptionProductRepository`를 `ProductQueryPort`로 교체하고, `product.getOptions()` 호출을 제거하여 Port 컨벤션을 준수하도록 쿼리 책임을 재배치했다. OptionRepository의 JPQL을 `@EntityGraph` + 파생 쿼리로 전환하고, Service 단위 테스트 9개를 추가했다.

---

## 1. 변경 내용

### 1-1. OptionService 교체 (구조 변경)

단순 교체가 아닌 **쿼리 책임 재배치**가 핵심이다. 기존에는 `OptionProductRepository`로 Product + Options를 한 번에 조회한 뒤 `product.getOptions()`를 호출했다. Port 컨벤션("참조 Port에서 받은 Entity의 비즈니스 메서드를 직접 호출하지 않는다")에 따라, 옵션 데이터는 자체 도메인인 `OptionRepository`에서 직접 조회하도록 재구성했다.

#### 의존성 변경

```java
// Before
private final OptionProductRepository productRepo;

// After
private final ProductQueryPort productQueryPort;
```

#### 메서드별 변경

| 메서드              | Before                                                                         | After                                                                       |
|------------------|--------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| `getOptions()`   | `productRepo.findByIdLeftJoinFetchOptions(id)` → `product.getOptions()`        | `productQueryPort.validateExists(id)` + `optionRepo.findAllByProductId(id)` |
| `createOption()` | `productRepo.findById(id).orElseThrow()`                                       | `productQueryPort.getReference(id)`                                         |
| `deleteOption()` | `productRepo.findByIdLeftJoinFetchOptions(id)` → `product.getOptions().size()` | `productQueryPort.validateExists(id)` + `optionRepo.countByProductId(id)`   |

- `getOptions()`: `validateExists()`는 트랜잭션 불필요 — 기존과 동일하게 비트랜잭셔널
- `createOption()`: `@Transactional` 선언 → `getReference()`의 MANDATORY 전파 충족
- `deleteOption()`: `product.getOptions()` 호출 제거 — Port 컨벤션 준수

#### OptionRepository 추가 메서드

```java
List<Option> findAllByProductId(Long productId);     // getOptions() 용

long countByProductId(Long productId);                // deleteOption() 용
```

수정 파일: `OptionService.java`, `OptionRepository.java`

### 1-2. 삭제 파일

| 파일                             | 이유                              |
|--------------------------------|---------------------------------|
| `OptionProductRepository.java` | 사용처 없음 (OptionService가 유일한 사용자) |

### 1-3. OptionRepository 쿼리 정리 (기능 변경)

JPQL `@Query`를 `@EntityGraph` + Spring Data 파생 쿼리로 전환했다.

| Before                                                                        | After                                                                  |
|-------------------------------------------------------------------------------|------------------------------------------------------------------------|
| `@Query("select o ... inner join fetch o.product where o.id = :id")`          | `@EntityGraph(attributePaths = "product")` + `findWithProductById(id)` |
| `@Query("select o ... where o.id = :optionId and o.product.id = :productId")` | JPQL 유지 (아래 논의 참조)                                                     |

수정 파일: `OptionRepository.java`, `OptionQueryAdaptor.java`

### 1-4. OptionServiceTest 추가 (9개 테스트)

파일: `src/test/java/gift/option/internal/OptionServiceTest.java`

| 테스트                                     | 검증 내용                                                                         |
|-----------------------------------------|-------------------------------------------------------------------------------|
| `testGetOptions`                        | `validateExists` + `findAllByProductId` 호출 + 응답 매핑                            |
| `testGetOptionsProductNotFound`         | `validateExists` → NotFoundException 전파                                       |
| `testCreateOption`                      | `getReference` 호출 + 중복 없음 → `save` + 응답 매핑                                    |
| `testCreateOptionProductNotFound`       | `getReference` → NotFoundException 전파                                         |
| `testCreateOptionDuplicateName`         | `existsByProductIdAndName` true → DuplicateOptionNameException                |
| `testDeleteOption`                      | `validateExists` + `countByProductId > 1` + `findByIdAndProductId` + `delete` |
| `testDeleteOptionProductNotFound`       | `validateExists` → NotFoundException 전파                                       |
| `testDeleteOptionInsufficientRemaining` | `countByProductId <= 1` → FailedToDeleteOptionException                       |
| `testDeleteOptionNotFound`              | `findByIdAndProductId` 빈 결과 → NotFoundException                               |

---

## 2. 논의: `findByIdAndProductId` JOIN 문제

### 문제

Spring Data 파생 쿼리 `findByIdAndProductId`는 `@ManyToOne` 연관관계를 탐색하면서 LEFT JOIN을 생성한다. `product_id`는 `options` 테이블에 FK 컬럼으로 존재하므로 JOIN이 불필요하다.

### 검토한 선택지

| 선택지                          | 방식                                                                            | 장점                                                 | 단점                                                |
|------------------------------|-------------------------------------------------------------------------------|----------------------------------------------------|---------------------------------------------------|
| A. Entity에 `productId` 필드 추가 | `@Column(name = "product_id", insertable = false, updatable = false)`         | 모든 `~ByProductId` 파생 쿼리가 JOIN 없이 FK 직접 사용          | 동일 컬럼 이중 매핑 — flush 전 `null`, 값 불일치 구간 존재, 개발자 혼동 |
| B. JPQL `@Query` 유지          | `select o from Option o where o.id = :optionId and o.product.id = :productId` | Entity 변경 없음, Hibernate 6이 `o.product.id`를 FK로 최적화 | 파생 쿼리 대비 가독성 약간 저하                                |

### 결정: 선택지 B

Entity에 중복 필드를 추가하면 잠재적 불일치 문제가 생긴다. 문제가 되는 쿼리(`findByIdAndProductId`)만 JPQL로 명시하고, 나머지 3개(`findAllByProductId`, `countByProductId`, `existsByProductIdAndName`)는 Hibernate 6의 FK 최적화에 의존하는 파생 쿼리를 유지한다.

---

## 3. 검증 결과

```bash
./gradlew test --tests "gift.option.internal.OptionServiceTest"  # BUILD SUCCESSFUL (9개 통과)
./gradlew test                # BUILD SUCCESSFUL (전체 단위 테스트 통과)
./gradlew acceptanceTest      # BUILD SUCCESSFUL (기존 75개 시나리오 통과)
```

---

## 4. Product 크로스 도메인 Repository 제거 현황

| Repository                | 사용자           | Port 교체         | Repository 삭제   |
|---------------------------|---------------|-----------------|-----------------|
| `WishProductRepository`   | WishService   | 완료 (progress-7) | 완료 (progress-7) |
| `OptionProductRepository` | OptionService | **완료**          | **완료**          |

ProductQueryPort 교체 대상 2개 모두 완료.

---

## 5. 전체 Port 교체 대상 Service 현황

| Service             | 교체 대상 Repository                 | 교체할 Port                                | 상태              |
|---------------------|----------------------------------|-----------------------------------------|-----------------|
| ProductService      | `ProductCategoryRepository`      | `CategoryQueryPort`                     | 완료 (progress-5) |
| AdminProductService | `AdminProductCategoryRepository` | `CategoryQueryPort`                     | 완료 (progress-6) |
| WishService         | `WishProductRepository`          | `ProductQueryPort`                      | 완료 (progress-7) |
| OptionService       | `OptionProductRepository`        | `ProductQueryPort`                      | **완료**          |
| OrderService        | `OrderOptionRepository`          | `OptionQueryPort` + `OptionCommandPort` | 미착수             |
| OrderService        | `OrderMemberRepository`          | `MemberQueryPort` + `MemberCommandPort` | 미착수             |
| AuthService (Kakao) | `AuthMemberRepository`           | `MemberQueryPort` + `MemberCommandPort` | 미착수             |
