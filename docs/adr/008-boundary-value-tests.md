# ADR-008: 경계값 테스트 추가

## 상태
- [x] 승인됨 (2026-03)

## 맥락
도메인 로직의 경계값 케이스에 대한 테스트가 부족했다. `Option.calculateTotalPrice()` 메서드에 수량 0에 대한 테스트가 없었다.

## 결정
경계값(0, 1, 일반값)에 대한 단위 테스트를 추가한다.

## 변경 사항

### OptionTest 경계값 테스트 추가

```java
@Test
@DisplayName("calculateTotalPrice: 수량이 0일 때 0을 반환한다")
void calculateTotalPrice_zeroQuantity() {
    // Given
    Option option = new Option(product, "옵션", 100);

    // When
    int totalPrice = option.calculateTotalPrice(0);

    // Then
    assertThat(totalPrice)
        .as("수량 0일 때 가격은 0이어야 한다")
        .isEqualTo(0);
}
```

## 테스트 커버리지

| 케이스 | 입력 | 기대 결과 |
|--------|------|-----------|
| 일반값 | 5 | 5000 (1000 * 5) |
| 최소값 | 1 | 1000 |
| 경계값 | 0 | 0 |

## 참고
음수 수량은 `OrderRequest`의 `@Min(1)` validation에서 걸러지므로 도메인 단위 테스트에서는 제외.

## 결과
- 80개 테스트 통과

## 관련 ADR
- [ADR-006: 도메인 책임 되찾기](006-domain-responsibility.md) - calculateTotalPrice() 도입
