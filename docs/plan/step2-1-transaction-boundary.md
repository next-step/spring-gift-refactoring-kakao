# Step 2-1: 트랜잭션 경계 세우기 세부 계획

## 현재 상황

### 코드 위치
`src/main/java/gift/order/OrderService.java:58-77`

### 현재 흐름
```java
public Optional<OrderResponse> createOrder(Member member, OrderRequest request) {
    return optionRepository.findById(request.optionId())
        .map(option -> {
            // 1. 재고 차감 + 저장 (개별 트랜잭션)
            option.subtractQuantity(request.quantity());
            optionRepository.save(option);

            // 2. 포인트 차감 + 저장 (개별 트랜잭션)
            int price = option.getProduct().getPrice() * request.quantity();
            member.deductPoint(price);  // ← 여기서 예외 발생 가능
            memberRepository.save(member);

            // 3. 주문 저장 (개별 트랜잭션)
            Order saved = orderRepository.save(...);

            // 4. 카카오 알림 (best-effort)
            sendKakaoMessageIfPossible(...);

            return OrderResponse.from(saved);
        });
}
```

### 문제점
- 2번에서 포인트 부족으로 `IllegalArgumentException` 발생 시
- 1번의 재고 차감은 이미 커밋됨
- **결과**: 재고만 줄고, 주문은 없는 데이터 불일치

---

## 해결 계획

### Step 1: 실패 시나리오 테스트 추가
**파일**: `src/test/java/gift/order/OrderAcceptanceTest.java`

```java
@Test
@DisplayName("실패: 포인트 부족 시 재고가 롤백되어야 한다 (트랜잭션 원자성)")
void fail_insufficientPoints_shouldRollbackStock() {
    // Given: 포인트 부족한 회원
    // When: 주문 시도
    // Then:
    //   - 주문 실패 (500)
    //   - 재고 변화 없음 (롤백 확인)
}
```

**예상 결과**: 현재는 테스트 실패 (재고가 차감되어 있음)

### Step 2: @Transactional 적용
**파일**: `src/main/java/gift/order/OrderService.java`

```java
import org.springframework.transaction.annotation.Transactional;

// TODO 주석 제거 후:
@Transactional
public Optional<OrderResponse> createOrder(Member member, OrderRequest request) {
    // 기존 로직 동일
}
```

### Step 3: 테스트 통과 확인
```bash
./gradlew test --tests "gift.order.*"
```

### Step 4: ADR 004 작성
**파일**: `docs/adr/004-transaction-boundary.md`

내용:
- 맥락: 부분 실패로 인한 데이터 불일치 가능성
- 결정: createOrder에 @Transactional 적용
- 근거: 재고 차감, 포인트 차감, 주문 생성이 하나의 논리 단위

---

## 체크리스트

- [x] Step 1: 실패 시나리오 테스트 추가
- [x] Step 2: @Transactional 적용
- [x] Step 3: 전체 테스트 통과 확인 (76개)
- [x] Step 4: ADR 004 작성
- [ ] 커밋: `feat: OrderService.createOrder 트랜잭션 경계 추가`

---

## 주의사항

### 카카오 메시지 전송
- `sendKakaoMessageIfPossible()`는 트랜잭션 내부에서 실행됨
- 외부 API 호출 실패가 트랜잭션 롤백을 유발하지 않도록 이미 try-catch로 감싸져 있음
- **추가 조치 불필요**

### 기존 TODO 주석 정리
적용 후 TODO 주석 제거:
```java
// TODO: @Transactional 추가 필요
//  - 현재 여러 save()가 개별 트랜잭션으로 실행됨
//  - 부분 실패 시나리오: 포인트 부족 시 재고만 차감되고 주문은 생성되지 않는 데이터 불일치 발생 가능
//  - 해결: @Transactional로 원자성 보장, 실패 시 전체 롤백
//  - 관련 테스트 추가 필요: 포인트 부족 시나리오
```
