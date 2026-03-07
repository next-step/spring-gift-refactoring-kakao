# Plan 1: 크로스 도메인 Repository → Port 교체

## 현재 상태

Phase 2 "도메인 책임 되찾기"의 Port 구성 단계가 완료되었다.
4개 도메인에 대해 6개 Port 인터페이스 + 6개 Adaptor 구현체 + 30개 테스트를 구축했다.
이제 **기존 크로스 도메인 Repository를 Port로 교체**하는 단계이다.

| Port              | 메서드                                               | 테스트 | 상태 |
|-------------------|---------------------------------------------------|-----|----|
| CategoryQueryPort | `getReference`, `findAll`                         | 5개  | 완료 |
| ProductQueryPort  | `getReference`, `validateExists`                  | 5개  | 완료 |
| MemberQueryPort   | `getIdByEmail`, `getEmail`, `getKakaoAccessToken` | 5개  | 완료 |
| MemberCommandPort | `deductPoint`, `create`, `updateKakaoAccessToken` | 5개  | 완료 |
| OptionQueryPort   | `getReference`, `getAssociatedProduct`            | 5개  | 완료 |
| OptionCommandPort | `subtractQuantity`                                | 3개  | 완료 |

---

## 교체 대상 분석

### 크로스 도메인 Repository 7개 (삭제 대상)

| 크로스 도메인 Repository               | 위치               | 참조 Entity |
|----------------------------------|------------------|-----------|
| `ProductCategoryRepository`      | product/internal | Category  |
| `AdminProductCategoryRepository` | product/admin    | Category  |
| `WishProductRepository`          | wish/internal    | Product   |
| `OptionProductRepository`        | option/internal  | Product   |
| `OrderOptionRepository`          | order/internal   | Option    |
| `OrderMemberRepository`          | order/internal   | Member    |
| `AuthMemberRepository`           | auth/internal    | Member    |

*(참고: `OrderWishRepository`는 ADR-003에서 이벤트로 대체 예정이므로 이 계획에 포함하지 않는다)*

---

## Service별 교체 상세

### 1. ProductService (gift.product.internal)

**교체**: `ProductCategoryRepository` → `CategoryQueryPort`

| 메서드               | 현재                                                | 교체 후                                         |
|-------------------|---------------------------------------------------|----------------------------------------------|
| `createProduct()` | `categoryRepo.findById(categoryId).orElseThrow()` | `categoryQueryPort.getReference(categoryId)` |
| `updateProduct()` | `categoryRepo.findById(categoryId).orElseThrow()` | `categoryQueryPort.getReference(categoryId)` |

두 메서드 모두 `@Transactional`이므로 `getReference(MANDATORY)` 사용 가능.

---

### 2. AdminProductService (gift.product.admin)

**교체**: `AdminProductCategoryRepository` → `CategoryQueryPort`

| 메서드                  | 현재                                                       | 교체 후                                         |
|----------------------|----------------------------------------------------------|----------------------------------------------|
| `getAllCategories()` | `categoryRepo.findAll().stream().map(CategoryDto::from)` | `categoryQueryPort.findAll()` + 매핑           |
| `createProduct()`    | `categoryRepo.findById(categoryId).orElseThrow()`        | `categoryQueryPort.getReference(categoryId)` |
| `updateProduct()`    | `categoryRepo.findById(categoryId).orElseThrow()`        | `categoryQueryPort.getReference(categoryId)` |

**CategoryDto 매핑**: `AdminProductService`가 사용하는 `ProductDto.CategoryDto`는 `(id, name)` 2개 필드이고, `CategoryQueryPort.findAll()`이 반환하는 `gift.category.CategoryDto`는 `(id, name, color, imageUrl, description)` 5개 필드이다. 작동 변경을 방지하기 위해 매핑한다:

```java
categoryQueryPort.findAll().stream()
        .map(dto -> new ProductDto.CategoryDto(dto.id(), dto.name()))
        .toList();
```

`createProduct()`, `updateProduct()`는 `@Transactional`이므로 `getReference(MANDATORY)` 사용 가능.

---

### 3. WishService (gift.wish.internal)

**교체**: `WishProductRepository` → `ProductQueryPort`

| 메서드         | 현재                                              | 교체 후                                       |
|-------------|-------------------------------------------------|--------------------------------------------|
| `addWish()` | `productRepo.findById(productId).orElseThrow()` | `productQueryPort.getReference(productId)` |

`addWish()`는 `@Transactional`이므로 `getReference(MANDATORY)` 사용 가능.
Product entity는 `Wish.builder().product(product)` FK 참조에 그대로 사용.

---

### 4. OptionService (gift.option.internal)

**교체**: `OptionProductRepository` → `ProductQueryPort`

| 메서드              | 현재                                                                                    | 교체 후                                                                                    |
|------------------|---------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| `getOptions()`   | `productRepo.findByIdLeftJoinFetchOptions(productId)` → `product.getOptions()`        | `productQueryPort.validateExists(productId)` + `optionRepo.findByProductId(productId)`  |
| `createOption()` | `productRepo.findById(productId).orElseThrow()`                                       | `productQueryPort.getReference(productId)`                                              |
| `deleteOption()` | `productRepo.findByIdLeftJoinFetchOptions(productId)` → `product.getOptions().size()` | `productQueryPort.validateExists(productId)` + `optionRepo.countByProductId(productId)` |

**주요 변경 포인트:**

`getOptions()`는 `@Transactional`이 없다. 현재는 `findByIdLeftJoinFetchOptions`로 Product + Options를 한 번에 로드하지만, Port 교체 후에는:

1. `productQueryPort.validateExists(productId)` — 상품 존재 검증 (트랜잭션 불필요)
2. `optionRepo.findByProductId(productId)` — Option 자체 도메인 조회

`deleteOption()`은 `@Transactional`이 있다. 현재는 Product의 Options 컬렉션으로 잔여 옵션 수를 확인하지만, Port 교체 후에는:

1. `productQueryPort.validateExists(productId)` — 상품 존재 검증
2. `optionRepo.countByProductId(productId)` — 잔여 옵션 수 확인

**OptionRepository 추가 메서드 (자체 도메인):**

- `List<Option> findByProductId(Long productId)` — Spring Data 파생 쿼리
- `long countByProductId(Long productId)` — Spring Data 파생 쿼리

**쿼리 횟수 비교 (deleteOption):**

|                 | 현재                                              | Port 교체 후                                                    |
|-----------------|-------------------------------------------------|--------------------------------------------------------------|
| 상품 검증 + 옵션 수 확인 | `findByIdLeftJoinFetchOptions` (1회, heavy join) | `validateExists` (1회) + `countByProductId` (1회, lightweight) |
| 옵션 조회           | `findByIdAndProductId` (1회)                     | `findByIdAndProductId` (1회)                                  |
| 합계              | **2회**                                          | **3회**                                                       |

1회 더 나가지만, 현재의 `findByIdLeftJoinFetchOptions`는 Product + 모든 Option을 통째로 로드하는 heavy join이다. 교체 후 쿼리 3개는 모두 lightweight이므로 실질적 DB 부하는 비슷하거나 오히려 가볍다.

`countByProductId`와 `findByIdAndProductId` 사이에 JPA 1차 캐시 이득은 없다. `countByProductId`는 스칼라를 반환하므로 엔티티를 캐시에 저장하지 않고, `findByIdAndProductId`는 커스텀 JPQL이라 항상 DB에 쿼리를 보낸다.

---

### 5. OrderService (gift.order.internal)

**교체**: `OrderOptionRepository` → `OptionQueryPort` + `OptionCommandPort`, `OrderMemberRepository` → `MemberCommandPort`

현재 `createOrder()` 흐름:

```java
// 현재
Option option = optionRepo.findByIdInnerJoinFetchProduct(optionId).orElseThrow();
option.subtractQuantity(quantity);

Product product = option.getProduct();
int price = product.getPrice() * quantity;
memberRepo.findById(memberId).orElseThrow().deductPoint(price);

Order build = Order.builder().option(option)...build();
```

```java
// 교체 후
optionCommandPort.subtractQuantity(optionId, quantity);              // 수량 차감

Option option = optionQueryPort.getReference(optionId);              // FK 참조
ProductDto product = optionQueryPort.getAssociatedProduct(optionId); // 가격 조회
int price = product.price() * quantity;

memberCommandPort.deductPoint(memberId, price);                      // 포인트 차감

Order build = Order.builder().option(option)...build();
```

**호출 순서 결정**: `subtractQuantity`를 `getReference`보다 먼저 호출한다. 기능적으로 두 순서 모두 동일하지만 (같은 `@Transactional` 내 1차 캐시 공유), `subtractQuantity`(비즈니스 액션)를 먼저 수행하고 `getReference`(FK 참조) + `getAssociatedProduct`(정보 조회)를 모아서 "수량 차감 → 데이터 수집 → 포인트 차감 → 주문 생성" 흐름이 의도 단위로 묶여 가독성이 좋다.

`createOrder()`는 `@Transactional`이므로 `getReference(MANDATORY)` 사용 가능.

---

### 6. KakaoMessagingService (gift.order.internal)

**교체**: `OrderMemberRepository` → `MemberQueryPort`

| 메서드                              | 현재                                                                  | 교체 후                                            |
|----------------------------------|---------------------------------------------------------------------|-------------------------------------------------|
| `sendDefaultTemplateMessageTo()` | `memberRepo.findById(memberId).orElseThrow().getKakaoAccessToken()` | `memberQueryPort.getKakaoAccessToken(memberId)` |

---

### 7. KakaoAuthService (gift.auth.internal)

**교체**: `AuthMemberRepository` → `MemberQueryPort` + `MemberCommandPort`

현재 `loginWithKakao()` 흐름:

```java
// 현재 — upsert 패턴
Member member = memberRepository.findByEmail(email)
        .orElseGet(() -> Member.builder().email(email).build());
member.updateKakaoAccessToken(kakaoToken.accessToken());
memberRepository.save(member);

String token = jwtProvider.createToken(member.getEmail());
```

```java
// 교체 후
try {
    Long memberId = memberQueryPort.getIdByEmail(email);
    memberCommandPort.updateKakaoAccessToken(memberId, kakaoToken.accessToken());
} catch (NotFoundException e) {
    memberCommandPort.create(new MemberInfo(email, null, kakaoToken.accessToken()));
}
String token = jwtProvider.createToken(email);
```

`getIdByEmail`은 NotFoundException을 던지므로 try-catch로 분기.
`email`은 카카오 응답에서 이미 확보했으므로 재조회 불필요.

---

### 8. AuthenticationPortImpl (gift.auth.internal)

**교체**: `AuthMemberRepository` → `MemberQueryPort`

현재:

```java
// 현재
memberId = memberRepo.findByEmail(memberEmail)
        .map(Member::getId)
        .orElse(null);
```

```java
// 교체 후
try {
    memberId = memberQueryPort.getIdByEmail(memberEmail);
} catch (NotFoundException ignored) {
}
```

---

## 교체 후 삭제 대상

### 크로스 도메인 Repository 7개 삭제

| 파일                                                       | 삭제 시점 |
|----------------------------------------------------------|-------|
| `gift/product/internal/ProductCategoryRepository.java`   | 1단계   |
| `gift/product/admin/AdminProductCategoryRepository.java` | 1단계   |
| `gift/wish/internal/WishProductRepository.java`          | 2단계   |
| `gift/option/internal/OptionProductRepository.java`      | 2단계   |
| `gift/order/internal/OrderMemberRepository.java`         | 3단계   |
| `gift/auth/internal/AuthMemberRepository.java`           | 3단계   |
| `gift/order/internal/OrderOptionRepository.java`         | 4단계   |

### 테스트용 Repository도 확인 필요

`src/test/java/gift/support/` 아래 테스트 전용 Repository 중 삭제된 크로스 도메인 Repository를 참조하는 것이 있는지 확인한다.

---

## 커밋 전략

Port 교체는 **구조 변경**이다. 외부 작동은 바뀌지 않으므로 기존 테스트가 모두 통과해야 한다.

교체 단위는 **Port 기준**으로 묶는다. 하나의 Port에 대한 교체가 하나의 커밋이다.

```
[1] refactor: CategoryQueryPort 교체, ProductCategoryRepository + AdminProductCategoryRepository 삭제
      수정: ProductService, AdminProductService
      삭제: ProductCategoryRepository, AdminProductCategoryRepository

[2] refactor: ProductQueryPort 교체, WishProductRepository + OptionProductRepository 삭제
      수정: WishService, OptionService, OptionRepository (자체 도메인 메서드 추가)
      삭제: WishProductRepository, OptionProductRepository

[3] refactor: MemberPort 교체, OrderMemberRepository + AuthMemberRepository 삭제
      수정: OrderService (member 부분), KakaoMessagingService, KakaoAuthService, AuthenticationPortImpl
      삭제: OrderMemberRepository, AuthMemberRepository

[4] refactor: OptionPort 교체, OrderOptionRepository 삭제
      수정: OrderService (option 부분)
      삭제: OrderOptionRepository

[5] refactor: AuthenticationPort 인터페이스 단순화
      수정: AuthenticationPort (Optional<Long> → Long + UnauthorizedException),
            AuthenticationPortImpl, WishController, OrderController

[6] refactor: WishRepository @EntityGraph 적용
      수정: WishRepository (JPQL fetch join → @EntityGraph + Spring Data 파생 쿼리)
```

각 커밋 후 `./gradlew test` + `./gradlew acceptanceTest` 통과를 확인한다.

**[1]~[4]**: 크로스 도메인 Repository → Port 교체 (핵심)
**[5]**: AuthenticationPort 시그니처 개선 — Port 교체와 별도 커밋으로 분리하여 목적 1개 유지
**[6]**: WishRepository JPQL → @EntityGraph 전환 — Port 교체와 무관한 독립적 구조 변경

---

## 논의 결정 사항

### 1. AdminProductService의 CategoryDto 매핑 — 결정됨

`gift.category.CategoryDto` (5개 필드)를 `ProductDto.CategoryDto` (2개 필드)로 매핑한다.
`gift.category.CategoryDto`를 그대로 사용하면 admin 응답 구조가 달라지므로 작동 변경이 된다.
`ProductDto.CategoryDto`는 `ProductDto`의 내부 클래스이므로 현재 구조를 유지하는 것이 자연스럽다.

### 2. OptionService의 자체 도메인 Repository 메서드 추가 — 결정됨

`findByIdLeftJoinFetchOptions` 대체를 위해 OptionRepository에 `findByProductId`, `countByProductId`를 추가한다.
이는 자체 도메인 Repository에 메서드를 추가하는 것이므로 크로스 도메인 문제가 아니다.

### 3. KakaoAuthService의 upsert 패턴 — 결정됨

`getIdByEmail`은 NotFoundException을 던지므로 try-catch로 "존재하면 update, 없으면 create" 분기를 처리한다.
현재 Port 인터페이스에서는 NotFoundException 패턴이 일관되므로 try-catch가 적절하다.

### 4. OrderService createOrder 호출 순서 — 결정됨

`subtractQuantity`를 `getReference`보다 먼저 호출한다.
비즈니스 액션(수량 차감)을 먼저 수행하고, 데이터 수집(FK 참조 + 가격 조회)을 모아서 의도 단위로 묶는다.

### 5. AuthenticationPort 인터페이스 변경 — 별도 커밋으로 분리

`Optional<Long>` → `Long` + `UnauthorizedException` 변경은 MemberPort 교체와 별도 커밋으로 분리한다.
MemberPort 교체 커밋의 목적은 "AuthMemberRepository → MemberQueryPort 교체"이고,
AuthenticationPort 시그니처 변경 + Controller 수정은 별개의 목적이다.

### 6. WishRepository @EntityGraph 적용 — 별도 커밋

Port 교체 후, 작동 변경 전에 별도 구조 변경으로 진행한다.
JPQL fetch join (`findByMemberIdInnerJoinFetchProduct`)을 `@EntityGraph` + Spring Data 파생 쿼리로 전환한다.
`@EntityGraph`는 LEFT JOIN FETCH를 사용하지만, Wish→Product FK가 항상 존재하므로 INNER JOIN과 결과가 동일하다.
