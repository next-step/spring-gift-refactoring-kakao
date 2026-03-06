# ADR-016: 카카오 메시지 전송을 트랜잭션 밖으로 분리

## 상태
- [x] 승인됨 (2026-03)

## 맥락
리뷰어가 `OrderService.createOrder()` 내 외부 API 호출 문제를 지적했다:

```java
@Transactional
public Optional<OrderResponse> createOrder(...) {
    // DB 작업들...

    // 문제: 트랜잭션 내부에서 외부 API 호출
    sendKakaoMessageIfPossible(member, saved, option);

    return OrderResponse.from(saved);
}
```

### 문제점
1. 카카오 API 응답 지연 시 DB 커넥션을 불필요하게 점유
2. 커넥션 풀 고갈 위험
3. 트랜잭션 타임아웃 가능성

## 결정
`TransactionSynchronization.afterCommit()`을 사용해 트랜잭션 커밋 후 실행하도록 분리한다.

## 변경 사항

### OrderService.java

```java
// Before
sendKakaoMessageIfPossible(member, saved, option);

// After
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override
    public void afterCommit() {
        sendKakaoMessageIfPossible(member, saved, option);
    }
});
```

## 대안 검토

| 방안 | 장점 | 단점 |
|------|------|------|
| `TransactionSynchronization` | 단순, 추가 인프라 불필요 | 동기 실행 |
| `@TransactionalEventListener` | 선언적, 테스트 용이 | 이벤트 클래스 필요 |
| `@Async` | 완전 비동기 | 스레드 풀 관리 필요 |

현재 규모에서는 `TransactionSynchronization`이 가장 단순하고 적합하다.

## 결과
- 90개 테스트 통과
- DB 커넥션 점유 시간 감소
- 주문 처리와 알림 전송 분리

## 시퀀스 다이어그램

```
Before:
[Transaction Start] → [DB 작업] → [카카오 API 호출 (대기)] → [Transaction Commit]
                                   ↑ 커넥션 점유

After:
[Transaction Start] → [DB 작업] → [Transaction Commit] → [카카오 API 호출]
                                   ↑ 커넥션 반환
```

## 관련 ADR
- [ADR-004: 트랜잭션 경계 설정](004-transaction-boundary.md)
