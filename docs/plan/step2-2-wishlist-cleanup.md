# Step 2-2: 누락된 작동 구현 (위시리스트 정리)

## 현재 상황

### 코드 위치
`src/main/java/gift/order/OrderService.java:24`

```java
private final WishRepository wishRepository;  // TODO: 주문 완료 후 위시리스트 정리 기능 구현 예정
```

### 문제점
- `WishRepository`가 주입되어 있지만 사용되지 않음
- 주문 완료 후 해당 상품이 위시리스트에 남아있음

### 요구사항
- 주문 완료 시 해당 상품을 위시리스트에서 제거

---

## 해결 계획

### Step 1: 위시리스트 정리 테스트 추가
**파일**: `src/test/java/gift/order/OrderAcceptanceTest.java`

```java
@Test
@DisplayName("성공: 주문 완료 시 위시리스트에서 해당 상품이 제거된다")
void success_removesFromWishlist() {
    // Given: 위시리스트에 상품 추가
    // When: 주문 생성
    // Then: 위시리스트에서 해당 상품 제거됨
}
```

### Step 2: 로직 구현
**파일**: `src/main/java/gift/order/OrderService.java`

```java
// save order 이후, kakao notification 이전에 추가
// cleanup wishlist
wishRepository.findByMemberIdAndProductId(member.getId(), option.getProduct().getId())
    .ifPresent(wishRepository::delete);
```

### Step 3: TODO 주석 제거

### Step 4: 테스트 통과 확인

---

## 체크리스트

- [x] Step 1: 위시리스트 정리 테스트 추가
- [x] Step 2: 로직 구현
- [x] Step 3: TODO 주석 제거
- [x] Step 4: 전체 테스트 통과 확인 (77개)
- [x] ADR 004 갱신
- [ ] 커밋
