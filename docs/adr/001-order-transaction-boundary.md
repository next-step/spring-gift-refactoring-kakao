# ADR-001: 주문 생성 트랜잭션 경계

## Status
Accepted

## Context
OrderService.createOrder()는 세 가지 저장 작업을 수행한다:
1. 옵션 재고 차감
2. 회원 포인트 차감
3. 주문 생성

@Transactional 없이 운영되어, 중간 단계 실패 시 부분 반영이 발생할 수 있었다.
예: 포인트 차감 후 주문 저장 실패 → 포인트만 사라지고 주문 없음.

## Decision
OrderService.createOrder()에 @Transactional을 적용한다.
카카오 메시지 전송(sendKakaoMessageIfPossible)은 트랜잭션 내부에 유지한다.

## Trade-off
카카오 API 호출이 트랜잭션 내부에서 발생하므로, API 응답이 느리면 DB 트랜잭션이 길어진다.

수용하는 이유:
1. 카카오 메시지 전송은 모든 예외를 catch하므로 트랜잭션 롤백을 유발하지 않는다
2. 카카오 액세스 토큰이 있는 회원에게만 실행되므로 항상 발생하지 않는다
3. 트랜잭션 외부로 이동하려면 반환 흐름을 재구성해야 하며, 현재 단계에서는 과도하다

## Consequences
- 재고 차감, 포인트 차감, 주문 저장이 원자적으로 수행된다
- 카카오 API가 느린 경우 트랜잭션 유지 시간이 길어질 수 있다
- 향후 개선: @TransactionalEventListener를 사용해 커밋 후 메시지 전송으로 분리 가능