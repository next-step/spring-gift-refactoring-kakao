# 테스트 전략 문서

레거시 코드 리팩토링을 위한 인수 테스트 전략을 정리한 문서입니다.

---

## 1. 검증할 행위 목록

### 선택 기준

1. **비즈니스 규칙이 있는 행위**: 단순 CRUD가 아닌, 조건부 로직이 있는 기능
2. **상태 변경이 복합적인 행위**: 여러 엔티티의 상태가 동시에 변경되는 기능
3. **인증/인가가 필요한 행위**: 권한 검증이 포함된 기능
4. **유효성 검증이 있는 행위**: 입력값에 대한 제약 조건이 있는 기능

### 도메인별 검증 행위

#### Member (회원)

| 행위 | 비즈니스 규칙 | 우선순위 |
|------|--------------|----------|
| 회원 가입 | 이메일 중복 불가 | 높음 |
| 로그인 | 이메일/비밀번호 일치 검증 | 높음 |

#### Category (카테고리)

| 행위 | 비즈니스 규칙 | 우선순위 |
|------|--------------|----------|
| 카테고리 목록 조회 | - | 낮음 |
| 카테고리 생성 | 필수값 검증 | 중 |
| 카테고리 수정 | 존재 여부 검증 | 중 |
| 카테고리 삭제 | - | 낮음 |

#### Product (상품)

| 행위 | 비즈니스 규칙 | 우선순위 |
|------|--------------|----------|
| 상품 목록 조회 (페이징) | - | 낮음 |
| 상품 단건 조회 | 존재 여부 검증 | 중 |
| 상품 생성 | **이름 검증**: 최대 15자, 허용 문자, "카카오" 제한 | **높음** |
| 상품 수정 | 이름 검증 + 존재 여부 | 높음 |
| 상품 삭제 | - | 낮음 |

#### Option (상품 옵션)

| 행위 | 비즈니스 규칙 | 우선순위 |
|------|--------------|----------|
| 옵션 목록 조회 | 상품 존재 여부 | 중 |
| 옵션 생성 | **이름 검증** + **중복 불가** | **높음** |
| 옵션 삭제 | **최소 1개 유지** | **높음** |

#### Order (주문)

| 행위 | 비즈니스 규칙 | 우선순위 |
|------|--------------|----------|
| 주문 목록 조회 | 인증 필요, 본인 주문만 | 중 |
| 주문 생성 | **인증** + **재고 차감** + **포인트 차감** | **최고** |

#### Wish (위시리스트)

| 행위 | 비즈니스 규칙 | 우선순위 |
|------|--------------|----------|
| 위시 목록 조회 | 인증 필요 | 중 |
| 위시 추가 | 인증 + **중복 시 기존 반환** | 높음 |
| 위시 삭제 | 인증 + **본인 것만 삭제 가능** | 높음 |

### 우선순위 요약

```
최고: 주문 생성 (복합 상태 변경)
높음: 회원 가입/로그인, 상품 이름 검증, 옵션 생성/삭제 규칙, 위시 권한
중:   존재 여부 검증이 있는 조회/수정
낮음: 단순 CRUD
```

---

## 2. 테스트 데이터 전략

### 준비 방법

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderAcceptanceTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

    private Member member;
    private Category category;
    private Product product;
    private Option option;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비 - Repository 직접 사용
        member = memberRepository.save(new Member("test@example.com", "password"));
        category = categoryRepository.save(new Category("테스트", "#000", "url", "desc"));
        product = productRepository.save(new Product("상품", 1000, "url", category));
        option = optionRepository.save(new Option(product, "옵션", 100));

        // 포인트 충전
        member.chargePoint(10000);
        memberRepository.save(member);
    }
}
```

### 정리 방법

```java
@AfterEach
void tearDown() {
    // 역순 삭제 (외래키 제약 고려)
    orderRepository.deleteAll();
    wishRepository.deleteAll();
    optionRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    memberRepository.deleteAll();
}
```

### 데이터 전략 원칙

1. **독립성**: 각 테스트는 자체 데이터를 생성하고 정리
2. **최소성**: 해당 테스트에 필요한 최소한의 데이터만 생성
3. **명시성**: 테스트 데이터의 의도가 명확하게 드러나도록 작성
4. **격리성**: Flyway 초기 데이터에 의존하지 않음

### 인증 토큰 획득

```java
private String getToken(String email, String password) {
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .body(new MemberRequest(email, password))
        .when()
        .post("/api/members/login")
        .then()
        .extract()
        .path("token");
}
```

---

## 3. 검증 전략

### 무엇을 검증하는가

| 검증 대상 | 검증 방법 | 예시 |
|----------|----------|------|
| HTTP 상태 코드 | RestAssured `.statusCode()` | 201 Created, 400 Bad Request |
| 응답 본문 | RestAssured `.body()` + Hamcrest | `body("name", equalTo("상품"))` |
| 데이터베이스 상태 | Repository 조회 후 단언 | `assertThat(option.getQuantity()).isEqualTo(90)` |
| 에러 메시지 | 응답 본문 검증 | `body(containsString("이미 존재하는"))` |

### Given-When-Then 패턴

```java
@Test
@DisplayName("주문 생성 시 재고가 차감된다")
void createOrder_subtractsStock() {
    // Given: 재고 100개인 옵션
    int initialQuantity = option.getQuantity(); // 100
    String token = getToken("test@example.com", "password");

    // When: 10개 주문
    RestAssured.given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(new OrderRequest(option.getId(), 10, "메시지"))
        .when()
        .post("/api/orders")
        .then()
        .statusCode(201);

    // Then: 재고 90개로 감소
    Option updated = optionRepository.findById(option.getId()).orElseThrow();
    assertThat(updated.getQuantity()).isEqualTo(initialQuantity - 10);
}
```

### 검증 우선순위

1. **상태 코드**: 기본적인 API 동작 확인
2. **비즈니스 규칙**: 핵심 로직이 정확히 동작하는지
3. **데이터 무결성**: DB 상태가 올바르게 변경되었는지
4. **에러 메시지**: 사용자에게 적절한 피드백이 전달되는지

---

## 4. 주요 의사결정 기록

### 결정 1: RestAssured 사용

**배경**: API 레벨 인수 테스트를 위한 HTTP 클라이언트 선택

**선택지**:
- `MockMvc`: Spring 내장, 서블릿 컨테이너 없이 테스트
- `RestAssured`: DSL 가독성 우수, 실제 HTTP 요청
- `WebTestClient`: WebFlux 친화적

**결정**: `RestAssured` 선택

**이유**:
- Given-When-Then DSL이 인수 테스트 스타일과 일치
- 실제 HTTP 요청으로 통합 테스트 수준의 신뢰성
- JSON 응답 검증이 직관적

**필요 의존성**:
```kotlin
testImplementation("io.rest-assured:rest-assured")
```

---

### 결정 2: 테스트 격리 방식

**배경**: 테스트 간 데이터 격리 방법 선택

**선택지**:
- `@Transactional` + Rollback: 간단하지만 실제 커밋 동작과 다름
- `@DirtiesContext`: 확실하지만 느림
- `@BeforeEach`/`@AfterEach`에서 수동 정리: 명시적이고 실제 동작과 유사

**결정**: `@BeforeEach`/`@AfterEach` 수동 정리

**이유**:
- RestAssured는 별도 스레드에서 실행되어 `@Transactional` 롤백 불가
- 실제 커밋된 데이터로 테스트하여 신뢰성 향상
- 테스트 데이터 의도가 명시적으로 드러남

---

### 결정 3: 외부 의존성 처리 (Kakao API)

**배경**: 주문 시 카카오 메시지 발송 로직 테스트 방법

**선택지**:
- 실제 API 호출: 느리고, API 할당량 소비, 불안정
- Mock 서버 (WireMock): 격리되지만 설정 복잡
- `@MockBean`: 간단하지만 통합 테스트 범위 축소

**결정**: `@MockBean`으로 `KakaoMessageClient` 모킹

**이유**:
- 카카오 메시지는 "best-effort"로 실패해도 주문은 성공
- 핵심 비즈니스 로직(재고, 포인트)에 집중
- 외부 API 의존성 제거로 테스트 안정성 확보

```java
@MockBean
private KakaoMessageClient kakaoMessageClient;

@BeforeEach
void setUp() {
    // 카카오 메시지는 항상 성공으로 가정
    doNothing().when(kakaoMessageClient).sendToMe(any(), any(), any());
}
```

---

### 결정 4: 테스트 클래스 구조

**배경**: 테스트 클래스 조직 방법

**선택지**:
- 도메인별 클래스: `MemberAcceptanceTest`, `ProductAcceptanceTest`
- 시나리오별 클래스: `주문_시나리오_테스트`, `회원가입_시나리오_테스트`
- 단일 클래스: 모든 테스트를 하나의 클래스에

**결정**: 도메인별 클래스

**이유**:
- 각 도메인의 행위가 명확히 분리됨
- 테스트 데이터 준비 로직 재사용 용이
- 파일 탐색 및 유지보수 편의성

```
src/test/java/gift/
├── member/MemberAcceptanceTest.java
├── category/CategoryAcceptanceTest.java
├── product/ProductAcceptanceTest.java
├── option/OptionAcceptanceTest.java
├── order/OrderAcceptanceTest.java
└── wish/WishAcceptanceTest.java
```

---

## 5. 테스트 구현 순서

리팩토링 안전망 확보를 위해 다음 순서로 구현:

1. **회원 (Member)**: 인증 기반, 다른 테스트의 선행 조건
2. **카테고리 (Category)**: 상품의 선행 조건, 단순 CRUD
3. **상품 (Product)**: 옵션의 선행 조건, 이름 검증 규칙
4. **옵션 (Option)**: 주문의 선행 조건, 최소 1개 규칙
5. **위시 (Wish)**: 인증 + 권한 검증
6. **주문 (Order)**: 가장 복잡한 비즈니스 로직

---

## AI 활용 기록

이 문서는 Claude Code를 활용하여 작성되었습니다.
- **활용 방식**: 컨트롤러 코드 분석을 통한 검증 행위 도출
- **분석 파일**: MemberController, CategoryController, ProductController, OptionController, OrderController, WishController
- **참고 자료**: PROJECT_STRUCTURE.md의 도메인 관계 및 비즈니스 로직
