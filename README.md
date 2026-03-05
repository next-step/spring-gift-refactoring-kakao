# spring-gift-refactoring

Spring Boot 기반의 **선물하기 커머스 플랫폼**입니다.
상품 관리, 회원 인증, 위시리스트, 주문 처리 및 카카오톡 연동 기능을 제공합니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 21, Kotlin 1.9 |
| Framework | Spring Boot 3.5.9 |
| Database | MySQL (운영), H2 (테스트) |
| ORM | Spring Data JPA |
| Migration | Flyway 12.0.1 |
| Auth | JWT (JJWT 0.13.0), Kakao OAuth2 |
| Template | Thymeleaf |
| Build | Gradle (Kotlin DSL) |
| Code Style | ktlint |

---

## 실행 방법

**1.** 프로젝트 루트에 `.env` 파일을 생성합니다.

```
JWT_SECRET=<256비트 이상의 시크릿 키>
JWT_EXPIRATION=3600000
KAKAO_CLIENT_ID=<카카오 앱 REST API 키>
KAKAO_CLIENT_SECRET=<카카오 앱 시크릿 키>
KAKAO_REDIRECT_URI=http://<로컬 서버 도메인>/api/auth/kakao/callback
```

**2.** MySQL 데이터베이스를 준비합니다.

**3.** 애플리케이션을 실행합니다.

```bash
./gradlew bootRun
```

---

## 기능 목록

### 회원 (Member)
- 이메일/비밀번호 기반 회원가입 및 로그인
- 카카오 OAuth2 로그인 (자동 회원가입 포함)
- JWT 기반 인증/인가
- 포인트 충전 및 차감

### 상품 (Product)
- 상품 CRUD (페이지네이션 지원)
- 상품명 유효성 검증 (최대 15자, 허용 특수문자, 한글 지원)
- 카테고리별 상품 분류

### 카테고리 (Category)
- 카테고리 CRUD

### 옵션 (Option)
- 상품별 옵션 관리 (사이즈, 색상 등)
- 옵션명 유효성 검증 (최대 50자)
- 재고 수량 관리

### 주문 (Order)
- 인증된 회원의 주문 생성
- 옵션 재고 차감 및 포인트 차감
- 카카오톡 메시지 알림 발송 (best-effort)
- 주문 내역 조회 (페이지네이션 지원)

### 위시리스트 (Wish)
- 상품 위시리스트 추가/삭제
- 중복 등록 방지
- 본인 위시리스트만 삭제 가능

---

## DB 스키마

### 구조도

```
┌──────────────┐       ┌──────────────┐
│   category   │       │    member    │
├──────────────┤       ├──────────────┤
│ id       PK  │       │ id       PK  │
│ name     UQ  │       │ email    UQ  │
│ color        │       │ password     │
│ image_url    │       │ kakao_token  │
│ description  │       │ point        │
└──────┬───────┘       └──┬───────┬───┘
       │                  │       │
       │ 1:N              │ 1:N   │ 1:N
       ▼                  │       ▼
┌──────────────┐          │  ┌──────────────┐
│   product    │          │  │    orders    │
├──────────────┤          │  ├──────────────┤
│ id       PK  │          │  │ id       PK  │
│ name         │          │  │ option_id FK ─┼─┐
│ price        │          │  │ member_id FK  │ │
│ image_url    │          │  │ quantity      │ │
│ category_id FK          │  │ message       │ │
└──┬───────┬───┘          │  │ order_date    │ │
   │       │              │  └──────────────┘ │
   │ 1:N   │ 1:N          │                   │
   ▼       ▼              ▼                   │
┌────────┐ ┌──────────────┐                   │
│options │ │    wish      │                   │
├────────┤ ├──────────────┤                   │
│ id  PK │ │ id       PK  │                   │
│ prod FK│ │ member_id FK  │                   │
│ name   │ │ product_id FK │                   │
│quantity│ └──────────────┘                   │
└────┬───┘                                    │
     │              ◄─────────────────────────┘
     └── 주문 시 옵션의 재고(quantity)가 차감됨
```

> **관계 요약**
> - `category` 1 → N `product` : 하나의 카테고리에 여러 상품
> - `product` 1 → N `options` : 하나의 상품에 여러 옵션 (사이즈, 색상 등)
> - `product` 1 → N `wish` : 하나의 상품이 여러 위시리스트에 등록
> - `member` 1 → N `wish` : 한 회원이 여러 상품을 위시리스트에 추가
> - `member` 1 → N `orders` : 한 회원이 여러 주문 생성
> - `options` 1 → N `orders` : 하나의 옵션이 여러 주문에서 선택

### 테이블 상세

<details>
<summary><code>category</code> — 카테고리</summary>

상품을 분류하기 위한 카테고리 정보를 저장한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 카테고리 고유 식별자 |
| `name` | VARCHAR(255) | NOT NULL, UNIQUE | 카테고리 이름 (중복 불가) |
| `color` | VARCHAR(7) | NOT NULL | 대표 색상 (HEX 코드, 예: `#FF5A5A`) |
| `image_url` | VARCHAR(255) | NOT NULL | 카테고리 대표 이미지 URL |
| `description` | VARCHAR(255) | | 카테고리 설명 |

</details>

<details>
<summary><code>product</code> — 상품</summary>

판매 상품의 기본 정보를 저장한다. 반드시 하나의 카테고리에 속한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 상품 고유 식별자 |
| `name` | VARCHAR(15) | NOT NULL | 상품명 (최대 15자, 특수문자 제한) |
| `price` | INT | NOT NULL | 상품 가격 (원) |
| `image_url` | VARCHAR(255) | NOT NULL | 상품 이미지 URL |
| `category_id` | BIGINT | FK → `category(id)` | 소속 카테고리 |

</details>

<details>
<summary><code>member</code> — 회원</summary>

서비스에 가입한 회원 정보를 저장한다. 이메일/비밀번호 가입 또는 카카오 OAuth2 로그인을 지원한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 회원 고유 식별자 |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | 로그인용 이메일 (중복 불가) |
| `password` | VARCHAR(255) | | 비밀번호 (카카오 로그인 회원은 NULL) |
| `kakao_access_token` | VARCHAR(512) | | 카카오 API 호출용 액세스 토큰 |
| `point` | INT | NOT NULL, DEFAULT 0 | 보유 포인트 (주문 시 차감, 충전 가능) |

</details>

<details>
<summary><code>options</code> — 상품 옵션</summary>

상품의 구매 가능한 옵션(사이즈, 색상 등)과 재고를 관리한다. 하나의 상품에 여러 옵션이 존재할 수 있다.

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 옵션 고유 식별자 |
| `product_id` | BIGINT | FK → `product(id)` | 소속 상품 |
| `name` | VARCHAR(50) | NOT NULL | 옵션명 (최대 50자) |
| `quantity` | INT | NOT NULL | 재고 수량 (주문 시 차감) |

</details>

<details>
<summary><code>wish</code> — 위시리스트</summary>

회원이 관심 있는 상품을 저장하는 위시리스트이다. 회원-상품 조합으로 중복 등록을 방지한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 위시 고유 식별자 |
| `member_id` | BIGINT | FK → `member(id)` | 위시리스트 소유 회원 |
| `product_id` | BIGINT | FK → `product(id)` | 위시리스트에 담긴 상품 |

</details>

<details>
<summary><code>orders</code> — 주문</summary>

회원의 주문 내역을 저장한다. 주문 시 옵션 재고가 차감되고 회원 포인트가 사용된다.

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 주문 고유 식별자 |
| `option_id` | BIGINT | FK → `options(id)` | 주문한 상품 옵션 |
| `member_id` | BIGINT | FK → `member(id)` | 주문한 회원 |
| `quantity` | INT | NOT NULL | 주문 수량 |
| `message` | VARCHAR(255) | | 선물 메시지 (카카오톡으로 전송) |
| `order_date_time` | TIMESTAMP | NOT NULL | 주문 일시 |

</details>

---

## 문서 관리

프로젝트 문서는 `./docs/` 디렉토리에서 관리합니다.

```
docs/
├── adr/       # Architecture Decision Records
└── history/   # 작업 히스토리
```

### ADR (Architecture Decision Record)

개발 과정에서 내린 중요한 아키텍처 결정사항을 기록합니다.

#### 작성 원칙
- **변경 불가(Immutable):** 작성된 ADR은 수정하지 않습니다. 결정이 바뀌면 새 ADR을 작성하여 이전 ADR을 대체합니다.
- **코드와 함께 관리:** `./docs/adr/`에 저장하여 커밋 히스토리로 변화를 추적합니다.
- **가볍게 작성:** 한두 페이지 내외로 핵심만 기록합니다.

### History (작업 히스토리)

당일 수행한 작업 내역을 기록합니다. 인덱스 파일([History.md](docs/history/History.md))에서 전체 타임라인을 확인할 수 있습니다.

#### 파일 규칙

- **위치:** `./docs/history/`
- **파일명:** `YYYY-MM-DD-HH-mm-<작업-제목>.md` (예: `2026-02-18-19-30-프로젝트-초기-셋업.md`)
- **인덱스:** 새 문서 작성 시 `History.md` 타임라인에 링크를 추가합니다.

#### 작성 원칙

1. **사실 기반:** 변경한 내용만 간결하게 기록합니다. 의견이나 감상은 제외합니다.
2. **한 작업, 한 문서:** 하나의 작업 단위(기능 추가, 리팩토링, 버그 수정 등)마다 별도 문서를 작성합니다.
3. **즉시 기록:** 작업 완료 직후 작성하여 누락을 방지합니다.
