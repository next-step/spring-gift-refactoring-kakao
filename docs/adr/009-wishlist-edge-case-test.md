# ADR-009: 위시리스트 엣지 케이스 테스트 추가

## 상태
- [x] 승인됨 (2026-03)

## 맥락
주문 시 위시리스트 정리 로직에 대한 테스트가 "위시리스트에 있는 상품 주문" 케이스만 있었다. 위시리스트에 없는 상품 주문 시에도 정상 동작하는지 검증이 필요했다.

## 결정
위시리스트에 없는 상품을 주문해도 성공하는지 테스트를 추가한다.

## 변경 사항

### OrderAcceptanceTest 테스트 추가

```java
@Test
@DisplayName("성공: 위시리스트에 없는 상품도 주문할 수 있다")
void success_orderWithoutWishlist() {
    // Given: 위시리스트가 비어있음
    assertThat(wishRepository.findByMemberIdAndProductId(member.getId(), product.getId()))
        .isEmpty();

    // When: 주문 생성
    RestAssured.given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(/* ... */)
        .when()
        .post("/api/orders")
        .then()
        .statusCode(201);

    // Then: 주문 성공 확인
    assertThat(orderRepository.findByMemberId(member.getId(), Pageable.unpaged()).getContent())
        .as("위시리스트에 없어도 주문이 생성되어야 한다")
        .hasSize(1);
}
```

## 테스트 커버리지

| 케이스 | 위시리스트 상태 | 기대 결과 |
|--------|----------------|-----------|
| 기존 | 상품 있음 | 주문 성공, 위시리스트에서 제거 |
| 추가 | 상품 없음 | 주문 성공, 에러 없음 |

## 결과
- 81개 테스트 통과

## 관련 ADR
- [ADR-005: 주문 완료 시 위시리스트 정리](005-wishlist-cleanup-on-order.md) - 위시리스트 정리 로직
