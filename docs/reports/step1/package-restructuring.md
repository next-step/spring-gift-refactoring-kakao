# 패키지 구조 변경: 도메인별 → 역할별(레이어별)

## 작업 목적 및 배경

기존 프로젝트는 도메인별(auth, category, member, option, order, product, wish)로 패키지가 구성되어 있었다.
같은 역할의 클래스들(Controller, Service, Repository 등)이 서로 다른 패키지에 분산되어 있어, 역할별로 일관된 관리가 어려웠다.
이를 역할별(레이어별) 패키지 구조로 변경하여, 같은 책임을 가진 클래스들이 한 곳에 모이도록 개선하였다.

## 변경 전 구조

```
gift/
├── Application.java
├── auth/          (12 files: AuthService, JwtProvider, KakaoAuthController, ...)
├── category/      (6 files: Category, CategoryController, CategoryService, ...)
├── common/        (4 files: ErrorResponse, GlobalExceptionHandler, NameValidator, WebConfig)
├── member/        (6 files: Member, MemberController, MemberService, ...)
├── option/        (6 files: Option, OptionController, OptionService, ...)
├── order/         (8 files: Order, OrderController, OrderService, KakaoMessageClient, ...)
├── product/       (7 files: Product, ProductController, ProductService, ...)
└── wish/          (6 files: Wish, WishController, WishService, ...)
```

## 변경 후 구조

```
gift/
├── Application.java
├── auth/          # 인증 인프라 (JwtProvider, LoginMember, AuthenticationResolver, LoginMemberArgumentResolver)
├── client/        # 외부 API 클라이언트 (KakaoLoginClient, KakaoMessageClient)
├── common/        # 공통 유틸리티 (NameValidator)
├── config/        # 설정 (WebConfig, KakaoLoginProperties)
├── controller/    # 모든 Controller (9개)
├── dto/           # 모든 Request/Response DTO (13개)
├── exception/     # 예외 클래스 + GlobalExceptionHandler
├── model/         # 모든 Entity (6개)
├── repository/    # 모든 Repository (6개)
└── service/       # 모든 Service (10개)
```

## 변경 사항 요약

### 새로운 패키지별 파일 구성

| 패키지 | 파일 | 설명 |
|--------|------|------|
| `gift.controller` | AdminMemberController, AdminProductController, CategoryController, KakaoAuthController, MemberController, OptionController, OrderController, ProductController, WishController | 모든 컨트롤러 |
| `gift.service` | AuthService, CategoryService, KakaoAuthService, KakaoNotificationService, MemberService, OptionService, OrderService, ProductService, WishService | 모든 서비스 |
| `gift.repository` | CategoryRepository, MemberRepository, OptionRepository, OrderRepository, ProductRepository, WishRepository | 모든 레포지토리 |
| `gift.model` | Category, Member, Option, Order, Product, Wish | 모든 엔티티 |
| `gift.dto` | CategoryRequest/Response, ErrorResponse, MemberRequest, OptionRequest/Response, OrderRequest/Response, ProductRequest/Response, TokenResponse, WishRequest/Response | 모든 DTO |
| `gift.auth` | AuthenticationResolver, JwtProvider, LoginMember, LoginMemberArgumentResolver | 인증 인프라 |
| `gift.client` | KakaoLoginClient, KakaoMessageClient | 외부 API 클라이언트 |
| `gift.config` | KakaoLoginProperties, WebConfig | 설정 |
| `gift.exception` | ForbiddenException, GlobalExceptionHandler, UnauthorizedException | 예외 처리 |
| `gift.common` | NameValidator | 공통 유틸리티 |

### 삭제된 패키지

- `gift.category` (전체 삭제)
- `gift.member` (전체 삭제)
- `gift.option` (전체 삭제)
- `gift.order` (전체 삭제)
- `gift.product` (전체 삭제)
- `gift.wish` (전체 삭제)

### 테스트 파일 변경

| 변경 전 | 변경 후 |
|---------|---------|
| `gift.category.CategoryControllerTest` | `gift.controller.CategoryControllerTest` |
| `gift.member.MemberControllerTest` | `gift.controller.MemberControllerTest` |
| `gift.option.OptionControllerTest` | `gift.controller.OptionControllerTest` |
| `gift.order.OrderControllerTest` | `gift.controller.OrderControllerTest` |
| `gift.product.ProductControllerTest` | `gift.controller.ProductControllerTest` |
| `gift.wish.WishControllerTest` | `gift.controller.WishControllerTest` |
| `gift.member.MemberTest` | `gift.model.MemberTest` |
| `gift.option.OptionTest` | `gift.model.OptionTest` |
| `gift.common.NameValidatorTest` | `gift.common.NameValidatorTest` (변경 없음) |
| `gift.AcceptanceTest` 등 4개 | 변경 없음 |

## 적용된 규칙

- **역할별 분리**: 같은 책임을 가진 클래스를 하나의 패키지에 모음
- **auth 패키지 유지**: 인증 인프라(JwtProvider, Argument Resolver 등)는 기존 auth 패키지에 유지하되, Controller/Service/DTO/Exception은 해당 역할 패키지로 이동
- **client 패키지 신설**: 외부 API 호출 클래스(KakaoLoginClient, KakaoMessageClient)를 별도 패키지로 분리
- **config 패키지 신설**: 설정 관련 클래스(WebConfig, KakaoLoginProperties)를 별도 패키지로 분리

## 참고 사항

- 모든 기존 테스트가 변경 후에도 정상 통과함을 확인
- 클래스의 내부 로직은 변경하지 않고, 패키지 선언과 import문만 수정
- `Application.java`는 `gift` 루트 패키지에 그대로 유지 (Component Scan 범위 유지)
