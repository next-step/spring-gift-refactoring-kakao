# Step2 리팩토링 구현 플랜

## Context
Step1에서 E2E 인수 테스트(64개 시나리오) + Service 계층 추출을 완료했다. Step2의 목표는 **작동 변경을 안전하게 수행하고 증거를 남기는 것**이다. 구조 변경 커밋과 작동 변경 커밋을 분리한다.

---

## 1. (구조 변경) GlobalExceptionHandler 통합 + Controller try-catch 제거

5개 Controller에 분산된 10개의 `try-catch(NoSuchElementException → 404)` 블록을 GlobalExceptionHandler 1곳으로 통합한다. 외부 응답(404 status)은 동일하게 유지한다.

---

## 2. (구조 변경) Order 도메인에 총 가격 계산 책임 이동

`product.getPrice() * quantity` 계산이 OrderService와 KakaoMessageClient에 중복되어 있다. Order 엔티티에 `calculateTotalPrice()` 메서드를 추가하고, 호출부를 단순화한다.

---

## 3. (작동 변경) 트랜잭션 경계 수정 — 포인트 부족 시 롤백 검증

`@Disabled("트랜잭션 경계가 잘 못 설정되어 실패")` 테스트를 활성화한다. Step1에서 `@Transactional`을 이미 설정했으므로 프로덕션 코드 변경 없이 테스트만 수정한다. DB 재조회로 재고/포인트/주문 상태 변경 없음을 증명한다.

---

## 4. (작동 변경) 주문 생성 시 위시 자동 삭제

OrderController 주석 `// 6. cleanup wish`에 명시된 미구현 작동을 완성한다. 주문 완료 후 해당 상품의 위시를 삭제하고, 테스트로 증거를 제출한다.

---

## 5. (작동 변경) 에러 코드 수정

### REST API — 부적절한 상태 코드

| API | 예외 원인 | 현재 | 적절한 코드 |
|-----|----------|------|------------|
| POST /api/members/register | 이메일 중복 | 400 | **409 Conflict** |
| POST /api/members/login | 존재하지 않는 이메일 | 400 | **401 Unauthorized** |
| POST /api/members/login | 잘못된 비밀번호 | 400 | **401 Unauthorized** |
| POST /api/.../options | 중복 옵션명 | 400 | **409 Conflict** |

### Admin Controller — GlobalExceptionHandler 부작용

| 경로 | 예외 | 현재 | 적절한 코드 |
|------|------|------|------------|
| GET /admin/members/{id}/edit | NoSuchElementException | 500 HTML | **404** |
| POST /admin/members/{id}/edit | NoSuchElementException | 500 HTML | **404** |
| POST /admin/members/{id}/charge-point | NoSuchElementException | 500 HTML | **404** |
| POST /admin/members/{id}/charge-point | IllegalArgumentException | 400 JSON | **400 HTML** |
| GET /admin/products/{id}/edit | NoSuchElementException | 500 HTML | **404** |

### 대응 방향
- REST API: 커스텀 예외 도입 또는 예외 타입 세분화로 상태 코드 정확하게 매핑
- Admin: GlobalExceptionHandler 적용 범위를 `@RestController`로 한정하거나, Admin용 `@ControllerAdvice` 별도 분리

---

## 6. (구조 변경) 카카오 알림을 트랜잭션 밖으로 분리

현재 `sendKakaoMessageIfPossible`이 `@Transactional` 메서드 안에서 호출되어 외부 API 응답 시간만큼 DB 커넥션을 점유한다. `@TransactionalEventListener(AFTER_COMMIT)`로 트랜잭션 커밋 후 실행하도록 분리한다.

---

## 7. (테스트) ArchUnit 아키텍처 규칙 자동 검증

Service 계층의 구조적 일관성(`@Service` + `@Transactional` + 생성자 주입)을 자동 검증하는 ArchUnit 테스트를 추가한다. 팀원 증가 시에도 규칙 위반을 테스트 실패로 감지할 수 있다.

---

## ADR: 예외 처리 전략

1번에서 예외 처리 방식을 변경하므로, 선택지와 트레이드오프를 ADR로 기록한다.

---

## 검증 방법
- 매 커밋 후 `./gradlew test` 전체 통과 확인
- 3번: @Disabled 테스트 활성화 후 DB 상태 검증
- 4번: 새 테스트에서 위시 삭제를 DB 재조회로 확인
