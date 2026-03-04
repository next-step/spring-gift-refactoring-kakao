# Step 1 ADR (Architecture Decision Records)

## ADR-001: 테스트 프레임워크 선택

### 상태

결정됨

### 맥락

리팩터링 전 현재 작동을 보호하는 테스트를 작성해야 한다. 통합 테스트 방식으로 MockMvc와 RestAssured 중 선택이 필요했다.

### 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| (A) MockMvc | Spring 기본 제공, 별도 의존성 불필요 | 실제 HTTP 요청이 아님, Given/When/Then 스타일이 자연스럽지 않음 |
| **(B) RestAssured** | 실제 HTTP 요청, Given/When/Then 가독성, 실제 서버 동작 검증 | 별도 의존성 필요, 서버 부팅 시간 |

### 결정

**(B) RestAssured** — `@SpringBootTest(webEnvironment = RANDOM_PORT)` + RestAssured 조합 사용.

### 근거

- Given/When/Then 스타일이 테스트 의도를 명확하게 표현한다.
- 실제 HTTP 요청으로 서블릿 필터, 인터셉터, 인증 리졸버까지 포함한 전체 경로를 검증할 수 있다.
- 리팩터링 시 내부 구조가 바뀌어도 API의 외부 동작만 검증하므로 테스트가 깨지지 않는다.

### 영향

- `build.gradle.kts`에 `io.rest-assured:rest-assured` 의존성 추가.
- 모든 통합 테스트가 `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@LocalServerPort`를 사용한다.

---

## ADR-002: 테스트 데이터 셋업 방식

### 상태

결정됨

### 맥락

통합 테스트에서 사전 데이터를 어떻게 준비할지 결정해야 한다. 처음 AI가 생성한 코드는 Repository를 주입하여 `@BeforeEach`에서 엔티티를 직접 생성하는 방식이었다.

### 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| (A) Repository 주입 | 타입 안전, IDE 자동완성 | 내부 구조 변경 시 테스트 깨짐, 서비스 추출 리팩터링에 취약 |
| **(B) SQL 파일** | 내부 구조와 독립, 데이터 상태가 명시적 | SQL 직접 관리 필요, 스키마 변경 시 수정 필요 |

### 결정

**(B) SQL 파일** — `@Sql` 애노테이션으로 `setup-data.sql` / `cleanup.sql`을 실행한다.

### 근거

- 서비스 계층 추출(3단계)에서 Controller의 의존성이 크게 바뀔 예정인데, SQL 방식은 이에 영향을 받지 않는다.
- 테스트 데이터의 초기 상태가 SQL 파일에 명시적으로 드러나 가독성이 좋다.
- `@Sql(executionPhase = BEFORE_TEST_METHOD)` + `@Sql(executionPhase = AFTER_TEST_METHOD)`로 테스트 간 격리를 보장한다.

### 영향

- `src/test/resources/setup-data.sql`, `setup-low-point.sql`, `cleanup.sql` 파일이 추가됨.
- 모든 통합 테스트 클래스에 `@Sql` 애노테이션으로 데이터 셋업/정리를 선언한다.

---

## ADR-003: @Transactional 추가 시점

### 상태

결정됨

### 맥락

서비스 계층을 추출하면서 `@Transactional`을 함께 추가하는 것이 일반적이다. 그러나 이 프로젝트의 원칙은 "구조 변경과 작동 변경을 분리"한다.

### 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| (A) 서비스 추출과 동시에 추가 | 한 번에 완성, 일반적인 관행 | 구조 변경과 작동 변경이 섞임, 문제 발생 시 원인 분리 어려움 |
| **(B) 별도 단계에서 추가** | 구조 변경과 작동 변경 분리, 각 커밋의 의도가 명확 | 일시적으로 트랜잭션 경계가 없는 상태 |

### 결정

**(B) 별도 단계에서 추가** — Step 1에서는 서비스 추출만 수행하고, `@Transactional`은 Step 2에서 작동 변경으로 추가한다.

### 근거

- 코드 리뷰에서 서비스 추출 시 임의로 추가한 `@Transactional`이 지적되었다. `@Transactional`은 런타임 동작을 바꾸는 작동 변경이다.
- 구조 변경 커밋에서는 `git diff`로 "구조만 바뀌었다"를 즉시 확인할 수 있어야 한다.
- 일시적으로 트랜잭션 경계가 없지만, 단일 저장 메서드는 `SimpleJpaRepository` 내부 트랜잭션으로 보호되므로 치명적이지 않다.

### 영향

- Step 1의 서비스 계층에는 `@Transactional`이 없다.
- Step 2에서 다중 저장 메서드(`OrderService.createOrder`, `KakaoAuthService.loginWithKakao`)에 `@Transactional`을 추가한다.

---

## ADR-004: 서비스 추출 범위

### 상태

결정됨

### 맥락

Controller에서 Service로 로직을 추출할 때, 어디까지를 Service의 책임으로 가져갈지 결정해야 한다.

### 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| **(A) 비즈니스 로직만 추출** | Controller가 얇아지면서도 HTTP 관심사는 유지, 변경 범위 최소 | Service가 DTO를 알지 못해 변환 코드가 Controller에 남음 |
| (B) DTO 변환까지 포함 | Service가 완전한 비즈니스 계층, Controller가 매우 얇음 | Service가 HTTP 관심사(Request/Response DTO)에 의존 |

### 결정

**(A) 비즈니스 로직만 추출** — Service는 엔티티와 Repository만 다루고, DTO 변환은 Controller에 남긴다.

### 근거

- Request/Response DTO는 HTTP 계층의 관심사다. Service가 이를 알면 Web 계층과의 결합도가 높아진다.
- Controller에 `ResponseEntity` 생성과 DTO 변환이 남아 있어도 충분히 얇다.
- 과잉 추상화(별도 Mapper, Assembler 등)를 만들지 않는 원칙에 부합한다.

### 영향

- Controller는 HTTP 매핑 + 요청 바인딩 + DTO 변환 + `ResponseEntity` 생성을 담당한다.
- Service는 엔티티 조회/생성/수정/삭제와 비즈니스 검증을 담당한다.
