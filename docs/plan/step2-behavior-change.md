# Step 2: 작동 변경 계획

## 목표
구조 변경이 완료된 상태에서 실제 작동을 개선한다.

## 원칙
- 각 작업 전 테스트로 현재 상태 검증
- 작업 후 ADR 최신화
- 커밋 단위는 논리적으로 분리

---

## 작업 목록

### 1. 트랜잭션 경계 세우기
- **대상**: `OrderService.createOrder()`
- **문제**: 여러 save()가 개별 트랜잭션으로 실행되어 부분 실패 시 데이터 불일치 발생
- **해결**: `@Transactional` 추가로 원자성 보장
- **상태**: [x] 완료
- **ADR**: [004-transaction-boundary.md](../adr/004-transaction-boundary.md)

### 2. 누락된 작동 구현
- **대상**: 주문 완료 후 위시리스트 정리
- **문제**: `WishRepository` 주입되어 있으나 미사용 (TODO 주석 존재)
- **해결**: 주문한 상품을 위시리스트에서 제거하는 로직 추가
- **상태**: [x] 완료
- **ADR**: [005-wishlist-cleanup-on-order.md](../adr/005-wishlist-cleanup-on-order.md)

### 3. 도메인 책임 되찾기 (최소 2개)
- **완료 항목**:
  - [x] `AuthenticationResolver.extractMember()`: `orElse(null)` → `Optional<Member>`
  - [x] 가격 계산 로직: Service → Domain으로 이동 (`Option.calculateTotalPrice()`)
- **상태**: [x] 완료
- **ADR**: [006-domain-responsibility.md](../adr/006-domain-responsibility.md)

---

## 작업 순서

```
1. 트랜잭션 경계 세우기
   ├── 실패 시나리오 테스트 추가
   ├── @Transactional 적용
   ├── 테스트 통과 확인
   └── ADR 004 작성

2. 누락된 작동 구현
   ├── 위시리스트 정리 테스트 추가
   ├── 로직 구현
   ├── 테스트 통과 확인
   └── ADR 005 작성

3. 도메인 책임 되찾기
   ├── 대상 선정 (최소 2개)
   ├── 각 대상별 테스트 → 구현 → 검증
   └── ADR 006 작성
```

---

## 완료 기준
- [x] 81개 테스트 통과 (기존 75개 + 신규 6개)
- [x] ADR 004, 005, 006, 007, 008, 009, 010 작성 완료
- [x] README.md 진행 상황 갱신
