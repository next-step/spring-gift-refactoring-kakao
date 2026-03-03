# 테스트 코드 작성 계획

## 현재 상태
- `OrderControllerTest`만 존재 (7개 테스트)
- Service 계층을 새로 추출했으니 Service 단위 테스트가 필요
- Controller 테스트도 `OrderController` 외에는 없음

## 테스트 방식
- **Service 테스트**: 순수 단위 테스트. Repository를 `@Mock`으로 모킹하고 비즈니스 로직만 검증
- **Controller 테스트**: `@WebMvcTest`로 HTTP 매핑 검증. Service를 `@MockitoBean`으로 모킹
- 기존 `OrderControllerTest`의 패턴(BDDMockito, `@Nested`, `@DisplayName`, Reflection setId)을 따름

## 진행 순서

비즈니스 로직이 복잡한 Service부터 테스트하고, 그 다음 Controller를 테스트한다.

### 1. `OrderServiceTest`
- `createOrder()`: 정상 주문, 옵션 미존재 시 예외, 재고 차감/포인트 차감 확인
- `getOrders()`: 페이징 조회
- 카카오 알림: 토큰 있을 때 전송, 없을 때 스킵, 실패해도 주문은 성공

### 2. `MemberServiceTest`
- `register()`: 정상 가입, 이메일 중복 시 예외
- `login()`: 정상 로그인, 이메일 없을 때 예외, 비밀번호 틀릴 때 예외
- `chargePoint()`, `updateMember()`, `deleteMember()` 등 Admin용 메서드

### 3. `OptionServiceTest`
- `createOption()`: 정상 생성, 상품 미존재 시 예외, 옵션명 중복 시 예외, 옵션명 검증 실패 시 예외
- `deleteOption()`: 정상 삭제, 옵션 1개뿐일 때 삭제 불가, 옵션 미존재 시 예외
- `getOptions()`: 정상 조회, 상품 미존재 시 예외

### 4. `WishServiceTest`
- `addWish()`: 신규 위시 생성(created=true), 중복 위시(created=false), 상품 미존재 시 예외
- `removeWish()`: 정상 삭제, 위시 미존재 시 예외, 본인 것 아닐 때 예외
- `getWishes()`: 페이징 조회

### 5. `ProductServiceTest`
- `createProduct()`: 정상 생성, 상품명 검증 실패 시 예외, 카테고리 미존재 시 예외
- `updateProduct()`: 정상 수정, 상품 미존재 시 예외
- `deleteProduct()`, `getProduct()`, `getProducts()`
- `allowKakao` 파라미터에 따른 상품명 검증 차이

### 6. `CategoryServiceTest`
- `createCategory()`, `updateCategory()`, `deleteCategory()`, `getCategories()`
- `updateCategory()` 시 카테고리 미존재 예외

### 7. `KakaoAuthServiceTest`
- `buildKakaoAuthUrl()`: URL에 client_id, redirect_uri, scope 포함 확인
- `handleCallback()`: 기존 회원이면 토큰 갱신, 신규 회원이면 생성

### 8. `MemberControllerTest`
- POST /api/members/register: 정상 가입(201), 이메일 중복(400), validation 실패(400)
- POST /api/members/login: 정상 로그인(200), 인증 실패(400)

### 9. `OptionControllerTest`
- GET /api/products/{id}/options: 정상 조회(200), 상품 미존재(404)
- POST /api/products/{id}/options: 정상 생성(201), 상품 미존재(404), 검증 실패(400)
- DELETE /api/products/{id}/options/{optionId}: 정상 삭제(204), 미존재(404), 최소 옵션(400)

### 10. `WishControllerTest`
- GET /api/wishes: 정상(200), 인증 실패(401)
- POST /api/wishes: 정상 신규(201), 기존(200), 상품 미존재(404), 인증 실패(401)
- DELETE /api/wishes/{id}: 정상(204), 미존재(404), 권한 없음(403), 인증 실패(401)

### 11. `ProductControllerTest`
- GET /api/products: 정상(200)
- GET /api/products/{id}: 정상(200), 미존재(404)
- POST /api/products: 정상(201), 검증 실패(400), 카테고리 미존재(404)
- PUT /api/products/{id}: 정상(200), 미존재(404)
- DELETE /api/products/{id}: 정상(204)

### 12. `CategoryControllerTest`
- CRUD 엔드포인트 기본 동작 확인

## 제약
- 작동 변경 금지. 테스트는 기존 동작을 검증만 함
- Admin Controller(`AdminProductController`, `AdminMemberController`)는 Thymeleaf 뷰를 반환하는 컨트롤러라 이번에는 제외

## 검증
매 테스트 파일 작성 후:
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test
```
