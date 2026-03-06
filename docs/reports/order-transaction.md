# OrderService.createOrder() 트랜잭션 적용 보고서

## 작업 목적 및 배경

`OrderService.createOrder()`는 재고 차감 → 포인트 차감 → 주문 저장 3개의 DB 쓰기를 수행하지만 `@Transactional`이 없었다. 중간 단계 실패 시 부분 반영이 발생하여 데이터 불일치 위험이 있었다.

## 변경 사항 요약

### 1. `@Transactional` 추가
- **파일**: `src/main/java/gift/service/OrderService.java`
- `createOrder()` 메서드에 `@Transactional` 어노테이션 추가
- 재고 차감, 포인트 차감, 주문 저장이 하나의 트랜잭션으로 묶여 원자성 보장

### 2. 롤백 검증 테스트 작성
- **파일**: `src/test/java/gift/service/OrderServiceTest.java` (신규)
- Mockito 기반 단위 테스트 3개 작성:
  - 성공 시나리오: 재고 차감, 포인트 차감, 주문 저장이 모두 수행됨
  - 포인트 부족 시나리오: 예외 전파, 주문 미저장 검증
  - 재고 부족 시나리오: 예외 전파, 포인트 미차감·주문 미저장 검증

## 변경하지 않은 것

- `OptionService`, `MemberService`에 `@Transactional` 미추가 (부모 트랜잭션에 참여)
- `KakaoNotificationService` 변경 없음 (기존 try-catch로 롤백 유발 안 함)
- 기존 테스트 수정 없음

## 검증

- `./gradlew test` 전체 테스트 통과 확인
