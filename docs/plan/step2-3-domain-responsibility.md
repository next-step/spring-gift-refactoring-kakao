# Step 2-3: 도메인 책임 되찾기

## 선정된 대상

### 1. 가격 계산 로직 → Option 도메인
**현재** (OrderService:55):
```java
int price = option.getProduct().getPrice() * request.quantity();
```

**개선**:
```java
int price = option.calculateTotalPrice(request.quantity());
```

**이유**: 가격 계산은 Option이 알아야 할 책임. 서비스는 도메인 객체에 위임만.

---

### 2. AuthenticationResolver → Optional 반환
**현재**:
```java
public Member extractMember(String authorization) {
    // ...
    return memberRepository.findByEmail(email).orElse(null);
    // catch: return null;
}
```

**개선**:
```java
public Optional<Member> extractMember(String authorization) {
    // ...
    return memberRepository.findByEmail(email);
    // catch: return Optional.empty();
}
```

**이유**: null 대신 Optional로 명시적 처리 강제. 호출자가 null 체크 누락 방지.

---

## 체크리스트

### 가격 계산
- [x] Option.calculateTotalPrice() 메서드 추가
- [x] OrderService에서 새 메서드 사용
- [x] 테스트 통과 확인

### AuthenticationResolver
- [x] 반환 타입 Optional<Member>로 변경
- [x] 호출하는 Controller들 수정 (OrderController, WishController)
- [x] 테스트 통과 확인 (77개)

### 마무리
- [x] ADR 006 작성
- [ ] 커밋
