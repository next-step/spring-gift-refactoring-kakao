# ADR-005: 주문 완료 시 위시리스트 정리

## 상태
- [x] 승인됨 (2026-03)

## 맥락
- `OrderService`에 `WishRepository`가 주입되어 있었으나 미사용 상태 (TODO 주석만 존재)
- 주문 완료 후 해당 상품이 위시리스트에 남아있는 문제

## 선택지

### 1. 위시리스트 유지
- 사용자가 직접 삭제하도록 함
- 단점: 이미 구매한 상품이 위시리스트에 남아있어 혼란

### 2. 주문 완료 시 자동 삭제 (선택)
- 주문한 상품을 위시리스트에서 자동 제거
- 장점: 사용자 경험 개선, 위시리스트가 "아직 구매하지 않은 상품" 목록으로 유지됨

## 결정
주문 저장 후, 카카오 알림 전에 위시리스트 정리 로직 추가

```java
// cleanup wishlist
wishRepository.findByMemberIdAndProductId(member.getId(), option.getProduct().getId())
    .ifPresent(wishRepository::delete);
```

## 근거
- 주문한 상품은 더 이상 위시리스트에 있을 필요 없음
- `@Transactional` 내에서 실행되어 주문 실패 시 위시리스트 삭제도 롤백됨

## 결과
- 77개 테스트 통과
- 상태 재조회로 검증: 주문 전 위시리스트 존재 확인 → 주문 후 삭제 확인

## 관련 ADR
- [ADR-004: 트랜잭션 경계 설정](004-transaction-boundary.md)
