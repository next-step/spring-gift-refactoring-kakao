# 테스트 범위 결정 분석 보고서

> 분석 범위: REST API Controller 7개의 전체 엔드포인트, 시나리오, 기존 커버리지, 인수 테스트 선정
> 분석 일자: 2026-02-26

## 1. 분석 목적

리팩토링 안전망 구축을 위한 인수 테스트 작성 전에, 어떤 시나리오를 테스트해야 하는지 분석하고 선정한다.
README Step1의 1-1 체크리스트 4개 항목을 모두 충족한다.

## 2. 분석 범위

### 분석 대상 (REST API Controller 7개)

| # | Controller | 파일 경로 |
|---|---|---|
| 1 | MemberController | `src/main/java/gift/member/MemberController.java` |
| 2 | CategoryController | `src/main/java/gift/category/CategoryController.java` |
| 3 | ProductController | `src/main/java/gift/product/ProductController.java` |
| 4 | OptionController | `src/main/java/gift/option/OptionController.java` |
| 5 | WishController | `src/main/java/gift/wish/WishController.java` |
| 6 | OrderController | `src/main/java/gift/order/OrderController.java` |
| 7 | KakaoAuthController | `src/main/java/gift/auth/KakaoAuthController.java` |

### 분석 제외 대상

| Controller | 제외 사유 |
|---|---|
| AdminProductController | HTML 템플릿 기반, API 리팩토링 대상 아님 |
| AdminMemberController | HTML 템플릿 기반, API 리팩토링 대상 아님 |

## 3. 분석 결과

### 3-1. API 엔드포인트 목록

#### MemberController (2개)

| HTTP | URL | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/members/register` | 불필요 | 회원 가입 |
| POST | `/api/members/login` | 불필요 | 로그인 |

- **근거**: `MemberController.java:33-55`

#### CategoryController (4개)

| HTTP | URL | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/categories` | 불필요 | 카테고리 목록 조회 |
| POST | `/api/categories` | 불필요 | 카테고리 생성 |
| PUT | `/api/categories/{id}` | 불필요 | 카테고리 수정 |
| DELETE | `/api/categories/{id}` | 불필요 | 카테고리 삭제 |

- **근거**: `CategoryController.java:26-60`

#### ProductController (5개)

| HTTP | URL | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/products` | 불필요 | 상품 목록 조회 (페이지네이션) |
| GET | `/api/products/{id}` | 불필요 | 상품 단건 조회 |
| POST | `/api/products` | 불필요 | 상품 생성 |
| PUT | `/api/products/{id}` | 불필요 | 상품 수정 |
| DELETE | `/api/products/{id}` | 불필요 | 상품 삭제 |

- **근거**: `ProductController.java:33-88`

#### OptionController (3개)

| HTTP | URL | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/products/{productId}/options` | 불필요 | 옵션 목록 조회 |
| POST | `/api/products/{productId}/options` | 불필요 | 옵션 생성 |
| DELETE | `/api/products/{productId}/options/{optionId}` | 불필요 | 옵션 삭제 |

- **근거**: `OptionController.java:36-91`

#### WishController (3개)

| HTTP | URL | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/wishes` | **필요** | 위시리스트 조회 (페이지네이션) |
| POST | `/api/wishes` | **필요** | 위시리스트 추가 |
| DELETE | `/api/wishes/{id}` | **필요** | 위시리스트 삭제 |

- **근거**: `WishController.java:37-101`

#### OrderController (2개)

| HTTP | URL | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/orders` | **필요** | 주문 목록 조회 (페이지네이션) |
| POST | `/api/orders` | **필요** | 주문 생성 |

- **근거**: `OrderController.java:47-102`

#### KakaoAuthController (2개)

| HTTP | URL | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/auth/kakao/login` | 불필요 | 카카오 로그인 페이지 리다이렉트 |
| GET | `/api/auth/kakao/callback` | 불필요 | 카카오 인가코드 콜백 처리 |

- **근거**: `KakaoAuthController.java:40-68`

#### 엔드포인트 수 요약

| Controller | 엔드포인트 수 |
|---|---|
| MemberController | 2 |
| CategoryController | 4 |
| ProductController | 5 |
| OptionController | 3 |
| WishController | 3 |
| OrderController | 2 |
| KakaoAuthController | 2 |
| **합계** | **21** |

---

### 3-2. 엔드포인트별 시나리오 (정상/실패)

각 시나리오는 코드 실행 결과 기준으로 도출했다.

#### MemberController

**POST /api/members/register**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| M-1 | 정상 등록 | 201 + JWT 토큰 | `MemberController.java:39-41` |
| M-2 | 이메일 중복 | 400 | `MemberController.java:35-37` — `existsByEmail` 체크 |
| M-3 | 유효성 실패 (이메일 형식, 빈 값) | 400 | `MemberRequest.java:13-14` — `@NotBlank @Email` |

**POST /api/members/login**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| M-4 | 정상 로그인 | 200 + JWT 토큰 | `MemberController.java:46-54` |
| M-5 | 존재하지 않는 이메일 | 400 | `MemberController.java:47` — `orElseThrow` |
| M-6 | 비밀번호 불일치 | 400 | `MemberController.java:49-51` |

#### CategoryController

**GET /api/categories**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| C-1 | 정상 목록 조회 | 200 + 카테고리 배열 | `CategoryController.java:27-32` |

**POST /api/categories**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| C-2 | 정상 생성 | 201 + Location 헤더 | `CategoryController.java:35-38` |
| C-3 | 유효성 실패 (빈 name/color/imageUrl) | 400 | `CategoryRequest.java:6-8` — `@NotBlank` |

**PUT /api/categories/{id}**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| C-4 | 정상 수정 | 200 + 수정된 카테고리 | `CategoryController.java:42-53` |
| C-5 | 존재하지 않는 카테고리 | 404 | `CategoryController.java:47-49` |
| C-6 | 유효성 실패 | 400 | `CategoryRequest.java:6-8` — `@NotBlank` |

**DELETE /api/categories/{id}**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| C-7 | 정상 삭제 | 204 | `CategoryController.java:57-59` |

#### ProductController

**GET /api/products**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| P-1 | 정상 목록 조회 (페이지네이션) | 200 + Page 객체 | `ProductController.java:34-36` |

**GET /api/products/{id}**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| P-2 | 정상 단건 조회 | 200 + 상품 정보 | `ProductController.java:40-45` |
| P-3 | 존재하지 않는 상품 | 404 | `ProductController.java:42-43` |

**POST /api/products**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| P-4 | 정상 생성 | 201 + Location 헤더 | `ProductController.java:49-59` |
| P-5 | 존재하지 않는 카테고리 | 404 | `ProductController.java:53-55` |
| P-6 | 상품명 유효성 실패 ("카카오" 포함) | 400 | `ProductNameValidator.java:35-37` |
| P-7 | 유효성 실패 (빈 name, 0 이하 price 등) | 400 | `ProductRequest.java:9-12` — Bean Validation |

**PUT /api/products/{id}**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| P-8 | 정상 수정 | 200 + 수정된 상품 | `ProductController.java:63-81` |
| P-9 | 존재하지 않는 상품 | 404 | `ProductController.java:75-77` |
| P-10 | 존재하지 않는 카테고리 | 404 | `ProductController.java:70-72` |

**DELETE /api/products/{id}**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| P-11 | 정상 삭제 | 204 | `ProductController.java:85-87` |

#### OptionController

**GET /api/products/{productId}/options**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| O-1 | 정상 목록 조회 | 200 + 옵션 배열 | `OptionController.java:36-44` |
| O-2 | 존재하지 않는 상품 | 404 | `OptionController.java:38-39` |

**POST /api/products/{productId}/options**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| O-3 | 정상 생성 | 201 + Location 헤더 | `OptionController.java:48-66` |
| O-4 | 존재하지 않는 상품 | 404 | `OptionController.java:54-56` |
| O-5 | 중복 옵션명 | 400 | `OptionController.java:59-61` — `existsByProductIdAndName` |
| O-6 | 옵션명 유효성 실패 | 400 | `OptionNameValidator.java:21-37` |

**DELETE /api/products/{productId}/options/{optionId}**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| O-7 | 정상 삭제 (옵션 2개 이상일 때) | 204 | `OptionController.java:69-91` |
| O-8 | 마지막 옵션 삭제 시도 | 400 | `OptionController.java:80-82` — 최소 1개 유지 |
| O-9 | 존재하지 않는 상품 | 404 | `OptionController.java:75-77` |
| O-10 | 존재하지 않는 옵션 | 404 | `OptionController.java:85-87` |

#### WishController

**GET /api/wishes**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| W-1 | 정상 목록 조회 | 200 + Page 객체 | `WishController.java:38-48` |
| W-2 | 인증 실패 | 401 | `WishController.java:44-45` |

**POST /api/wishes**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| W-3 | 정상 추가 | 201 + Location 헤더 | `WishController.java:52-76` |
| W-4 | 중복 추가 (멱등 처리) | 200 + 기존 위시 반환 | `WishController.java:69-71` |
| W-5 | 존재하지 않는 상품 | 404 | `WishController.java:63-65` |
| W-6 | 인증 실패 | 401 | `WishController.java:58-59` |

**DELETE /api/wishes/{id}**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| W-7 | 정상 삭제 | 204 | `WishController.java:79-100` |
| W-8 | 다른 사용자의 위시 삭제 시도 | 403 | `WishController.java:95-96` |
| W-9 | 존재하지 않는 위시 | 404 | `WishController.java:91-93` |
| W-10 | 인증 실패 | 401 | `WishController.java:85-86` |

#### OrderController

**GET /api/orders**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| R-1 | 정상 목록 조회 | 200 + Page 객체 | `OrderController.java:47-58` |
| R-2 | 인증 실패 | 401 | `OrderController.java:54-55` |

**POST /api/orders**

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| R-3 | 정상 주문 (재고 차감 + 포인트 차감) | 201 | `OrderController.java:69-101` |
| R-4 | 재고 부족 | 500 | `Option.java:40-43` — `subtractQuantity`에서 예외 발생, 핸들러 없음 |
| R-5 | 포인트 부족 | 500 | `Member.java:61-63` — `deductPoint`에서 예외 발생, 핸들러 없음 |
| R-6 | 존재하지 않는 옵션 | 404 | `OrderController.java:82-83` |
| R-7 | 인증 실패 | 401 | `OrderController.java:76-77` |

> **발견 사항**: OrderController에는 `@ExceptionHandler`가 없다. `Option.subtractQuantity()`와 `Member.deductPoint()`에서 발생하는 `IllegalArgumentException`이 잡히지 않아 500으로 응답된다. 이는 현재 코드의 실제 동작이며, 분석 단계에서는 코드 실행 결과 기준으로 기록한다.

#### KakaoAuthController

| # | 시나리오 | 기대 응답 | 근거 |
|---|---|---|---|
| K-1 | 로그인 리다이렉트 | 302 + Location 헤더 (카카오 URL) | `KakaoAuthController.java:41-52` |
| K-2 | 콜백 정상 처리 | 200 + JWT 토큰 | `KakaoAuthController.java:55-67` |

---

### 3-3. 기존 테스트 커버리지 분석

현재 3개 feature 파일, 8개 시나리오가 존재한다.

#### 이미 테스트된 시나리오

| feature 파일 | 시나리오 | 커버하는 엔드포인트 | 매핑 시나리오 |
|---|---|---|---|
| `category.feature` | 카테고리를 생성하면 목록에서 조회된다 | POST + GET `/api/categories` | C-1, C-2 |
| `product.feature` | 상품을 생성하면 목록에서 조회된다 | POST + GET `/api/products` | P-1, P-4 |
| `product.feature` | 존재하지 않는 카테고리로 상품을 등록하면 실패한다 | POST `/api/products` | P-5 |
| `gift.feature` | 주문이 성공하면 옵션 재고가 차감된다 | POST `/api/orders` + GET options | R-3 |
| `gift.feature` | 재고보다 많은 수량을 주문하면 실패하고 재고는 변경되지 않는다 | POST `/api/orders` + GET options | R-4 |
| `gift.feature` | 존재하지 않는 옵션으로 주문하면 실패한다 | POST `/api/orders` | R-6 |
| `gift.feature` | 인증되지 않은 사용자가 주문하면 실패한다 | POST `/api/orders` | R-7 |

> **참고**: gift.feature의 배경에서 회원 등록(register)과 옵션 목록 조회를 간접적으로 사용하지만, 이들은 전용 시나리오가 아닌 테스트 데이터 셋업용이다.

#### 테스트 안 된 엔드포인트/시나리오

| Controller | 빠진 엔드포인트 | 빠진 시나리오 |
|---|---|---|
| **MemberController** | register, login (전용 시나리오 없음) | M-1~M-6 전체 |
| **CategoryController** | PUT, DELETE | C-4~C-7 (수정, 삭제) |
| **ProductController** | GET /{id}, PUT, DELETE | P-2, P-3, P-6~P-11 (단건 조회, 수정, 삭제, 이름 유효성) |
| **OptionController** | GET, POST, DELETE (전체) | O-1~O-10 전체 |
| **WishController** | GET, POST, DELETE (전체) | W-1~W-10 전체 |
| **OrderController** | GET (목록 조회) | R-1, R-2, R-5 (목록 조회, 포인트 부족) |
| **KakaoAuthController** | login, callback (전체) | K-1, K-2 전체 |

#### 커버리지 요약

- **테스트 있음**: 7개 시나리오 (21개 엔드포인트 중 6개 부분 커버)
- **테스트 없음**: 약 38개 시나리오 미커버
- **완전 미커버 Controller**: Member, Option, Wish (전용 시나리오 0개)

---

### 3-4. 인수 테스트에 반영할 시나리오 선정

#### 설계 원칙: 조회 시나리오 통합

생성 시나리오에서 조회를 함께 검증한다. 별도 조회 테스트는 작성하지 않는다.
정렬/필터/페이지네이션 등 조회 고유 요구사항이 생기면 그때 분리한다.

- 기존 테스트가 이미 이 패턴을 따르고 있다:
  - `category.feature`: "카테고리를 **생성**하면 **목록에서 조회**된다"
  - `product.feature`: "상품을 **생성**하면 **목록에서 조회**된다"
  - `gift.feature`: "주문이 **성공**하면 옵션 재고가 **차감되어 있다**" (옵션 목록 조회로 검증)

#### 제외 대상

| 대상 | 제외 사유 |
|---|---|
| **KakaoAuthController** (K-1, K-2) | 외부 Kakao API 의존. Docker E2E 환경에서 카카오 서버 모킹 불가. 리팩토링 안전망 관점에서 비용 대비 효과 낮음 |
| **AdminProductController, AdminMemberController** | HTML 템플릿 기반. API 리팩토링 범위 밖 |

#### 신규 작성 대상 시나리오

아래 시나리오를 feature 파일로 작성한다. 우선순위는 리팩토링 대상과의 관련도 기준.

**우선순위 1 — 리팩토링 핵심 대상 (OrderController)**

| ID | 시나리오 | 기대 응답 | 사유 |
|---|---|---|---|
| R-5 | 주문 생성 - 포인트 부족 | 500 | OrderController 핵심 비즈니스 로직. 기존 시나리오에서 미커버 |

> R-1(주문 목록 조회)은 기존 R-3 시나리오에 "주문 목록에서 조회" 검증 스텝을 추가하여 통합한다.

**우선순위 2 — 완전 미커버 도메인 (Member, Option, Wish)**

| ID | 시나리오 | 기대 응답 | 사유 |
|---|---|---|---|
| M-1 | 회원 가입 성공 | 201 | 인증 흐름의 기반 |
| M-2 | 회원 가입 - 이메일 중복 | 400 | 중복 방지 검증 |
| M-4 | 로그인 성공 | 200 | 인증 흐름의 기반 |
| M-5 | 로그인 - 존재하지 않는 이메일 | 400 | 실패 케이스 |
| O-3 | 옵션을 생성하면 목록에서 조회된다 | 201→200 | 생성 + 목록 조회 통합 |
| O-5 | 옵션 생성 - 중복 옵션명 | 400 | 비즈니스 규칙 |
| O-7 | 옵션 삭제 (2개 이상일 때) | 204 | Option CRUD |
| O-8 | 옵션 삭제 - 마지막 옵션 | 400 | 핵심 비즈니스 규칙 |
| W-3 | 위시를 추가하면 목록에서 조회된다 | 201→200 | 생성 + 목록 조회 통합 |
| W-4 | 위시 중복 추가 (멱등) | 200 | 비즈니스 규칙 |
| W-7 | 위시 삭제 | 204 | Wish CRUD |
| W-8 | 위시 삭제 - 다른 사용자 | 403 | 권한 검증 (인증 경로도 간접 검증) |

**우선순위 3 — 빠진 CRUD 시나리오 (Category, Product)**

| ID | 시나리오 | 기대 응답 | 사유 |
|---|---|---|---|
| C-4 | 카테고리 수정 | 200 | CRUD 완성 |
| C-7 | 카테고리 삭제 | 204 | CRUD 완성 |
| P-8 | 상품 수정 | 200 | CRUD 완성 |
| P-11 | 상품 삭제 | 204 | CRUD 완성 |

#### 선정 제외

**조회 시나리오 통합으로 제외:**

| ID | 시나리오 | 제외 사유 |
|---|---|---|
| R-1 | 주문 목록 조회 | 기존 R-3(주문 성공) 시나리오에 목록 조회 스텝 추가로 통합 |
| R-2 | 주문 목록 조회 - 인증 실패 | R-7에서 동일 인증 메커니즘 이미 커버 |
| O-1 | 옵션 목록 조회 | O-3(옵션 생성) 시나리오에 목록 조회 통합 |
| W-1 | 위시 목록 조회 | W-3(위시 추가) 시나리오에 목록 조회 통합 |
| P-2 | 상품 단건 조회 | 기존 생성 시나리오에서 이미 커버, 수정(P-8)에서도 간접 커버 |
| P-3 | 상품 단건 조회 - 없음 | P-5(존재하지 않는 카테고리)에서 404 패턴 이미 커버 |

**과잉 시나리오 제외:**

| ID | 시나리오 | 제외 사유 |
|---|---|---|
| W-10 | 위시 인증 실패 | `authenticationResolver.extractMember()` 동일 메커니즘을 R-7에서 이미 커버. W-8에서 인증 경로 간접 검증 |
| C-5 | 카테고리 수정 - 404 | P-5에서 동일 404 패턴 이미 커버 |

**유효성 검증 시나리오 제외:**

| ID | 시나리오 | 제외 사유 |
|---|---|---|
| M-3 | 이메일 형식 유효성 | Bean Validation 기본 동작 |
| M-6 | 비밀번호 불일치 | M-5와 동일 응답(400), 외부에서 구분 불가 |
| C-3, C-6 | 카테고리 유효성 실패 | Bean Validation 기본 동작 |
| P-6, P-7 | 상품명/필드 유효성 | Bean Validation + 이름 검증기는 단위 테스트 영역 |
| P-9, P-10 | 상품 수정 시 404 | 생성 시 404(P-5)와 동일 패턴 |
| O-2, O-4, O-6 | 옵션 404/유효성 | 기본 패턴 반복 |
| O-9, O-10 | 옵션 삭제 404 | 기본 패턴 반복 |
| W-2, W-5, W-6, W-9 | 위시 401/404 | 인증은 R-7에서 대표 커버, 404는 기본 패턴 |

---

## 4. 종합 요약

### 전체 현황

| 항목 | 수치 |
|---|---|
| REST API Controller | 7개 |
| 전체 엔드포인트 | 21개 |
| 도출된 전체 시나리오 | 약 45개 |
| 기존 테스트 시나리오 | 7개 (3 feature 파일) |
| 신규 작성 대상 시나리오 | 17개 |
| 제외 — KakaoAuth | 2개 |
| 제외 — 조회 통합 | 6개 |
| 제외 — 과잉 (패턴 중복) | 2개 |
| 제외 — 유효성 검증 | 약 13개 |

### 신규 작성 대상 feature 파일 구성 (안)

| feature 파일 | 시나리오 수 | 내용 |
|---|---|---|
| `member.feature` (신규) | 4개 | 회원가입 성공/중복, 로그인 성공/실패 |
| `category.feature` (기존 확장) | +2개 | 수정, 삭제 |
| `product.feature` (기존 확장) | +2개 | 수정, 삭제 |
| `option.feature` (신규) | 4개 | 생성(+목록 검증), 중복명 실패, 삭제, 마지막 옵션 삭제 실패 |
| `wish.feature` (신규) | 4개 | 추가(+목록 검증), 중복 추가, 삭제, 타인 위시 삭제 실패 |
| `gift.feature` (기존 확장) | +1개 | 포인트 부족 |
| **합계** | **17개** (기존 7 + 신규 17 = 24개) | |

> 기존 gift.feature의 "주문이 성공하면 옵션 재고가 차감된다" 시나리오에 주문 목록 조회 검증 스텝을 추가하여 GET /api/orders를 커버한다.

## 5. 권장 사항

1. **OrderController의 예외 처리 누락**: `subtractQuantity`, `deductPoint`에서 발생하는 `IllegalArgumentException`이 핸들링되지 않아 500으로 응답된다. 인수 테스트는 현재 동작(500) 기준으로 작성하되, 서비스 계층 추출(Step 4) 시 적절한 예외 처리를 추가하는 것을 권장한다.
2. **테스트 작성 순서**: 우선순위 1(Order) → 2(Member, Option, Wish) → 3(Category, Product CRUD 확장) 순서로 작성하면 리팩토링 핵심 대상의 안전망이 먼저 구축된다.
3. **KakaoAuth 테스트**: 현재 E2E 환경에서는 제외하되, 향후 WireMock 등으로 외부 API를 모킹할 수 있는 환경이 구성되면 추가를 검토한다.
