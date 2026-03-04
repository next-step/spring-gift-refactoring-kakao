# PROJECT_STRUCTURE.md

프로젝트 구조 분석 결과를 정리한 문서입니다.

---

## 기술 스택

- **Java 21** + **Spring Boot 3.5.9**
- **Gradle 8.14** (Kotlin DSL)
- **H2** (개발), **MySQL 8** (운영)
- **Flyway** - DB 마이그레이션
- **JWT** - 인증
- **Kakao OAuth2** - 소셜 로그인

---

## 패키지 구조

```
src/main/java/gift/
├── auth/       # 인증 (JWT, Kakao OAuth2)
├── category/   # 카테고리
├── member/     # 회원
├── option/     # 상품 옵션
├── order/      # 주문
├── product/    # 상품
└── wish/       # 위시리스트
```

각 패키지는 동일한 계층 구조를 따릅니다:
- `Entity.java` - JPA 엔티티
- `Controller.java` - REST API
- `Repository.java` - 데이터 접근
- `Request.java` / `Response.java` - DTO

---

## 도메인 목록

| 도메인 | 설명 | 주요 속성 |
|--------|------|-----------|
| **Category** | 상품 카테고리 | name, color, imageUrl, description |
| **Member** | 회원 | email, password, kakaoAccessToken, point |
| **Product** | 상품 | name, price, imageUrl |
| **Option** | 상품 옵션 (재고 관리) | name, quantity |
| **Order** | 주문 | quantity, message, orderDateTime |
| **Wish** | 위시리스트 | (연관 관계만) |

---

## 도메인 관계도

```
┌──────────┐      ┌──────────┐      ┌──────────┐      ┌──────────┐
│ Category │◀─────│ Product  │─────▶│  Option  │◀─────│  Order   │
└──────────┘ M:1  └──────────┘ 1:N  └──────────┘ M:1  └──────────┘
                        │                                   │
                        │ M:1                               │ memberId (FK)
                        ▼                                   ▼
                  ┌──────────┐                        ┌──────────┐
                  │   Wish   │───────────────────────▶│  Member  │
                  └──────────┘     memberId (FK)      └──────────┘
```

### 연관관계 상세

| 관계 | 유형 | JPA 매핑 |
|------|------|----------|
| Product → Category | N:1 | `@ManyToOne` |
| Product ↔ Option | 1:N (양방향) | `@OneToMany` / `@ManyToOne` |
| Order → Option | N:1 | `@ManyToOne` |
| Order → Member | N:1 | `memberId` (primitive FK) |
| Wish → Product | N:1 | `@ManyToOne` |
| Wish → Member | N:1 | `memberId` (primitive FK) |

> **참고**: Order와 Wish는 Member에 대해 JPA 연관관계 대신 primitive FK(`memberId`)를 사용합니다.

---

## 핵심 비즈니스 로직

### 포인트 시스템 (Member)
- `chargePoint(amount)` - 포인트 충전
- `deductPoint(amount)` - 포인트 차감 (주문 결제 시 사용)

### 재고 관리 (Option)
- `subtractQuantity(amount)` - 재고 차감 (주문 시 사용)

### 주문 흐름 (OrderController)
1. 옵션 유효성 검증
2. 재고 차감 (Option)
3. 포인트 차감 (Member)
4. 주문 생성 (Order)
5. 위시리스트 삭제 (Wish)
6. 카카오 알림 발송

---

## DB 마이그레이션

`src/main/resources/db/migration/` 위치에 Flyway 마이그레이션 파일이 있습니다.

- `V1__Initialize_project_tables.sql` - 테이블 생성
- `V2__Insert_default_data.sql` - 기본 데이터 삽입

---

## AI 활용 기록

이 문서는 Claude Code를 활용하여 작성되었습니다.
- **활용 방식**: 엔티티 파일들을 분석하여 도메인 관계를 파악
- **분석 파일**: Category.java, Member.java, Product.java, Option.java, Order.java, Wish.java
