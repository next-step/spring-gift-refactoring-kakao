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
