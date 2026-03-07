# 서비스 계층 추출 코드 리뷰

**리뷰 대상:** 커밋 `9fd28e3` ~ `3265c0a` (최근 5개 커밋, 서비스 계층 추출 전체)
**리뷰 일자:** 2026-02-26

---

## 총평

서비스 계층 추출 작업이 REQUIREMENT.md의 핵심 원칙인 **"구조 변경, 작동 변경 없음"** 을 충실히 따르며 완수되었다. 7개 도메인(Member, Auth, Category, Product, Option, Wish, Order) + KakaoAuth + KakaoNotification까지 총 9개 서비스로 분리되었고, 모든 Controller가 "요청 검증 + 위임"만 담당하는 얇은 구조로 변경되었다. `@LoginMember` ArgumentResolver 도입으로 인증 보일러플레이트가 깔끔하게 제거된 점이 특히 좋다. 커밋 단위도 논리적이며, AngularJS 커밋 컨벤션을 일관되게 따르고 있다.

> **참고:** 이후 패키지 구조가 도메인별에서 역할별(레이어별)로 변경되었다. 아래 파일 경로는 현재 구조 기준으로 업데이트되었다.

---

## 발견된 문제점

### 🟡 1. OrderService.createOrder()에 트랜잭션 경계 부재

**파일:** `gift/service/OrderService.java`

`createOrder()` 메서드는 아래 3가지 상태 변경을 순차적으로 수행한다:
1. `optionService.subtractQuantity()` — 재고 차감
2. `memberService.deductPoint(member, price)` — 포인트 차감
3. `orderRepository.save(...)` — 주문 저장

`@Transactional`이 없으므로, 예를 들어 2번에서 실패하면 1번의 재고 차감은 이미 DB에 반영된 상태로 남는다. 데이터 정합성이 깨질 수 있다.

**단, 원래 Controller에도 `@Transactional`이 없었으므로 이번 리팩토링의 원칙("작동 변경 없음")에는 위배되지 않는다.** 다음 단계(작동 변경 단계)에서 반드시 보완해야 할 항목이다.

```java
// 개선 방향 (다음 단계에서 적용)
@Transactional
public Order createOrder(Member member, Long optionId, int quantity, String message) {
    // ...
}
```

---

### ~~🟡 2. OrderService가 MemberRepository를 직접 의존~~ ✅ 해결됨

`MemberService`에 `deductPoint(Member member, int amount)` 메서드를 추가하고, `OrderService`가 `MemberRepository` 대신 `MemberService`를 통해 포인트 차감을 수행하도록 수정 완료.

```java
// 수정 전: Repository 직접 의존
private final MemberRepository memberRepository;
member.deductPoint(price);
memberRepository.save(member);

// 수정 후: MemberService를 통한 일관된 접근
private final MemberService memberService;
memberService.deductPoint(member, price);
```

---

### 🟡 3. KakaoNotificationService의 무조건적 예외 무시

**파일:** `gift/service/KakaoNotificationService.java`

```java
try {
    var product = option.getProduct();
    kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
} catch (Exception ignored) {
}
```

모든 예외를 무시하므로 카카오 API 장애 시 디버깅이 불가능하다. 원래 Controller 코드에서 그대로 이동한 것이므로 이번 리팩토링 원칙에는 위배되지 않지만, best-effort 알림이라도 최소한의 로깅은 필요하다.

```java
// 개선 방향 (다음 단계)
} catch (Exception e) {
    log.warn("Failed to send Kakao notification. orderId={}", order.getId(), e);
}
```

---

### 🟡 4. GlobalExceptionHandler 응답 본문 불일치

**파일:** `gift/exception/GlobalExceptionHandler.java`

| 예외                      | HTTP Status | 응답 본문         |
|--------------------------|-------------|-----------------|
| `IllegalArgumentException` | 400         | `ErrorResponse` |
| `NoSuchElementException`   | 404         | `ErrorResponse` |
| `UnauthorizedException`    | 401         | **없음 (Void)**  |
| `ForbiddenException`       | 403         | **없음 (Void)**  |

401/403에서만 응답 본문이 없다. 클라이언트 입장에서 에러 응답의 구조가 상태 코드에 따라 달라지면 파싱 로직이 복잡해진다.

```java
// 개선 방향: 일관된 ErrorResponse 반환
@ExceptionHandler(UnauthorizedException.class)
public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ErrorResponse(e.getMessage()));
}
```

---

### 🟢 5. OptionService.findByProductId()의 불필요한 조회

**파일:** `gift/service/OptionService.java`

```java
public List<Option> findByProductId(Long productId) {
    productService.findById(productId);  // 결과를 사용하지 않음 — 존재 검증만
    return optionRepository.findByProductId(productId);
}
```

`productService.findById()`를 호출하지만 반환값을 사용하지 않는다. 상품 존재 여부 확인만이 목적이므로, `existsById()` 같은 경량 메서드가 더 적절하다. 다만 원래 Controller 코드의 동작을 그대로 옮긴 것이므로 현 단계에서는 수용 가능하다.

---

### 🟢 6. WishController.addWish()에 남아있는 분기 로직

**파일:** `gift/controller/WishController.java`

```java
var existing = wishService.findByMemberIdAndProductId(member.getId(), request.productId());
if (existing.isPresent()) {
    return ResponseEntity.ok(WishResponse.from(existing.get()));  // 200 OK
}
Wish wish = wishService.addWish(member.getId(), request.productId());
return ResponseEntity.created(URI.create("/api/wishes/" + wish.getId()))  // 201 Created
    .body(WishResponse.from(wish));
```

중복 여부에 따라 HTTP 상태 코드(200 vs 201)가 달라지므로 Controller에 남겨둔 것으로 보인다. HTTP 응답 의미론(semantics)을 Controller가 결정하는 것은 합리적이지만, "이미 존재하면 기존 항목 반환"이라는 비즈니스 규칙 자체는 Service 레벨에 있는 것이 더 자연스럽다.

```java
// 대안: Service에서 멱등성 처리
public Wish addWishIdempotent(Long memberId, Long productId) {
    return wishRepository.findByMemberIdAndProductId(memberId, productId)
        .orElseGet(() -> {
            Product product = productService.findById(productId);
            return wishRepository.save(new Wish(memberId, product));
        });
}
```

단, 이 경우 Controller에서 새로 생성된 것인지 기존 항목인지 구분하기 어려워지므로 현재 구조도 나쁘지 않다.

---

### 🟢 7. AuthenticationResolver의 null 반환 패턴

**파일:** `gift/auth/AuthenticationResolver.java`

```java
public Member extractMember(String authorization) {
    try {
        final String token = authorization.replace("Bearer ", "");
        final String email = jwtProvider.getEmail(token);
        return memberRepository.findByEmail(email).orElse(null);
    } catch (Exception e) {
        return null;
    }
}
```

`LoginMemberArgumentResolver`에서 null 체크 후 `UnauthorizedException`을 던지는 구조이다. 인증 실패 시 `AuthenticationResolver` 자체에서 예외를 던지는 것이 더 명확하지만, 기존 코드(`AdminProductController` 등)에서 null 반환에 의존하는 곳이 있을 수 있으므로 현 단계에서는 수용 가능하다.

---

## 잘한 점

### 1. "구조 변경, 작동 변경 없음" 원칙 준수
모든 서비스 추출이 기존 동작을 그대로 유지하면서 구조만 변경했다. `@Transactional` 추가나 예외 처리 개선 같은 작동 변경을 의도적으로 배제한 것이 REQUIREMENT.md의 원칙에 정확히 부합한다.

### 2. @LoginMember ArgumentResolver 도입
WishController, OrderController에서 반복되던 인증 보일러플레이트(6줄 × 메서드 수)를 `@LoginMember` 어노테이션 한 줄로 대체한 것은 이번 리팩토링에서 가장 효과적인 개선이다. `WebConfig`에 등록하는 것까지 빠짐없이 처리되었다.

### 3. 커밋 단위와 메시지
각 커밋이 정확히 하나의 도메인 서비스 추출만을 다루고 있다. `refactor(wish):`, `refactor(option):` 등 AngularJS 컨벤션을 일관되게 사용했다.

### 4. 의존성 방향의 일관성
`Controller → Service → Repository` 계층이 일관되며, Service 간 접근 시 `Service → Service` 패턴을 따른다:
- `OptionService → ProductService`
- `WishService → ProductService`
- `OrderService → OptionService, MemberService`
- `ProductService → CategoryService`

### 5. 커스텀 예외 + GlobalExceptionHandler 확장
`ForbiddenException`, `UnauthorizedException`을 추가하고 `GlobalExceptionHandler`에 등록하여, 기존에 Controller에서 직접 `ResponseEntity.status(401/403).build()`하던 패턴을 예외 기반으로 통일했다.

---

## 추가 조언

### 다음 단계(작동 변경)에서 우선적으로 처리할 항목

1. **`@Transactional` 적용** — 특히 `OrderService.createOrder()`, `OptionService.subtractQuantity()` 등 다중 상태 변경 메서드에 필수.
2. **로깅 추가** — `KakaoNotificationService`의 silent catch, `AuthenticationResolver`의 예외 무시 등에 최소한 `log.warn()` 수준의 로깅 추가.
3. **GlobalExceptionHandler 응답 통일** — 모든 예외에 대해 `ErrorResponse` 본문을 포함하도록 변경.

### 참고 패턴
- **Service 계층에서의 조회 메서드 네이밍:** `findById()`가 `orElseThrow`를 내포하고 있으므로, `getById()` (없으면 예외)와 `findById()` (Optional 반환)를 구분하는 컨벤션을 도입하면 의도가 더 명확해진다 (Spring Data JPA의 `getReferenceById()` vs `findById()` 참고).
