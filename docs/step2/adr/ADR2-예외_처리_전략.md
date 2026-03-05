# ADR-2: 예외 처리 전략

## 상태

승인됨

## 맥락

Phase 3에서 Service 계층의 `orElse(null)` + Controller null 체크 패턴을 예외 기반으로 전환한다.
이때 "엔티티를 찾을 수 없는 경우" 어떤 예외 타입을 사용할지 결정해야 한다.

현재 프로젝트에서 사용 중인 예외:
- `IllegalArgumentException`: 비즈니스 검증 실패 (GlobalExceptionHandler → 400)
- `NoSuchElementException`: Admin 전용 메서드에서 엔티티 미조회 시 사용 중

## 선택지

### 1. `NoSuchElementException` (java.util)

- 장점: 이미 프로젝트에서 사용 중 (`ProductService.createFromAdmin`), 새 클래스 불필요
- 단점: 범용적이라 도메인별 구분 어려움

### 2. 커스텀 `NotFoundException` extends RuntimeException

- 장점: 명확한 의도 전달, 도메인별 서브클래스 가능
- 단점: 새 예외 클래스 생성 필요, 현재 단계에서 과도한 추상화

### 3. `ResponseStatusException(HttpStatus.NOT_FOUND)`

- 장점: Spring이 자동 처리, 별도 핸들러 불필요
- 단점: Service 계층이 HTTP 개념에 의존 — 계층 분리 원칙 위반

### 4. `EntityNotFoundException` (JPA)

- 장점: JPA 표준
- 단점: JPA에 강하게 결합, Service가 영속성 기술에 의존

## 결정

**`NoSuchElementException`을 사용한다.**

## 근거

1. **최소 변경 원칙**: 이미 프로젝트에서 동일 목적으로 사용 중이므로 새 클래스 불필요
2. **일관성**: `ProductService.createFromAdmin()`이 이미 `NoSuchElementException`을 사용 — 통일
3. **계층 분리**: Service가 HTTP 개념에 의존하지 않음
4. **적정 수준**: 현재 규모에서 커스텀 예외 계층은 과도함. 필요시 추후 전환 가능

## 결과

- `GlobalExceptionHandler`에 `NoSuchElementException` → 404 핸들러 추가
- 핸들러 적용 범위를 `@RestController`로 제한하여 Admin 페이지(`@Controller`) 미영향
- 추후 도메인별 세분화가 필요하면 커스텀 예외로 전환 가능
