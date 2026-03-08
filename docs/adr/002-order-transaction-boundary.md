# 002. 주문 생성 시 Member 조회 전략

- **상태**: Accepted
- **날짜**: 2026-03-05

## 맥락 (Context)

`OrderController`에서 `AuthenticationResolver.extractMember()`로 조회한 `Member` 엔티티를
`OrderService.createOrder(Member, OrderRequest)`로 전달하고 있다.
이 `Member`는 트랜잭션 밖에서 조회된 detached 엔티티이므로,
`member.deductPoint()` 호출 후 `memberRepository.save(member)`를 명시적으로 호출해야 변경이 반영된다.
같은 이유로 `option` 변경도 `optionRepository.save(option)`을 명시적으로 호출하고 있다.

이 구조는 다음의 문제를 갖는다:
- JPA dirty checking을 활용하지 못해 명시적 save 호출이 필요
- 트랜잭션 시작 시점의 최신 데이터가 아닌, 이전 시점의 데이터로 작업할 가능성
- 트랜잭션 경계가 명확하지 않아 데이터 정합성 보장이 어려움

## 선택지 (Options)

### A. 현행 유지 — detached Member를 전달받아 명시적 save
- 장점: 변경 불필요
- 단점: dirty checking 미활용, 트랜잭션 경계 불명확, 동시성 이슈 가능성

### B. memberId만 전달하고 트랜잭션 내부에서 조회
- 장점: managed 엔티티로 dirty checking 자동 적용, 트랜잭션 경계 명확, 최신 데이터 보장
- 단점: DB 조회 1회 추가 (이미 인증 시 조회했으므로 중복)

## 결정 (Decision)

**B. memberId만 전달하고 트랜잭션 내부에서 조회**를 선택한다.
주문 생성은 재고 차감, 포인트 차감 등 데이터 정합성이 중요한 작업이므로,
추가 조회 비용보다 트랜잭션 내 managed 엔티티 활용의 이점이 크다.

## 결과 (Consequences)

- `optionRepository.save()`, `memberRepository.save()` 명시 호출 제거 — dirty checking이 트랜잭션 커밋 시 자동 반영
- 트랜잭션 시작 시점의 최신 데이터로 작업하여 정합성 향상
- Controller → Service 인터페이스가 `Member` 엔티티 대신 `Long memberId`로 단순화
