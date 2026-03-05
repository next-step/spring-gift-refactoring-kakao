# Progress 0: Phase 2 작업 계획 수립

**기간**: 2026-03-03 ~ 2026-03-04

---

## 이 문서의 목적

Phase 2 구현에 앞서 "무엇을, 왜, 어떻게 바꿀 것인가"를 충분히 논의하고 기록하는 데 집중했다. PR을 올리기 전에 PR의 목적, 영향 범위, 변경 내용을 꼼꼼히 정리하려는 의도였다.

코드를 한 줄도 쓰지 않았다. 대신 현재 코드를 전수 분석하고, 부족한 점을 나열하고, 각각을 어떻게 보완할지 논의하며 구체화했다.

---

## 1. 출발점: 현재 코드에서 무엇이 부족한가

Phase 1에서 도메인별 패키지 분리(`internal/` 격리)와 인수 테스트 75개 구축을 마쳤다. 그 위에서 Phase 2 과제 요구사항(트랜잭션 경계, 누락 작동 구현, 도메인 책임 되찾기, 증거 제출)을 충족하려면 무엇이 필요한지 분석했다.

### 1-1. 크로스 도메인 Repository 폭발

Phase 1에서 `internal/` 패키지를 도입하면서 "다른 도메인의 Repository를 import하지 않는다"는 원칙을 적용한 결과, 6개 Entity에 16개 Repository가 생겼다. 같은 Entity의 Repository를 패키지마다 복제한 것이 원인이었다.

예를 들어 `Member` Entity 하나에 `MemberRepository`, `AdminMemberRepository`, `OrderMemberRepository`, `AuthMemberRepository` 4개가 존재했다. 이 중 상당수는 커스텀 메서드 없이 `JpaRepository`만 상속하는 빈 인터페이스였다.

부수적으로, 타 도메인 Repository를 통해 Entity를 직접 조작하는 코드가 발생했다. `OrderService`가 `OrderMemberRepository`로 `Member`를 조회해 `deductPoint()`를 호출하는 식이다. Member의 포인트 정책이 변경되면 `OrderService`를 수정해야 하는 Feature Envy 안티패턴이었다.

### 1-2. TODO로 남은 위시 정리

`OrderController.java`에 `// TODO: cleanup wish`라는 주석이 있었다. 주문 후 해당 상품의 위시를 제거하는 로직이 설계 의도로 남아있지만 구현되지 않았다. 심지어 `OrderWishRepository`라는 Repository까지 만들어져 있었지만 사용하는 코드는 없었다.

### 1-3. 누락된 API 엔드포인트

API 명세와 실제 구현을 대조하면서 세 가지 누락을 발견했다:

- `OptionController`에 GET, POST, DELETE만 존재하고 PUT(수정)이 없음
- `CategoryController`에 목록 조회(GET)만 있고 단건 조회(GET /{id})가 없음
- `GET /api/products`가 전체 목록만 반환하고 카테고리별 필터링이 불가능함

### 1-4. 내부 부품 테스트 부재

Phase 1에서 구축한 인수 테스트는 API 계약(HTTP 상태 코드 + 응답 본문)을 검증한다. 그러나 내부 컴포넌트가 개별적으로 올바르게 동작하는지 확인하는 테스트가 없었다. API 테스트가 실패하면 Controller, Service, Repository 중 어디가 원인인지 특정할 수 없는 상태였다.

---

## 2. 논의 과정

부족한 점을 나열한 후, 각각에 대해 "어떻게 해결할 것인가"를 논의했다. 단순한 구현 방법뿐 아니라 트레이드오프가 있는 부분은 선택지를 비교하고 근거를 기록했다.

### 2-1. 크로스 도메인 Repository → Port 패턴 (ADR-001)

기존 `AuthenticationPort`, `JwtPort`와 동일한 패턴을 다른 도메인에도 적용하기로 했다. 각 도메인이 Port 인터페이스를 패키지 루트에 공개하고, 다른 도메인은 Port를 통해서만 접근한다.

논의가 깊어진 지점이 세 곳 있었다.

**Port가 Entity를 반환해야 하는 문제.** JPA `@ManyToOne` FK 설정에 영속 Entity 객체가 필요하므로, 참조용 Port는 Entity를 반환할 수밖에 없다. 그러면 호출자가 Entity의 비즈니스 메서드를 직접 호출할 수 있게 된다. 네 가지 선택지(A: Port + 컨벤션, B: 빈혈 모델 + package-private, C: primitive FK 전환, A+B 혼합)를 비교했다. B와 A+B는 Java의 패키지 접근 제어 한계(`gift.option`과 `gift.option.internal`은 별개 패키지)로 기각했고, C는 시스템 개편 범위로 기각했다. A를 채택하되 MANDATORY 트랜잭션으로 영속성 안전을 강제하기로 했다. Entity 메서드 직접 호출은 JPA의 구조적 한계로 수용하고, 코드 리뷰로 잡기로 했다.

**Query/Command Port 분리.** 하나의 Port에 조회와 명령이 섞이면 ISP 위반이 발생한다. WishService는 Product의 `getReference()`만 필요한데 `subtractQuantity()`까지 보인다. 크로스 도메인 접근 유형에 따라 필요한 Port만 만들기로 했다. Category와 Product는 조회만 발생하므로 QueryPort만, Option과 Member는 조회+명령 모두 발생하므로 QueryPort + CommandPort를 두기로 했다.

**트랜잭션 전파 전략.** QueryPort의 `getReference()`에는 MANDATORY(호출자에 트랜잭션이 없으면 즉시 예외, detached entity 원천 차단), 기타 조회는 트랜잭션 선언 없음(단순 값 반환이므로 영속성 컨텍스트 무관), CommandPort는 REQUIRED(비트랜잭셔널 호출자인 KakaoAuthService가 외부 API 호출 동안 DB 커넥션을 점유하지 않기 위해 의도적으로 `@Transactional`을 선언하지 않으므로, MANDATORY를 쓰면 이 서비스가 동작하지 않음). 원자성 책임은 Port가 아닌 호출자에게 있다는 원칙도 확인했다.

이 논의를 `docs/adr-001-cross-domain-repository.md`에 기록했다.

### 2-2. 위시 정리 + 부수 효과 처리 (ADR-003)

위시 정리 구현 방법을 논의하면서, 카카오 알림과 함께 "주문의 부수 효과"라는 더 큰 주제로 확장되었다. 세 가지 논의가 이어졌다.

**위시 정리 트랜잭션 경계.** 먼저 사용자 관점에서 위시의 성격을 분석했다. 위시는 "나중에 사려고 찜해둔 것"이고, 주문은 Option 단위인 반면 위시는 Product 단위이다. 주문 후 위시가 정리되지 않으면 사용자가 직접 삭제하면 될 뿐, 기능 장애가 아니라 UX 불편이다. 또한 OrderService의 책임은 "주문 체결"이며 위시 정리 책임을 가지지 않아야 한다. 따라서 별도 트랜잭션으로 분리하기로 했다.

**부수 효과 실행 방식.** 기존 코드의 `// send kakao notification if possible` + `try-catch (Exception ignored)` 패턴에서 설계 의도를 읽었다. "카카오 메시지는 주 흐름이 아니다." 위시 정리도 동일한 성격이다. Controller에 try-catch로 직접 작성하는 방식(A)과 Spring ApplicationEvent로 분리하는 방식(B)을 비교했다. A의 "흐름이 한눈에 보인다"는 장점을 재검토하면서, **"주 흐름이 아닌 것이 주 흐름에 보이는 것은 가시성이 아니라 오염이다"**라는 판단에 도달했다. B를 채택했다. `ApplicationEventPublisher`는 Spring 코어에 포함되어 추가 의존성이 없고, 부수 효과 추가가 리스너 추가로 해결되어 OCP를 만족한다. 부수적으로 order → wish 의존성이 이벤트로 디커플링되어 WishCommandPort가 불필요해졌다.

**리스너 실패 처리.** 확장 가능한 실패 처리 구조를 검토했다. try-catch + 로깅(A), 공통 EventErrorHandler(B), 실패 이벤트 테이블(C) 세 선택지를 비교했다. B가 이상적이지만 리스너 2개에 추상화는 과도하고, A → B 전환 비용이 낮으므로 A를 채택했다.

이 논의를 `docs/adr-003-order-side-effects.md`에 기록했다.

### 2-3. 테스트 전략 (ADR-002)

Port 도입 후 내부 부품 테스트가 필요해졌다. 통합 테스트 중심(A), 계층별 분리(B), 계약 테스트(C)를 비교했다.

**Port → 통합 테스트 (`@DataJpaTest`).** Port의 핵심 가치는 트랜잭션 전파와 실제 DB 조작이다. MANDATORY가 트랜잭션 없이 호출되면 예외를 던지는지, REQUIRED가 자체 트랜잭션을 생성하는지, `subtractQuantity()` 후 재조회하면 값이 바뀌어 있는지 — Mock으로는 검증 불가능하다. `@DataJpaTest`의 자동 트랜잭션을 `NOT_SUPPORTED`로 해제해야 Port의 `@Transactional`만 작동하여 실제 환경과 동일한 동작을 검증할 수 있다는 결정도 함께 내렸다.

**Service → 단위 테스트 (Mockito).** Service는 여러 Port를 조합하는 오케스트레이션이다. Port가 올바르게 동작한다는 전제 하에, "올바른 인자로 올바른 Port를 호출하는가"만 검증하면 된다.

**트랜잭션 원자성 → 기존 인수 테스트.** "포인트 부족으로 주문 실패 → 재고 원복 확인" 같은 시나리오는 이미 인수 테스트에 존재한다.

이 논의를 `docs/adr-002-test-strategy.md`에 기록했다.

### 2-4. 누락 엔드포인트

Option PUT, Category GET /{id}, Product 카테고리 필터 — 이 세 가지는 트레이드오프가 크지 않아 ADR로 남기지 않았다. 대신 각각의 API 스펙(요청/응답 형식, 에러 코드)과 구현 방식을 `plan-0.md`에 기록했다. OptionRequest를 생성/수정에 공유할지도 논의했는데, Category와 Product가 이미 같은 패턴을 사용하고 있어 동일하게 구성하기로 했다.

---

## 3. 결과물

### 작업 계획 (`plan-0.md`)

PR의 전체 구조를 정의하는 문서. 각 작업에 대해 네 가지 질문에 답했다:

- a. 이 작업은 무엇을 목적으로 하는가
- b. 어떤 소스 코드가 변경되는가
- c. 소프트웨어/개발자 관점에서 무엇이 바뀌는가
- d. 사용자 관점에서 무엇이 바뀌는가

| 작업                      | 유형    | 요약                                                     |
|-------------------------|-------|--------------------------------------------------------|
| 1-1. Port 도입            | 구조 변경 | 크로스 도메인 Repository 8개 → QueryPort/CommandPort 6개로 통합   |
| 1-2. Service 호출부 단순화    | 구조 변경 | `repo.findById().orElseThrow().메서드()` → `port.메서드(id)` |
| 2-1. 주문 부수 효과           | 작동 변경 | 위시 정리 구현 + 카카오 알림을 이벤트 리스너로 이동                         |
| 2-2. Option PUT         | 작동 변경 | 옵션 수정 엔드포인트 추가                                         |
| 2-3. Category GET /{id} | 작동 변경 | 카테고리 단건 조회 추가                                          |
| 2-4. Product 필터         | 작동 변경 | 카테고리별 상품 필터링 추가                                        |
| 3. 테스트                  | 증거    | Port 통합 + Service 단위 + 인수 테스트                          |

### ADR (Architecture Decision Records)

트레이드오프가 있는 결정은 별도 문서로 기록했다.

| ADR                                           | 제목                 | 핵심 결정                                                         |
|-----------------------------------------------|--------------------|---------------------------------------------------------------|
| [ADR-001](adr-001-cross-domain-repository.md) | 크로스 도메인 Repository | Port + 컨벤션, Query/Command 분리, MANDATORY/REQUIRED 전파           |
| [ADR-002](adr-002-test-strategy.md)           | 내부 컴포넌트 테스트 전략     | 계층별 분리 (Port 통합 + Service 단위 + 인수), `@DataJpaTest` 자동 트랜잭션 해제 |
| [ADR-003](adr-003-order-side-effects.md)      | 주문 부수 효과 처리        | 트랜잭션 분리, Spring ApplicationEvent, try-catch + 로깅              |

### 커밋 전략

구조 변경과 작동 변경을 절대 섞지 않는다. 구조 변경 커밋은 기존 테스트가 모두 통과해야 하고, 작동 변경 커밋은 새 테스트와 함께 제출한다. 구체적인 커밋 분리는 `plan-0.md`의 커밋 전략 섹션에 정의되어 있다.

---

## 4. 논의에서 드러난 원칙들

계획 수립 과정에서 반복적으로 등장한 판단 기준을 정리한다.

**사용자 관점에서 먼저 생각한다.** 위시 정리 트랜잭션을 논의할 때, 기술적 선택지를 비교하기 전에 "위시가 정리되지 않으면 사용자에게 어떤 일이 벌어지는가"를 먼저 분석했다. 이 분석이 "별도 트랜잭션 분리"라는 결정의 근거가 되었다.

**기존 코드의 설계 의도를 읽는다.** `if possible` + `ignored`라는 기존 코드의 패턴에서 "이 작업은 주 흐름이 아니다"라는 의도를 읽었다. 이것이 이벤트 기반 분리의 근거가 되었다. 기존 코드가 불완전하더라도 그 안에 담긴 의도는 존중했다.

**JPA의 구조적 한계를 인정한다.** `@ManyToOne` FK에 Entity 객체가 필요하다는 한계, Java 패키지 접근 제어로 하위 패키지를 구분할 수 없다는 한계를 인정하고, 그 안에서 최선의 선택을 했다. 완벽한 캡슐화가 불가능할 때 "컨벤션으로 잡는다"는 현실적 판단을 내렸다.

**전환 비용이 낮으면 단순한 쪽을 선택한다.** 리스너 실패 처리에서 EventErrorHandler가 이상적이지만, 리스너 2개에 추상화는 과도했다. try-catch → EventErrorHandler 전환 비용이 catch 내용 교체 수준이므로 단순한 쪽을 선택했다. 과제를 위한 오버 엔지니어링은 하지 않았다.
