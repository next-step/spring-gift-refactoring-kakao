# ADR-006: 도메인 책임 되찾기

## 상태
- [x] 승인됨 (2026-03)

## 맥락
서비스 계층에 도메인이 담당해야 할 로직이 있었다. 도메인 객체가 자신의 책임을 수행하도록 개선한다.

## 변경 사항

### 1. 가격 계산 → Option 도메인

**변경 전** (OrderService):
```java
int price = option.getProduct().getPrice() * request.quantity();
```

**변경 후**:
```java
// Option.java
public int calculateTotalPrice(int orderQuantity) {
    return product.getPrice() * orderQuantity;
}

// OrderService.java
int price = option.calculateTotalPrice(request.quantity());
```

**근거**: 가격 계산은 Option이 알아야 할 책임. 서비스는 도메인에 위임.

---

### 2. AuthenticationResolver → Optional 반환

**변경 전**:
```java
public Member extractMember(String authorization) {
    // ...
    return memberRepository.findByEmail(email).orElse(null);
}

// Controller
Member member = authenticationResolver.extractMember(authorization);
if (member == null) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
}
```

**변경 후**:
```java
public Optional<Member> extractMember(String authorization) {
    // ...
    return memberRepository.findByEmail(email);
}

// Controller
return authenticationResolver.extractMember(authorization)
    .map(member -> { /* 로직 */ })
    .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
```

**근거**:
- null 대신 Optional로 명시적 처리 강제
- 호출자가 null 체크 누락 방지
- ADR-003에서 제외했던 항목 해결

## 결과
- 79개 테스트 통과 (기존 77개 + 가격 계산 단위 테스트 2개)
- 도메인 객체가 자신의 책임 수행
- null 안전성 개선

### 증거: 가격 계산 단위 테스트
```java
@Test
void calculateTotalPrice() {
    // Given: 가격 1000원 상품
    // When
    int totalPrice = option.calculateTotalPrice(5);
    // Then: 1000 * 5 = 5000
    assertThat(totalPrice).isEqualTo(5000);
}
```

## 관련 ADR
- [ADR-003: 코드 스멜 수정](003-code-smell-fixes.md) - 제외 사항 해결
