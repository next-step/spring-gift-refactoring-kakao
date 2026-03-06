---
name: acceptance-test
description: 인수테스트 작성법 스킬. 사용자가 "인수테스트 작성", "API 테스트", "MockMvc 테스트", "테스트 추가", "테스트 작성" 등을 언급하면 이 스킬을 사용한다. 리팩터링 전 안전망 확보를 위한 테스트 구조, 네이밍 규칙, MockMvc/RestAssured 패턴을 정의한다.
---

# 인수테스트(Acceptance Test) 작성 가이드

인수테스트는 리팩터링의 **안전망**이다.
코드 구조를 변경해도 기존 동작이 깨지지 않음을 보장한다.

---

## 1. 테스트 목적

- **리팩터링 보호**: 구조 변경 후에도 API 동작이 동일함을 검증
- **회귀 방지**: 기존 기능이 의도치 않게 변경되는 것을 방지
- **문서 역할**: API의 기대 동작을 코드로 명시

---

## 2. 테스트 파일 구조

```
src/test/java/gift/
├── product/
│   └── ProductAcceptanceTest.java
├── category/
│   └── CategoryAcceptanceTest.java
├── order/
│   └── OrderAcceptanceTest.java
├── option/
│   └── OptionAcceptanceTest.java
├── wish/
│   └── WishAcceptanceTest.java
├── member/
│   └── MemberAcceptanceTest.java
└── auth/
    └── KakaoAuthAcceptanceTest.java
```

도메인 패키지 하위에 `*AcceptanceTest.java` 파일을 생성한다.

---

## 3. 클래스 구조 템플릿

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ProductAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("상품 목록을 조회한다")
    void getProducts() throws Exception {
        // given - when - then
        mockMvc.perform(get("/api/products")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk());
    }
}
```

### 어노테이션 기본 구성
| 어노테이션 | 용도 |
|---|---|
| `@SpringBootTest(webEnvironment = RANDOM_PORT)` | 전체 애플리케이션 컨텍스트 로드 |
| `@AutoConfigureMockMvc` | MockMvc 자동 구성 |
| `@Transactional` | 테스트 후 DB 롤백 (필요 시) |
| `@DisplayName` | 테스트 의도를 한글로 명시 |

---

## 4. 네이밍 규칙

### 클래스명
```
{도메인}AcceptanceTest
```
예: `ProductAcceptanceTest`, `OrderAcceptanceTest`

### 메서드명
```java
// 패턴: 행위를 한글 DisplayName으로 표현
@DisplayName("상품을 등록한다")
void createProduct()

@DisplayName("존재하지 않는 상품을 조회하면 404를 반환한다")
void getProduct_NotFound()
```

- `@DisplayName`에 한글로 기대 동작을 명시
- 실패 케이스는 `_실패사유` 접미사 사용

---

## 5. 테스트 패턴

### 5-1. GET 요청 (목록 조회)
```java
@Test
@DisplayName("상품 목록을 페이지네이션으로 조회한다")
void getProducts() throws Exception {
    mockMvc.perform(get("/api/products")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
}
```

### 5-2. GET 요청 (단건 조회)
```java
@Test
@DisplayName("상품을 단건 조회한다")
void getProduct() throws Exception {
    mockMvc.perform(get("/api/products/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
}
```

### 5-3. POST 요청 (생성)
```java
@Test
@DisplayName("새로운 카테고리를 생성한다")
void createCategory() throws Exception {
    var request = new CategoryRequest("테스트", "#000000", "http://img.url", "설명");

    mockMvc.perform(post("/api/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
}
```

### 5-4. PUT 요청 (수정)
```java
@Test
@DisplayName("카테고리를 수정한다")
void updateCategory() throws Exception {
    var request = new CategoryRequest("수정됨", "#FFFFFF", "http://new.url", "수정 설명");

    mockMvc.perform(put("/api/categories/{id}", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
}
```

### 5-5. DELETE 요청 (삭제)
```java
@Test
@DisplayName("카테고리를 삭제한다")
void deleteCategory() throws Exception {
    mockMvc.perform(delete("/api/categories/{id}", 1L))
        .andExpect(status().isNoContent());
}
```

### 5-6. 인증이 필요한 API
```java
@Test
@DisplayName("인증된 사용자가 위시리스트를 조회한다")
void getWishes() throws Exception {
    String token = obtainAccessToken();

    mockMvc.perform(get("/api/wishes")
            .header("Authorization", "Bearer " + token)
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk());
}
```

인증 토큰이 필요한 API는 헬퍼 메서드로 토큰을 발급받아 사용한다.

---

## 6. 테스트 데이터 전략

### Flyway 시드 데이터 활용
- `V2__Insert_default_data.sql`에 이미 시드 데이터가 존재한다.
- 테스트에서는 시드 데이터를 기반으로 조회/수정/삭제를 검증한다.
- 생성 테스트는 새 데이터를 직접 요청한다.

### H2 인메모리 DB
- `test` 프로파일에서 H2를 사용하여 테스트 격리를 보장한다.
- 각 테스트 클래스가 독립적으로 실행 가능해야 한다.

---

## 7. 도메인별 테스트 범위

| 도메인 | 엔드포인트 | 테스트 우선순위 |
|---|---|---|
| `category` | GET, POST, PUT, DELETE `/api/categories` | 높음 (의존성 없음) |
| `product` | GET, POST, PUT, DELETE `/api/products` | 높음 |
| `option` | GET, POST, DELETE `/api/products/{id}/options` | 중간 (product 의존) |
| `member` | POST `/api/members/register`, `/api/members/login` | 높음 |
| `wish` | GET, POST, DELETE `/api/wishes` | 중간 (인증 필요) |
| `order` | GET, POST `/api/orders` | 낮음 (외부 API 의존) |
| `auth` | GET `/api/auth/kakao/*` | 낮음 (Kakao 외부 의존) |

**작성 순서**: 의존성이 적은 도메인부터 → `category` → `member` → `product` → `option` → `wish` → `order` → `auth`

---

## 8. 외부 의존성 처리

Kakao API 등 외부 서비스에 의존하는 테스트는 Mock을 사용한다:

```java
@MockBean
private KakaoMessageClient kakaoMessageClient;

@BeforeEach
void setUp() {
    // 외부 API 호출을 Mock 처리
    given(kakaoMessageClient.sendMessage(any(), any()))
        .willReturn(ResponseEntity.ok().build());
}
```

---

## 9. 검증 체크리스트

테스트 작성 완료 후 확인:
- [ ] `./gradlew test` 전체 통과
- [ ] 각 테스트가 독립적으로 실행 가능 (순서 무관)
- [ ] 외부 API 의존성이 Mock 처리됨
- [ ] 정상 흐름(Happy Path)이 모두 커버됨
- [ ] `@DisplayName`으로 테스트 의도가 명확히 표현됨
