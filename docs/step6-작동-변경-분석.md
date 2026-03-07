# 작동 변경 분석 보고서

> 분석 일자: 2026-03-05
> 분석 범위: src/main/java/gift/ 전체 (10개 핵심 파일) + DB 마이그레이션 스키마

## 1. 분석 목적

`docs/step5-구조-변경-분석.md`에서 구조 변경(입출력 유지) 작업을 완료했다. 이제 **작동 변경**이 필요한 영역을 식별한다.

작동 변경이란 입출력이 바뀌는 변경을 말한다:
- HTTP 응답 상태 코드 변경
- 예외 처리 방식 변경 (새 핸들러 추가, 응답 본문 변경)
- 트랜잭션 경계 변경
- 도메인 검증 로직 추가/수정
- 비즈니스 로직 추가
- DB 스키마 제약 조건 추가

구조 변경과 달리, 작동 변경은 기존 인수 테스트가 실패하거나 새로운 테스트 시나리오가 필요할 수 있다. 각 항목에 테스트 영향을 명시한다.

## 2. 분석 범위

| 파일 | 역할 |
|------|------|
| `src/main/java/gift/order/OrderService.java` | 주문 생성 (트랜잭션, 외부 API 호출) |
| `src/main/java/gift/GlobalExceptionHandler.java` | 전역 예외 → HTTP 응답 매핑 |
| `src/main/java/gift/option/Option.java` | 옵션 엔티티 (재고 차감) |
| `src/main/java/gift/member/Member.java` | 회원 엔티티 (포인트 충전/차감) |
| `src/main/java/gift/wish/WishService.java` | 위시 추가/삭제 |
| `src/main/java/gift/category/CategoryService.java` | 카테고리 CRUD |
| `src/main/java/gift/product/ProductService.java` | 상품 CRUD |
| `src/main/java/gift/member/MemberService.java` | 회원 등록/로그인/CRUD |
| `src/main/java/gift/auth/AuthenticationResolver.java` | 인증 헤더 → Member 변환 |
| `src/main/resources/db/migration/V1__Initialize_project_tables.sql` | DB 스키마 |

---

## 3. 분석 결과

### 3-1. Authorization 헤더 누락 시 400 → 401 변경

**현재 상태**

Authorization 헤더가 없으면 `MissingRequestHeaderException`이 발생하여 Spring 기본 동작으로 400 Bad Request를 반환한다.

```java
// AuthenticationResolver.java:44-46
if (authorization == null) {
    // TODO: 의미적으로는 401이 맞으나, 기존 작동(400)을 유지한다.
    throw new MissingRequestHeaderException("Authorization", parameter);
}
```

**발견 사항**

인증 헤더 누락은 "잘못된 요청 형식"(400)이 아니라 "인증 자격 증명 누락"(401)이다. RFC 7235에 따르면 인증이 필요한 리소스에 자격 증명 없이 접근하면 401을 반환해야 한다. 현재 TODO 주석으로 의도가 기록되어 있으나, 구조 변경 단계에서는 기존 작동을 유지했다.

**근거**: `AuthenticationResolver.java:44-46`

---

### 3-2. 트랜잭션 내부 외부 API 호출

**현재 상태**

`OrderService.createOrder()`가 `@Transactional` 메서드 내에서 Kakao API 호출(`sendMessageIfPossible`)을 수행한다.

```java
// OrderService.java:35-52
@Transactional
public Order createOrder(Long memberId, Long optionId, int quantity, String message) {
    // ... 재고 차감, 포인트 차감, 주문 저장 ...
    Order saved = orderRepository.save(new Order(option, memberId, quantity, message));
    sendMessageIfPossible(member, saved, option);  // 라인 49: 외부 API 호출
    return saved;
}
```

**발견 사항**

외부 API 호출이 트랜잭션 내부에 있으므로:
1. Kakao API 응답 지연(타임아웃) 동안 DB 커넥션이 점유된다.
2. 트랜잭션 잠금이 필요 이상으로 오래 유지된다.
3. 외부 API 실패 시에도 `catch (Exception ignored)`로 무시하므로(라인 61), 트랜잭션 롤백은 발생하지 않는다. 하지만 커넥션 점유 문제는 남는다.

메시지 전송은 주문 완료의 부수 효과(side effect)이므로, 트랜잭션 커밋 이후로 분리하는 것이 적절하다.

**근거**: `OrderService.java:35-52`, `OrderService.java:54-63`

---

### 3-3. 동시성 미보호 — 재고 차감 / 포인트 차감

**현재 상태**

`Option`과 `Member` 엔티티에 `@Version` 필드가 없다. 비관적 잠금(`@Lock`)도 사용하지 않는다.

```java
// Option.java:38-43
public void subtractQuantity(int amount) {
    if (amount > this.quantity) {
        throw new IllegalArgumentException("차감할 수량이 현재 재고보다 많습니다.");
    }
    this.quantity -= amount;
}
```

```java
// Member.java:56-64
public void deductPoint(int amount) {
    if (amount <= 0) { ... }
    if (amount > this.point) { ... }
    this.point -= amount;
}
```

**발견 사항**

두 사용자가 동시에 같은 옵션의 마지막 1개 재고를 주문하면:
1. 두 트랜잭션 모두 `quantity = 1`을 읽는다.
2. 두 트랜잭션 모두 `1 > 1` 검증을 통과한다.
3. 두 트랜잭션 모두 `quantity = 0`으로 업데이트한다.
4. 결과: 재고 1개에 대해 주문 2건이 생성된다.

포인트 차감도 동일한 race condition이 존재한다.

**근거**: `Option.java:15-60` (`@Version` 없음), `Member.java:15-89` (`@Version` 없음), `OrderService.java:36-52` (잠금 없이 조회 → 검증 → 수정)

---

### 3-4. GlobalExceptionHandler — MethodArgumentNotValidException 미처리

**현재 상태**

`GlobalExceptionHandler`에 `MethodArgumentNotValidException` 핸들러가 없다.

```java
// GlobalExceptionHandler.java:11-32
// 처리하는 예외: AuthenticationException, NoSuchElementException,
//               IllegalArgumentException, IllegalStateException
// 미처리: MethodArgumentNotValidException
```

**발견 사항**

9개 엔드포인트에서 `@Valid`를 사용한다:

| 컨트롤러 | 메서드 | Request DTO | 검증 어노테이션 |
|----------|--------|-------------|----------------|
| `ProductController:39` | createProduct | ProductRequest | `@NotBlank`, `@Positive`, `@NotNull` |
| `ProductController:48` | updateProduct | ProductRequest | 동일 |
| `CategoryController:33` | createCategory | CategoryRequest | `@NotBlank` |
| `CategoryController:41` | updateCategory | CategoryRequest | 동일 |
| `OptionController:38` | createOption | OptionRequest | `@NotBlank`, `@Min`, `@Max` |
| `OrderController:33` | createOrder | OrderRequest | `@NotNull`, `@Min` |
| `WishController:35` | addWish | WishRequest | `@NotNull` |
| `MemberController:27` | register | MemberRequest | `@NotBlank`, `@Email` |
| `MemberController:33` | login | MemberRequest | 동일 |

Bean Validation 실패 시 Spring Boot 기본 응답(RFC 7807 Problem Details 또는 DefaultHandlerExceptionResolver)이 반환된다. 이는 `IllegalArgumentException` 핸들러가 반환하는 `400 + 문자열 메시지`와 형식이 다르다. 클라이언트가 동일한 "잘못된 입력" 상황에서 두 가지 다른 응답 형식을 받는다.

**근거**: `GlobalExceptionHandler.java:1-32`, 위 9개 엔드포인트

---

### 3-5. GlobalExceptionHandler — 응답 본문 불일관

**현재 상태**

```java
// GlobalExceptionHandler.java:13-31
handleUnauthorized    → ResponseEntity<Void>   (본문 없음)
handleNotFound        → ResponseEntity<Void>   (본문 없음)
handleIllegalArgument → ResponseEntity<String> (e.getMessage())
handleForbidden       → ResponseEntity<Void>   (본문 없음)
```

**발견 사항**

`IllegalArgumentException`만 에러 메시지를 본문에 포함하고, 나머지 예외는 본문이 비어 있다. 특히:
- `NoSuchElementException`은 `"상품이 존재하지 않습니다. id=5"` 같은 유용한 메시지를 담고 있으나 클라이언트에 전달되지 않는다.
- `AuthenticationException`은 `"인증에 실패했습니다."` 메시지를 담고 있으나 클라이언트에 전달되지 않는다.

에러 응답 형식의 일관성은 API 계약의 일부이므로, 통일된 에러 응답 구조가 필요하다.

**근거**: `GlobalExceptionHandler.java:13-31`

---

### 3-6. 에러 메시지 언어 혼재

**현재 상태**

```java
// Member.java:49-51
public void chargePoint(int amount) {
    if (amount <= 0) {
        throw new IllegalArgumentException("Amount must be greater than zero.");
    }
```

```java
// Member.java:56-63
public void deductPoint(int amount) {
    if (amount <= 0) {
        throw new IllegalArgumentException("차감 금액은 1 이상이어야 합니다.");
    }
    if (amount > this.point) {
        throw new IllegalArgumentException("포인트가 부족합니다.");
    }
```

**발견 사항**

동일 클래스(`Member`) 내에서 `chargePoint`는 영어, `deductPoint`는 한국어로 에러 메시지를 작성한다. `IllegalArgumentException` 메시지가 `GlobalExceptionHandler`를 통해 클라이언트 응답 본문에 노출되므로, 이는 API 응답의 언어 불일관이다.

프로젝트 전체를 보면 `MemberService.register/login`은 영어(`"Email is already registered."`, `"Invalid email or password."`), 나머지 서비스/엔티티는 주로 한국어를 사용한다.

**근거**: `Member.java:50` (영어), `Member.java:58` (한국어), `MemberService.java:21` (영어), `MemberService.java:31` (영어)

---

### 3-7. Option.subtractQuantity — 음수/영 검증 누락

**현재 상태**

```java
// Option.java:38-43
public void subtractQuantity(int amount) {
    if (amount > this.quantity) {
        throw new IllegalArgumentException("차감할 수량이 현재 재고보다 많습니다.");
    }
    this.quantity -= amount;
}
```

**발견 사항**

`amount <= 0` 검증이 없다. `subtractQuantity(-5)`를 호출하면:
- `-5 > this.quantity`는 `false`이므로 검증을 통과한다.
- `this.quantity -= (-5)` → 재고가 5 증가한다.

현재 호출 경로(`OrderService.createOrder`)에서 `quantity`는 `OrderRequest`의 `@Min(1)` 검증을 거치므로 정상 흐름에서는 음수가 전달되지 않는다. 그러나 도메인 객체는 자신의 불변식을 스스로 보호해야 한다. `Member.deductPoint()`는 `amount <= 0` 검증이 있으므로(`Member.java:57-59`), `Option.subtractQuantity()`에도 동일한 방어가 필요하다.

**근거**: `Option.java:38-43`, `Member.java:57-59` (대조), `OrderRequest.java:6` (`@Min(1)`)

---

### 3-8. 삭제 시 FK 위반 미처리 — 500 에러

**현재 상태**

DB 스키마에 FK 제약이 있으나 `ON DELETE CASCADE`가 없다:

```sql
-- V1__Initialize_project_tables.sql
-- product.category_id → category(id)     (라인 17)
-- wish.product_id → product(id)          (라인 35)
-- wish.member_id → member(id)            (라인 34)
-- options.product_id → product(id)       (라인 44)
-- orders.option_id → options(id)         (라인 55)
-- orders.member_id → member(id)          (라인 56)
```

JPA 엔티티에도 `cascade = CascadeType.REMOVE`가 없다. 삭제 서비스 메서드는 단순히 `deleteById(id)`를 호출한다:

```java
// CategoryService.java:41-43
public void delete(Long id) { categoryRepository.deleteById(id); }

// ProductService.java:54-56
public void delete(Long id) { productRepository.deleteById(id); }

// MemberService.java:86-88
public void delete(Long id) { memberRepository.deleteById(id); }
```

**발견 사항**

하위 엔티티가 존재하는 상위 엔티티를 삭제하면 `DataIntegrityViolationException`이 발생한다. 이 예외는 `GlobalExceptionHandler`에서 처리하지 않으므로 500 Internal Server Error가 반환된다.

| 삭제 대상 | FK 위반 원인 | 서비스 위치 |
|-----------|-------------|------------|
| Category | Product.category_id | `CategoryService.java:42` |
| Product | Option.product_id, Wish.product_id | `ProductService.java:55` |
| Option | Order.option_id | `OptionService.java:64` |
| Member | Order.member_id, Wish.member_id | `MemberService.java:87` |

삭제 정책(cascade vs restrict vs 사전 검증)을 결정하고, 어떤 정책이든 클라이언트에 500이 아닌 의미 있는 응답을 반환해야 한다.

**근거**: `V1__Initialize_project_tables.sql:17,34-35,44,55-56`, `GlobalExceptionHandler.java:1-32` (`DataIntegrityViolationException` 핸들러 없음)

---

### 3-9. wish 테이블 — UNIQUE 제약 누락

**현재 상태**

```sql
-- V1__Initialize_project_tables.sql:29-36
create table wish (
    id         bigint auto_increment primary key,
    member_id  bigint not null,
    product_id bigint not null,
    foreign key (member_id) references member (id),
    foreign key (product_id) references product (id)
    -- UNIQUE(member_id, product_id) 없음
);
```

애플리케이션 레벨에서 중복을 방지한다:

```java
// WishService.java:30-34
var existing = wishRepository
        .findByMemberIdAndProductId(memberId, product.getId())
        .orElse(null);
if (existing != null) {
    return new AddWishResult(existing, false);
}
```

**발견 사항**

애플리케이션 레벨 중복 검사는 동시 요청에 취약하다. 두 요청이 동시에 `findByMemberIdAndProductId`를 호출하면 둘 다 `null`을 받고, 둘 다 `save()`를 실행하여 중복 위시가 생성된다. DB 레벨에 `UNIQUE(member_id, product_id)` 제약을 추가해야 한다.

**근거**: `V1__Initialize_project_tables.sql:29-36`, `WishService.java:27-38`

---

### 3-10. 주문 시 위시 삭제 누락

**현재 상태**

```java
// OrderService.java:36-52
public Order createOrder(Long memberId, Long optionId, int quantity, String message) {
    Option option = optionService.findById(optionId);
    option.subtractQuantity(quantity);
    optionService.save(option);

    Member member = memberService.findById(memberId);
    int price = option.getProduct().getPrice() * quantity;
    member.deductPoint(price);
    memberService.save(member);

    Order saved = orderRepository.save(new Order(option, memberId, quantity, message));
    sendMessageIfPossible(member, saved, option);
    return saved;
}
```

**발견 사항**

주문 생성 시 해당 상품에 대한 위시를 자동 삭제하지 않는다. `WishRepository.findByMemberIdAndProductId()` 메서드가 존재하지만(`WishRepository.java:11`) `OrderService`에서 호출하지 않는다.

이것이 의도적인 정책인지 누락인지 확인이 필요하다. 주문한 상품이 위시리스트에 남아 있으면 사용자 경험에 혼동을 줄 수 있다.

**근거**: `OrderService.java:36-52`, `WishRepository.java:11`

---

## 4. 종합 요약

| # | 항목 | 유형 | 현재 작동 | 변경 후 작동 | 영향 범위 |
|---|------|------|----------|------------|----------|
| 3-1 | Authorization 누락 시 상태 코드 | HTTP 응답 변경 | 400 | 401 | 인증 필요 엔드포인트 전체 |
| 3-2 | 트랜잭션 내 외부 API | 트랜잭션 경계 변경 | 트랜잭션 내 API 호출 | 트랜잭션 커밋 후 API 호출 | OrderService |
| 3-3 | 동시성 미보호 | 동시성 제어 추가 | 잠금 없음 | 낙관적/비관적 잠금 | Option, Member, OrderService |
| 3-4 | MethodArgumentNotValidException | 예외 핸들러 추가 | Spring 기본 응답 | 통일된 400 응답 | GlobalExceptionHandler |
| 3-5 | 에러 응답 본문 불일관 | 응답 형식 변경 | 예외별 상이 | 통일된 에러 형식 | GlobalExceptionHandler |
| 3-6 | 에러 메시지 언어 혼재 | 메시지 통일 | 영어/한국어 혼재 | 단일 언어 | Member, MemberService |
| 3-7 | Option 음수 차감 허용 | 검증 추가 | 음수로 재고 증가 | 예외 발생 | Option |
| 3-8 | FK 위반 시 500 에러 | 삭제 정책 수립 | 500 | 의미 있는 4xx | 전 Service의 delete() |
| 3-9 | wish UNIQUE 미보장 | DB 제약 추가 | 동시 요청 시 중복 | DB 레벨 중복 방지 | wish 테이블 |
| 3-10 | 주문 시 위시 미삭제 | 비즈니스 로직 추가 | 위시 유지 | 위시 자동 삭제 | OrderService |

---

## 5. 작업 목록

### 작업 1: Authorization 헤더 누락 시 401 반환

`AuthenticationResolver`에서 `MissingRequestHeaderException` 대신 `AuthenticationException`을 던져 401을 반환한다.

**리팩토링 이점**: 변경 위험 통제 — HTTP 의미론에 맞는 응답으로 클라이언트가 인증 실패(401)와 잘못된 요청(400)을 구분하여 적절한 재시도 전략(토큰 재발급 vs 요청 수정)을 적용할 수 있다.

**테스트 영향**: 기존 인수 테스트 "인증되지 않은 사용자가 주문하면 실패한다" 시나리오의 단언을 400 → 401로 수정. "인증에 실패한다" 전용 스텝 신규 추가.

**영향 범위**: `AuthenticationResolver.java`, `gift.feature`, `GiftStepDefinitions.java`
**우선순위**: 높음

**검증 결과**: `./gradlew cucumberTest` — 17개 시나리오 전체 통과

---

### 작업 2: 트랜잭션에서 외부 API 호출 분리

`OrderService.createOrder()`의 `sendMessageIfPossible()` 호출을 트랜잭션 커밋 이후로 이동한다.

**리팩토링 이점**: 변경 위험 통제 — 외부 API 장애가 DB 커넥션 풀에 영향을 주지 않게 되어, 메시지 전송 채널 변경이나 타임아웃 설정 변경 시 트랜잭션 안정성을 별도로 검증할 필요가 없다.

**테스트 영향**: 기존 인수 테스트 통과 유지 (외부 작동 동일). 트랜잭션 경계 변경은 단위 테스트로 검증.

**영향 범위**: `OrderService.java`, `OrderCreatedEvent.java`(신규), `OrderMessageEventListener.java`(신규)
**우선순위**: 높음

**검증 결과**: `./gradlew cucumberTest` — 17개 시나리오 전체 통과

---

### 작업 3: Option.subtractQuantity 음수/영 검증 추가

`amount <= 0`일 때 `IllegalArgumentException`을 던진다. `Member.deductPoint()`와 동일한 방어 패턴.

**리팩토링 이점**: 변경 가능성 확보 — 새로운 호출 경로가 추가되어도 도메인 객체가 불변식을 자체 보호하므로, 호출자가 검증을 누락해도 안전하다.

**테스트 영향**: 새 테스트 시나리오 추가 (음수 수량 주문 시 예외 발생).

**영향 범위**: `Option.java`
**우선순위**: 높음

**검증 결과**: `./gradlew cucumberTest` — 17개 시나리오 전체 통과

---

### 작업 4: 삭제 시 FK 위반 처리

하위 엔티티가 있는 상위 엔티티 삭제 시 `DataIntegrityViolationException` 대신 의미 있는 응답을 반환한다.

**리팩토링 이점**: 변경 위험 통제 — 삭제 API의 실패 동작이 정의되어 있어야, 향후 삭제 정책(soft delete, cascade 등) 변경 시 기존 동작과의 차이를 명확히 비교할 수 있다.

**테스트 영향**: 새 테스트 시나리오 추가 (하위 엔티티가 있는 상태에서 삭제 시 4xx 반환).

**영향 범위**: 각 Service의 `delete()` 메서드, `GlobalExceptionHandler.java` (안전망)
**우선순위**: 높음

**구현 방식**: Restrict + 예외 포착 (ADR-001 참조). `deleteById()` + `flush()` 후 `DataIntegrityViolationException`을 잡아 `IllegalArgumentException`으로 변환. `existsBy*` 사전 검증은 크로스 패키지 Repository 참조(5-3 원칙 위반)와 불필요한 추가 조회 문제로 채택하지 않음.

**변경 내역**:
- `existsBy*` 메서드 6개 제거 (`ProductRepository`, `WishRepository`, `OrderRepository`)
- 크로스 패키지 Repository 주입 4건 제거 (`CategoryService`←`ProductRepository`, `ProductService`←`WishRepository`/`OrderRepository`, `OptionService`←`OrderRepository`, `MemberService`←`OrderRepository`/`WishRepository`)
- 각 Service `delete()`를 try-catch 패턴으로 변경
- `GlobalExceptionHandler`에 `DataIntegrityViolationException` → 409 안전망 유지

**검증 결과**: `./gradlew cucumberTest` — 20개 시나리오 전체 통과 (기존 17 + 삭제 실패 3)

---

### 작업 5: MethodArgumentNotValidException 핸들러 추가

`GlobalExceptionHandler`에 Bean Validation 실패 핸들러를 추가하여 통일된 400 응답을 반환한다.

**리팩토링 이점**: 변경 위험 통제 — 에러 응답 형식이 통일되어, 클라이언트 에러 처리 로직이 단순해진다. 새 DTO에 검증 어노테이션을 추가할 때 응답 형식을 별도로 걱정할 필요가 없다.

**테스트 영향**: 기존 인수 테스트 통과 유지 (정상 흐름에는 영향 없음). Bean Validation 실패 시나리오 테스트 추가.

**영향 범위**: `GlobalExceptionHandler.java`
**우선순위**: 중간

**구현 내역**:
- `GlobalExceptionHandler`에 `MethodArgumentNotValidException` → 400 핸들러 추가
- `BindingResult.getFieldErrors()`에서 필드명 + 기본 메시지를 조합하여 `ResponseEntity<String>` 반환
- 기존 `IllegalArgumentException` 핸들러와 동일한 `400 + 문자열 메시지` 형식 유지
- 응답 예시: `name: must not be blank, price: must be greater than 0`

**검증 결과**: `./gradlew cucumberTest` — 20개 시나리오 전체 통과 (기존 시나리오는 정상 입력만 사용하므로 영향 없음)

---

### 작업 6: wish 테이블 UNIQUE 제약 추가

`V2` 마이그레이션으로 `UNIQUE(member_id, product_id)` 제약을 추가한다.

**리팩토링 이점**: 변경 위험 통제 — DB 레벨에서 데이터 정합성이 보장되어, 위시 관련 로직 변경 시 중복 데이터 발생 위험을 신경 쓸 필요가 없다.

**테스트 영향**: 기존 인수 테스트 통과 유지 (현재 중복 추가 시나리오는 애플리케이션 레벨에서 처리됨).

**영향 범위**: DB 마이그레이션, `WishService.java` (예외 처리 보완)
**우선순위**: 중간

**구현 내역**:
- `V3__Add_wish_unique_constraint.sql`: `ALTER TABLE wish ADD CONSTRAINT uk_wish_member_product UNIQUE (member_id, product_id)`
- `WishService.addWish()`: `save()` 호출을 try-catch로 감싸 `DataIntegrityViolationException` → `IllegalArgumentException("이미 위시리스트에 추가된 상품입니다.")` 변환
- 기존 app-level 중복 검사(`findByMemberIdAndProductId` → 200 OK) 유지. DB UNIQUE 제약은 race condition 안전망
- `DataIntegrityViolationException` 후 Hibernate 세션이 오류 상태이므로 기존 위시 재조회 불가 → `IllegalArgumentException`(→ 400)으로 변환
- IDENTITY 전략이므로 `save()` 시점에 즉시 INSERT 실행 → 별도 `flush()` 불필요

**검증 결과**: `./gradlew cucumberTest` — 20개 시나리오 전체 통과

---

### 작업 7: 에러 메시지 언어 통일

프로젝트 전체의 에러 메시지 언어를 한 가지로 통일한다.

**리팩토링 이점**: 변경 위험 통제 — 에러 메시지 변경이나 국제화 도입 시 기준이 명확해진다.

**테스트 영향**: 인수 테스트에서 에러 메시지 본문을 검증하는 시나리오가 없으므로 수정 불필요.

**영향 범위**: `Member.java`, `MemberService.java`
**우선순위**: 낮음

**구현 내역**:

영어 메시지 4개를 한국어로 변환:

| 파일 | 변경 전 | 변경 후 | 분류 |
|------|---------|---------|------|
| `Member.java:50` | `"Amount must be greater than zero."` | `"충전 금액은 1 이상이어야 합니다."` | 작동 변경 (응답 본문 노출) |
| `MemberService.java:29` | `"Invalid email or password."` | `"이메일 또는 비밀번호가 올바르지 않습니다."` | 작동 변경 (응답 본문 노출) |
| `MemberService.java:31` | `"Invalid email or password."` | `"이메일 또는 비밀번호가 올바르지 않습니다."` | 작동 변경 (응답 본문 노출) |
| `MemberService.java:58` | `"Member not found. id="` | `"회원이 존재하지 않습니다. id="` | 구조 변경 (NoSuchElementException → `ResponseEntity<Void>`, 응답 본문 미노출) |
| `MemberService.java:64` | `"Email is already registered."` | `"이미 등록된 이메일입니다."` | 작동 변경 (응답 본문 노출) |

> **참고**: `NoSuchElementException` 메시지(`"Member not found. id="`)는 `GlobalExceptionHandler`에서 `ResponseEntity<Void>`(본문 없음)로 처리되므로 클라이언트 응답에 노출되지 않는다. 따라서 이 1건은 클라이언트 관점에서 작동 변경이 아닌 구조 변경이다.

**검증 결과**: `./gradlew cucumberTest` — 20개 시나리오 전체 통과

---

### 작업 8: 에러 응답 본문 통일 (3-5)

**목표**: 모든 예외 핸들러가 `{"message": "..."}` JSON 구조를 반환하도록 통일.

**현재 상태**: 401/403/404는 body 없음, 400/409는 plain text String body.

**리팩토링 이점**: API 계약을 먼저 확립. 이후 작업에서 발생하는 에러도 통일된 형식으로 반환됨.

**변경 파일**:

| 파일 | 변경 |
|------|------|
| `src/main/java/gift/ErrorResponse.java` (신규) | `public record ErrorResponse(String message) {}` |
| `src/main/java/gift/GlobalExceptionHandler.java` | 6개 핸들러 모두 `ResponseEntity<ErrorResponse>` 반환 |

**핸들러별 변경**:

| 핸들러 | 변경 전 | 변경 후 |
|--------|---------|---------|
| `handleUnauthorized` | `ResponseEntity<Void>` | `ResponseEntity<ErrorResponse>` + body(e.getMessage()) |
| `handleNotFound` | `ResponseEntity<Void>` | `ResponseEntity<ErrorResponse>` + body(e.getMessage()) |
| `handleIllegalArgument` | `ResponseEntity<String>` | `ResponseEntity<ErrorResponse>` + wrap |
| `handleForbidden` | `ResponseEntity<Void>` | `ResponseEntity<ErrorResponse>` + body(e.getMessage()) |
| `handleMethodArgumentNotValid` | `ResponseEntity<String>` | `ResponseEntity<ErrorResponse>` + wrap |
| `handleDataIntegrityViolation` | `ResponseEntity<String>` | `ResponseEntity<ErrorResponse>` + wrap |

**테스트 영향**: 인수 테스트는 상태 코드만 단언하므로 통과 유지.

**ADR**: ADR-003 참조

**검증 결과**: `./gradlew cucumberTest` — 20개 시나리오 전체 통과

---

### 작업 9: 주문 시 위시 삭제 (3-10)

**목표**: 주문 생성 시 해당 상품이 위시리스트에 있으면 자동 삭제.

**현재 상태**: `OrderService.createOrder()`에 위시 삭제 로직 없음. `WishRepository.findByMemberIdAndProductId()` 존재하지만 미사용.

**리팩토링 이점**: 작업 10 전에 OrderService를 최종 형태로 완성. 잠금 범위 재조정 불필요.

**변경 파일**:

| 파일 | 변경 |
|------|------|
| `src/main/java/gift/wish/WishRepository.java` | `void deleteByMemberIdAndProductId(Long, Long)` 추가 |
| `src/main/java/gift/wish/WishService.java` | `removeWishByMemberIdAndProductId()` 메서드 추가 |
| `src/main/java/gift/order/OrderService.java` | WishService 의존 추가, 주문 저장 후 위시 삭제 호출 |
| `src/test/resources/features/gift.feature` | 시나리오 1개 추가 |

**OrderService 변경**:

```java
// 기존 코드 (line 48) 이후, publishEvent (line 50) 이전에 추가:
Long productId = option.getProduct().getId();
wishService.removeWishByMemberIdAndProductId(memberId, productId);
```

- 위시 없으면 아무 일도 안 함 (soft delete — Spring Data derived delete는 매칭 없으면 무시)
- UNIQUE 제약(V3)으로 최대 1건만 삭제됨

**인수 테스트 시나리오** (gift.feature에 추가):

```gherkin
시나리오: 주문하면 위시리스트에서 해당 상품이 삭제된다
  만일 해당 상품을 위시리스트에 추가한다
  그리고 회원이 1개를 주문한다
  그러면 주문이 성공한다
  그리고 위시리스트가 비어있다
```

4개 스텝 모두 기존 정의 재사용 (WishStepDefinitions:19, GiftStepDefinitions:67, GiftStepDefinitions:149, WishStepDefinitions:163). 새 스텝 정의 불필요.

**순환 참조 검증**: OrderService → WishService → ProductService. OrderService → OptionService → ProductService. 순환 없음.

**테스트 영향**: 기존 인수 테스트 통과 유지 + 신규 시나리오 1개 추가.

**검증 결과**: `./gradlew cucumberTest` — 21개 시나리오 전체 통과 (기존 20 + 신규 1)

---

### 작업 10: 동시성 제어 (3-3)

**목표**: `Option.subtractQuantity()`와 `Member.deductPoint()`의 read-then-write race condition 방지.

**현재 상태**: `@Version` 없음, `@Lock` 없음, DB CHECK 제약 없음. 동시 주문 시 재고/포인트가 음수가 될 수 있음.

**리팩토링 이점**: OrderService가 최종 형태일 때 적용하여 잠금 범위를 한 번에 확정.

**전략**: 비관적 잠금 (ADR-004 참조)

**변경 파일**:

| 파일 | 변경 |
|------|------|
| `src/main/java/gift/option/OptionRepository.java` | `findByIdForUpdate()` + `@Lock(PESSIMISTIC_WRITE)` |
| `src/main/java/gift/member/MemberRepository.java` | `findByIdForUpdate()` + `@Lock(PESSIMISTIC_WRITE)` |
| `src/main/java/gift/option/OptionService.java` | `findByIdForUpdate()` wrapper |
| `src/main/java/gift/member/MemberService.java` | `findByIdForUpdate()` wrapper |
| `src/main/java/gift/order/OrderService.java` | `findById()` → `findByIdForUpdate()` (2곳) |
| `src/main/resources/db/migration/V4__Add_check_constraints.sql` (신규) | CHECK 제약 |

**Repository 추가 메서드**:

```java
// OptionRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT o FROM Option o WHERE o.id = :id")
Optional<Option> findByIdForUpdate(@Param("id") Long id);

// MemberRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT m FROM Member m WHERE m.id = :id")
Optional<Member> findByIdForUpdate(@Param("id") Long id);
```

**DB 마이그레이션 (V4)**:

```sql
ALTER TABLE options ADD CONSTRAINT chk_option_quantity_non_negative CHECK (quantity >= 0);
ALTER TABLE member ADD CONSTRAINT chk_member_point_non_negative CHECK (point >= 0);
```

**OrderService 변경 (2줄)**:

```
line 38: optionService.findById(optionId)     → optionService.findByIdForUpdate(optionId)
line 43: memberService.findById(memberId)     → memberService.findByIdForUpdate(memberId)
```

**잠금 순서**: Option → Member (일관된 순서 유지로 데드락 방지. createOrder()만 두 엔티티를 동시 잠금)

**테스트 영향**: 단일 스레드 동작은 동일하므로 기존 인수 테스트 통과. 동시성 테스트는 Cucumber E2E로 불가 — 향후 JUnit 통합 테스트로 별도 검증.

**검증 결과**: `./gradlew cucumberTest` — 21개 시나리오 전체 통과

---

## 6. 현행 유지 항목

분석 결과 작동 변경이 불필요하거나, 현 시점에서 변경 비용 대비 이점이 낮은 항목을 정리한다.

> **갱신 (2026-03-05)**: 아래 3건은 작업 8/9/10으로 승격되었다. 구현 계획은 "5. 작업 목록"을 참조한다.

| 항목 | 현재 상태 | 사유 | 승격 |
|------|----------|------|------|
| 동시성 제어 (3-3) | 잠금 없음 | 낙관적/비관적 잠금 도입은 성능 특성을 변경하며, 트랜잭션 재시도 로직이 필요하다. 현재 트래픽 수준에서 race condition 발생 확률이 낮고, 도입 시 전체 조회-수정 패턴을 재검토해야 한다. 독립된 작업으로 별도 계획이 필요하므로 이번 범위에서 제외한다. | → **작업 10** |
| 주문 시 위시 삭제 (3-10) | 위시 유지 | 비즈니스 요구사항 확인이 필요하다. "주문해도 위시에 남겨두고 재주문할 수 있다"가 의도된 정책일 수 있다. 요구사항이 확인되기 전까지 현행 유지한다. | → **작업 9** |
| 에러 응답 본문 통일 (3-5) | 예외별 상이 | 작업 5(MethodArgumentNotValidException 핸들러)와 작업 7(메시지 언어 통일)을 먼저 수행하면 자연스럽게 재검토 시점이 된다. 별도 작업으로 분리하지 않고 작업 5 수행 시 함께 결정한다. | → **작업 8** |

---

## 7. ADR

### ADR-001: 삭제 정책 — Restrict + 예외 포착

- **맥락**: 상위 엔티티 삭제 시 FK 위반으로 500이 발생한다. 삭제 정책을 정해야 한다.
- **선택지**:
  - A: ON DELETE CASCADE — 상위 삭제 시 하위 자동 삭제. 구현 단순. 의도치 않은 대량 삭제 위험 (Category 삭제 시 모든 Product, Option, Order, Wish가 연쇄 삭제).
  - B: Restrict + 사전 검증 (`existsBy*`) — 하위 엔티티 존재 시 삭제 거부. 원인별 세분화된 에러 메시지. 크로스 패키지 Repository 주입 필요 (5-3 원칙 위반), 매 삭제마다 추가 SELECT, race condition에 취약.
  - C: Restrict + 예외 포착 (try-catch) — 삭제 시도 후 FK 위반을 잡아 변환. 추가 조회 없음, 크로스 패키지 참조 불필요, race condition에 안전. 엔티티 단위 메시지만 가능.
  - D: Soft delete — 실제 삭제 대신 `deleted` 플래그. 데이터 보존. 조회 쿼리 전체에 `WHERE deleted = false` 추가 필요, 복잡도 증가.
- **결정**: C안 — Restrict + 예외 포착 (초기에 B안 선택 후 C안으로 전환)
- **전환 사유**: B안(사전 검증)은 크로스 패키지 Repository 주입 4건이 필요하여 5-3 원칙(크로스 패키지 Repository 참조 금지)과 충돌했다. 또한 매 삭제마다 1~2회 SELECT가 추가 실행되는 불필요한 오버헤드가 있었다.
- **근거**: 현재 DB 스키마의 FK가 이미 Restrict(기본값)이다. 삭제를 시도하고 `DataIntegrityViolationException`을 잡아 `IllegalArgumentException`으로 변환하면, DB 제약이 직접 보호하므로 race condition에도 안전하다. `deleteById()` 후 `flush()`를 호출하여 같은 메서드 내에서 예외를 포착한다.
- **결과**: 각 Service의 `delete()` 메서드에 try-catch 패턴을 적용한다. `existsBy*` 메서드 6개와 크로스 패키지 Repository 주입 4건을 제거한다. `GlobalExceptionHandler`에 `DataIntegrityViolationException` → 409 핸들러를 안전망으로 유지한다.

### ADR-002: 트랜잭션 외부 API 분리 방식

- **맥락**: `OrderService.createOrder()`의 `@Transactional` 내에서 외부 API를 호출한다. 분리 방법을 결정해야 한다.
- **선택지**:
  - A: `@TransactionalEventListener(phase = AFTER_COMMIT)` — Spring 이벤트로 트랜잭션 커밋 후 실행. 프레임워크 표준. OrderService가 메시지 전송 구현을 몰라도 됨. 이벤트 발행/구독 간접 계층 추가.
  - B: 서비스 분리 — 트랜잭션 메서드와 비트랜잭션 메서드를 분리하고, 컨트롤러(또는 파사드)에서 순차 호출. 명시적 흐름. 컨트롤러에 오케스트레이션 로직이 들어감.
  - C: `TransactionTemplate` 수동 관리 — 프로그래밍 방식으로 트랜잭션 범위를 제한. 세밀한 제어 가능. `@Transactional` 선언적 방식과 혼용되어 일관성 저하.
- **결정**: A안 — `@TransactionalEventListener`
- **근거**: 메시지 전송은 주문의 부수 효과이며, 실패해도 주문을 롤백하지 않는다(현재 `catch (Exception ignored)`가 이를 증명). 이벤트로 분리하면 OrderService는 "주문 완료됨" 이벤트를 발행할 뿐이고, 메시지 전송은 리스너가 담당한다. 이는 현재 `sendMessageIfPossible`의 의도("가능하면 보낸다")와 정확히 일치한다. B안은 컨트롤러에 비즈니스 흐름이 노출되고, C안은 선언적/프로그래밍 방식이 혼용된다.
- **결과**: `OrderCreatedEvent` 도메인 이벤트 클래스를 생성하고, 메시지 전송을 `@TransactionalEventListener`로 이동한다. `OrderService.createOrder()`에서 `sendMessageIfPossible()` 호출을 제거한다.

### ADR-003: 에러 응답 형식 — JSON `{"message": "..."}`

- **맥락**: 예외 핸들러의 응답 형식이 통일되지 않았다. 401/403/404는 body 없음, 400/409는 plain text String body.
- **선택지**:
  - A: plain text String body 유지 — 현행 유지. Content-Type이 `text/plain`과 `application/json`으로 혼재.
  - B: JSON `{"message": "..."}` — `ErrorResponse` record 도입. Content-Type 통일(`application/json`), 기계 파싱 용이, 성공/실패 응답 형식 일관성.
  - C: RFC 7807 Problem Details — Spring Boot 3 기본 지원. 풍부한 에러 정보. 현재 프로젝트 규모에 과잉.
- **결정**: B안 — JSON `{"message": "..."}`
- **근거**: Content-Type이 `application/json`으로 통일되어 클라이언트가 성공/실패 응답을 동일한 방식으로 파싱할 수 있다. `ErrorResponse` record 하나로 모든 핸들러의 반환 타입을 통일하므로 구현이 단순하다. RFC 7807은 현재 프로젝트 규모에 과잉이며, 필요 시 `ErrorResponse`를 확장하면 된다.
- **결과**: `ErrorResponse` record를 생성하고, 6개 핸들러 모두 `ResponseEntity<ErrorResponse>`를 반환하도록 변경한다.

### ADR-004: 동시성 제어 — 비관적 잠금 (SELECT FOR UPDATE)

- **맥락**: `Option.subtractQuantity()`와 `Member.deductPoint()`에 read-then-write race condition이 존재한다. 동시 주문 시 재고/포인트가 음수가 될 수 있다.
- **선택지**:
  - A: 원자적 SQL UPDATE (`UPDATE ... SET quantity = quantity - :amount WHERE quantity >= :amount`) — SQL WHERE절에서 검증. 도메인 객체가 검증 책임을 잃음. `@Modifying(clearAutomatically=true)` 필수로 JPA 1차 캐시 이슈 발생. OrderService 15줄+ 재작성 필요. 에러 메시지 세분화 불가.
  - B: 비관적 잠금 (`@Lock(PESSIMISTIC_WRITE)`) — SELECT FOR UPDATE로 row-level X lock. 도메인 모델/에러 메시지 보존. JPA dirty checking 정상 동작. OrderService 2줄만 변경.
  - C: 낙관적 잠금 (`@Version`) — version 불일치 시 재시도. spring-retry 의존 추가, version 컬럼 마이그레이션 필요. 이벤트 중복 발행 위험.
- **결정**: B안 — 비관적 잠금
- **근거**:
  1. SQL 라운드트립이 3안 모두 동일(5문). 원자적 SQL의 "성능 이점"은 엔티티 재조회가 필요한 이 흐름에서 사라진다.
  2. 프로젝트 방향(검증을 도메인 객체로 이동: 작업 3, 프롬프트 24)과 일치한다. 원자적 SQL은 역행.
  3. OrderService에서 2줄만 변경하여 코드 변경이 최소이다.
  4. `@Modifying` 캐시 관리 불필요. JPA와 자연스럽게 동작한다.
  5. 잠금 경합: 트랜잭션 짧고(외부 API 분리됨) 범위 좁아(2행) 무시 가능.
- **결과**: `OptionRepository`, `MemberRepository`에 `findByIdForUpdate()` 추가. `OrderService.createOrder()`에서 `findById()` → `findByIdForUpdate()` 변경(2곳). V4 마이그레이션으로 CHECK 제약 추가.
