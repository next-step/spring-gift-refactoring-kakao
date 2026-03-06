# 6-1. 디미터 법칙 위반 해소 — DTO 변환에서 엔티티 체인 접근 제거

## 작업 목적 및 배경

DTO의 `from()` 메서드와 서비스 코드에서 `wish.getProduct().getName()` 같은 엔티티 체인 접근(디미터 법칙 위반)이 존재했다. 엔티티에 위임 메서드를 추가하고 호출부를 변경하여 디미터 법칙을 준수하도록 개선했다.

## 변경 사항 요약

### 엔티티 위임 메서드 추가 (총 7개)

| 엔티티 | 메서드 | 위임 대상 |
|--------|--------|----------|
| `Wish` | `productId()` | `product.getId()` |
| `Wish` | `productName()` | `product.getName()` |
| `Wish` | `productPrice()` | `product.getPrice()` |
| `Wish` | `productImageUrl()` | `product.getImageUrl()` |
| `Product` | `categoryId()` | `category.getId()` |
| `Order` | `optionId()` | `option.getId()` |
| `Option` | `productId()` | `product.getId()` |

### 호출부 변경 (총 8건)

| 파일 | 변경 전 | 변경 후 |
|------|---------|---------|
| `WishResponse.java` | `wish.getProduct().getId()` | `wish.productId()` |
| `WishResponse.java` | `wish.getProduct().getName()` | `wish.productName()` |
| `WishResponse.java` | `wish.getProduct().getPrice()` | `wish.productPrice()` |
| `WishResponse.java` | `wish.getProduct().getImageUrl()` | `wish.productImageUrl()` |
| `ProductResponse.java` | `product.getCategory().getId()` | `product.categoryId()` |
| `OrderResponse.java` | `order.getOption().getId()` | `order.optionId()` |
| `OptionService.java` (delete) | `option.getProduct().getId()` | `option.productId()` |
| `OptionService.java` (update) | `option.getProduct().getId()` | `option.productId()` |

### 수정된 파일 목록

- `src/main/java/gift/model/Wish.java`
- `src/main/java/gift/model/Product.java`
- `src/main/java/gift/model/Order.java`
- `src/main/java/gift/model/Option.java`
- `src/main/java/gift/dto/WishResponse.java`
- `src/main/java/gift/dto/ProductResponse.java`
- `src/main/java/gift/dto/OrderResponse.java`
- `src/main/java/gift/service/OptionService.java`

## 적용된 규칙

- **위임 메서드 네이밍**: `get` prefix 없이 `productId()`, `categoryId()` 형태를 사용.
  - 이유: `getProductId()` 형태는 JavaBean 프로퍼티로 인식되어 Spring Data JPA의 파생 쿼리(`findByProductId` 등) 해석과 충돌함. `get` prefix를 제거하면 JavaBean 프로퍼티로 인식되지 않아 충돌을 방지할 수 있음.
- 기존 `getProduct()`, `getCategory()`, `getOption()` getter는 유지 (다른 곳에서 사용 중)
- `Option.calculatePrice()` 내부의 `product.getPrice()`는 별도 처리 대상 (6-2)

## 참고 사항

- 구조 변경만 수행, 작동 변경 없음
- 전체 테스트 (131건) 통과 확인
