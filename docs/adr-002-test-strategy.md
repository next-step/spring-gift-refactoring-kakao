# ADR-002: 내부 컴포넌트 테스트 전략

**상태**: 결정됨
**날짜**: 2026-03-04

---

## 맥락

Phase 1에서 Cucumber + RestAssured 기반 인수 테스트 75개를 구축했다. 이 테스트는 사용자 관점에서 API 계약(HTTP Method + URL + Status Code + Response Body)을 검증한다.

Phase 2에서 Port 패턴을 도입하면서 새로운 내부 컴포넌트(QueryPort, CommandPort 구현체)가 추가된다. 그러나 **내부 부품의 동작을 확인하는 테스트가 존재하지 않는다.**

```
현재:  인수 테스트(API) ──────────────── 사용자 관점 ✓
부재:  컴포넌트 테스트(Service, Port) ── 내부 부품 관점 ✗
```

### 문제

인수 테스트만으로는 두 가지가 부족하다.

1. **실패 지점 특정 불가** — API 테스트가 실패하면 Controller, Service, Port, Repository 중 어디가 원인인지 알 수 없다. 디버깅 비용이 높다.

2. **Port의 핵심 설계를 검증할 수 없다** — ADR-001에서 결정한 트랜잭션 전파 전략(QueryPort의 MANDATORY, CommandPort의 REQUIRED)이 올바르게 작동하는지 API 레벨에서는 확인할 수 없다.

---

## 검증 대상 분석

테스트를 추가하기 전에, 각 컴포넌트에서 무엇을 검증해야 하는지 분석했다.

### Port 구현체에서 검증할 것

| 검증 대상       | 예시                                                       | 단위 테스트 (Mock) | 통합 테스트 (Spring) |
|-------------|----------------------------------------------------------|:-------------:|:---------------:|
| 위임 호출       | `deductPoint()` → Repository 조회 → `member.deductPoint()` |       O       |        O        |
| 예외 변환       | `findById()` 결과 없음 → `NotFoundException`                 |       O       |        O        |
| 트랜잭션 전파     | `getReference()`에 MANDATORY, 호출자 tx 없으면 예외               |     **X**     |        O        |
| 영속성 컨텍스트    | 반환된 Entity가 managed 상태인지                                 |     **X**     |        O        |
| 실제 DB 상태 변화 | `subtractQuantity()` 후 재조회 시 값 변경 확인                     |     **X**     |        O        |

### Service에서 검증할 것

| 검증 대상       | 예시                                           | 단위 테스트 (Mock) | 통합 테스트 (Spring) |
|-------------|----------------------------------------------|:-------------:|:---------------:|
| 오케스트레이션 순서  | 재고 차감 → 포인트 차감 → 주문 저장                       |       O       |        O        |
| Port 호출 인자  | `deductPoint(memberId, price * quantity)` 계산 |       O       |        O        |
| 트랜잭션 원자성    | 포인트 차감 실패 시 재고 차감도 롤백                        |     **X**     |        O        |
| 실제 DB 상태 변화 | 주문 후 재고가 실제로 줄었는지                            |     **X**     |        O        |

핵심 차이는 **트랜잭션과 영속성 컨텍스트**이다. Port의 설계 결정(MANDATORY/REQUIRED)이 올바르게 작동하는지, 실제 DB에 의도한 변경이 반영되는지는 단위 테스트로 검증할 수 없다.

---

## 검토한 선택지

### A. 통합 테스트 중심

Port와 Service 모두 `@SpringBootTest` + H2로 테스트한다.

- 트랜잭션, 영속성, 실제 DB 상태까지 검증 가능
- 느리고, 인수 테스트와 검증 범위가 겹침
- Service 테스트에서 Port의 실제 구현까지 실행되므로 실패 지점 특정이 여전히 어려움

### B. 계층별 분리

- **Port** → 통합 테스트 (`@DataJpaTest`). 실제 DB 조작 + 트랜잭션 전파 검증.
- **Service** → 단위 테스트 (Mockito). Port를 Mock하고 오케스트레이션만 검증.
- **트랜잭션 원자성** → 기존 인수 테스트가 커버.

각 계층이 다른 것을 검증한다. 실패 시 원인을 빠르게 특정할 수 있다.

### C. 계약 테스트

Port 인터페이스를 기준으로 "이 Port는 이런 계약을 지켜야 한다"를 테스트한다. 통합 테스트의 한 형태이지만 인터페이스 중심으로 구성.

- B와 유사하나 인터페이스 기준이라는 관점 차이

---

## 결정: B. 계층별 분리

```
Port 통합 테스트     → 실제 DB 조작 + 트랜잭션 전파 검증
Service 단위 테스트  → Port Mock + 오케스트레이션 검증
인수 테스트 (기존)   → 트랜잭션 원자성 + 사용자 관점 검증
```

### 근거

1. **Port의 핵심 가치는 트랜잭션 전파와 실제 DB 조작이다.** ADR-001에서 MANDATORY/REQUIRED를 설계 결정으로 채택했다. 이것이 올바르게 작동하는지 검증하려면 실제 Spring 컨텍스트가 필요하다. Mock으로는 불가능.

2. **Service는 Port에 작업을 위임한다.** Service의 역할은 여러 Port를 조합하는 오케스트레이션이다. Port가 올바르게 동작한다는 전제 하에, Service는 "올바른 인자로 올바른 Port를 호출하는가"만 검증하면 된다. Mock이 적합.

3. **트랜잭션 원자성은 인수 테스트가 커버한다.** "포인트 부족으로 주문 실패 → 재고 원복 확인"같은 시나리오는 이미 인수 테스트에 존재한다. Service 단위 테스트에서 원자성까지 검증할 필요 없음.

4. **실패 지점을 빠르게 특정할 수 있다.** Port 통합 테스트 실패 → Port 구현 문제. Service 단위 테스트 실패 → 오케스트레이션 문제. 인수 테스트 실패 → API 계약 또는 통합 문제.

---

## `@DataJpaTest` 자동 트랜잭션 해제

### 문제: `@DataJpaTest`의 기본 동작

`@DataJpaTest`는 클래스 레벨에 `@Transactional`이 메타 어노테이션으로 걸려 있다. 각 테스트 메서드가 트랜잭션 안에서 실행되고, 끝나면 자동 롤백된다.

이 동작은 Port 테스트에서 세 가지 문제를 일으킨다.

1. **MANDATORY 테스트 불가** — 테스트가 항상 트랜잭션을 제공하므로, "트랜잭션 없을 때 예외"를 검증할 수 없다.

2. **REQUIRED 동작 왜곡** — CommandPort의 REQUIRED가 자체 트랜잭션을 생성하지 않고 테스트 트랜잭션에 참여한다. 실제 운영 환경과 다른 동작.

3. **flush 타이밍 차이** — 롤백 전제이므로 JPA가 flush를 생략할 수 있다. 원래는 실패해야 할 테스트가 성공하는 상황이 발생할 수 있다.

### 결정: `NOT_SUPPORTED`로 해제

```java
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
```

이렇게 하면 테스트 메서드 자체의 트랜잭션만 해제된다. **Port 구현체에 선언된 `@Transactional`은 그대로 작동한다.**

```
테스트 메서드 (tx 없음)
  → optionCommandPort.subtractQuantity()
    → @Transactional(REQUIRED) → 자체 tx 생성
    → 로직 수행
    → tx 커밋 (실제 DB에 반영)
  → TestRepository로 재조회 → 변경된 값 확인 가능
```

```
테스트 메서드 (tx 없음)
  → categoryQueryPort.getReference()
    → @Transactional(MANDATORY) → 호출자에 tx 없음 → 즉시 예외 ✓
```

이것이 실제 운영 환경과 동일한 동작이다.

### 트레이드오프: 자동 롤백 상실

자동 트랜잭션을 해제하면 자동 롤백도 사라진다. 테스트 간 데이터 격리를 위해 **수동 정리(cleanup)가 필요**하다. 이를 위해 통합 테스트용 DataManipulator를 구성한다.

---

## 테스트 인프라

### 기존 인프라 재사용

| 컴포넌트                                 | `@DataJpaTest`에서 사용 | 방법                                      |
|--------------------------------------|:-------------------:|-----------------------------------------|
| `TestOptionRepository` 등 (6개)        |          O          | JpaRepository → 자동 스캔                   |
| `OptionRepository` 등 (main)          |          O          | JpaRepository → 자동 스캔                   |
| Port 구현체 (`OptionCommandPortImpl` 등) |          O          | `@Import` 필요                            |
| `CucumberTestDataManipulator`        |          X          | `@Profile("acceptance-test")` → 프로필 불일치 |

`@DataJpaTest`는 `@Entity`와 `JpaRepository`를 패키지 구분 없이 전부 스캔한다. `src/test`의 TestRepository도, `src/main`의 모든 Repository도 사용 가능하다. Port 구현체만 `@Component`이므로 `@Import`로 수동 등록이 필요하다.

### 통합 테스트용 DataManipulator

`CucumberTestDataManipulator`는 `@Profile("acceptance-test")`이므로 `@DataJpaTest`에서 사용할 수 없다. 통합 테스트용 DataManipulator를 별도 구성한다.

역할:

- 테스트 데이터 생성 (`addCategory`, `addProduct`, `addOption` 등)
- 테스트 간 데이터 정리 (`initAll` — `@AfterEach`에서 호출)

기존 `DataManipulator` 인터페이스를 공유하고, 통합 테스트용 구현체를 추가하는 방식으로 구성한다.

---

## 구현 가이드

### Port 통합 테스트 구조

```java
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(OptionCommandPortImpl.class)
class OptionCommandPortTest {

    @Autowired
    private OptionCommandPort optionCommandPort;

    // 데이터 준비 + 결과 검증용
    @Autowired
    private TestOptionRepository testOptionRepo;
    @Autowired
    private TestProductRepository testProductRepo;
    @Autowired
    private TestCategoryRepository testCategoryRepo;

    @AfterEach
    void cleanup() {
        testOptionRepo.deleteAll();
        testProductRepo.deleteAll();
        testCategoryRepo.deleteAll();
    }

    @Test
    void subtractQuantity_재고_차감() {
        // Given — TestRepository로 데이터 준비
        Category category = testCategoryRepo.save(
                Category.builder().name("카테고리").color("#000000")
                        .imageUrl("url").description("설명").build()
        );
        Product product = testProductRepo.save(
                Product.builder().name("상품").price(1000)
                        .imageUrl("url").category(category).build()
        );
        Option option = testOptionRepo.save(
                Option.builder().name("옵션").quantity(10)
                        .product(product).build()
        );

        // When — Port를 통해 재고 차감
        optionCommandPort.subtractQuantity(option.getId(), 3);

        // Then — TestRepository로 실제 DB 상태 검증
        Option found = testOptionRepo.findById(option.getId()).orElseThrow();
        assertThat(found.getQuantity()).isEqualTo(7);
    }

    @Test
    void subtractQuantity_재고_부족_예외() {
        // Given
        // ...옵션 quantity=5로 준비

        // When & Then
        assertThatThrownBy(() -> optionCommandPort.subtractQuantity(optionId, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

### QueryPort MANDATORY 테스트

```java
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(CategoryQueryPortImpl.class)
class CategoryQueryPortTest {

    @Autowired
    private CategoryQueryPort categoryQueryPort;

    @Test
    void getReference_트랜잭션_없으면_예외() {
        // 테스트 메서드에 tx 없음 → MANDATORY가 즉시 예외
        assertThatThrownBy(() -> categoryQueryPort.getReference(1L))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
```

### Service 단위 테스트 구조

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepo;
    @Mock
    private OptionQueryPort optionQueryPort;
    @Mock
    private OptionCommandPort optionCommandPort;
    @Mock
    private MemberCommandPort memberCommandPort;

    @Test
    void createOrder_재고차감_후_포인트차감_호출() {
        // Given
        Long memberId = 1L;
        Long optionId = 2L;
        int quantity = 3;
        int price = 1000;

        Option option = mock(Option.class);
        Product product = mock(Product.class);
        given(optionQueryPort.getReferenceWithProduct(optionId)).willReturn(option);
        given(option.getProduct()).willReturn(product);
        given(product.getPrice()).willReturn(price);

        Order savedOrder = mock(Order.class);
        given(orderRepo.save(any())).willReturn(savedOrder);

        // When
        orderService.createOrder(memberId, new OrderRequest(optionId, quantity, "msg"));

        // Then — 올바른 인자로 Port가 호출되었는지 검증
        then(optionCommandPort).should().subtractQuantity(optionId, quantity);
        then(memberCommandPort).should().deductPoint(memberId, price * quantity);
        then(orderRepo).should().save(any(Order.class));
    }
}
```

---

## 테스트 계층 요약

| 계층             | 대상       | 방식                                                | 검증 내용                                         |
|----------------|----------|---------------------------------------------------|-----------------------------------------------|
| **Port 통합**    | Port 구현체 | `@DataJpaTest` + `NOT_SUPPORTED` + `@Import`      | 실제 DB 조작, 트랜잭션 전파 (MANDATORY/REQUIRED), 예외 처리 |
| **Service 단위** | Service  | `@ExtendWith(MockitoExtension.class)` + Mock Port | 오케스트레이션 순서, 호출 인자 정확성, 분기 로직                  |
| **인수 테스트**     | API 전체   | Cucumber + RestAssured (`@SpringBootTest`)        | 사용자 관점 계약, 트랜잭션 원자성, 상태 재조회                   |
