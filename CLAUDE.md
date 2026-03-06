# CLAUDE.md

## 프로젝트 개요

Spring Boot 3.5 + Java 21 기반의 선물하기 서비스. Gradle Kotlin DSL, Flyway, H2(테스트), MySQL(운영).

## 빌드 및 테스트

- `./gradlew test` — 단위 + 통합 테스트 실행
- `./gradlew cucumberTest` — Docker 기반 인수 테스트 실행

---

## 변경 전략

### 커밋 규율

- 구조 변경(코드 이동, 추출, 네이밍)과 작동 변경(트랜잭션, 예외 처리, 정책)을 하나의 커밋에 섞지 않는다.
- 하나의 커밋은 하나의 의도만 담는다.
- 작동 변경은 반드시 테스트(증거)와 함께 커밋한다.
- 선택지가 2개 이상이고 트레이드오프가 있었으면 커밋 메시지에 ADR(결정 근거)을 남긴다.

### 변경 전 확인

- 코드를 수정하기 전에 "왜 이 구조가 문제인지"를 먼저 정리한다.
- 커밋 전에 `git diff`로 의도하지 않은 변경이 없는지 확인한다.

---

## 코드 규칙

### @Transactional

- **조회 후 쓰기를 수행하는 Service 메서드에 `@Transactional`을 명시한다.** (조회 → 검증 → 저장/삭제 패턴)
- 단일 repository 호출만 수행하는 메서드(`save`, `deleteById`)는 생략 가능 (repository가 자체 트랜잭션 제공).
- 조회 전용 메서드는 생략 가능.
- 새로운 Service 메서드를 추가할 때 조회+쓰기 결합 여부를 확인하고 누락하지 않는다.

### 예외 처리

- `IllegalArgumentException` → 400 Bad Request (GlobalExceptionHandler)
- `NoSuchElementException` → 404 Not Found (GlobalExceptionHandler)
- `UnauthorizedException` → 401 Unauthorized (@ResponseStatus)
- `ForbiddenException` → 403 Forbidden (@ResponseStatus)
- 인증 오류와 시스템 오류를 구분한다. JWT 파싱 실패만 UnauthorizedException으로 처리하고, DB 오류 등 시스템 장애는 500으로 전파한다.
- 컨트롤러에 로컬 `@ExceptionHandler`를 두지 않고, GlobalExceptionHandler에서 통합 관리한다.

### 도메인 설계

- 비즈니스 로직(계산, 검증, 판단)은 엔티티 메서드로 구현한다.
- 부정형 판단 메서드를 제공하여 호출부에서 `!` 연산자 사용을 피한다. (예: `isNotOwnedBy()`, `hasNoKakaoAccessToken()`)
- 컬렉션 반환 시 `Collections.unmodifiableList()`로 캡슐화를 보호한다.
- Law of Demeter: `entity.getA().getB()` 체인 대신 엔티티에 편의 메서드를 둔다.

### 테스트

- 테스트 이름이 약속한 것을 실제로 검증한다. "재고가 차감된다"면 상태코드가 아니라 차감된 재고를 검증한다.
- 작동 변경의 증거는 상태를 재조회하여 관찰 가능한 방식으로 검증한다.
- 통합 테스트(`@SpringBootTest`)에서는 테스트 클래스에 `@Transactional`을 붙이지 않는다 (서비스와 트랜잭션을 공유하여 롤백 검증이 불가능해짐).
- 테스트 데이터는 테스트 간 고유하게 생성하여 간섭을 방지한다.
- H2 테스트 설정: `src/test/resources/application.properties` (MODE=MYSQL, ddl-auto=create-drop)
