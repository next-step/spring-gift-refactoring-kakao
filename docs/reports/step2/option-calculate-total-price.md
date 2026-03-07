# 작업 4: 주문 금액 계산 도메인 이동 - 실행 계획 보고서

## 작업 목적 및 배경

`OrderService.createOrder()`에서 주문 금액을 계산할 때 `option.getProduct().getPrice() * quantity`로 디미터 법칙을 위반하고 있다.
가격 계산 책임을 Option 도메인 객체로 이동하여 캡슐화를 개선한다.

## 현황 분석

### OrderService.java (line 39)
```java
var price = option.getProduct().getPrice() * quantity;
```
- 열차 충돌 호출: `option` -> `product` -> `price`
- 가격 계산 로직이 서비스 레이어에 존재 (도메인 빈약 모델)

### Option.java
- `Product product` 필드를 이미 보유 (ManyToOne 관계)
- `getProduct()` getter가 존재하므로 내부에서 `product.getPrice()` 접근 가능
- 현재 `subtractQuantity()` 메서드만 존재

### OptionTest.java
- `subtractQuantity()` 관련 단위 테스트 4개 존재
- `@BeforeEach`에서 Product(price=10000) 생성 → 재사용 가능

## 변경 대상 파일

### 1. `src/main/java/gift/model/Option.java`
- **추가**: `calculateTotalPrice(int quantity)` 메서드
```java
public int calculateTotalPrice(int quantity) {
    return product.getPrice() * quantity;
}
```
- 반환 타입: `int` (Product.price가 int이므로 동일)

### 2. `src/main/java/gift/service/OrderService.java` (line 39)
- **기존**: `var price = option.getProduct().getPrice() * quantity;`
- **변경**: `var price = option.calculateTotalPrice(quantity);`
- 한 줄만 변경, 나머지 로직 동일

### 3. `src/test/java/gift/model/OptionTest.java`
- **추가**: `calculateTotalPrice` 단위 테스트
  - 수량 1일 때 상품 가격과 동일한지 검증
  - 수량 N일 때 가격 * N 결과 검증

## 바꾸지 않는 것
- 계산 결과값 (가격 x 수량)은 동일하게 유지
- Product 클래스 변경 없음
- Order 클래스 변경 없음
- 기존 OrderControllerTest, OptionTest 전체 통과 필수
- `getProduct()` getter는 유지 (다른 곳에서 사용될 수 있음)

## 커밋 메시지
```
refactor(option): 주문 금액 계산을 Option 도메인으로 이동
```
