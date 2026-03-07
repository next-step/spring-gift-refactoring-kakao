# 트랜잭션 경계 세우기 - 실행 계획 보고서

## 작업 목적 및 배경

`OrderService.createOrder()`는 3개의 독립적인 DB 저장을 수행한다:
1. 재고 차감 (`optionService.subtractQuantity`)
2. 포인트 차감 (`memberService.deductPoint`)
3. 주문 저장 (`orderRepository.save`)

현재 `@Transactional`이 없어서, 중간 단계에서 예외가 발생하면 부분 반영(partial commit)이 발생한다.
예: 재고는 차감됐는데 포인트 부족으로 실패 -> 재고만 감소된 채로 남음

## 현황 분석

### OrderService.java (수정 대상)
- `createOrder()`: `@Transactional` 없음
- `findByMemberId()`: `@Transactional` 없음
- 하위 서비스(`OptionService`, `MemberService`)에도 `@Transactional` 없음

### 호출 흐름
```
createOrder(member, optionId, quantity, message)
  ├── optionService.subtractQuantity(optionId, quantity)  // DB 저장
  ├── memberService.deductPoint(member, price)            // DB 저장
  ├── orderRepository.save(...)                           // DB 저장
  └── kakaoNotificationService.sendOrderNotification(...) // 외부 API (best-effort)
```

### 카카오 알림 (KakaoNotificationService)
- 외부 API 호출이므로 트랜잭션 롤백 불가
- 이미 try-catch로 예외를 잡아서 로그만 남기는 best-effort 패턴 적용됨
- 트랜잭션 내부에서 호출해도 문제없음 (실패해도 예외 전파 안 함)
- 단, 트랜잭션 롤백 시 알림은 이미 전송된 상태일 수 있음 (허용 가능한 트레이드오프)

## 변경 대상 파일

### 1. `src/main/java/gift/service/OrderService.java`

#### 변경 내용
- `createOrder()`에 `@Transactional` 추가
- `findByMemberId()`에 `@Transactional(readOnly = true)` 추가
- `import org.springframework.transaction.annotation.Transactional` 추가

#### 변경 전
```java
public Order createOrder(Member member, Long optionId, int quantity, String message) {
```

#### 변경 후
```java
@Transactional
public Order createOrder(Member member, Long optionId, int quantity, String message) {
```

#### 변경 전
```java
public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
```

#### 변경 후
```java
@Transactional(readOnly = true)
public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
```

### 2. `src/test/java/gift/controller/OrderControllerTest.java` (테스트 추가)

#### 추가할 테스트 케이스
- **테스트명**: `createOrderInsufficientPoints_stockRemainsUnchanged`
- **시나리오**: 포인트 부족으로 주문 실패 시, 재고가 원래대로 유지되는지 검증
- **목적**: 트랜잭션 롤백이 제대로 동작하는지 확인

```java
@Test
@DisplayName("POST /api/orders - 포인트 부족 시 주문 실패하면 재고가 원래대로 유지된다")
void createOrderInsufficientPoints_stockRemainsUnchanged() throws Exception {
    // 포인트가 적은 회원 생성
    var poorMember = new Member("poor-stock@example.com", "password");
    poorMember.chargePoint(100);
    poorMember = memberRepository.save(poorMember);
    var poorToken = jwtProvider.createToken(poorMember.getEmail());

    int originalStock = option.getQuantity(); // 100

    var request = new OrderRequest(option.getId(), 1, null);

    mockMvc.perform(post("/api/orders")
            .header("Authorization", "Bearer " + poorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    // 재고가 원래대로 유지되어야 함 (트랜잭션 롤백)
    var updatedOption = optionRepository.findById(option.getId()).orElseThrow();
    assertThat(updatedOption.getQuantity()).isEqualTo(originalStock);
}
```

**주의**: 테스트 클래스에 `@Transactional`이 있으므로, 이 테스트가 트랜잭션 롤백을 정확히 검증하려면 서비스의 트랜잭션이 중첩 트랜잭션으로 동작하는 점을 고려해야 한다. 기본 전파 설정(`REQUIRED`)에서는 테스트 트랜잭션에 참여하므로, 서비스에서 예외 발생 시 같은 트랜잭션이 롤백 마킹되어 재고 차감도 함께 롤백된다.

## 바꾸지 않는 것

- `KakaoNotificationService` - 기존 best-effort 패턴 유지
- `OptionService`, `MemberService` - 개별 트랜잭션 추가 불필요 (OrderService에서 감싸므로)
- 기존 테스트 케이스 - 동작 변경 없음

## 리스크 분석

| 항목 | 리스크 | 대응 |
|------|--------|------|
| 카카오 알림 후 롤백 | 알림 전송 후 DB 롤백 시 불일치 | 현재 구조상 알림은 DB 저장 후 호출되므로, 알림 자체가 예외를 던지지 않아 롤백 발생 안 함. 롤백은 알림 전 단계에서만 발생 |
| 테스트 트랜잭션 중첩 | 테스트의 `@Transactional`과 서비스의 `@Transactional`이 같은 트랜잭션 | 기본 전파(`REQUIRED`)로 문제 없음 |

## 커밋 메시지

```
feat(order): 트랜잭션 경계 추가로 주문 원자성 보장
```
