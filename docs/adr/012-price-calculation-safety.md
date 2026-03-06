# ADR-012: 가격 계산 안전성 강화

## 상태
- [x] 승인됨 (2026-03)

## 맥락
리뷰어가 `Option.calculateTotalPrice()` 메서드의 두 가지 엣지 케이스를 지적했다:

1. **Integer Overflow**: 100,000원 × 30,000개 = 3,000,000,000 (Integer.MAX_VALUE 초과)
2. **음수 수량**: 음수 입력 시 음수 가격 반환

## 문제 코드

```java
public int calculateTotalPrice(int orderQuantity) {
    return product.getPrice() * orderQuantity;  // 오버플로우, 음수 무방비
}
```

## 결정
1. 반환 타입을 `long`으로 변경하여 오버플로우 방지
2. 음수 수량 검증 추가

## 변경 사항

### Option.java
```java
public long calculateTotalPrice(int orderQuantity) {
    if (orderQuantity < 0) {
        throw new IllegalArgumentException("주문 수량은 0 이상이어야 합니다.");
    }
    return (long) product.getPrice() * orderQuantity;
}
```

### OrderService.java
```java
long price = option.calculateTotalPrice(request.quantity());
member.deductPoint(Math.toIntExact(price));  // int 범위 초과 시 예외
```

### OptionTest.java
- 음수 수량 예외 테스트 추가
- 대량 주문 (int 범위 초과) 테스트 추가

## 테스트 증거

```java
@Test
@DisplayName("calculateTotalPrice: 음수 수량은 예외를 발생시킨다")
void calculateTotalPrice_negativeQuantity() {
    assertThatThrownBy(() -> option.calculateTotalPrice(-1))
        .isInstanceOf(IllegalArgumentException.class);
}

@Test
@DisplayName("calculateTotalPrice: 대량 주문도 오버플로우 없이 계산한다")
void calculateTotalPrice_largeQuantity() {
    // 100,000원 * 30,000개 = 3,000,000,000 (int 범위 초과)
    long totalPrice = option.calculateTotalPrice(30_000);
    assertThat(totalPrice).isEqualTo(3_000_000_000L);
}
```

## 결과
- 83개 테스트 통과 (기존 81개 + 음수 테스트 1개 + 대량 주문 테스트 1개)
- Integer 오버플로우 방지
- 도메인 객체의 자기 방어 강화

## 관련 ADR
- [ADR-006: 도메인 책임 되찾기](006-domain-responsibility.md) - calculateTotalPrice 도입
