# 2단계 ADR (Architecture Decision Records)

이 문서는 2단계 리팩터링 과정에서 내린 의사결정을 기록합니다.

---

## ADR-001: @Transactional 적용 범위
### 맥락
프로젝트 전체에 `@Transactional`이 하나도 없었다.

특히 `OrderService.createOrder()`는 재고 차감(`optionRepository.save`) <br>
-> 포인트 차감(`memberRepository.save`) <br> 
-> 주문 저장(`orderRepository.save`) <br>

3개의 저장을 수행하는데, 중간에 실패하면 부분 반영이 발생하는 상태였다.

### 결정
상태를 변경하는 Service 메서드 중 조회 + 수정 + 저장 패턴, 또는 여러 저장이 하나의 논리 작업인 메서드에 `@Transactional`을 적용한다.

적용 대상 (10개 메서드):
- `OrderService.createOrder()`: 3개 save, 가장 위험
- `MemberService.register()`: 중복 체크 + save
- `MemberService.updateMember()`: read + modify + save
- `MemberService.chargePoint()`: read + modify + save
- `ProductService.createProduct()`: 카테고리 조회 + save
- `ProductService.updateProduct()`: read + modify + save
- `CategoryService.updateCategory()`: read + modify + save
- `OptionService.createOption()`: 중복 체크 + save
- `OptionService.deleteOption()`: 옵션 수 체크 + delete
- `WishService.addWish()`: 존재 여부 체크 + conditional save
- `KakaoAuthService.handleCallback()`: find or create + save

단순 단건 save(`createMember`, `createCategory`)나 단건 delete, 읽기 전용 메서드에는 적용하지 않았다.

### 근거
- 대안 1: 클래스 레벨 `@Transactional`: 읽기 전용 메서드에도 불필요한 트랜잭션이 열린다. 메서드별로 필요한 곳에만 적용하는 것이 의도가 명확하다.
- 대안 2: 클래스 레벨 `@Transactional(readOnly = true)` + 쓰기 메서드에 `@Transactional`: 깔끔하지만 현재 프로젝트 규모에서는 오버엔지니어링.

### 결과
- `OrderService.createOrder()` 중간 실패 시 전체 롤백됨 (통합 테스트로 증명)
- 다른 Service도 check-then-act 패턴의 동시성 안전성 향상
- 성능 영향은 미미 (단건 트랜잭션 -> 동일 범위 트랜잭션)

### 증거
- `OrderServiceTransactionTest.rollsBackOnOrderSaveFailure()`: `@SpringBootTest`로 실제 DB에서 롤백 검증
  - `@Transactional` 없이 실행 시: 재고와 포인트가 차감된 채로 남음 (테스트 실패)
  - `@Transactional` 적용 후: 전체 롤백되어 원래 상태 유지 (테스트 통과)

## Order에 totalPrice 영속화
### 맥락
`OrderService.createOrder()`에서 `option.getProduct().getPrice() * request.quantity()`로 총 금액을 계산하여 포인트를 차감하지만, 이 값을 `Order` 엔티티에 저장하지 않았다. 동일한 계산이 `KakaoMessageClient.buildTemplate()`에서도 독립적으로 수행되고 있었다.

이로 인한 문제:
1. 주문 후 상품 가격이 변경되면 과거 주문의 실제 결제 금액을 알 수 없음
2. `OrderResponse`에 가격 정보가 없어 클라이언트가 주문 금액을 알 수 없음
3. 가격 계산이 두 곳에 흩어져 있어 불일치 위험

### 결정
`Order` 엔티티에 `totalPrice` 필드를 추가하고, 주문 생성 시 계산된 금액을 저장한다.

- `Order` 생성자에 `totalPrice` 파라미터 추가
- `OrderService.createOrder()`에서 계산한 `price`를 `Order` 생성자에 전달
- `OrderResponse`에 `totalPrice` 필드 추가
- Flyway `V3__Add_total_price_to_orders.sql`로 DB 마이그레이션

### 근거
- 대안 1: 조회 시마다 재계산: 상품 가격이 변경되면 과거 주문 금액이 바뀌는 문제. 주문 시점의 가격을 보존해야 한다.
- 대안 2: `Order`에 `unitPrice` + `quantity`를 저장하고 `getTotalPrice()`로 계산: 유연하지만, 현재 `quantity`는 이미 있고 `unitPrice`를 추가하면 `Product.price`와 중복. 현 단계에서는 `totalPrice` 직접 저장이 단순하다.

### 결과
- 주문 시점의 결제 금액이 DB에 영속화됨
- `OrderResponse`에 `totalPrice`가 포함되어 클라이언트가 금액 확인 가능
- 가격 계산 로직의 위치 이동(중복 제거)은 단계 3(구조 변경)에서 별도 수행

### 증거
- `OrderTotalPriceTest.savesTotalPriceOnOrderCreation()`: 주문 생성 후 `EntityManager.flush()` + `clear()` 후 DB에서 재조회하여 `totalPrice == 10000` (5000 × 2) 검증
- `OrderTotalPriceTest.totalPriceEqualsUnitPriceForQuantityOne()`: 수량 1일 때 `totalPrice == 5000` 검증

## 인증 중복 제거: `HandlerMethodArgumentResolver` 도입
### 맥락
`OrderController`(2곳)와 `WishController`(3곳), 총 5개 메서드에서 동일한 인증 패턴이 반복되고 있었다:
```java
var member = authenticationResolver.extractMember(authorization);
if (member == null) {
    return ResponseEntity.status(401).build();
}
```

### 결정
Spring의 `HandlerMethodArgumentResolver` 인터페이스를 구현하여 인증 로직을 MVC 인프라로 이동한다.

- `@LoginMember` 어노테이션 생성
- `MemberArgumentResolver` 생성 (`HandlerMethodArgumentResolver` 구현)
- `WebMvcConfig` 생성 (`WebMvcConfigurer` 구현, resolver 등록)
- 컨트롤러에서 `@RequestHeader("Authorization")` + null 체크 -> `@LoginMember Member member`로 교체
- 기존 `AuthenticationResolver`는 유지 (`MemberArgumentResolver`가 위임)

### 근거
- 대안 1: `AuthenticationResolver.extractMember()`에서 null 대신 예외를 던지도록 변경: null 체크 제거는 가능하지만, 컨트롤러마다 `extractMember()` 호출 자체가 반복됨. 호출부 단순화 부족.
- 대안 2: Spring Security 도입: 현재 프로젝트 규모에 비해 과도한 의존성. `HandlerMethodArgumentResolver`가 적절한 수준.
- 대안 3: `HandlerMethodArgumentResolver` (선택됨): 컨트롤러 파라미터에 `@LoginMember Member member`만 선언하면 인증이 자동 처리. 호출부가 가장 단순해지고, Spring MVC의 표준 확장 방식.

### 인터페이스 활용
- `HandlerMethodArgumentResolver`: 컨트롤러 메서드 파라미터를 자동으로 resolve하는 Spring MVC 표준 인터페이스
- `WebMvcConfigurer`: MVC 설정을 커스터마이징하는 Spring MVC 표준 인터페이스

### 결과
- 컨트롤러에서 인증 관련 코드 5블록(15줄) 제거
- `OrderController`와 `WishController`에서 `AuthenticationResolver` 의존 제거
- `@WebMvcTest` 사용하는 모든 테스트에 `@MockitoBean AuthenticationResolver` 필요 (WebMvcConfig 의존으로 인해)
- 향후 인증이 필요한 새 컨트롤러는 `@LoginMember Member member`만 추가하면 됨

---

## 판단 로직을 엔티티로 이동
### 맥락
서비스 계층에서 엔티티의 데이터를 꺼내(get) 비교/판단하는 패턴이 3곳에 있었다:
1. `WishService.removeWish()`: `wish.getMemberId().equals(memberId)` 비교
2. `OrderService.sendKakaoMessageIfPossible()`: `member.getKakaoAccessToken() == null` 판단
3. `OptionService.deleteOption()`: `option.getProduct().getId().equals(productId)` 비교 + `.orElse(null)` 후 null 체크

이런 패턴은 서비스가 엔티티의 내부 상태를 알아야 하므로 캡슐화를 약화시킨다.

### 결정
각 엔티티에 의미 있는 이름의 메서드를 추가하여, 서비스가 "물어보지 말고 시키는(Tell Don't Ask)" 방식으로 호출하도록 변경한다.

- `Wish.validateOwnership(Long requesterId)`: 소유권 검증, 실패 시 `IllegalArgumentException`
- `Member.canReceiveKakaoMessage()`: 카카오 토큰 존재 여부 반환
- `Option.validateBelongsTo(Long productId)`: 옵션-상품 소속 검증, 실패 시 `NoSuchElementException`

### 근거
- 대안 1: 서비스에 그대로 두기: 판단 로직이 서비스에 흩어져 있으면 같은 판단이 여러 서비스에서 중복될 수 있고, 엔티티의 내부 구조가 바뀌면 서비스도 수정해야 함.
- 대안 2: 별도 Validator 클래스: 현재 규모에서는 과도. 엔티티 자신이 가장 적절한 판단 주체.

### 결과
- 서비스에서 getter 호출 + 비교 코드 제거 -> 호출부 단순화
- 엔티티가 자신의 불변 조건을 스스로 보호
- `OptionService.deleteOption()`에서 `.orElse(null)` + null 체크 -> `.orElseThrow()` + `validateBelongsTo()`로 개선


## 엔티티 위임 메서드와 Spring Data 충돌
### 맥락
`ProductResponse`, `OrderResponse`, `WishResponse`, `KakaoMessageClient`에서 getter 체인(`a.getB().getC()`)이 반복되고 있었다:
- `product.getCategory().getId()` (ProductResponse)
- `order.getOption().getId()`, `order.getOption().getName()`, `order.getOption().getProduct().getName()` (OrderResponse, KakaoMessageClient)
- `wish.getProduct().getId()`, `wish.getProduct().getName()` 등 (WishResponse)

### 결정
엔티티에 위임 메서드를 추가하되, **Spring Data JPA 파생 쿼리와 충돌하는 경우는 적용하지 않는다.**

적용한 것:
- `Product.getCategoryId()`: `ProductResponse`에서 사용
- `Order.getOptionId()`, `Order.getOptionName()`, `Order.getProductName()`: `OrderResponse`, `KakaoMessageClient`에서 사용

적용하지 않은 것:
- `Wish.getProductId()` 등 위임 메서드: Spring Data JPA의 `findByMemberIdAndProductId()` 파생 쿼리와 충돌

### 근거
- `Wish`에 `getProductId()`를 추가하면 Spring Data JPA가 `Wish.productId`라는 직접 프로퍼티가 있다고 판단하여, `findByMemberIdAndProductId()` 파생 쿼리에서 `product.id` 경로 대신 `productId` 직접 필드를 찾음 -> `PathElementException` 발생
- 대안 1: `@Query`로 명시적 JPQL 작성: 가능하지만, Spring Data의 파생 쿼리 편의성을 포기해야 함
- 대안 2: 위임 메서드 이름을 `getProductId()` 외의 이름으로 변경: JavaBean 규약과 맞지 않고 혼란 유발
- 대안 3: `Wish`에는 위임 메서드를 두지 않음 (선택됨): `WishResponse`에서의 getter 체인은 Response 매핑이라는 인프라 관심사이므로, Demeter 위반의 실질적 위험이 낮음

### 결과
- `Product`, `Order`에서는 getter 체인이 제거되어 호출부가 단순해짐
- `Wish`에서는 Spring Data 호환성을 우선하여 기존 getter 체인 유지
- 향후 `@ManyToOne` 관계가 있는 엔티티에 위임 메서드를 추가할 때, 해당 엔티티의 Repository에 파생 쿼리가 있는지 반드시 확인해야 함
