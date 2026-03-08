# 003. 외부 API 호출을 트랜잭션 밖으로 분리

- **상태**: Accepted
- **날짜**: 2026-03-05

## 맥락 (Context)

`KakaoAuthService.kakaoLogin()`과 `OrderService.createOrder()`에서
카카오 외부 API 호출(토큰 교환, 사용자 정보 조회, 메시지 전송)이
`@Transactional` 범위 안에서 수행되고 있다.

이 구조는 다음의 문제를 갖는다:
- 외부 API 응답 지연 시 DB 커넥션이 불필요하게 점유됨
- 외부 API 타임아웃이 트랜잭션 타임아웃으로 이어질 수 있음
- 외부 API 실패 시 이미 성공한 비즈니스 로직까지 롤백될 위험 (try-catch로 방어 중이나 구조적 보장이 아님)

## 선택지 (Options)

### A. 현행 유지 — 트랜잭션 안에서 외부 호출
- 장점: 변경 불필요
- 단점: 커넥션 점유, 타임아웃 전파, 롤백 위험

### B. 외부 API 호출을 트랜잭션 밖으로 분리
- 장점: DB 커넥션 점유 최소화, 트랜잭션과 외부 호출의 실패가 독립적
- 단점: 트랜잭션 커밋 후 외부 호출이 실패하면 알림 누락 (best-effort 허용)

### C. 이벤트 기반 비동기 처리 (@TransactionalEventListener)
- 장점: 트랜잭션 커밋 후 자동 실행, 관심사 완전 분리
- 단점: 현재 규모 대비 과도한 복잡성, 디버깅 난이도 증가

## 결정 (Decision)

**B. 외부 API 호출을 트랜잭션 밖으로 분리**를 선택한다.

- `KakaoAuthService`: `@Transactional` 제거, DB 작업은 `MemberService`에 위임하여 트랜잭션 범위를 좁힘 (4-1)
- `OrderService`: `sendKakaoMessageIfPossible()`을 `createOrder()` 밖으로 분리, Controller에서 별도 호출 (4-2)

카카오 메시지 전송은 이미 best-effort 정책이므로, 트랜잭션 밖에서 실패해도 주문에 영향을 주지 않는 것이 바람직하다.

## 결과 (Consequences)

- `KakaoAuthService`가 `@Transactional`을 갖지 않아 외부 호출 중 DB 커넥션 미점유
- `OrderService.createOrder()`의 트랜잭션 범위가 순수 비즈니스 로직으로 축소
- 카카오 메시지 전송 실패가 구조적으로 주문 트랜잭션에 영향을 줄 수 없음
- 향후 알림을 비동기로 전환할 때 Controller 호출부만 이벤트 발행으로 교체하면 됨
