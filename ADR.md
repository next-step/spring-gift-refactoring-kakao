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
