# Architecture Decision Records

## ADR-001: 테이블 네이밍 컨벤션

### 문제 상황
DDL 테이블명이 단수/복수 혼용되어 있었다.
- 단수: category, product, member, wish
- 복수: options, orders

### 논의 후보
1. **모두 단수로 통일** — `options` → `option`, `orders` → `order`. 단, `order`는 SQL 예약어라 quoting 필요.
2. **모두 복수로 통일** — 단수인 4개를 복수로 변경. 변경 범위가 훨씬 큼.
3. **단수 기본, 예약어만 복수 유지** — `options` → `option`, `orders`는 유지.
4. **단수 기본, 예약어는 별칭** — `orders` → `purchase_order` 등으로 변경.

### 결정
> **3번 채택. 테이블명은 단수형을 기본으로 하되, `orders`는 SQL 예약어 회피를 위해 복수형을 유지한다.**

### 이유
- 기존 테이블 6개 중 4개가 이미 단수형이고, JPA 엔티티 클래스명(단수)과 일치시키는 것이 자연스럽다.
- `order`는 SQL 예약어이므로 quoting(백틱)으로 회피할 수 있지만, Flyway 마이그레이션·직접 SQL·테스트 코드 등 사람이 작성하는 SQL에서 실수할 여지가 지속적으로 남는다.
- `purchase_order` 등 별칭은 다른 테이블이 모두 단일 단어인데 혼자 복합어가 되어 일관성이 떨어지고, 하위 개념 테이블이 추가될 때 혼란을 유발할 수 있다.

### 변경 내용
- `options` → `option` (Flyway V3 마이그레이션)
- JPA 엔티티, 테스트 코드의 테이블명 참조 일괄 수정

## ADR-002: 엔티티 컬럼 제약 조건 전략

### 문제 상황
DDL(Flyway)에 not null, 길이 제약 등이 명시되어 있지만, JPA 엔티티의 `@Column` 어노테이션은 Option에만 있고 나머지 엔티티에는 누락되어 있었다.

### 논의 후보
1. **모든 엔티티에 `@Column` 추가** — DDL과 JPA 양쪽에 제약을 명시. 코드만 보고 제약 파악 가능.
2. **Bean Validation(`@NotNull`, `@Size`)으로 표현** — 실제 런타임에 동작하는 검증.
3. **`@Column` 사용하지 않음** — Flyway가 DDL의 원천이므로 JPA에 중복 관리하지 않음. `@JoinColumn(nullable = false)`만 명시.

### 결정
> **3번 채택. `@Column` 제약은 사용하지 않고, `@JoinColumn(nullable = false)`만 명시한다.**

### 이유
- Flyway가 DDL의 단일 원천(single source of truth)이므로, `@Column`에 같은 제약을 중복 기재하면 관리 지점이 분산된다.
- `ddl-auto=validate`도 nullable, length는 검사하지 않아 불일치를 자동으로 감지할 수 없다.
- `@JoinColumn(nullable = false)`는 Hibernate가 LEFT JOIN 대신 INNER JOIN을 생성하는 실질적 효과가 있으므로 명시한다.
- Bean Validation은 입력 경계(Request DTO)와 도메인의 역할을 분리하는 별도 작업(validation 전략 통일)에서 다룬다.

### 변경 내용
- Product, Wish, Order: `@JoinColumn`에 `nullable = false` 추가
- Option: 기존 `@Column(nullable = false, length = 50)`, `@Column(nullable = false)` 제거

## ADR-003: FK 참조 전략

### 문제 상황
Wish, Order 엔티티가 Member를 `Long memberId`로만 참조하면서 주석에 "느슨한 결합 유지"라고 명시했지만, DDL에는 FK 제약이 정의되어 있어 실제로는 느슨한 결합이 아닌 하이브리드 상태였다. <br>
다른 관계(Product→Category, Wish→Product, Order→Option, Option→Product)는 모두 `@ManyToOne`을 사용하므로 일관성이 없었다.

### 논의 후보
1. **모두 `@ManyToOne`으로 통일** — Wish, Order에 `@ManyToOne Member` 추가. JPA 관계 전략을 일관되게 맞춤.
2. **현재 유지(하이브리드 공식화)** — Member 엔티티 연관관계에 한해서만 `Long memberId` 유지, DDL FK 유지. ADR로 의도를 문서화.
3. **모두 ID로 통일** — 모든 관계를 Long ID로 변경. DDL FK 제거 필요. JPA의 관계 관리(cascade, 객체 그래프 탐색)를 포기.

### 결정
> **1번 채택. 모든 FK 참조를 `@ManyToOne`으로 통일한다.**

### 이유
- 모놀리스에서 JPA를 사용하면서 관계를 끊는 것은 JPA의 핵심 장점(cascade, 객체 그래프 탐색, dirty checking)을 포기하는 것이다.
- 3번(모두 ID)은 DDL FK 제거까지 필요하여 변경 범위가 과도하고, 서비스 레이어가 크게 복잡해진다.
- 2번(하이브리드)은 DDL에 FK가 있으면서 JPA에서 관계를 인식하지 못하는 모순이 남는다.
- 1번은 기존 DDL 변경 없이 엔티티만 수정하면 되므로 변경 범위가 적절하다.

### 변경 내용
- Wish, Order: `Long memberId` → `@ManyToOne @JoinColumn(name = "member_id", nullable = false) Member member`
- WishService: `MemberService` 의존 추가 (Wish 생성 시 Member 조회 필요)

## ADR-004: 검증(Validation) 전략

### 문제 상황
검증 로직이 세 곳에 분산되어 있었다.
- **Request DTO**: Bean Validation(`@NotBlank`, `@Min`, `@Email`, `@Positive`)
- **서비스**: `ProductNameValidator`, `OptionNameValidator` 유틸리티 클래스 호출
- **컨트롤러**: `AdminProductController`에서 `ProductNameValidator`를 직접 호출 (서비스와 중복)

비즈니스 규칙(이름 길이/패턴, 수량 범위, 가격, 이메일 형식)과 형식 검사(`@NotNull`, `@NotBlank`)가 구분 없이 DTO에 혼재되어 있어, 서비스 내부 호출 시 도메인 무결성이 보장되지 않았다.

### 논의 후보
1. **서비스에 일원화** — DTO의 Bean Validation을 제거하고 서비스에서만 검증. 잘못된 요청이 트랜잭션까지 진입.
2. **DTO에 일원화** — Validator 클래스를 제거하고 Bean Validation + Custom Annotation으로 처리. 서비스 직접 호출 시 보호 불가.
3. **도메인 엔티티에 비즈니스 규칙, DTO에 형식 검사** — 역할을 분리. 도메인이 스스로 무결성을 보장하고, DTO는 명백한 쓰레기 입력만 조기 차단.

### 결정
> **3번 채택. 비즈니스 규칙은 도메인 엔티티가, 형식 검사는 DTO가 담당한다.**

### 이유
- "상품 이름은 15자 이내", "옵션 수량은 1 이상", "이메일 형식"은 도메인 불변식이다. 누가 호출하든(컨트롤러, 서비스, 테스트) 도메인 객체가 스스로 보장해야 한다.
- DTO의 `@NotBlank`, `@NotNull`은 "값이 존재하는가"의 형식 검사로, 트랜잭션 진입 전에 쓰레기 입력을 차단하는 방어 계층 역할이다.
- "카카오 포함 여부"는 호출자(Admin vs API)에 따라 달라지는 정책이므로 도메인 불변식이 아닌 서비스에 유지한다.
- Validator 유틸리티 클래스와 컨트롤러의 중복 호출이 제거되어 검증 규칙의 관리 지점이 단일화된다.

### 변경 내용
- Product, Option: 생성자/update에서 이름(blank, 길이, 패턴) 검증
- Product: 가격(`price > 0`) 검증
- Option: 수량 범위(`1 ~ 99,999,999`) 검증
- Order: 수량(`quantity >= 1`) 검증
- Member: 이메일(blank, `@` 포함) 검증
- Request DTO: 비즈니스 규칙 어노테이션(`@Min`, `@Max`, `@Email`, `@Positive`) 제거, 형식 검사(`@NotBlank`, `@NotNull`)만 유지
- `ProductNameValidator`, `OptionNameValidator` 삭제
- `AdminProductController`: 서비스 예외 catch 방식으로 전환
