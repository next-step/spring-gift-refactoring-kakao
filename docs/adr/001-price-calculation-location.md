# 001. 가격 계산 로직 위치 결정

- **상태**: Accepted
- **날짜**: 2026-03-05

## 맥락 (Context)

주문 총액 계산(`option.getProduct().getPrice() * quantity`)이 `OrderService`에 인라인으로 존재한다.
서비스 계층이 도메인 객체의 내부 구조(`Option → Product → price`)를 직접 탐색하고 있어
디미터 법칙(Law of Demeter)을 위반하며, 가격 계산 정책이 변경될 경우 서비스 계층을 수정해야 한다.

## 선택지 (Options)

### A. 현행 유지 — OrderService에 인라인 계산
- 장점: 변경 불필요
- 단점: 디미터 법칙 위반, 계산 로직이 도메인 외부에 산재할 위험

### B. Option 도메인에 calculateTotalPrice(int quantity) 메서드 추가
- 장점: 도메인 객체가 자신의 데이터를 이용해 직접 계산, 디미터 법칙 준수, 계산 정책 변경 시 도메인만 수정
- 단점: 메서드 하나 추가

## 결정 (Decision)

**B. Option 도메인에 메서드 추가**를 선택한다.
Option은 이미 `subtractQuantity()`라는 도메인 로직을 보유하고 있어, 가격 계산도 같은 수준에서 관리하는 것이 일관적이다.

## 결과 (Consequences)

- `OrderService`가 `option.getProduct().getPrice()`를 직접 호출하지 않게 되어 결합도 감소
- 향후 할인/수수료 등 가격 정책 변경 시 `Option` 도메인만 수정하면 됨
