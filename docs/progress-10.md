# Progress 10: OrderService + KakaoMessagingService — Port 교체 + 단위 테스트

**기간**: 2026-03-05

---

## 이 문서의 목적

Phase 2 "도메인 책임 되찾기" — 여섯 번째(마지막) Service 교체 작업. `OrderService`가 사용하던 크로스 도메인 Repository 3개(`OrderOptionRepository`, `OrderMemberRepository`, `OrderWishRepository`)를 Port로 교체하고, `KakaoMessagingService`의 `OrderMemberRepository` 사용도 함께 교체했다. 단위 테스트 8개를 추가했다.

---

## 1. 변경 내용

### 1-1. OrderService 교체 (구조 변경)

#### 의존성 변경

| Before                  | After               |
|-------------------------|---------------------|
| `OrderOptionRepository` | `OptionQueryPort`   |
|                         | `OptionCommandPort` |
| `OrderMemberRepository` | `MemberCommandPort` |

#### createOrder() 재구성

```java
// Before
Option option = optionRepo.findByIdInnerJoinFetchProduct(optionId)
                .orElseThrow(NotFoundException::optionNotFound);
option.

subtractQuantity(quantity);

Product product = option.getProduct();
int price = product.getPrice() * quantity;
memberRepo.

findById(memberId)
        .

orElseThrow(NotFoundException::memberNotFound)
        .

deductPoint(price);

Order build = Order.builder().option(option)...

// After
        optionCommandPort.

subtractQuantity(optionId, quantity);

ProductDto product = optionQueryPort.getAssociatedProduct(optionId);
int price = product.price() * quantity;
memberCommandPort.

deductPoint(memberId, price);

Option optionRef = optionQueryPort.getReference(optionId);
Order build = Order.builder().option(optionRef)...
```

- `option.subtractQuantity()` 직접 호출 제거 → `optionCommandPort.subtractQuantity()` (Port 컨벤션 준수)
- `option.getProduct()` 직접 호출 제거 → `optionQueryPort.getAssociatedProduct()` → `ProductDto`
- `member.deductPoint()` 직접 호출 제거 → `memberCommandPort.deductPoint()`
- Order 빌더 FK 참조: `optionQueryPort.getReference(optionId)` — `@Transactional` → MANDATORY 충족

### 1-2. KakaoMessagingService 교체 (구조 변경)

`OrderMemberRepository` 삭제로 인한 필수 수정.

| Before                                                              | After                                           |
|---------------------------------------------------------------------|-------------------------------------------------|
| `memberRepo.findById(memberId).orElseThrow().getKakaoAccessToken()` | `memberQueryPort.getKakaoAccessToken(memberId)` |

### 1-3. 삭제 파일

| 파일                           | 이유                                                |
|------------------------------|---------------------------------------------------|
| `OrderOptionRepository.java` | OrderService가 Port로 교체 완료                         |
| `OrderMemberRepository.java` | OrderService + KakaoMessagingService가 Port로 교체 완료 |
| `OrderWishRepository.java`   | 미사용 (사용처 없음)                                      |

---

## 2. 단위 테스트

### OrderServiceTest (6개)

파일: `src/test/java/gift/order/internal/OrderServiceTest.java`

| 테스트                                 | 검증 내용                                                                                           |
|-------------------------------------|-------------------------------------------------------------------------------------------------|
| `testGetOrders`                     | `orderRepo.findByMemberId` 호출 + PagedModel 응답 매핑 + 메타데이터                                        |
| `testCreateOrder`                   | 호출 여부 + 인자 검증 (subtractQuantity, getAssociatedProduct, deductPoint, getReference, save) + 응답 매핑 |
| `testCreateOrderOptionNotFound`     | `optionCommandPort.subtractQuantity` → NotFoundException 전파                                     |
| `testCreateOrderInsufficientStock`  | `optionCommandPort.subtractQuantity` → IllegalArgumentException 전파                              |
| `testCreateOrderMemberNotFound`     | `memberCommandPort.deductPoint` → NotFoundException 전파                                          |
| `testCreateOrderInsufficientPoints` | `memberCommandPort.deductPoint` → IllegalArgumentException 전파                                   |

### KakaoMessagingServiceTest (2개)

파일: `src/test/java/gift/order/internal/KakaoMessagingServiceTest.java`

| 테스트                                | 검증 내용                                                       |
|------------------------------------|-------------------------------------------------------------|
| `testSendDefaultTemplateMessageTo` | 토큰 조회 → 메시지 전송 순서 (InOrder) + 인자 전달                         |
| `testSendMessageMemberNotFound`    | 토큰 조회 → NotFoundException 전파 + kakaoMessageClient 호출 안 됨 확인 |

---

## 3. 논의 사항

### 3-1. InOrder 호출 순서 검증 제거 (OrderServiceTest)

초기 구현에서 `testCreateOrder`는 `Mockito.inOrder()`로 5단계 호출 순서를 검증했다. 논의 결과, `@Transactional` 안에서는 어느 것이 먼저 실패하든 전체가 롤백되므로 호출 순서가 비즈니스 규칙이 아니라고 판단했다. 순서를 강제하면 구현 세부사항에 테스트가 결합되어, 리팩터링 시 불필요하게 테스트가 깨진다.

**결정: `InOrder` 제거 → `then().should()`로 호출 여부 + 인자만 검증.**

### 3-2. KakaoMessagingService 교체 (계획 외 변경)

계획에서는 `OrderService`만 교체 대상이었으나, `OrderMemberRepository` 삭제 시 `KakaoMessagingService`도 이를 참조하고 있어 함께 교체했다. `MemberQueryPort.getKakaoAccessToken()`이 이미 존재하므로 신규 Port 메서드 추가 없이 해결되었다.

---

## 4. 검증 결과

```bash
./gradlew test --tests "gift.order.internal.OrderServiceTest"           # 6개 통과
./gradlew test --tests "gift.order.internal.KakaoMessagingServiceTest"  # 2개 통과
./gradlew test                # BUILD SUCCESSFUL (전체 단위 테스트 통과)
./gradlew acceptanceTest      # BUILD SUCCESSFUL (기존 75개 시나리오 통과)
```

---

## 5. 수정/삭제 파일 목록

| 파일                               | 변경                                                          |
|----------------------------------|-------------------------------------------------------------|
| `OrderService.java`              | `OrderOptionRepository` + `OrderMemberRepository` → Port 3개 |
| `KakaoMessagingService.java`     | `OrderMemberRepository` → `MemberQueryPort`                 |
| `OrderOptionRepository.java`     | **삭제**                                                      |
| `OrderMemberRepository.java`     | **삭제**                                                      |
| `OrderWishRepository.java`       | **삭제** (미사용)                                                |
| `OrderServiceTest.java`          | **신규** — 6개 테스트                                             |
| `KakaoMessagingServiceTest.java` | **신규** — 2개 테스트                                             |

---

## 6. 전체 Port 교체 완료 현황

| Service               | 교체 대상 Repository                 | 교체할 Port                                | 상태              |
|-----------------------|----------------------------------|-----------------------------------------|-----------------|
| ProductService        | `ProductCategoryRepository`      | `CategoryQueryPort`                     | 완료 (progress-5) |
| AdminProductService   | `AdminProductCategoryRepository` | `CategoryQueryPort`                     | 완료 (progress-6) |
| WishService           | `WishProductRepository`          | `ProductQueryPort`                      | 완료 (progress-7) |
| OptionService         | `OptionProductRepository`        | `ProductQueryPort`                      | 완료 (progress-8) |
| Auth (3개 클래스)         | `AuthMemberRepository`           | `MemberQueryPort` + `MemberCommandPort` | 완료 (progress-9) |
| OrderService          | `OrderOptionRepository`          | `OptionQueryPort` + `OptionCommandPort` | **완료**          |
| OrderService          | `OrderMemberRepository`          | `MemberCommandPort`                     | **완료**          |
| KakaoMessagingService | `OrderMemberRepository`          | `MemberQueryPort`                       | **완료**          |

**모든 크로스 도메인 Repository 교체가 완료되었다.**
