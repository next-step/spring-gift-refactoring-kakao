# Step 2 계획: 작동 변경

Step 1에서 구조만 정리했다. Step 2에서는 작동 변경을 수행하고 테스트로 증명한다.
모든 작동 변경은 **상태 변화를 재조회하여 검증**한다.

---

## 1단계: @Transactional 적용 (작동 변경)

다중 저장소를 사용하는 서비스 메서드에 트랜잭션 경계를 설정한다.

### OrderService.createOrder()

**현재 문제:** 재고 차감(`optionRepository.save`) 후 포인트 차감(`memberRepository.save`)에서 예외가 발생하면 재고만 차감된 채 남는다.

**변경:**
- `OrderService.createOrder()`에 `@Transactional` 추가

**변경 파일:**
- `src/main/java/gift/order/OrderService.java`
- `src/test/java/gift/order/OrderControllerTest.java`

**코드:**

`OrderService.java`:
```java
import org.springframework.transaction.annotation.Transactional;

@Transactional
public Order createOrder(Member member, Long optionId, int quantity, String message) {
    // 기존 코드 그대로
}
```

**테스트:** `createOrderInsufficientPoints` 테스트 끝에 재고 롤백 검증 추가:
```java
// @Transactional 덕분에 재고가 원래대로 유지되어야 한다
given()
.when()
    .get("/api/products/1/options")
.then()
    .body("[0].quantity", equalTo(100));
```

### KakaoAuthService.loginWithKakao()

**현재 문제:** 카카오 토큰 교환 → 회원 조회/생성 → 토큰 갱신 → 저장이 하나의 트랜잭션으로 묶이지 않는다.

**변경:**
- `KakaoAuthService.loginWithKakao()`에 `@Transactional` 추가

**변경 파일:**
- `src/main/java/gift/auth/KakaoAuthService.java`
- `src/test/java/gift/auth/KakaoAuthServiceTest.java` (신규)

**코드:**

`KakaoAuthService.java`:
```java
import org.springframework.transaction.annotation.Transactional;

@Transactional
public TokenResponse loginWithKakao(String code) {
    // 기존 코드 그대로
}
```

`KakaoAuthServiceTest.java` — `KakaoLoginClient`를 `@MockBean`으로 모킹:
- 테스트 1: 신규 회원 — `requestAccessToken` → `requestUserInfo` 스텁 → `loginWithKakao` 호출 → 회원 저장 확인 + JWT 반환 검증
- 테스트 2: 기존 회원 — 이미 존재하는 이메일로 로그인 → 카카오 토큰 갱신 확인 + JWT 반환 검증

---

## 2단계: 주문 시 위시리스트 자동 삭제 (작동 변경)

`OrderService.createOrder()`의 `// TODO: cleanup wish`를 구현한다.

**변경:**
- `WishRepository`에 `deleteByMemberIdAndProductId` 추가
- `OrderService`에 `WishRepository` 의존성 주입, 주문 저장 후 삭제 호출

**변경 파일:**
- `src/main/java/gift/wish/WishRepository.java`
- `src/main/java/gift/order/OrderService.java`
- `src/test/java/gift/order/OrderControllerTest.java`

**코드:**

`WishRepository.java`:
```java
void deleteByMemberIdAndProductId(Long memberId, Long productId);
```

`OrderService.java`:
```java
// 필드 추가
private final WishRepository wishRepository;

// 생성자에 WishRepository 파라미터 추가

// createOrder() 내 "// TODO: cleanup wish" 교체
wishRepository.deleteByMemberIdAndProductId(member.getId(), option.getProduct().getId());
```

**테스트:** 주문 전 위시 1개 존재 → 주문 후 `/api/wishes` 재조회 → 위시 0개 확인:
```java
@Test
@DisplayName("POST /api/orders - 주문 생성 후 해당 상품의 위시가 삭제된다")
void createOrderDeletesWish() {
    // setup-data.sql: wish (member_id=1, product_id=1) 존재
    given()
        .header("Authorization", token)
        .contentType(ContentType.JSON)
        .body(Map.of("optionId", 1, "quantity", 1, "message", "선물"))
    .when()
        .post("/api/orders")
    .then()
        .statusCode(201);

    // 위시 목록 재조회 → 0개
    given()
        .header("Authorization", token)
    .when()
        .get("/api/wishes")
    .then()
        .statusCode(200)
        .body("content.size()", equalTo(0));
}
```

---

## 3단계: 예외 삼킴(swallow) 로그 추가 (작동 변경)

예외를 삼키고 있는 catch 블록에 로그를 추가한다.

### OrderService.sendKakaoMessageIfPossible

카카오 메시지 전송 실패 시 예외를 삼키지 않고 warn 로그를 남긴다.

**변경 파일:**
- `src/main/java/gift/order/OrderService.java`

**코드:**

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private void sendKakaoMessageIfPossible(Member member, Order order, Option option) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            Product product = option.getProduct();
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception e) {
            log.warn("카카오 메시지 전송 실패: memberId={}, orderId={}", member.getId(), order.getId(), e);
        }
    }
}
```

**테스트:** 기존 `createOrder` 테스트가 카카오 토큰 없이 실행되므로 전송 실패 경로를 이미 커버. 카카오 전송이 실패해도 주문이 정상 생성되는지는 기존 테스트로 검증됨.

### AuthenticationResolver.extractMember

JWT 검증 실패 시 `catch (Exception e) { return null; }`로 예외를 삼키고 있다. 잘못된 토큰이 왜 실패했는지 추적할 수 없으므로 debug 로그를 추가한다. (인증 실패는 정상 흐름이므로 warn이 아닌 debug 레벨)

**변경 파일:**
- `src/main/java/gift/auth/AuthenticationResolver.java`

**코드:**

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AuthenticationResolver {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationResolver.class);

    public Member extractMember(String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");
            String email = jwtProvider.getEmail(token);
            return memberRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            log.debug("인증 토큰 추출 실패: {}", e.getMessage());
            return null;
        }
    }
}
```

---

## 4단계: 가격 계산 도메인 메서드 추가 (작동 변경 + 구조 변경)

### 4-1: Option.calculateTotalPrice 추가 (작동 변경 — 도메인 책임)

현재 `OrderService`에 있는 `option.getProduct().getPrice() * quantity` 계산을 도메인으로 이동한다.

**변경 파일:**
- `src/main/java/gift/option/Option.java`
- `src/test/java/gift/option/OptionTest.java` (신규)

**코드:**

`Option.java`:
```java
public int calculateTotalPrice(int quantity) {
    return product.getPrice() * quantity;
}
```

`OptionTest.java`:
```java
package gift.option;

import gift.category.Category;
import gift.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OptionTest {

    @Test
    @DisplayName("calculateTotalPrice는 상품 가격 × 수량을 반환한다")
    void calculateTotalPrice() {
        Category category = new Category("카테고리", "#000000", "http://img.test/c.png", "설명");
        Product product = new Product("상품", 1000, "http://img.test/p.png", category);
        Option option = new Option(product, "옵션", 100);

        assertThat(option.calculateTotalPrice(3)).isEqualTo(3000);
    }
}
```

### 4-2: OrderService에서 calculateTotalPrice로 위임 (구조 변경)

**변경 파일:**
- `src/main/java/gift/order/OrderService.java`

**코드:**
```java
// Before
int price = option.getProduct().getPrice() * quantity;

// After
int price = option.calculateTotalPrice(quantity);
```

---

## 5단계: 이메일 중복 검증 로직 통합 (구조 변경)

`MemberService`의 `register()`와 `create()`에 중복된 이메일 체크 로직을 private 메서드로 추출한다.

**변경 파일:**
- `src/main/java/gift/member/MemberService.java`

**코드:**

```java
private void validateEmailNotDuplicated(String email) {
    if (memberRepository.existsByEmail(email)) {
        throw new IllegalArgumentException("이미 등록된 이메일입니다.");
    }
}

public TokenResponse register(String email, String password) {
    validateEmailNotDuplicated(email);
    // ...
}

public void create(String email, String password) {
    validateEmailNotDuplicated(email);
    // ...
}
```

---

## 6단계: 서비스 단위 테스트 추가

Step 1에서 작성한 통합 테스트(RestAssured 기반)와 별개로, 서비스 계층의 비즈니스 로직을 단위 테스트로 보강한다. 트랜잭션 롤백 등 서비스 레벨 동작을 검증한다.

**대상:**
- 1단계에서 추가한 `@Transactional`의 롤백 동작
- 서비스 메서드의 비즈니스 검증 로직 (예외 케이스 등)

**변경 파일:**
- `src/test/java/gift/order/OrderServiceTest.java` (신규)
- `src/test/java/gift/auth/KakaoAuthServiceTest.java` (1단계에서 생성, 필요 시 보강)
- 기타 서비스 테스트 필요 시 추가

---

## 7단계: @RestControllerAdvice 도입 (작동 변경)

REST API(`/api/...`)의 예외 처리를 일원화한다. View 컨트롤러(`/admin/...`)만 개별 try-catch를 유지한다.

**변경:**
- `@RestControllerAdvice` 클래스 생성 — `/api/...` 대상
- `NoSuchElementException` → 404, `IllegalArgumentException` → 400 등 매핑
- 각 REST 컨트롤러의 개별 try-catch 제거

**변경 파일:**
- `src/main/java/gift/common/GlobalExceptionHandler.java` (신규)
- REST API 컨트롤러들 — 개별 예외 처리 코드 제거

---

## 검증

매 단계 완료 후 `./gradlew test` 전체 통과 확인.
