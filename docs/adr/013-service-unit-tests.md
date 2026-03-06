# ADR-013: 서비스 레벨 단위 테스트 추가

## 상태
- [x] 승인됨 (2026-03)

## 맥락
리뷰어가 현재 인수 테스트 중심의 테스트 구성에 대해 서비스 레벨 단위 테스트 부재를 지적했다.

기존 테스트 구성:
- 인수 테스트: 6개 (Category, Product, Option, Member, Wish, Order)
- 도메인 단위 테스트: 1개 (OptionTest)

## 결정
**핵심 비즈니스 규칙**에 대해서만 서비스 레벨 단위 테스트를 추가한다.

### 추가 대상 선정 기준
1. 실패 시 비즈니스에 직접적 영향을 주는 검증 로직
2. 인수 테스트로는 원인 파악이 어려운 케이스
3. Mock 기반이지만 검증 가치가 높은 로직

### 추가하지 않는 케이스
- 단순 CRUD 조율 (orchestration)
- 인수 테스트로 충분히 커버되는 흐름

## 변경 사항

### MemberServiceTest.java (신규)
```java
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    // register() - 이메일 중복 검사
    // login() - 비밀번호 불일치 검증
}
```

### OptionTest.java (추가)
```java
// subtractQuantity() - 재고 부족 검증
```

## 테스트 증거

```java
@Test
@DisplayName("이미 등록된 이메일이면 예외를 발생시킨다")
void duplicateEmail_throwsException() {
    given(memberRepository.existsByEmail(duplicateEmail)).willReturn(true);
    assertThatThrownBy(() -> memberService.register(request))
        .isInstanceOf(IllegalArgumentException.class);
}

@Test
@DisplayName("비밀번호가 일치하지 않으면 예외를 발생시킨다")
void wrongPassword_throwsException() {
    given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
    assertThatThrownBy(() -> memberService.login(request))
        .isInstanceOf(IllegalArgumentException.class);
}

@Test
@DisplayName("재고보다 많은 수량을 차감하면 예외를 발생시킨다")
void subtractQuantity_insufficientStock() {
    assertThatThrownBy(() -> option.subtractQuantity(20))
        .isInstanceOf(IllegalArgumentException.class);
}
```

## 결과
- 90개 테스트 통과 (기존 83개 + 7개)
- 핵심 비즈니스 규칙에 대한 빠른 피드백 확보
- 나머지 조율 로직은 인수 테스트로 커버 유지

## 테스트 전략 요약

| 계층 | 테스트 방식 | 대상 |
|------|------------|------|
| 도메인 | 단위 테스트 | 순수 비즈니스 로직 (calculateTotalPrice, subtractQuantity) |
| 서비스 | 단위 테스트 (Mock) | 핵심 검증 로직 (이메일 중복, 비밀번호 검증) |
| 서비스 | 인수 테스트 | 전체 흐름, CRUD 조율 |
