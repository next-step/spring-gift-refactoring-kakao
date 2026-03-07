# 주문 시 위시리스트 자동 정리

## 목적
주문 시 해당 상품이 회원의 위시리스트에 있으면 자동으로 제거하여, 이미 주문 완료된 상품이 위시리스트에 남아있는 불편함을 해소한다.

## 변경 사항

### 1. WishRepository - 삭제 메서드 추가
- `deleteByMemberIdAndProductId(Long memberId, Long productId)` 추가
- Spring Data JPA 파생 쿼리로 구현

### 2. WishService - 정리 메서드 추가
- `removeByMemberIdAndProductId(Long memberId, Long productId)` 메서드 추가
- 위시에 없으면 예외 없이 정상 진행 (deleteBy는 없으면 0건 삭제)

### 3. OrderService - 위시 정리 호출 추가
- WishService 의존성 주입 (생성자)
- `createOrder()`에서 주문 저장 후 `wishService.removeByMemberIdAndProductId()` 호출
- `option.getProduct().getId()`로 productId 획득
- 기존 @Transactional 안에 포함되어 원자성 보장

### 4. OrderControllerTest - 위시 정리 테스트 추가
- 위시리스트에 상품 추가 -> 해당 상품 주문 -> 위시리스트에서 제거됨 확인

## 설계 결정
- `deleteByMemberIdAndProductId`는 해당 row가 없어도 예외를 던지지 않으므로, 위시에 없는 상품 주문 시에도 정상 동작
- OrderService의 @Transactional 안에서 실행되므로 주문 실패 시 위시 삭제도 롤백됨
