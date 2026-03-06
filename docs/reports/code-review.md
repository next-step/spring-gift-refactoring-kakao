# 코드 리뷰 보고서

**프로젝트:** spring-gift-refactoring-kakao
**리뷰 날짜:** 2026-03-04
**리뷰 범위:** 전체 소스 코드 (모델, 서비스, 컨트롤러, DTO, 인증, 외부 클라이언트)

---

## 총평

전반적으로 레이어 분리(Controller → Service → Repository)가 잘 되어 있고, 디미터 법칙 위반 해소와 트랜잭션 경계 설정 등 의미 있는 리팩토링이 진행된 상태입니다. 다만 **비밀번호 평문 저장**, **트랜잭션 내 외부 HTTP 호출**, **동시성 제어 부재** 등 운영 환경에서 반드시 해결해야 할 문제점이 발견되었습니다.

---

## 발견된 문제점

### 🔴 치명적 (Critical)

#### 1. 비밀번호 평문 저장 — 보안 취약점

**파일:** `AuthService.java:24`, `Member.java:67-69`

`AuthService.register()`에서 비밀번호를 해싱 없이 그대로 저장하고, `Member.passwordMatches()`에서 평문 비교를 수행합니다. DB가 탈취되면 모든 사용자의 비밀번호가 그대로 노출됩니다.

```java
// 현재 코드 — 평문 저장
final Member member = memberRepository.save(new Member(email, password));

// 현재 코드 — 평문 비교
public boolean passwordMatches(String rawPassword) {
    return password != null && password.equals(rawPassword);
}
```

**개선 방향:**
```java
// BCryptPasswordEncoder 사용
@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    // ...
    public TokenResponse register(String email, String password) {
        String encodedPassword = passwordEncoder.encode(password);
        final Member member = memberRepository.save(new Member(email, encodedPassword));
        // ...
    }
}

// Member 엔티티
public boolean passwordMatches(String rawPassword, PasswordEncoder encoder) {
    return password != null && encoder.matches(rawPassword, password);
}
```

---

#### 2. 트랜잭션 내부에서 외부 HTTP 호출 — 성능/안정성 위험

**파일:** `OrderService.java:35-51`

`createOrder()`는 `@Transactional`로 감싸져 있으면서, 그 안에서 `kakaoNotificationService.sendOrderNotification()`이 Kakao API로 HTTP 요청을 보냅니다. DB 커넥션이 HTTP 응답을 기다리는 동안 계속 점유되어, 카카오 API 지연 시 **커넥션 풀 고갈**로 이어질 수 있습니다.

```java
@Transactional
public Order createOrder(Member member, Long optionId, int quantity, String message) {
    Option option = optionService.subtractQuantity(optionId, quantity);
    var price = option.calculatePrice(quantity);
    memberService.deductPoint(member, price);
    var saved = orderRepository.save(new Order(option, member.getId(), quantity, message));

    // ⚠️ 트랜잭션 내부에서 외부 HTTP 호출
    kakaoNotificationService.sendOrderNotification(member, saved, option);
    return saved;
}
```

**개선 방향:** 알림 전송을 트랜잭션 밖으로 분리합니다.
```java
@Transactional
public Order createOrder(Member member, Long optionId, int quantity, String message) {
    Option option = optionService.subtractQuantity(optionId, quantity);
    var price = option.calculatePrice(quantity);
    memberService.deductPoint(member, price);
    return orderRepository.save(new Order(option, member.getId(), quantity, message));
}

// Controller 또는 Facade에서 호출
public Order placeOrder(Member member, Long optionId, int quantity, String message) {
    Order saved = createOrder(member, optionId, quantity, message);
    kakaoNotificationService.sendOrderNotification(member, saved, saved.getOption());
    return saved;
}
```

또는 `@TransactionalEventListener(phase = AFTER_COMMIT)`을 사용하여 커밋 후에 알림을 전송하는 방법도 있습니다.

---

#### 3. 재고 차감 동시성 제어 부재 — 데이터 정합성 위험

**파일:** `OptionService.java:75-79`, `Option.java:43-48`

`subtractQuantity()`는 읽기 → 검증 → 쓰기 패턴으로 동작하지만, 낙관적/비관적 락이 없습니다. 두 사용자가 동시에 주문하면 둘 다 재고 검증을 통과하여 **음수 재고**가 발생할 수 있습니다.

```java
// 현재: 락 없는 읽기-검증-쓰기
public Option subtractQuantity(Long optionId, int quantity) {
    Option option = findById(optionId);        // 1. 읽기
    option.subtractQuantity(quantity);          // 2. 검증 + 수정
    return optionRepository.save(option);       // 3. 쓰기 (race condition!)
}
```

**개선 방향:** 비관적 락 적용
```java
// Repository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT o FROM Option o WHERE o.id = :id")
Optional<Option> findByIdForUpdate(@Param("id") Long id);
```

또는 낙관적 락(`@Version` 필드 추가) 적용 후 `OptimisticLockException` 발생 시 재시도 로직을 구현합니다.

---

### 🟡 경고 (Warning)

#### 4. 카카오 알림 예외 무시 — 운영 모니터링 불가

**파일:** `KakaoNotificationService.java:21-24`

`catch (Exception ignored)` 블록이 모든 예외를 삼켜버립니다. 알림 실패 원인(토큰 만료, API 장애 등)을 전혀 파악할 수 없습니다.

```java
try {
    kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, option);
} catch (Exception ignored) {  // ⚠️ 로깅도 없이 예외 소멸
}
```

**개선 코드:**
```java
private static final Logger log = LoggerFactory.getLogger(KakaoNotificationService.class);

try {
    kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, option);
} catch (Exception e) {
    log.warn("카카오 알림 전송 실패. memberId={}, orderId={}", member.getId(), order.getId(), e);
}
```

---

#### 5. JSON 문자열 수동 조립 — 인젝션 위험

**파일:** `KakaoMessageClient.java:32-51`

`buildTemplate()`에서 `order.getMessage()` 값을 이스케이프 없이 JSON 문자열에 직접 삽입합니다. 사용자가 메시지에 `"` 또는 `\`를 포함하면 JSON이 깨지고, 의도치 않은 데이터가 전송될 수 있습니다.

```java
// 현재: 사용자 입력이 JSON 안에 직접 삽입됨
var message = order.hasMessage()
    ? "\\n\\n💌 " + order.getMessage()  // ⚠️ 이스케이프 없음
    : "";
```

**개선 방향:** `ObjectMapper`나 `Map` 구조를 사용하여 JSON을 안전하게 직렬화합니다.
```java
private String buildTemplate(Order order, Option option) {
    var text = "🎁 선물이 도착했어요!\n\n%s (%s)\n수량: %d개\n금액: %s원%s".formatted(
        option.productName(), order.optionName(), order.getQuantity(),
        String.format("%,d", option.calculatePrice(order.getQuantity())),
        order.hasMessage() ? "\n\n💌 " + order.getMessage() : ""
    );
    var template = Map.of(
        "object_type", "text",
        "text", text,
        "link", Map.of(),
        "button_title", "선물 확인하기"
    );
    return objectMapper.writeValueAsString(template);
}
```

---

#### 6. `MemberService.findById()` 예외 타입 불일치

**파일:** `MemberService.java:22-23` vs `ProductService.java:33-34`

`MemberService.findById()`는 `IllegalArgumentException`(→ 400 Bad Request)을 던지고, `ProductService.findById()`는 `NoSuchElementException`(→ 404 Not Found)을 던집니다. "리소스를 찾을 수 없다"는 동일한 의미인데 HTTP 상태 코드가 달라집니다.

```java
// MemberService — 400 Bad Request 응답
throw new IllegalArgumentException("Member not found. id=" + id);

// ProductService — 404 Not Found 응답
throw new NoSuchElementException("Product not found. id=" + id);
```

**개선 방향:** "Not Found" 상황에는 일관되게 `NoSuchElementException`을 사용합니다.

---

#### 7. `MemberService` API 비일관성 — `deductPoint` vs `chargePoint`

**파일:** `MemberService.java:39-48`

`chargePoint(Long id, int amount)`는 ID로 멤버를 조회하는데, `deductPoint(Member member, int amount)`는 이미 조회된 엔티티를 받습니다. 같은 서비스 내에서 동일한 패턴의 메서드가 서로 다른 시그니처를 가지면 혼동을 유발합니다.

```java
public void chargePoint(Long id, int amount) {     // ID 기반
    final Member member = findById(id);
    member.chargePoint(amount);
    memberRepository.save(member);
}

public void deductPoint(Member member, int amount) { // 엔티티 기반
    member.deductPoint(amount);
    memberRepository.save(member);
}
```

**개선 방향:** 둘 다 동일한 시그니처 패턴으로 통일합니다 (ID 기반 또는 엔티티 기반 중 하나).

---

#### 8. `CategoryService.create()`이 DTO를 직접 받는 패턴 불일치

**파일:** `CategoryService.java:28-30`

`ProductService`, `OptionService`는 서비스 메서드가 원시값(String, int, Long)을 받는데, `CategoryService.create()`만 `CategoryRequest` DTO를 직접 받습니다. 서비스 레이어가 presentation 레이어의 DTO에 의존하게 됩니다.

```java
// CategoryService — DTO 의존
public Category create(CategoryRequest request) {
    return categoryRepository.save(request.toEntity());
}

// ProductService — 원시값 사용 (일관됨)
public Product create(String name, int price, String imageUrl, Long categoryId) { ... }
```

**개선 방향:** `CategoryService`도 원시값 파라미터 방식으로 통일합니다.

---

#### 9. Admin 컨트롤러 인증/인가 부재

**파일:** `AdminMemberController.java`, `AdminProductController.java`

관리자 기능(회원 목록 조회, 삭제, 포인트 충전, 상품 관리)에 어떤 인증이나 권한 검증도 없습니다. 누구나 `/admin/members`에 접근하여 회원 정보를 열람하고 삭제할 수 있습니다.

**개선 방향:** 최소한 `@LoginMember` 기반 인증 + 역할(role) 체크를 추가하거나, Spring Security의 `@PreAuthorize`를 도입합니다.

---

#### 10. `OrderController` 반환 타입에 와일드카드 사용

**파일:** `OrderController.java:29,38`

`getOrders()`와 `createOrder()`가 `ResponseEntity<?>`를 반환합니다. 다른 컨트롤러는 모두 구체적 타입(`ResponseEntity<Page<WishResponse>>` 등)을 명시하고 있어 일관성이 떨어지며, API 문서 자동 생성(Swagger/OpenAPI)에도 불리합니다.

```java
public ResponseEntity<?> getOrders(...) { ... }   // ⚠️ 와일드카드
public ResponseEntity<?> createOrder(...) { ... }  // ⚠️ 와일드카드
```

**개선 코드:**
```java
public ResponseEntity<Page<OrderResponse>> getOrders(...) { ... }
public ResponseEntity<OrderResponse> createOrder(...) { ... }
```

---

### 🟢 개선 권장 (Suggestion)

#### 11. `ProductRequest.toEntity()` 미사용 코드

**파일:** `ProductRequest.java:15-17`

`toEntity(Category)` 메서드가 존재하지만, `ProductService.create()`에서는 직접 `new Product()`를 호출합니다. 사용되지 않는 코드입니다.

---

#### 12. `Product.getOptions()` — 내부 컬렉션 직접 노출

**파일:** `Product.java:72-74`

`getOptions()`가 내부 `ArrayList`를 그대로 반환합니다. 외부에서 `product.getOptions().clear()` 등을 호출하면 엔티티의 상태가 의도치 않게 변경됩니다.

**개선 방향:**
```java
public List<Option> getOptions() {
    return Collections.unmodifiableList(options);
}
```

---

#### 13. `AuthenticationResolver.extractMember()` — 광범위한 예외 처리

**파일:** `AuthenticationResolver.java:23-31`

`catch (Exception e)` 블록이 JWT 파싱 실패, DB 조회 실패, NullPointerException 등 모든 예외를 동일하게 `null`로 처리합니다. 만료 토큰과 변조 토큰의 구분이 불가합니다.

**개선 방향:** JWT 관련 예외(`ExpiredJwtException`, `JwtException`)만 catch하고, 나머지는 전파되도록 합니다.

---

#### 14. `WishController.addWish()` — TOCTOU 경쟁 조건

**파일:** `WishController.java:46-53`

`findByMemberIdAndProductId` 조회 시점과 `addWish` 저장 시점 사이에 다른 요청이 같은 위시를 생성하면, 중복이 발생하거나 DB 제약 조건 위반이 발생합니다.

**개선 방향:** DB에 `(member_id, product_id)` unique 제약을 걸고, `DataIntegrityViolationException` 발생 시 기존 위시를 반환하는 방식으로 처리합니다.

---

#### 15. 읽기 전용 트랜잭션 미지정

**파일:** 모든 서비스의 조회 메서드

조회 메서드에 `@Transactional(readOnly = true)`를 적용하면 Hibernate flush가 생략되어 성능이 개선되고, DB 복제 구성 시 replica로 라우팅할 수 있습니다.

---

#### 16. 엔티티 생성자 유효성 검증 부재

**파일:** `Option.java:32-36`, `Product.java:34-39`, `Member.java:31-34`

엔티티 생성자에서 `price < 0`, `quantity < 0` 등의 도메인 불변식을 검증하지 않습니다. DTO의 `@Positive`나 `@Min` 어노테이션에만 의존하면, 서비스 레이어에서 직접 생성할 때 잘못된 값이 들어올 수 있습니다.

---

## 추가 조언

1. **보안**: 비밀번호 해싱은 반드시 1순위로 처리해야 합니다. `spring-boot-starter-security`의 `BCryptPasswordEncoder`가 표준 선택입니다.

2. **동시성**: 재고 관리가 있는 시스템에서 락 없는 read-modify-write는 장애의 근본 원인이 됩니다. 트래픽이 낮더라도 비관적 락이나 `@Version` 기반 낙관적 락을 초기에 도입해 두는 것이 좋습니다.

3. **트랜잭션 경계**: "트랜잭션 안에서 외부 호출을 하지 않는다"는 원칙을 팀 규칙으로 명문화하세요. Spring의 `@TransactionalEventListener`나 `ApplicationEventPublisher`를 활용하면 자연스럽게 분리할 수 있습니다.

4. **예외 전략**: 현재 `IllegalArgumentException`과 `NoSuchElementException`을 혼용하고 있습니다. 커스텀 예외 계층(예: `NotFoundException extends RuntimeException`)을 도입하면, 예외 타입만으로 의미를 명확히 전달할 수 있습니다.

5. **테스트**: 동시성 관련 테스트(`CountDownLatch` + `ExecutorService`를 활용한 동시 주문 테스트)를 추가하면 재고 차감 로직의 안전성을 검증할 수 있습니다.
