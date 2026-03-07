# Progress 7: WishService — ProductQueryPort 교체 + @EntityGraph 전환 + 단위 테스트

**기간**: 2026-03-05

---

## 이 문서의 목적

Phase 2 "도메인 책임 되찾기" — 세 번째 Service 교체 작업. WishService에서 `WishProductRepository`를 `ProductQueryPort`로 교체하고, WishRepository의 JPQL fetch join을 `@EntityGraph` + Spring Data 파생 쿼리로 전환했다. Service 단위 테스트 7개를 추가했다.

---

## 1. 변경 내용

### WishService 교체 (구조 변경 — 커밋 1)

| 항목        | Before                                                                            | After                                      |
|-----------|-----------------------------------------------------------------------------------|--------------------------------------------|
| 의존성       | `WishProductRepository`                                                           | `ProductQueryPort`                         |
| addWish() | `productRepo.findById(productId).orElseThrow(NotFoundException::productNotFound)` | `productQueryPort.getReference(productId)` |

- `addWish()`는 `@Transactional` 선언 → `ProductQueryPort.getReference()`의 MANDATORY 전파 충족
- 반환 타입 동일(`Product`), 예외 타입 동일(`NotFoundException`) → catch + re-throw 불필요
- `getWishes()`, `removeWish()`는 자체 도메인 `WishRepository`만 사용하므로 변경 없음

수정 파일: `src/main/java/gift/wish/internal/WishService.java`

### 삭제 파일

| 파일                           | 이유                            |
|------------------------------|-------------------------------|
| `WishProductRepository.java` | 사용처 없음 (WishService가 유일한 사용자) |

### WishRepository @EntityGraph 전환 (구조 변경 — 커밋 2)

JPQL `@Query` + `inner join fetch`를 `@EntityGraph` + Spring Data 파생 쿼리로 전환했다.

| Before                                                                 | After                                             |
|------------------------------------------------------------------------|---------------------------------------------------|
| `@Query("select w from Wish w inner join fetch w.product where ...")`  | `@EntityGraph(attributePaths = "product")`        |
| `findByMemberIdInnerJoinFetchProduct(memberId, pageable)`              | `findByMemberId(memberId, pageable)`              |
| `findByMemberIdAndProductIdInnerJoinFetchProduct(memberId, productId)` | `findByMemberIdAndProductId(memberId, productId)` |

`@EntityGraph`는 LEFT JOIN FETCH를 사용하지만, Wish→Product FK가 항상 존재하므로 INNER JOIN과 결과가 동일하다. 메서드명이 단순화되고, JPQL 수동 관리가 제거되었다.

수정 파일: `WishRepository.java`, `WishService.java`, `WishServiceTest.java`

### WishServiceTest 추가 (7개 테스트)

파일: `src/test/java/gift/wish/internal/WishServiceTest.java`

| 테스트                          | 검증 내용                                                                                    |
|------------------------------|------------------------------------------------------------------------------------------|
| `testGetWishes`              | `wishRepo.findByMemberId` 호출 + PagedModel 응답 매핑 + 페이지 메타데이터                              |
| `testAddWishNew`             | `productQueryPort.getReference(productId)` 호출 + 중복 없음 → `wishRepo.save` + `created=true` |
| `testAddWishDuplicate`       | `productQueryPort.getReference` 호출 + 중복 있음 → save 안 함 + `created=false`                  |
| `testAddWishProductNotFound` | `productQueryPort.getReference` → NotFoundException 전파                                   |
| `testRemoveWish`             | `wishRepo.findById` + 소유자 일치 → `wishRepo.delete`                                         |
| `testRemoveWishNotFound`     | `wishRepo.findById` 빈 결과 → NotFoundException                                             |
| `testRemoveWishForbidden`    | `wishRepo.findById` + 소유자 불일치 → ForbiddenException                                       |

---

## 2. 검증 결과

```bash
./gradlew test --tests "gift.wish.internal.WishServiceTest"  # BUILD SUCCESSFUL (7개 통과)
./gradlew test                # BUILD SUCCESSFUL (전체 단위 테스트 통과)
./gradlew acceptanceTest      # BUILD SUCCESSFUL (기존 75개 시나리오 통과)
```

---

## 3. Product 크로스 도메인 Repository 제거 현황

| Repository                | 사용자           | Port 교체 | Repository 삭제 |
|---------------------------|---------------|---------|---------------|
| `WishProductRepository`   | WishService   | **완료**  | **완료**        |
| `OptionProductRepository` | OptionService | 미착수     | 미착수           |

---

## 4. 전체 Port 교체 대상 Service 현황

| Service             | 교체 대상 Repository                 | 교체할 Port                                | 상태              |
|---------------------|----------------------------------|-----------------------------------------|-----------------|
| ProductService      | `ProductCategoryRepository`      | `CategoryQueryPort`                     | 완료 (progress-5) |
| AdminProductService | `AdminProductCategoryRepository` | `CategoryQueryPort`                     | 완료 (progress-6) |
| WishService         | `WishProductRepository`          | `ProductQueryPort`                      | **완료**          |
| OptionService       | `OptionProductRepository`        | `ProductQueryPort`                      | 미착수             |
| OrderService        | `OrderOptionRepository`          | `OptionQueryPort` + `OptionCommandPort` | 미착수             |
| OrderService        | `OrderMemberRepository`          | `MemberQueryPort` + `MemberCommandPort` | 미착수             |
| AuthService (Kakao) | `AuthMemberRepository`           | `MemberQueryPort` + `MemberCommandPort` | 미착수             |
