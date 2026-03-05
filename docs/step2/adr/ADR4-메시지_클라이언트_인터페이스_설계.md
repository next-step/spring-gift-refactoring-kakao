# ADR-4: 메시지 클라이언트 인터페이스 설계

## 상태

승인됨

## 맥락

`OrderService`가 `KakaoMessageClient`(구체 클래스)에 직접 의존하고 있다.
알림 채널 변경이나 테스트 시 대체가 어려우므로 인터페이스를 추출한다.

현재 `OrderService`가 호출하는 메서드:
```java
kakaoMessageClient.sendToMe(String accessToken, Order order, Product product);
```

## 선택지

### 1. 최소 변경 — 현재 시그니처 유지

- `MessageClient` 인터페이스에 `sendToMe(String, Order, Product)` 그대로 선언
- 장점: 변경 범위 최소, 구조 변경만으로 완결
- 단점: 시그니처가 카카오에 종속적 (`accessToken` 파라미터)

### 2. 범용 추상화 — 시그니처 변경

- `send(NotificationMessage message)` 같은 범용 시그니처
- 장점: 채널 독립적
- 단점: 시그니처 변경은 **작동 변경이 혼입**될 위험

## 결정

**선택지 1: 최소 변경 (현재 시그니처 유지)**

## 근거

1. **원칙 2 준수**: 구조 변경에 작동 변경을 섞지 않는다
2. **최소 변경**: 인터페이스 추출만으로 의존성 역전 달성
3. **단계적 접근**: 시그니처 추상화가 필요하면 별도 Phase에서 수행

## 결과

- `MessageClient` 인터페이스를 `order` 패키지에 생성
- `KakaoMessageClient`가 `MessageClient`를 구현하도록 변경
- `OrderService`가 `MessageClient` 인터페이스에만 의존
- 런타임에는 기존과 동일하게 `KakaoMessageClient`가 주입됨
