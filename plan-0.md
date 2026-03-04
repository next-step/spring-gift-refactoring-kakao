## PR이 소스 코드에 얼마나 영향을 미칠지 고려하기

- a. 이 PR은 무엇을 목적으로 하는가
- b. 이 PR이 적용되면 변경되는 소스 코드는 무엇인가
- c. 이 PR이 적용되면 소프트웨어 & 개발자 관점에서 무엇이 바뀌는가
- d. 이 PR이 적용되면 사용자 관점에서 무엇이 바뀌는가

---

## 과제 요구사항 ↔ 작업 매핑

| 과제 요구사항            | 해당 작업                                                  | 변경 유형 |
|--------------------|--------------------------------------------------------|-------|
| 트랜잭션 경계 세우기        | 작업 2-1: 주문 흐름에 위시 정리를 포함하고 트랜잭션 경계 설정                  | 작동 변경 |
| 누락된 작동 구현          | 작업 2: 위시 정리 TODO, Option PUT, Category GET, Product 필터 | 작동 변경 |
| 도메인 책임 되찾기 (최소 2개) | 작업 1: Port 패턴 확장 (중복 제거 + 호출부 단순화)                     | 구조 변경 |
| 증거 제출              | 작업 3: 인수 테스트 + 상태 검증 테스트 + 내부 컴포넌트 테스트                 | -     |
| ADR                | 작업 1, 2에서 트레이드오프가 있는 결정 시 작성                           | -     |

---

### 1. 도메인별 Port 추가 (구조 변경 — 도메인 책임 되찾기)

`AuthenticationPort`처럼 도메인 작업을 요청할 수 있는 Port를 구성한다.
이는 `entity 생성`, `entity 특정 값 제공`, `entity 상태 변경` 같은 작업을 포함하고, `entity에 대한 비즈니스 로직 수행` 또한 포함된다.

현재 8개의 크로스 도메인 Repository가 존재한다. 각 도메인이 다른 도메인의 Entity를 조회하기 위해 자기 `internal/` 안에 별도 Repository를 만들고 있어 중복이 폭발한다.

| 소속 도메인             | Repository                       | 참조하는 Entity (타 도메인) |
|--------------------|----------------------------------|---------------------|
| `order/internal`   | `OrderMemberRepository`          | `Member`            |
| `order/internal`   | `OrderOptionRepository`          | `Option`            |
| `order/internal`   | `OrderWishRepository`            | `Wish`              |
| `wish/internal`    | `WishProductRepository`          | `Product`           |
| `option/internal`  | `OptionProductRepository`        | `Product`           |
| `product/internal` | `ProductCategoryRepository`      | `Category`          |
| `product/admin`    | `AdminProductCategoryRepository` | `Category`          |
| `auth/internal`    | `AuthMemberRepository`           | `Member`            |

이들은 다른 도메인의 `internal/` 패키지를 직접 import하지는 않지만, 타 도메인 Entity를 자기 Repository로 직접 조회·조작할 수 있어 도메인 경계가 무의미해진다.
예를 들어 `OrderMemberRepository`로 `Member` 엔티티를 직접 조회해 `deductPoint()`를 호출하면, Member 도메인이 제공하는 비즈니스 규칙을 우회할 수 있다.

**도메인 책임 되찾기 — 개선 항목 (최소 2개)**

---

#### 개선 1: 크로스 도메인 Repository 중복 제거

**결정됨 — ADR-001 참고 (`docs/adr-001-cross-domain-repository.md`)**

Port를 QueryPort(조회)와 CommandPort(명령)로 분리한다. 크로스 도메인 접근 유형에 따라 필요한 Port만 만든다.

| 도메인          | QueryPort | CommandPort | 대체 대상                                                         |
|--------------|:---------:|:-----------:|---------------------------------------------------------------|
| **Category** |     O     |      X      | `ProductCategoryRepository`, `AdminProductCategoryRepository` |
| **Product**  |     O     |      X      | `WishProductRepository`, `OptionProductRepository`            |
| **Option**   |     O     |      O      | `OrderOptionRepository`                                       |
| **Member**   |     O     |      O      | `OrderMemberRepository`, `AuthMemberRepository`               |
| **Wish**     |     —     |      —      | `OrderWishRepository`는 이벤트로 대체되어 삭제 (ADR-003)                 |

**Port 인터페이스 설계:**

```java
// === Category ===
public interface CategoryQueryPort {
    Category getReference(Long categoryId);  // Product 생성 시 FK
}

// === Product ===
public interface ProductQueryPort {
    Product getReference(Long productId);  // Wish, Option 생성 시 FK
}

// === Option ===
public interface OptionQueryPort {
    Option getReference(Long optionId);              // Order 생성 시 FK
    Option getReferenceWithProduct(Long optionId);   // 가격 조회용
}
public interface OptionCommandPort {
    void subtractQuantity(Long optionId, int quantity);
}

// === Member ===
public interface MemberQueryPort {
    Long findByEmail(String email);
    String getKakaoAccessToken(Long id);
    String getEmail(Long id);
}
public interface MemberCommandPort {
    void deductPoint(Long id, int amount);
    Long create(MemberInfo info);
    void updateKakaoAccessToken(Long id, String newToken);
}
```

**트랜잭션 전파 전략:**

| Port 유형     | 메서드              | 트랜잭션        | 근거                                     |
|-------------|------------------|-------------|----------------------------------------|
| QueryPort   | `getReference()` | `MANDATORY` | 영속 Entity 반환 → detached 방지 (fast-fail) |
| QueryPort   | 기타 조회            | 없음          | 단순 값 반환 → 영속성 컨텍스트 무관                  |
| CommandPort | 모든 메서드           | `REQUIRED`  | 비트랜잭셔널 호출자(KakaoAuthService) 수용        |

원자성 책임은 Port가 아닌 호출자에게 있다.

**적용 후 Repository 변화:**

```
Before (16개 Repository):
  Member: 4개, Category: 3개, Product: 4개, Option: 2개, Wish: 2개, Order: 1개

After (6개 Repository + 6개 Port):
  각 도메인 1개 Repository (internal/) + 필요한 Port (패키지 루트)
  AdminProductRepository, AdminMemberRepository는 같은 도메인의 main Repository로 통합
```

- a. 크로스 도메인 Repository 중복을 제거하기 위한 목적
- a. 타 도메인 Entity에 대한 접근을 Port 인터페이스로 통일해 도메인 경계를 강화하기 위한 목적


- b. 크로스 도메인 Repository 8개 삭제 → QueryPort/CommandPort 인터페이스 + 구현체로 대체
- b. 각 도메인 Service가 타 도메인 Repository 대신 Port 인터페이스에 의존하도록 변경
- b. Port 구현체는 해당 도메인의 `internal/` Repository를 감싸서 구현


- c. 타 도메인 Entity에 대한 접근이 Port로 통일되어, 도메인이 자기 Entity에 대한 비즈니스 규칙을 직접 관리할 수 있다
- c. Repository 인터페이스 수가 줄어든다 (크로스 도메인 8개 → Port 6개)
- c. QueryPort로 받은 Entity의 비즈니스 메서드 직접 호출은 컨벤션으로 금지 (JPA 구조적 한계로 컴파일 차단 불가)


- d. 없음. (내부 구조 변경이므로 외부 작동 변화 없음)

---

#### 개선 2: Service 호출부 단순화

Port를 도입하면 Service 코드에서 `repo.findById().orElseThrow().메서드()` 패턴이 `port.메서드(id)` 호출로 단순화된다.

**OrderService — 포인트 차감**:

```java
// Before: OrderService가 OrderMemberRepository로 Member를 직접 조회·조작
memberRepo.findById(memberId)
        .

orElseThrow(NotFoundException::memberNotFound)
        .

deductPoint(price);

// After: MemberCommandPort가 조회 + 예외 + 차감을 캡슐화
memberCommandPort.

deductPoint(memberId, price);
```

**KakaoMessagingService — 카카오 토큰 조회**:

```java
// Before: OrderMemberRepository로 Member를 직접 조회해서 토큰 추출
String kakaoAccessToken = memberRepo.findById(memberId)
                .orElseThrow(NotFoundException::memberNotFound)
                .getKakaoAccessToken();

// After: MemberQueryPort가 조회 + 예외를 캡슐화
String kakaoAccessToken = memberQueryPort.getKakaoAccessToken(memberId);
```

**JwtPortImpl — 이메일 조회**:

```java
// Before: AuthMemberRepository로 Member를 직접 조회해서 이메일 추출
Member find = memberRepo.findById(memberId)
                .orElseThrow(NotFoundException::memberNotFound);
String memberEmail = find.getEmail();

// After:
String memberEmail = memberQueryPort.getEmail(memberId);
```

- a. Service가 타 도메인의 조회·예외처리 세부사항을 알 필요 없이, 비즈니스 의도만 표현하도록 하기 위한 목적


- b. `OrderService`: `memberRepo.findById().orElseThrow().deductPoint()` → `memberCommandPort.deductPoint(memberId, price)`
- b. `KakaoMessagingService`: `memberRepo.findById().orElseThrow().getKakaoAccessToken()` → `memberQueryPort.getKakaoAccessToken(memberId)`
- b. `JwtPortImpl`: `memberRepo.findById().orElseThrow().getEmail()` → `memberQueryPort.getEmail(memberId)`
- b. `AuthenticationPortImpl`: `memberRepo.findByEmail().map(Member::getId)` → `memberQueryPort.findByEmail(email)`
- b. `KakaoAuthService`: `memberRepo.findByEmail()`, `memberRepo.save()` → `memberQueryPort.findByEmail()`, `memberCommandPort.create()`, `memberCommandPort.updateKakaoAccessToken()`


- c. Service 코드가 "무엇을 하는가"(의도)만 표현하고, "어떻게 찾는가"(조회+예외)는 Port 내부로 숨겨진다
- c. 조회 → 예외 → 조작 패턴의 중복이 Port 구현체 한 곳으로 집중된다
- c. Service의 import가 줄어든다 (타 도메인 Entity import 불필요)


- d. 없음. (호출 코드 형태만 변경, 외부 작동 동일)

---

### 2. 누락된 작동 구현 + 트랜잭션 경계 (작동 변경)

기존 코드에 의도가 남아 있었지만 구현되지 않은 작동을 완료하고, API 명세 중 누락된 엔드포인트를 추가한다.

---

#### 2-1. 주문 부수 효과 처리 (위시 정리 + 카카오 알림 구조 개선)

**결정됨 — ADR-003 참고 (`docs/adr-003-order-side-effects.md`)**

`OrderController.java:45-66`에 주문 흐름이 주석으로 정의되어 있으나 6단계(위시 정리)가 미구현이고, 7단계(카카오 알림)는 Controller에 try-catch로 직접 작성되어 있다.

```
현재 OrderController.createOrder():
  1. 인증 확인                    ← 핵심
  2. orderService.createOrder()   ← 핵심 (@Transactional)
     - 옵션 검증
     - 재고 차감
     - 포인트 차감
     - 주문 저장
  3. // TODO: cleanup wish        ← 부수 효과 (미구현)
  4. kakaoMessaging.send()        ← 부수 효과 (try-catch ignore)
  5. 응답 반환                    ← 핵심
```

##### 논의 1: 위시 정리 트랜잭션 경계

사용자 관점에서 위시를 분석했다. 위시는 "나중에 사려고 찜해둔 것"이다. 주문 후 위시가 정리되지 않으면:

| 시나리오                     | 결과              | 심각도        |
|--------------------------|-----------------|------------|
| 이미 산 상품이 위시에 남아있음        | 사용자가 직접 삭제하면 됨  | 낮음 — UX 불편 |
| 같은 상품의 다른 옵션을 더 사고 싶은 경우 | 위시가 남아있으면 자연스러움 | 해당 없음      |

**위시 미정리는 불편함(UX)이지 기능 장애가 아니다.** 위시 정리 실패가 주문을 롤백시킬 만큼 심각하지 않다. 또한 OrderService의 책임은 "주문 체결"이며 위시 정리 책임을 가지지 않아야 한다.

→ **결정: 위시 정리는 주문 트랜잭션에 포함하지 않는다.**

##### 논의 2: 부수 효과 실행 방식

기존 코드에 설계 의도가 드러나 있다:

```java
// send kakao notification if possible   ← "가능하면" 보낸다
try{
        kakaoMessagingService.send(...);
}catch(
Exception ignored){            ←
실패해도 무시
}
```

`if possible` + `ignored`는 "카카오 메시지는 주 흐름이 아니다"라는 의도이다. 위시 정리도 동일한 성격이다. 현재 Controller에 부수 효과 코드가 8줄 이상 차지하며 주 흐름(인증 → 주문 → 응답)을 방해하고 있다.

**"주 흐름이 아닌 것이 주 흐름에 보이는 것은 가시성이 아니라 오염이다"**라는 판단 하에 Spring ApplicationEvent로 분리하기로 결정했다.

- `ApplicationEventPublisher`는 Spring 코어에 포함되어 추가 의존성 없음
- 부수 효과 추가 = 리스너 추가 (기존 코드 수정 없음, OCP)
- order → wish 의존성이 이벤트로 디커플링되어 WishCommandPort 불필요

→ **결정: Spring ApplicationEvent로 부수 효과 분리**

##### 논의 3: 리스너 실패 처리

리스너 예외 발생 시 확장 가능한 구조를 검토했다:

| 선택지                     | 설명                    | 판단              |
|-------------------------|-----------------------|-----------------|
| A. try-catch + 로깅       | 각 리스너에서 catch, 로그만 남김 | **채택**          |
| B. 공통 EventErrorHandler | 실패 처리 정책을 한 곳에 모음     | 리스너 2개에 추상화는 과도 |
| C. 실패 이벤트 테이블           | DB 저장 + 스케줄러 재처리      | 현재 규모에 불필요      |

A → B 전환 비용이 낮으므로(catch 내용만 교체) 필요 시 확장 가능하다.

→ **결정: try-catch + 로깅**

##### 변경 후 구조

```
OrderService.createOrder() (@Transactional):
  옵션 검증 → 재고 차감 → 포인트 차감 → 주문 저장
  → OrderCreatedEvent 발행

@TransactionalEventListener(phase = AFTER_COMMIT):
  WishCleanupService       ← 별도 @Transactional, try-catch + 로깅
  KakaoNotificationListener ← try-catch ignore (기존과 동일)

Controller:
  인증 → orderService.createOrder() → 응답 반환 (부수 효과 코드 없음)
```

- a. 미구현 위시 정리를 완료하고, 부수 효과를 주 흐름에서 분리하기 위한 목적
- a. Controller가 핵심 책임(인증 + 위임 + 응답)만 갖도록 정리하기 위한 목적


- b. `OrderService`: `ApplicationEventPublisher` 의존성 추가, `OrderCreatedEvent` 발행
- b. `order/OrderCreatedEvent.java`: 새 이벤트 record (memberId, productId, orderId)
- b. `wish/internal/WishCleanupService.java`: 새 리스너 (`@TransactionalEventListener` + `@Transactional`)
- b. `order/internal/KakaoNotificationEventListener.java`: 기존 Controller의 카카오 알림 코드를 리스너로 이동
- b. `OrderController`: TODO 주석 제거, 카카오 알림 코드 제거, `OrderMessageBuilder`·`KakaoMessagingService` 의존성 제거
- b. `OrderWishRepository` 삭제 (이벤트로 대체되어 불필요)


- c. Controller에서 부수 효과 코드가 전부 사라지고, `OrderMessageBuilder`·`KakaoMessagingService` 의존성이 제거됨
- c. order 도메인이 wish 도메인을 모름 (이벤트로 디커플링). WishCommandPort 불필요
- c. 부수 효과 추가 시 기존 코드 수정 없이 리스너만 추가하면 됨 (OCP)


- d. 주문 시 해당 상품에 대한 위시가 자동으로 제거됨 (실패 시 위시가 남을 수 있으나 기능 장애 아님)

---

#### 2-2. Option 수정 엔드포인트 추가 (PUT)

현재 `OptionController`에 GET, POST, DELETE만 존재. PUT(수정)이 없음.

**API 스펙:**

```
PUT /api/products/{productId}/options/{optionId}

Request Body:
{
    "name": "옵션 이름",       // NotBlank, Size(max=50), Pattern(영문/한글/숫자/특수문자)
    "quantity": 100            // Min(1), Max(99_999_999)
}

Response: 200 OK
{
    "id": 1,
    "name": "옵션 이름",
    "quantity": 100
}

Error:
  404 — productId 또는 optionId에 해당하는 엔티티 없음
  400 — Validation 실패, 동일 상품 내 옵션 이름 중복
```

**구현 방식:**

- `OptionRequest`를 생성/수정에 공유 (Category, Product와 동일 패턴. 필드가 동일하므로 별도 Request 불필요)
- `OptionService.updateOption(productId, optionId, request)`: 옵션 조회 → 상품 소속 검증 → 중복 이름 검증 → `option.update(name, quantity)`
- `Option` 엔티티에 `update(name, quantity)` 메서드 추가 (현재 없음)
- 기존 `addOption()`의 중복 이름 검증 로직을 `updateOption()`에서도 공유

- a. API 명세 기준 누락된 CRUD 엔드포인트를 채우기 위한 목적


- b. `OptionController`에 PUT 엔드포인트 추가
- b. `OptionService`에 `updateOption()` 메서드 추가
- b. `Option` 엔티티에 `update(name, quantity)` 메서드 추가


- c. `OptionService`에 update 메서드가 추가되면서 중복 이름 검증 로직이 create/update에서 공유됨


- d. 옵션 이름, 수량을 수정할 수 있음

---

#### 2-3. Category 단건 조회 엔드포인트 추가 (GET /{id})

현재 `CategoryController`에 목록 조회(GET), 생성(POST), 수정(PUT), 삭제(DELETE)가 있으나 단건 조회가 없음.

**API 스펙:**

```
GET /api/categories/{id}

Response: 200 OK
{
    "id": 1,
    "name": "카테고리",
    "color": "#000000",
    "imageUrl": "https://...",
    "description": "설명"
}

Error:
  404 — id에 해당하는 카테고리 없음
```

**구현 방식:**

- `CategoryService.getCategory(id)`: `categoryRepo.findById(id).orElseThrow()` → `CategoryResponse.from(entity)`
- Product의 `getProduct(id)` 패턴과 동일

- a. API 명세 기준 누락된 CRUD 엔드포인트를 채우기 위한 목적


- b. `CategoryController`에 `GET /{id}` 엔드포인트 추가
- b. `CategoryService`에 `getCategory(id)` 메서드 추가


- c. 단순 추가. 기존 구조에 영향 없음.


- d. 카테고리를 ID로 단건 조회할 수 있음

---

#### 2-4. Product 카테고리별 필터링 추가

현재 `GET /api/products`는 전체 목록만 반환. 카테고리로 필터링 불가.

**API 스펙:**

```
GET /api/products?categoryId={categoryId}&page=0&size=10

categoryId가 없으면 전체 목록 (기존 동작 유지)
categoryId가 있으면 해당 카테고리 상품만 필터링

Response: 200 OK (기존과 동일한 PagedModel<ProductResponse>)
{
    "content": [
        { "id": 1, "name": "상품", "price": 1000, "imageUrl": "...", "categoryId": 1 }
    ],
    "page": { "size": 10, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

**구현 방식:**

- `ProductController.getProducts()`에 `@RequestParam(required = false) Long categoryId` 파라미터 추가
- `ProductService`: categoryId == null이면 기존 `findAll(pageable)`, 있으면 `findByCategoryId(categoryId, pageable)`
- `ProductRepository`에 `findByCategoryId(Long categoryId, Pageable pageable)` 쿼리 추가 (Spring Data 파생 쿼리로 충분)

- a. API 명세 기준 누락된 필터링 기능을 추가하기 위한 목적


- b. `ProductController.getProducts()`에 categoryId 파라미터 추가
- b. `ProductRepository`에 `findByCategoryId()` 쿼리 추가
- b. `ProductService`에 categoryId 유무에 따른 분기 로직 추가


- c. `ProductRepository`에 조건부 쿼리가 추가됨


- d. 특정 카테고리의 상품만 조회할 수 있음

---

### 3. 테스트 구축 (증거)

과제 요구사항: **"작동 변경은 반드시 증거와 함께 제출한다. 예외가 발생하는지만 확인하는 것으로 충분하지 않다. 상태를 재조회하거나 결과를 관찰 가능한 방식으로 검증해야 한다."**

**결정됨 — ADR-002 참고 (`docs/adr-002-test-strategy.md`)**

#### 테스트 계층 전략

| 계층                 | 대상       | 방식                                                | 검증 내용                                         |
|--------------------|----------|---------------------------------------------------|-----------------------------------------------|
| **Port 통합 테스트**    | Port 구현체 | `@DataJpaTest` + `NOT_SUPPORTED` + `@Import`      | 실제 DB 조작, 트랜잭션 전파 (MANDATORY/REQUIRED), 예외 처리 |
| **Service 단위 테스트** | Service  | `@ExtendWith(MockitoExtension.class)` + Mock Port | 오케스트레이션 순서, 호출 인자 정확성, 분기 로직                  |
| **인수 테스트**         | API 전체   | Cucumber + RestAssured (`@SpringBootTest`)        | 사용자 관점 계약, 트랜잭션 원자성, 상태 재조회                   |

**핵심 결정: `@DataJpaTest` 자동 트랜잭션 해제**

`@DataJpaTest`는 기본적으로 각 테스트에 `@Transactional`을 건다 (자동 롤백). 이를 해제하지 않으면:

- MANDATORY 테스트 불가 (테스트가 항상 tx를 제공)
- REQUIRED 동작 왜곡 (Port가 자체 tx를 생성하지 않고 테스트 tx에 참여)
- flush 타이밍 차이 (원래 실패해야 할 테스트가 성공할 수 있음)

`@Transactional(propagation = NOT_SUPPORTED)`로 해제하면 Port의 `@Transactional`만 작동하여 실제 환경과 동일한 동작을 검증할 수 있다. 대신 자동 롤백이 없으므로 통합 테스트용 DataManipulator로 수동 정리 필요.

#### 작동 변경별 필요한 증거

| 작동 변경              | 검증 방식               | 구체적 증거                                        |
|--------------------|---------------------|-----------------------------------------------|
| 위시 정리              | 주문 후 위시 목록 재조회      | 주문 전 위시 존재 → 주문 후 위시 목록에서 제거됨                 |
| 위시 정리 트랜잭션         | 주문 실패 시 위시 유지 확인    | 포인트 부족으로 주문 실패 → 위시가 그대로 남아있음                 |
| Option PUT         | 수정 후 옵션 목록 재조회      | 이름/수량 수정 → 재조회 시 변경된 값 반영                     |
| Category GET /{id} | 생성 후 단건 조회          | 생성한 카테고리를 ID로 조회 → 동일 데이터 반환                  |
| Product 카테고리 필터    | 다른 카테고리 상품 포함/제외 확인 | 카테고리A 상품 2개 + 카테고리B 상품 1개 → 카테고리A 필터 시 2개만 반환 |

- a. 추가된 작동이 올바르게 동작함을 증명하기 위한 목적 (과제 필수 요구사항)
- a. 추후 리팩터링에 대한 안전망을 강화하기 위한 목적
- a. 기존 기능이 작동 변경 이후에도 유지됨을 확인하기 위한 목적


- b. Port 통합 테스트: Port 구현체의 DB 조작 + 트랜잭션 전파 검증
- b. Service 단위 테스트: Port Mock으로 오케스트레이션 검증
- b. 인수 테스트: 위시 정리, Option PUT, Category 단건 조회, Product 카테고리 필터 시나리오 추가
- b. 상태 검증 테스트: 주문 후 위시 재조회, 주문 실패 시 위시 유지, 옵션 수정 후 재조회 등


- c. 테스트가 기능 명세 역할을 겸한다 — "이 시스템은 주문 시 위시를 정리한다"가 코드로 문서화됨
- c. 상태 재조회 패턴이 테스트에 도입됨 — 단순 상태코드 확인이 아닌 실제 데이터 변화 검증
- c. 작동 변경의 근거가 테스트로 남아, 추후 "왜 이렇게 바뀌었는가"를 추적 가능
- c. 내부 부품 테스트가 추가되어 실패 지점을 빠르게 특정할 수 있다


- d. 없음.

---

### 커밋 전략 (구조 변경 ↔ 작동 변경 분리)

과제 요구사항: **"구조 변경과 작동 변경을 섞지 않는다. 구조 변경 커밋과 작동 변경 커밋을 분리한다."**

```
[구조 변경] refactor: MemberQueryPort + MemberCommandPort 도입, OrderMemberRepository + AuthMemberRepository 제거
[구조 변경] refactor: CategoryQueryPort 도입, ProductCategoryRepository + AdminProductCategoryRepository 제거
[구조 변경] refactor: ProductQueryPort 도입, WishProductRepository + OptionProductRepository 제거
[구조 변경] refactor: OptionQueryPort + OptionCommandPort 도입, OrderOptionRepository 제거
---
[테스트] test: Port 통합 테스트 (트랜잭션 전파 + DB 조작 검증)
[테스트] test: Service 단위 테스트 (오케스트레이션 검증)
---
[작동 변경] feat: 주문 시 위시 자동 정리 구현
[작동 변경] feat: Option 수정 엔드포인트 추가
[작동 변경] feat: Category 단건 조회 엔드포인트 추가
[작동 변경] feat: Product 카테고리별 필터링 추가
---
[테스트] test: 위시 정리 인수 테스트 (상태 재조회 검증)
[테스트] test: Option PUT 인수 테스트
[테스트] test: Category 단건 조회 인수 테스트
[테스트] test: Product 카테고리 필터 인수 테스트
```

주의: 구조 변경 커밋은 기존 테스트가 모두 통과해야 한다 (외부 작동 불변).
작동 변경 커밋은 새 테스트와 함께 제출한다 (증거).

---

### ADR 현황

| 번호          | 결정 사항                                        | 상태  | 문서                                        |
|-------------|----------------------------------------------|-----|-------------------------------------------|
| ADR-001     | Port 패턴 + Query/Command 분리 + 트랜잭션 전파 전략      | 결정됨 | `docs/adr-001-cross-domain-repository.md` |
| ADR-002     | 내부 컴포넌트 테스트 전략 (Port 통합 + Service Mock + 인수) | 결정됨 | `docs/adr-002-test-strategy.md`           |
| ADR-003     | 주문 부수 효과 처리 전략 (트랜잭션 분리 + 이벤트 발행 + 실패 처리)    | 결정됨 | `docs/adr-003-order-side-effects.md`      |
| ~~ADR-004~~ | OptionRequest 공유 여부                          | 해소  | 다른 도메인과 동일하게 공유. ADR 불필요                  |
