# OptionService + OrderService + KakaoNotificationService 추출 작업 보고서

## 작업 목적 및 배경

README.md 3-4(OptionService 추출)와 3-6(OrderService + KakaoNotificationService 추출)을 동시에 진행.
OrderService가 재고 차감을 위해 OptionService에 의존하므로, OptionService를 먼저 생성하고 OrderService가 이를 사용하는 구조로 설계했다.

## 변경 사항 요약

### 신규 파일 (3개)

| 파일 | 역할 |
|------|------|
| `src/main/java/gift/service/OptionService.java` | 옵션 CRUD + 재고 차감 비즈니스 로직 |
| `src/main/java/gift/service/KakaoNotificationService.java` | 카카오 알림 전송 (best-effort) |
| `src/main/java/gift/service/OrderService.java` | 주문 생성 + 조회 비즈니스 로직 |

### 수정 파일 (2개)

| 파일 | 변경 내용 |
|------|-----------|
| `src/main/java/gift/controller/OptionController.java` | `OptionRepository` + `ProductRepository` → `OptionService` 단일 의존, `validateName()` 제거 |
| `src/main/java/gift/controller/OrderController.java` | `OrderRepository` + `OptionRepository` + `MemberRepository` + `KakaoMessageClient` → `OrderService` 단일 의존, `sendKakaoMessageIfPossible()` 제거 |

### 의존 체인

```
OptionController → OptionService → OptionRepository, ProductService
OrderController  → OrderService  → OptionService, MemberRepository, OrderRepository, KakaoNotificationService
KakaoNotificationService → KakaoMessageClient
```

## 적용된 규칙

- 기존 서비스 추출 패턴과 동일하게 `@Service` 클래스로 추출
- Controller는 HTTP 관심사(요청/응답 변환, URI 생성)만 담당
- Service는 비즈니스 로직(검증, 엔티티 조회, 상태 변경) 담당
- `@Transactional` 미추가 (기존 동작 유지)
- cleanup wish 미구현 (사용자 요청에 따라 별도 처리)

## 검증

- `./gradlew clean build` — BUILD SUCCESSFUL, 모든 테스트 통과
- HTTP 응답 동작 변경 없음 (동일한 예외 → 동일한 상태 코드)
