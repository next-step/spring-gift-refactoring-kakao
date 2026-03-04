# Progress 1: MemberQueryPort + MemberCommandPort 구현

**기간**: 2026-03-04

---

## 이 문서의 목적

Phase 2 첫 번째 구조 변경으로, 크로스 도메인 MemberRepository 2개(`AuthMemberRepository`, `OrderMemberRepository`)를 대체할 Port 인터페이스와 구현체를 구성했다. 이 단계에서는 Port + 테스트까지만 수행하고, 기존 Repository 교체는 다음 단계에서 진행한다.

---

## 1. 변경 대상 분석

### 크로스 도메인 MemberRepository 사용 현황

| Repository              | 소속 도메인         | 사용자                      | 호출 패턴                                    |
|-------------------------|----------------|--------------------------|------------------------------------------|
| `AuthMemberRepository`  | auth/internal  | `AuthenticationPortImpl` | `findByEmail(email)` → `Member::getId`   |
| `AuthMemberRepository`  | auth/internal  | `JwtPortImpl`            | `findById(id)` → `getEmail()`            |
| `AuthMemberRepository`  | auth/internal  | `KakaoAuthService`       | `findByEmail(email)`, `save(member)`     |
| `OrderMemberRepository` | order/internal | `OrderService`           | `findById(id)` → `deductPoint(price)`    |
| `OrderMemberRepository` | order/internal | `KakaoMessagingService`  | `findById(id)` → `getKakaoAccessToken()` |

Member는 `@ManyToOne`으로 참조하는 곳이 없으므로(primitive FK `memberId` 사용) `getReference()` 메서드가 불필요했다. 모든 호출을 단순 값 반환 또는 상태 변경으로 분류할 수 있었다.

---

## 2. 구현 결과

### 생성한 파일 (7개)

**Production (5개)**

| 파일                                               | 역할                                          |
|--------------------------------------------------|---------------------------------------------|
| `gift/member/MemberQueryPort.java`               | 조회 Port 인터페이스                               |
| `gift/member/MemberCommandPort.java`             | 명령 Port 인터페이스                               |
| `gift/member/MemberInfo.java`                    | `create()` 용 DTO record                     |
| `gift/member/internal/MemberQueryAdaptor.java`   | QueryPort 구현체 (트랜잭션 없음)                     |
| `gift/member/internal/MemberCommandAdaptor.java` | CommandPort 구현체 (`@Transactional` REQUIRED) |

**Test (2개)**

| 파일                                                   | 테스트 수 |
|------------------------------------------------------|-------|
| `gift/member/internal/MemberQueryAdaptorTest.java`   | 6개    |
| `gift/member/internal/MemberCommandAdaptorTest.java` | 6개    |

### 수정한 파일 (1개)

- `MemberRepository.java` — `findByEmail(String email)` 메서드 추가. 기존에 `AuthMemberRepository`에만 있던 메서드를 본래 Repository로 이동.

### Port 메서드 구성

**MemberQueryPort** — 트랜잭션 선언 없음 (ADR-001 결정 3: 단순 값 반환)

| 메서드                            | 반환       | 호출자                                      |
|--------------------------------|----------|------------------------------------------|
| `getIdByEmail(String email)`   | `Long`   | AuthenticationPortImpl, KakaoAuthService |
| `getEmail(Long id)`            | `String` | JwtPortImpl                              |
| `getKakaoAccessToken(Long id)` | `String` | KakaoMessagingService                    |

**MemberCommandPort** — 모든 메서드 `@Transactional` REQUIRED (ADR-001 결정 3: 비트랜잭셔널 호출자 수용)

| 메서드                                                | 반환     | 호출자              |
|----------------------------------------------------|--------|------------------|
| `deductPoint(Long id, int amount)`                 | `void` | OrderService     |
| `create(MemberInfo info)`                          | `Long` | KakaoAuthService |
| `updateKakaoAccessToken(Long id, String newToken)` | `void` | KakaoAuthService |

---

## 3. 논의 사항

### 3-1. 인터페이스에 `@Transactional` 선언 여부

MemberCommandPort 인터페이스 레벨에 `@Transactional`을 선언해도 작동하는지 논의했다.

**결론: 작동은 하지만 구현체에 두는 것이 맞다.**

Spring 공식 문서에서 인터페이스의 `@Transactional`을 권장하지 않는다. Spring Boot 기본값인 CGLIB 프록시(클래스 기반)에서는 인터페이스의 `@Transactional`이 상속되지 않을 수 있다. 설계적으로도 트랜잭션 경계는 구현 세부사항이므로 인터페이스가 아닌 구현체에 두는 것이 자연스럽다.

다만 MemberCommandPort Javadoc에 "모든 메서드는 `@Transactional(REQUIRED)`로 구성됨"을 명시하여 호출자가 트랜잭션 전파 동작을 예측할 수 있도록 했다.

### 3-2. 카카오 회원 생성 테스트 제거

초안에서 `create()` 테스트를 두 개(일반 회원, 카카오 회원)로 분리했으나, 입력 조합이 다를 뿐 검증하는 행위가 동일했다. nullable 필드 조합별 저장은 `Member.builder()` + JPA `save()`가 담당하는 영역이지, Port Adaptor가 검증할 책임이 아니다. 하나로 합쳤다.

### 3-3. Port 테스트용 DataManipulator 도입 여부

인수 테스트의 `DataManipulator`와 통일성을 위해 Port 테스트용 DataManipulator를 만들지 논의했다.

**결론: 현재는 도입하지 않는다.**

Member Port 테스트는 FK 의존성이 없어 `TestMemberRepository` 직접 사용으로 충분하다. 인수 테스트의 DataManipulator는 시나리오 간 데이터 정리가 핵심 책임이고, Port 테스트는 `@DataJpaTest` + `deleteAllInBatch()`로 충분하므로 목적이 다르다.

다만 이후 Option, Product Port 테스트에서는 상황이 달라진다. `@Transactional(NOT_SUPPORTED)` 환경에서 `testProductRepo.save()` 반환 객체는 커밋은 됐지만 detached 상태다. 이 detached Product를 Option의 `@ManyToOne` 필드에 넣으면 영속 상태 오류가 발생할 수 있다. 그 시점에 `@Transactional` 헬퍼로 데이터 준비를 묶는 DataManipulator 도입을 검토한다.

---

## 4. 테스트 전략 (ADR-002 적용)

`@DataJpaTest` + `@Transactional(propagation = NOT_SUPPORTED)` + `@Import(Adaptor.class)`

- 자동 트랜잭션 해제 → Adaptor의 `@Transactional`만 작동 (실제 환경과 동일)
- `TestMemberRepository`로 데이터 준비 및 결과 재조회
- `@AfterEach`에서 `deleteAllInBatch()`로 정리
- `@DataJpaTest`가 Flyway를 자동 비활성화하고 Hibernate `create-drop`으로 스키마 관리

### 테스트 목록

**MemberQueryAdaptorTest (6개)**

| 테스트                               | 검증 내용                      |
|-----------------------------------|----------------------------|
| `testGetIdByEmail`                | 이메일로 회원 ID 조회              |
| `testGetIdByEmailNotFound`        | 없는 이메일 → NotFoundException |
| `testGetEmail`                    | ID로 이메일 조회                 |
| `testGetEmailNotFound`            | 없는 ID → NotFoundException  |
| `testGetKakaoAccessToken`         | ID로 카카오 토큰 조회              |
| `testGetKakaoAccessTokenNotFound` | 없는 ID → NotFoundException  |

**MemberCommandAdaptorTest (6개)**

| 테스트                                      | 검증 내용                            |
|------------------------------------------|----------------------------------|
| `testDeductPoint`                        | 포인트 차감 후 재조회로 확인                 |
| `testDeductPointInsufficientMemberPoint` | 잔액 부족 → IllegalArgumentException |
| `testDeductPointNotFound`                | 없는 회원 → NotFoundException        |
| `testCreate`                             | 회원 생성 후 재조회로 전체 필드 확인            |
| `testUpdateKakaoAccessToken`             | 토큰 갱신 후 재조회로 확인                  |
| `testUpdateKakaoAccessTokenNotFound`     | 없는 회원 → NotFoundException        |

---

## 5. 검증 결과

```bash
./gradlew test    # BUILD SUCCESSFUL (Port 테스트 12개 포함)
./gradlew acceptanceTest    # BUILD SUCCESSFUL (기존 75개 시나리오 통과)
```

이 단계에서는 기존 코드의 호출부를 수정하지 않았으므로 (MemberRepository에 메서드 추가만) 기존 테스트에 영향이 없다.

---

## 6. 다음 단계

같은 패턴으로 Category, Product, Option Port를 순차 구성한다 (Port 인터페이스 + 구현체 + Port 통합 테스트). 모든 Port 구성이 완료되면, 기존 크로스 도메인 Repository를 Port로 교체하면서 Service 단위 테스트(Mock Port)를 함께 추가한다.
