# spring-gift-refactoring

## Step2: 작동 변경 리팩터링

---

### 1. 트랜잭션 경계 세우기

#### KakaoAuthService 외부 API 호출을 트랜잭션 밖으로 분리

**문제**: `processCallback()`에 `@Transactional`이 걸려 있어, 카카오 외부 HTTP 호출(`requestAccessToken`, `requestUserInfo`) 동안 DB 커넥션을 점유하고 있었다.

**변경**: `@Transactional` 제거. 외부 API 호출이 트랜잭션 범위 밖에서 실행되도록 분리했다.

```java
// Before: 외부 HTTP 호출이 트랜잭션 안에서 실행됨
@Transactional
public TokenResponse processCallback(String code) {
    KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);  // HTTP
    KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(...);        // HTTP
    memberRepository.save(member);  // DB
}

// After: 트랜잭션 제거 → DB 커넥션 점유 해소
public TokenResponse processCallback(String code) {
    KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);  // HTTP (트랜잭션 밖)
    KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(...);        // HTTP (트랜잭션 밖)
    memberRepository.save(member);  // DB (자체 트랜잭션)
}
```

**증거**: `memberRepository.save()`는 SimpleJpaRepository의 `@Transactional`로 개별 보장된다. 외부 API 실패 시에도 불필요한 DB 롤백이 발생하지 않으며, 카카오 API 지연이 커넥션 풀에 영향을 주지 않는다.

> 커밋: `321e28f` (구조 변경, 작동 유지)

---

### 2. 누락된 작동 구현

#### wish, options 테이블에 UNIQUE 제약 조건 추가

**문제**: 애플리케이션 레벨에서 중복 검사를 하고 있었지만, DB 레벨 제약 조건이 없어 동시 요청 시 중복 데이터가 삽입될 수 있었다.
- 같은 회원이 같은 상품을 위시리스트에 중복 추가 가능
- 같은 상품에 같은 이름의 옵션을 중복 추가 가능

**변경**: Flyway 마이그레이션(`V3__Add_unique_constraints.sql`)으로 DB 레벨 UNIQUE 제약 조건을 추가했다.

```sql
alter table wish
    add constraint uk_wish_member_product unique (member_id, product_id);

alter table options
    add constraint uk_options_product_name unique (product_id, name);
```

**증거**: 기존 인수 테스트 `option.feature`의 "같은 이름의 옵션을 추가하면 실패한다" 시나리오가 DB 레벨에서도 보장된다. 동시 요청 환경에서 애플리케이션 검사를 통과하더라도 DB 제약 조건에 의해 중복이 차단된다.

> 커밋: `e4b34d1` (작동 변경)

---

### 3. 도메인 책임 되찾기

#### 개선 1: 주문 가격 계산을 Option 도메인으로 이동

**문제**: `OrderService`가 `option.getProduct().getPrice() * quantity`로 직접 가격을 계산하고 있었다. 가격 계산 로직이 서비스에 누수되어 있고, 다른 곳에서 동일한 계산이 필요할 때 중복이 발생한다.

**변경**: `Option.calculateTotalPrice(quantity)` 도메인 메서드를 추가하고, `OrderService`는 이를 호출하도록 변경했다.

```java
// Before (OrderService): 서비스가 가격 계산 책임을 가짐
int price = option.getProduct().getPrice() * request.quantity();
member.deductPoint(price);

// After (Option): 도메인이 가격 계산 책임을 가짐
public int calculateTotalPrice(int quantity) {
    return product.getPrice() * quantity;
}

// After (OrderService): 호출부 단순화
member.deductPoint(option.calculateTotalPrice(request.quantity()));
```

**증거**: `order.feature` 7개 시나리오 전체 통과. 특히 "포인트가 부족하면 주문이 실패한다" 시나리오에서 가격 계산 → 포인트 차감 → 롤백 흐름이 변경 전과 동일하게 동작함을 확인했다.

> 커밋: `49a0faa` (구조 변경, 작동 유지)

---

#### 개선 2: AdminMemberController 중복 이메일 검사 TOCTOU 제거

**문제**: `AdminMemberController`에서 `existsByEmail()` → `create()` 순서로 호출하는 check-then-act 패턴이 있었다. `MemberService.create()` 내부에 이미 동일한 중복 검사가 존재하므로:
1. 검사 로직이 컨트롤러와 서비스에 **중복**
2. 두 호출 사이에 다른 요청이 같은 이메일로 가입하면 **TOCTOU(Time-of-Check to Time-of-Use)** 문제 발생 가능

**변경**: 컨트롤러의 선행 검사를 제거하고, `MemberService.create()`의 `@Transactional` 내부 검사에 일원화했다.

```java
// Before: check-then-act (TOCTOU 취약)
if (memberService.existsByEmail(email)) {           // 1. 검사
    populateNewFormError(model, email, "이미 등록된 이메일입니다.");
    return "member/new";
}
memberService.create(email, password);               // 2. 실행 (사이에 다른 요청 가능)

// After: 원자적 실행
try {
    memberService.create(email, password);            // 검사 + 실행이 @Transactional 안에서 원자적
} catch (IllegalArgumentException e) {
    populateNewFormError(model, email, e.getMessage());
    return "member/new";
}
```

**증거**: `MemberService.create()`는 `@Transactional` 내에서 `existsByEmail()` 검사 후 즉시 `save()`를 수행하므로 TOCTOU 창이 제거된다. `member.feature`의 "이미 가입된 이메일로 회원가입하면 실패한다" 시나리오로 중복 이메일 차단이 정상 동작함을 확인했다. 불필요한 `existsByEmail()` public 메서드도 제거하여 API surface를 축소했다.

> 커밋: `940d0ca` (구조 변경 + 작동 유지)

---

### 4. 환경 설정 개선

#### docker-compose env_file을 optional로 변경

`.env` 파일이 없는 환경에서도 `docker-compose up`이 실패하지 않도록 `required: false` 설정을 추가했다. `environment` 블록에 기본값이 이미 정의되어 있으므로 `.env`는 오버라이드 용도로만 사용된다.

> 커밋: `95d3bb7` (chore)

---

## 변경 요약

| 구분 | 커밋 | 유형 |
|------|------|------|
| 트랜잭션 경계 | KakaoAuthService 외부 API 호출 트랜잭션 분리 | 구조 변경 |
| 누락된 작동 | wish, options UNIQUE 제약 조건 추가 | 작동 변경 |
| 도메인 책임 | 가격 계산을 Option 도메인으로 이동 | 구조 변경 |
| 도메인 책임 | 중복 이메일 검사 TOCTOU 제거 및 중복 제거 | 구조 변경 |
| 환경 설정 | docker-compose env_file optional 변경 | chore |
