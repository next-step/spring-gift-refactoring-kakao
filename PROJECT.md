# Spring Gift - 선물 쇼핑 플랫폼

## 개요

Spring Boot 기반의 선물/쇼핑 플랫폼으로, 상품 관리, 위시리스트, 포인트 결제, 주문 기능을 제공한다. JWT 인증과 카카오 OAuth2 로그인을 지원하며, 관리자용 HTML UI와 회원용 REST API를 동시에 제공한다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Framework | Spring Boot 3.5.9 |
| Language | Java 25 (Kotlin 지원) |
| Database | MySQL (운영) / H2 (테스트) |
| Migration | Flyway |
| Authentication | JWT (JJWT 0.13.0) + Kakao OAuth2 |
| Template | Thymeleaf |
| Validation | Jakarta Bean Validation |
| Build | Gradle (Kotlin DSL) |

## 프로젝트 구조

```
src/main/java/gift/
├── auth/                  # 인증/인가 (JWT, Kakao OAuth)
│   ├── AuthenticationResolver.java
│   ├── JwtProvider.java
│   ├── KakaoAuthController.java
│   ├── KakaoLoginClient.java
│   ├── KakaoLoginProperties.java
│   ├── KakaoTokenResponse.java
│   ├── KakaoUserResponse.java
│   └── TokenResponse.java
├── category/              # 카테고리
│   ├── Category.java
│   ├── CategoryController.java
│   ├── CategoryRepository.java
│   ├── CategoryRequest.java
│   └── CategoryResponse.java
├── member/                # 회원
│   ├── AdminMemberController.java
│   ├── Member.java
│   ├── MemberController.java
│   ├── MemberRepository.java
│   └── MemberRequest.java
├── option/                # 상품 옵션
│   ├── Option.java
│   ├── OptionController.java
│   ├── OptionNameValidator.java
│   ├── OptionRepository.java
│   ├── OptionRequest.java
│   └── OptionResponse.java
├── order/                 # 주문
│   ├── KakaoMessageClient.java
│   ├── Order.java
│   ├── OrderController.java
│   ├── OrderRepository.java
│   ├── OrderRequest.java
│   └── OrderResponse.java
├── product/               # 상품
│   ├── AdminProductController.java
│   ├── Product.java
│   ├── ProductController.java
│   ├── ProductNameValidator.java
│   ├── ProductRepository.java
│   ├── ProductRequest.java
│   └── ProductResponse.java
├── wish/                  # 위시리스트
│   ├── Wish.java
│   ├── WishController.java
│   ├── WishRepository.java
│   ├── WishRequest.java
│   └── WishResponse.java
└── Application.java
```

## 데이터 모델

```
category ──< product ──< options
                │            │
                │            │
               wish        orders
                │            │
                └── member ──┘
```

| 테이블 | 설명 | 주요 컬럼 |
|--------|------|-----------|
| `category` | 상품 카테고리 | name (unique), color, image_url, description |
| `product` | 상품 | name (15자 제한), price, image_url, category_id (FK) |
| `options` | 상품 옵션(재고) | product_id (FK), name (50자 제한), quantity |
| `member` | 회원 | email (unique), password, kakao_access_token, point |
| `wish` | 위시리스트 | member_id (FK), product_id (FK) |
| `orders` | 주문 | option_id (FK), member_id (FK), quantity, message, order_date_time |

## API 엔드포인트

### 인증

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/members/register` | 회원 가입 (JWT 반환) |
| POST | `/api/members/login` | 로그인 (JWT 반환) |
| GET | `/api/auth/kakao/login` | 카카오 로그인 리다이렉트 |
| GET | `/api/auth/kakao/callback` | 카카오 OAuth 콜백 |

### 카테고리

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/categories` | 전체 카테고리 조회 |
| POST | `/api/categories` | 카테고리 생성 |
| PUT | `/api/categories/{id}` | 카테고리 수정 |
| DELETE | `/api/categories/{id}` | 카테고리 삭제 |

### 상품

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/products` | 상품 목록 (페이지네이션) |
| GET | `/api/products/{id}` | 상품 상세 |
| POST | `/api/products` | 상품 생성 |
| PUT | `/api/products/{id}` | 상품 수정 |
| DELETE | `/api/products/{id}` | 상품 삭제 |

### 옵션

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/products/{productId}/options` | 옵션 목록 |
| POST | `/api/products/{productId}/options` | 옵션 추가 |
| DELETE | `/api/products/{productId}/options/{optionId}` | 옵션 삭제 (최소 1개 유지) |

### 위시리스트 (인증 필요)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/wishes` | 위시리스트 조회 (페이지네이션) |
| POST | `/api/wishes` | 위시리스트 추가 |
| DELETE | `/api/wishes/{id}` | 위시리스트 삭제 |

### 주문 (인증 필요)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/orders` | 주문 목록 (페이지네이션) |
| POST | `/api/orders` | 주문 생성 |

### 관리자 (HTML)

| Path | 설명 |
|------|------|
| `/admin/members/**` | 회원 관리 (목록, 생성, 수정, 포인트 충전, 삭제) |
| `/admin/products/**` | 상품 관리 (목록, 생성, 수정, 삭제) |

## 핵심 비즈니스 규칙

### 상품명 검증
- 최대 15자, 허용 문자: 한글, 영문, 숫자, `( ) [ ] + - & / _`
- "카카오" 포함 시 별도 승인 필요 (`allowKakao` 플래그)

### 옵션명 검증
- 최대 50자 (공백 포함), 상품명과 동일한 문자 규칙

### 주문 프로세스
1. 인증 확인 (Bearer JWT)
2. 옵션 존재 여부 확인
3. 옵션 재고 차감 (`subtractQuantity`)
4. 회원 포인트 차감 (`deductPoint` = price * quantity)
5. 주문 저장
6. 카카오톡 알림 전송 (Best-effort, 실패해도 주문 성공)

### 포인트 시스템
- 관리자가 회원에게 포인트 충전 가능
- 주문 시 `상품 가격 * 수량`만큼 차감
- 잔액 부족 시 주문 불가 (`IllegalArgumentException`)

## 아키텍처 특징

- **도메인별 패키지 구조**: 기능 단위로 패키지를 분리하여 응집도 향상
- **Record 기반 DTO**: 불변 객체인 Java Record로 요청/응답 정의
- **Primitive FK 패턴**: `Wish`, `Order`에서 `memberId`를 원시 타입으로 유지하여 불필요한 엔티티 로딩 방지
- **Cascade 삭제**: `Product` 삭제 시 연관된 `Option`을 함께 삭제 (`CascadeType.ALL`, `orphanRemoval`)
- **이중 레이어**: REST API (`@RestController`)와 관리자 HTML UI (`@Controller`) 동시 제공
- **Best-effort 외부 연동**: 카카오톡 알림 실패가 주문 성공에 영향을 주지 않음

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `JWT_SECRET` | JWT 서명 키 | `a-string-secret-at-least-256-bits-long` |
| `JWT_EXPIRATION` | JWT 만료 시간 (ms) | `3600000` (1시간) |
| `KAKAO_CLIENT_ID` | 카카오 앱 클라이언트 ID | - |
| `KAKAO_CLIENT_SECRET` | 카카오 앱 클라이언트 시크릿 | - |
| `KAKAO_REDIRECT_URI` | 카카오 OAuth 콜백 URI | `http://localhost:8080/api/auth/kakao/callback` |

## 실행 방법

```bash
./gradlew bootRun
```

관리자 페이지: `http://localhost:8080/admin/products`
