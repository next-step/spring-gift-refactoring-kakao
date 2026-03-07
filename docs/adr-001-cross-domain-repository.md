# ADR-001: 크로스 도메인 Repository 폭발 해결 방안

**상태**: 결정됨
**날짜**: 2026-03-04

---

## 맥락

Phase 1 리팩터링에서 도메인 간 내부 구현을 격리하기 위해 `internal` 패키지를 도입했다. 이때 "다른 도메인의 Repository를 import하면 안 된다"는 원칙을 적용한 결과, **같은 Entity에 대한 Repository를 패키지마다 복제**하게 되었다.

현재 6개 Entity에 대해 16개 Repository가 존재한다.

| Entity   | Repository 수 | 목록                                                                                                |
|----------|--------------|---------------------------------------------------------------------------------------------------|
| Member   | 4개           | `MemberRepository`, `AdminMemberRepository`, `OrderMemberRepository`, `AuthMemberRepository`      |
| Category | 3개           | `CategoryRepository`, `ProductCategoryRepository`, `AdminProductCategoryRepository`               |
| Product  | 4개           | `ProductRepository`, `AdminProductRepository`, `OptionProductRepository`, `WishProductRepository` |
| Option   | 2개           | `OptionRepository`, `OrderOptionRepository`                                                       |
| Wish     | 2개           | `WishRepository`, `OrderWishRepository` (미사용)                                                     |
| Order    | 1개           | 변경 없음                                                                                             |

이 중 `ProductCategoryRepository`, `AdminProductCategoryRepository`, `OrderMemberRepository`, `OrderWishRepository`, `WishProductRepository`는 **커스텀 메서드 없이 `JpaRepository`만 상속**하는 빈 인터페이스이다.

### 부수 문제: 도메인 로직 누출

Repository 복제의 결과로, Service가 다른 도메인의 Entity를 직접 조작하는 코드가 발생했다.

```java
// OrderService.java — Member의 비즈니스 규칙이 Order 패키지에 누출
memberRepo.findById(memberId)
        .orElseThrow(NotFoundException::memberNotFound)
        .deductPoint(price);
```

Member의 포인트 차감 정책이 변경되면 `OrderService`를 수정해야 한다. 이는 Feature Envy — 다른 도메인의 내부 구현에 의존하는 안티패턴이다.

---

## 해결 방향: Port 패턴 확장

기존 `AuthenticationPort`, `JwtPort`와 동일한 패턴을 다른 도메인에도 적용한다. 각 도메인이 Port 인터페이스를 패키지 루트에 공개하고, 다른 도메인은 Port를 통해서만 접근한다. Repository는 `internal`에 격리된 채로 유지.

```
Before: OrderService → OrderMemberRepository (Member Repository 복제본)
After:  OrderService → MemberPort (Member 도메인이 공개한 계약)
```

---

## 크로스 도메인 접근 유형 분석

현재 프로젝트에서 도메인을 횡단하는 접근을 분석하면 두 가지 유형이 나타난다.

### 유형 1: FK 참조 (Entity 반환 필요)

JPA `@ManyToOne` 관계 설정을 위해 영속 Entity 객체가 필요한 경우.

| 호출자                 | 대상 도메인   | 용도                                           |
|---------------------|----------|----------------------------------------------|
| ProductService      | Category | Product 생성/수정 시 `category` FK                |
| AdminProductService | Category | 동일                                           |
| WishService         | Product  | Wish 생성 시 `product` FK                       |
| OptionService       | Product  | Option 생성 시 `product` FK, 삭제 시 options 목록 조회 |
| OrderService        | Option   | Order 생성 시 `option` FK + Product 가격 조회       |

### 유형 2: 오퍼레이션 (Entity 반환 불필요)

ID만 받고 상태 변경이나 데이터 조회를 수행하는 경우. Entity 객체를 호출자에게 반환하지 않는다.

| 호출자                   | 대상 도메인 | 용도                                                            |
|-----------------------|--------|---------------------------------------------------------------|
| OrderService          | Member | `deductPoint(memberId, price)` — 포인트 차감                       |
| OrderService          | Option | `option.subtractQuantity(quantity)` — 재고 차감 (현재 Entity 직접 호출) |
| KakaoAuthService      | Member | `findByEmail`, `create`, `updateKakaoAccessToken`             |
| KakaoMessagingService | Member | `getKakaoAccessToken(memberId)` — 토큰 조회                       |

---

## 핵심 고민: 참조 Port의 Entity 반환과 비즈니스 메서드 노출

Port가 Entity를 반환하면, 호출자가 Entity의 비즈니스 메서드를 직접 호출할 수 있다.

```java
// 의도하지 않은 사용이 가능해짐
Product product = productPort.getReference(productId);
product.update("이름변경", 0, "url", category);  // 도메인 침범

Option option = optionPort.getReference(optionId);
option.subtractQuantity(10);  // Port를 우회한 직접 호출
```

JPA에서 `@ManyToOne` FK에 Entity 객체가 필요하므로, Entity를 반환하지 않을 수 없다. 이것은 JPA의 구조적 한계이다.

---

## 검토한 선택지

### A. Port + 컨벤션

Port에 오퍼레이션 메서드를 제공하여 "올바른 방법"을 명시한다. Entity 메서드의 직접 호출은 컨벤션으로 금지한다.

```java
public interface OptionPort {

    Option getReference(Long optionId);                    // FK용

    void subtractQuantity(Long optionId, int quantity);    // 올바른 방법
}
```

- Rich Domain Model 유지, 구현 간단, Phase 1 범위 준수
- Entity method 노출을 컴파일 단계에서 막지 못함

### B. Entity를 빈혈 모델로 전환 + package-private

비즈니스 메서드를 Entity에서 제거하고 Service/Port에만 배치한다. Entity의 상태 변경 메서드를 package-private으로 만든다.

```java

@Entity
@Getter
public class Option {

    private int quantity;

    void changeQuantity(int newQuantity) {
        this.quantity = newQuantity;
    }  // package-private
}
```

- 구조적으로 Entity method 노출 차단 (컴파일 단계)
- Rich Domain Model 포기, 작동 변경 가능성

### C. 모든 FK를 primitive(Long)로 전환

`@ManyToOne` 대신 `Long productId` 사용. Entity 반환 자체가 불필요해지므로 모든 Port가 오퍼레이션 전용이 된다.

- 완전 캡슐화
- JPA 연관관계 이점 전부 상실 (Lazy loading, join fetch, cascade)
- DB 스키마 변경이 필요하며, 리팩터링이 아닌 시스템 개편에 해당

### A+B 혼합: Port + MANDATORY 트랜잭션 + Entity package-private

Port가 `@Transactional(propagation = MANDATORY)`로 영속 Entity를 반환하고, Entity의 비즈니스 메서드는 package-private으로 제한하여 다른 도메인에서 호출할 수 없도록 한다. Port가 Entity의 package-private 메서드를 위임 호출하는 구조.

- Entity에 비즈니스 로직을 유지하면서도 외부 노출을 차단하는 가장 이상적인 방안

---

## 기각 사유

### B 기각: Java 패키지 접근 제어의 한계

Java는 하위 패키지 접근 개념이 없다. `gift.option`과 `gift.option.internal`은 완전히 별개의 패키지이다.

```
gift/option/
├── Option.java              ← 패키지: gift.option
└── internal/
    ├── OptionService.java   ← 패키지: gift.option.internal (별개 패키지!)
    └── OptionPortImpl.java  ← 패키지: gift.option.internal (별개 패키지!)
```

`Option`의 package-private 메서드를 `gift.option.internal`의 `OptionService`나 `OptionPortImpl`에서 **호출할 수 없다**. 자기 도메인의 Service조차 Entity의 package-private 메서드에 접근 불가.

이를 해결하려면 `internal` 패키지 구조 자체를 포기하거나 재편해야 한다.

### A+B 혼합 기각: B와 동일한 Java 패키지 한계

A+B 혼합의 핵심인 "Entity package-private + Port 위임"은 B와 동일한 패키지 접근 제어 한계에 부딪힌다. Port 구현체가 `internal` 패키지에 있으므로 Entity의 package-private 메서드를 호출할 수 없다.

### C 기각: 범위 초과

모든 `@ManyToOne`을 `Long`으로 변환하면 DB 스키마 변경, Flyway 마이그레이션, JPQL 전면 수정이 필요하다. 이는 코드 리팩터링이 아닌 시스템 개편이며 현재 목표(구조 변경만)를 벗어난다.

---

## 비교

|                     | Entity method 차단 | JPA 연관관계 유지 | Rich Domain Model | Phase 1 범위 | 기각 사유          |
|---------------------|------------------|-------------|-------------------|------------|----------------|
| **A. Port + 컨벤션**   | 컨벤션으로만           | 유지          | 유지                | O          | —              |
| **B. 빈혈 모델**        | 컴파일 단계 차단        | 유지          | 포기                | 위험         | Java 패키지 접근 한계 |
| **A+B 혼합**          | 컴파일 단계 차단        | 유지          | 유지                | 위험         | Java 패키지 접근 한계 |
| **C. primitive FK** | 원천 차단            | 포기          | 유지                | X          | 시스템 개편 범위      |

---

## 결정 1: 선택지 A + MANDATORY 트랜잭션

### 근거

1. **JPA의 구조적 한계를 인정한다.** `@ManyToOne` FK에 Entity 객체가 필요하므로 참조 Port의 Entity 반환은 불가피하다. Entity의 public 비즈니스 메서드 노출을 컴파일 단계에서 차단할 수 있는 방법(B, A+B)은 현재 `internal` 하위 패키지 구조에서 Java 패키지 접근 제어 한계로 적용 불가능하다.

2. **Port가 "올바른 방법"을 제공한다.** 비즈니스 오퍼레이션을 Port 메서드로 명시하여, Entity 메서드의 직접 호출은 "의도적 우회"로 간주한다. Port가 없던 이전 상태(Repository 복제 + 직접 Entity 조작)보다 확실히 개선된다.

3. **primitive FK인 Member는 완전 캡슐화된다.** Member는 `@ManyToOne`으로 참조하는 곳이 없으므로 오퍼레이션 Port만 제공하면 된다. Entity가 반환되지 않으므로 `member.deductPoint()` 누출이 원천 차단된다.

4. **MANDATORY 트랜잭션으로 영속성 안전을 강제한다.** 참조 Port의 `getReference()` 메서드에 `@Transactional(propagation = MANDATORY)`를 적용하여, 호출자에 트랜잭션이 없으면 즉시 예외가 발생하도록 한다.

### 수용한 한계

참조 Port가 반환한 Entity의 public 비즈니스 메서드를 컴파일 단계에서 차단할 수 없다. 이는 JPA가 `@ManyToOne` FK에 Entity 객체를 요구하는 구조적 한계와 Java의 패키지 접근 제어 한계가 결합된 결과이다. Port 오퍼레이션 메서드가 "올바른 방법"으로 존재하므로, 직접 호출은 코드 리뷰에서 잡는다.

---

## 영속성 컨텍스트 제약 (JPA)

참조 Port가 Entity를 반환할 때, 반환된 Entity가 호출자의 트랜잭션에서 영속 상태여야 한다. 트랜잭션 전파 방식 선택이 이 안전을 좌우한다.

### `@Transactional(propagation = REQUIRED)` 의 위험

`REQUIRED`는 호출자에 트랜잭션이 없을 때 **자체 트랜잭션을 조용히 생성**한다. Port 메서드가 종료되면 이 트랜잭션이 커밋되고, 반환된 Entity는 detached 상태가 된다. 이후 FK로 사용하면 `save()` 시점에 런타임 에러가 발생하며, 원인 추적이 어렵다.

### `@Transactional(propagation = MANDATORY)` 선택

`MANDATORY`는 호출자에 트랜잭션이 없으면 **Port 호출 시점에 즉시 예외**를 발생시킨다. detached entity가 만들어지는 것 자체를 차단하므로 fast-fail이 보장된다.

```
REQUIRED:  호출자에 트랜잭션 없음 → 자체 생성 → Entity detached → save 시 런타임 에러 (늦은 발견)
MANDATORY: 호출자에 트랜잭션 없음 → 즉시 예외 (빠른 발견)
```

---

## 결정 2: Query/Command Port 분리

결정 1에서 Port의 큰 방향이 정해진 후, Port 인터페이스를 더 세분화할 수 있는지 논의했다.

### 배경: 하나의 Port에 조회와 명령이 섞이는 문제

결정 1의 단일 Port 구조에서는 조회(FK 참조, 데이터 읽기)와 명령(상태 변경)이 한 인터페이스에 공존한다.

```java
// 단일 Port — 조회와 명령이 혼합
public interface OptionPort {

    Option getReference(Long optionId);                    // 조회: FK 참조

    Option getReferenceWithProduct(Long optionId);         // 조회: 가격 확인

    void subtractQuantity(Long optionId, int quantity);    // 명령: 재고 차감
}
```

호출자 입장에서 불필요한 의존이 발생한다.

- **WishService**는 Product의 `getReference()`만 필요한데, `subtractQuantity()` 같은 명령 메서드까지 보인다.
- **OrderService**는 Option의 `getReference()`와 `subtractQuantity()` 모두 필요하다.
- 인터페이스 분리 원칙(ISP) 위반 — 소비자가 사용하지 않는 메서드에 의존한다.

### 해결: QueryPort와 CommandPort로 분리

```java
// option/OptionQueryPort.java — 조회 전용
public interface OptionQueryPort {

    Option getReference(Long optionId);

    Option getReferenceWithProduct(Long optionId);
}

// option/OptionCommandPort.java — 명령 전용
public interface OptionCommandPort {

    void subtractQuantity(Long optionId, int quantity);
}
```

### 도메인별 분리 적용 여부

모든 도메인에 Query/Command 분리가 필요한 것은 아니다. 크로스 도메인 접근 유형에 따라 필요한 Port만 만든다.

| 도메인          | QueryPort | CommandPort | 근거                                                                                      |
|--------------|-----------|-------------|-----------------------------------------------------------------------------------------|
| **Category** | O         | X           | Product/AdminProduct가 FK 참조와 목록 조회만 수행. 다른 도메인이 Category를 변경하지 않음                       |
| **Product**  | O         | X           | Wish/Option이 FK 참조만 수행. 다른 도메인이 Product를 변경하지 않음                                        |
| **Option**   | O         | O           | Order가 FK 참조(QueryPort)와 재고 차감(CommandPort) 모두 수행                                       |
| **Member**   | O         | O           | Order가 포인트 차감(CommandPort), Auth가 회원 생성/토큰 갱신(CommandPort), Messaging이 토큰 조회(QueryPort) |

Category와 Product는 크로스 도메인에서 **조회만** 발생하므로 QueryPort만 공개한다. 불필요한 CommandPort를 만들지 않는다.

### 분리의 이점

**1. 인터페이스 분리 원칙 (ISP)**

소비자가 필요한 계약에만 의존한다.

```java
// WishService — Product의 참조만 필요
@Service
@RequiredArgsConstructor
public class WishService {

    private final ProductQueryPort productQueryPort;  // 조회 계약만 의존
    // ProductCommandPort는 보이지도 않음
}

// OrderService — Option의 참조와 재고 차감 모두 필요
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OptionQueryPort optionQueryPort;      // FK 참조
    private final OptionCommandPort optionCommandPort;  // 재고 차감
    private final MemberCommandPort memberCommandPort;  // 포인트 차감
}
```

**2. 의존 방향의 명시성**

Service가 주입받는 Port 타입만으로 해당 Service가 다른 도메인에 무엇을 요구하는지 알 수 있다.

- `QueryPort`만 주입 → "이 서비스는 해당 도메인을 읽기만 한다"
- `CommandPort`도 주입 → "이 서비스는 해당 도메인의 상태를 변경한다"

이는 코드 리뷰에서 "왜 이 서비스가 다른 도메인을 변경하는가?"라는 질문을 자연스럽게 유도한다.

**3. 테스트 용이성**

Mock 대상이 작아진다. QueryPort만 사용하는 서비스를 테스트할 때 CommandPort를 Mock할 필요가 없다.

### Port 인터페이스 설계 예시

```java
// === Category 도메인 ===

// category/CategoryQueryPort.java
public interface CategoryQueryPort {

    Category getReference(Long categoryId);
}

// === Product 도메인 ===

// product/ProductQueryPort.java
public interface ProductQueryPort {

    Product getReference(Long productId);
}

// === Option 도메인 ===

// option/OptionQueryPort.java
public interface OptionQueryPort {

    Option getReference(Long optionId);

    Option getReferenceWithProduct(Long optionId);
}

// option/OptionCommandPort.java
public interface OptionCommandPort {

    void subtractQuantity(Long optionId, int quantity);
}

// === Member 도메인 ===

// member/MemberQueryPort.java
public interface MemberQueryPort {

    Long findByEmail(String email);

    String getKakaoAccessToken(Long id);

    String getEmail(Long id);
}

// member/MemberCommandPort.java
public interface MemberCommandPort {

    void deductPoint(Long id, int amount);

    Long create(MemberInfo info);

    void updateKakaoAccessToken(Long id, String newToken);
}
```

---

## 결정 3: 트랜잭션 전파 전략

Query/Command Port 분리에 따라 각 Port 유형에 적합한 트랜잭션 전파 방식을 결정했다.

### 논의 과정

#### QueryPort.getReference()는 왜 MANDATORY인가

`getReference()`는 영속 Entity를 반환한다. 호출자에 트랜잭션이 없으면 반환된 Entity가 detached 상태가 되어 FK로 사용할 수 없다. MANDATORY는 이 상황을 **호출 시점에 즉시** 잡아낸다.

```java
// 개발자 실수: @Transactional 누락
public void createProduct(String name, Long categoryId) {
    Category category = categoryQueryPort.getReference(categoryId);
    // MANDATORY → 여기서 즉시 예외. detached entity가 만들어지기 전에 차단.
}
```

MANDATORY는 단순히 "트랜잭션이 있어야 한다"는 기술적 제약을 강제하는 것이 아니라, **detached entity로 인한 런타임 에러를 원천 차단하는 안전 장치**이다.

#### QueryPort의 다른 메서드는 왜 트랜잭션 선언이 없는가

`findByEmail()`, `getKakaoAccessToken()`, `getEmail()` 등은 Entity를 반환하지 않고 단순 값(Long, String)을 반환한다. 영속성 컨텍스트와 무관하므로 트랜잭션 강제가 불필요하다.

- 호출자에 트랜잭션이 있으면 참여한다.
- 호출자에 트랜잭션이 없으면 트랜잭션 없이 실행된다.

어느 쪽이든 반환값의 정합성에 영향이 없다.

#### CommandPort는 왜 REQUIRED인가

CommandPort 메서드(`deductPoint`, `create`, `subtractQuantity` 등)는 상태를 변경하므로 트랜잭션이 반드시 필요하다.

**MANDATORY가 아닌 이유**: KakaoAuthService가 의도적으로 `@Transactional`을 선언하지 않는다.

```java
// KakaoAuthService — 의도적으로 @Transactional 없음
@Service
public class KakaoAuthService {

    public TokenResponse loginWithKakao(String code) {
        // 1. 외부 API 호출 (네트워크 I/O) — 트랜잭션 없이 수행
        KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);
        KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(accessToken);

        // 2. DB 쓰기 — CommandPort가 자체 트랜잭션 생성
        try {
            Long memberId = memberQueryPort.findByEmail(email);
            memberCommandPort.updateKakaoAccessToken(memberId, accessToken);
        } catch (NotFoundException ignored) {
            memberCommandPort.create(info);
        }

        // 3. JWT 생성 — 트랜잭션 불필요
        return new TokenResponse(jwtProvider.createToken(email));
    }
}
```

KakaoAuthService가 `@Transactional`을 선언하지 않는 이유는, 외부 API 호출(네트워크 I/O) 동안 DB 커넥션을 점유하지 않기 위함이다. MANDATORY를 사용하면 이 서비스가 동작하지 않는다. REQUIRED는 호출자에 트랜잭션이 없을 때 자체 트랜잭션을 생성하므로, KakaoAuthService 같은 비트랜잭셔널 호출자도 수용할 수 있다.

#### 원자성 책임은 Port가 아닌 호출자에게 있다

CommandPort에 REQUIRED를 사용하면, 호출자에 트랜잭션이 없을 때 각 CommandPort 호출이 독립 트랜잭션을 생성한다. 이때 여러 CommandPort 호출의 원자성은 보장되지 않는다.

```java
// 호출자에 @Transactional이 없다면?
public void riskyOperation(Long fromId, Long toId, int amount) {
    memberCommandPort.deductPoint(fromId, amount);  // REQUIRED → 자체 tx → 커밋됨
    memberCommandPort.chargePoint(toId, amount);     // REQUIRED → 자체 tx → 실패하면?
    // fromId의 포인트는 이미 차감 → 롤백 불가
}
```

이것은 CommandPort의 설계 결함이 아니다. **여러 쓰기 작업의 원자성은 호출자가 보장해야 하는 일반적인 트랜잭션 관리 책임**이다. JDBC든, JpaRepository든, CommandPort든, 여러 쓰기를 하나로 묶으려면 호출자가 트랜잭션 경계를 선언해야 한다. 이것은 별도 컨벤션이 필요한 특수 케이스가 아니라, 트랜잭션의 기본 원칙이다.

### 최종 트랜잭션 전략

| Port 유형         | 메서드              | 트랜잭션        | 근거                                              |
|-----------------|------------------|-------------|-------------------------------------------------|
| **QueryPort**   | `getReference()` | `MANDATORY` | 영속 Entity 반환 → detached 방지 (fast-fail)          |
| **QueryPort**   | 기타 조회 메서드        | 없음          | 단순 값 반환 → 영속성 컨텍스트 무관                           |
| **CommandPort** | 모든 메서드           | `REQUIRED`  | 상태 변경에 트랜잭션 필수. 비트랜잭셔널 호출자(KakaoAuthService) 수용 |

---

## 컨벤션

1. **참조 Port에서 받은 Entity의 비즈니스 메서드를 직접 호출하지 않는다.** 오퍼레이션이 필요하면 해당 도메인의 CommandPort 메서드를 사용한다.

2. **QueryPort 구현체의 `getReference()` 메서드에는 `@Transactional(propagation = MANDATORY)`를 붙인다.**

3. **CommandPort 구현체의 모든 메서드에는 `@Transactional`을 붙인다.** (기본 propagation = REQUIRED)

4. **크로스 도메인 접근은 반드시 Port를 통한다.** 다른 도메인의 Repository를 직접 import하지 않는다.

---

## 적용 후 구조 (예상)

### Repository 변화

Port 도입 후 복제된 Repository를 제거한다. 각 도메인은 하나의 Repository만 유지.

```
Before (16개):
  Member:   MemberRepository, AdminMemberRepository, OrderMemberRepository, AuthMemberRepository
  Category: CategoryRepository, ProductCategoryRepository, AdminProductCategoryRepository
  Product:  ProductRepository, AdminProductRepository, OptionProductRepository, WishProductRepository
  Option:   OptionRepository, OrderOptionRepository
  Wish:     WishRepository, OrderWishRepository
  Order:    OrderRepository

After (6개 Repository + 6개 Port):
  Member:   MemberRepository + MemberQueryPort + MemberCommandPort
  Category: CategoryRepository + CategoryQueryPort
  Product:  ProductRepository + ProductQueryPort
  Option:   OptionRepository + OptionQueryPort + OptionCommandPort
  Wish:     WishRepository (크로스 도메인 접근 없음)
  Order:    OrderRepository (크로스 도메인 접근 없음)
```

### 패키지 구조 (Option 도메인 예시)

```
gift/option/
├── Option.java                ← Entity (패키지 루트에 공개)
├── OptionQueryPort.java       ← QueryPort 인터페이스 (패키지 루트에 공개)
├── OptionCommandPort.java     ← CommandPort 인터페이스 (패키지 루트에 공개)
└── internal/
    ├── OptionService.java     ← 도메인 내부 서비스
    ├── OptionRepository.java  ← 유일한 Repository (internal에 격리)
    ├── OptionQueryPortImpl.java   ← QueryPort 구현체
    └── OptionCommandPortImpl.java ← CommandPort 구현체
```

### OrderService 변화 (Before/After)

```java
// Before — 크로스 도메인 Repository 직접 사용, Entity 직접 조작
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderOptionRepository optionRepo;   // Option Repository 복제본
    private final MemberQueryPort memberQueryPort;    // 이미 Port 적용됨

    @Transactional
    public OrderResponse createOrder(Long memberId, OrderRequest request) {
        Option option = optionRepo.findByIdInnerJoinFetchProduct(optionId)
                .orElseThrow(NotFoundException::optionNotFound);

        option.subtractQuantity(quantity);  // Entity 비즈니스 메서드 직접 호출

        int price = option.getProduct().getPrice() * quantity;
        memberQueryPort.deductPoint(memberId, price);

        Order build = Order.builder()
                .option(option)
                .memberId(memberId)
                .quantity(quantity)
                .message(message)
                .build();

        return OrderResponse.from(orderRepo.save(build));
    }
}

// After — Port를 통한 접근, 역할 분리
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OptionQueryPort optionQueryPort;      // 참조 + 조회
    private final OptionCommandPort optionCommandPort;  // 재고 차감
    private final MemberCommandPort memberCommandPort;  // 포인트 차감

    @Transactional
    public OrderResponse createOrder(Long memberId, OrderRequest request) {
        Option option = optionQueryPort.getReferenceWithProduct(optionId);

        int price = option.getProduct().getPrice() * quantity;
        optionCommandPort.subtractQuantity(optionId, quantity);  // Port 통해 호출
        memberCommandPort.deductPoint(memberId, price);          // Port 통해 호출

        Order build = Order.builder()
                .option(option)
                .memberId(memberId)
                .quantity(quantity)
                .message(message)
                .build();

        return OrderResponse.from(orderRepo.save(build));
    }
}
```
