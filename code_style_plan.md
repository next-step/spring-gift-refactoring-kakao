# 리팩토링 계획: 구조 변경으로 변경 난이도 낮추기

## Context

현재 프로젝트는 Service 계층 없이 Controller가 Repository를 직접 사용하고 있다. 코드 스타일도 작성자에 따라 불일치가 많다. 이 상태에서 기능을 추가하거나 수정하면 Controller가 비대해지고 중복 코드가 늘어난다. 이번 작업의 목표는 **작동은 그대로 유지하면서 구조를 정리**하여, 이후 기능 변경이 쉬운 코드베이스를 만드는 것이다.

3단계로 진행한다:
1. **Phase 1**: 스타일 정리 (작동 변경 없음)
2. **Phase 2**: 불필요한 코드 제거 (작동 변경 없음)
3. **Phase 3**: 서비스 계층 추출 (구조 변경, 작동 변경 없음)

---

## Phase 1: 스타일 정리

### 1-1. `@Autowired` 제거 [x]

Spring 4.3+ 단일 생성자 자동 주입. 4개 파일에서 `@Autowired` 어노테이션 + import 제거.

| 파일 | 라인 |
|------|------|
| `gift/member/MemberController.java` | `@Autowired` + import |
| `gift/member/AdminMemberController.java` | `@Autowired` + import |
| `gift/auth/AuthenticationResolver.java` | `@Autowired` + import |
| `gift/auth/JwtProvider.java` | `@Autowired` + import |

### 1-2. 불필요한 클래스 레벨 주석 제거 [x]

Javadoc(`@author`, `@since`)과 블록 주석 제거. 단, `JwtProvider`의 **메서드 레벨** Javadoc은 API 문서 역할이므로 유지.

| 파일 | 제거 대상 |
|------|-----------|
| `gift/member/Member.java` | 클래스 레벨 Javadoc |
| `gift/member/MemberController.java` | 클래스 레벨 Javadoc |
| `gift/member/AdminMemberController.java` | 클래스 레벨 Javadoc |
| `gift/member/MemberRepository.java` | 클래스 레벨 Javadoc |
| `gift/member/MemberRequest.java` | 클래스 레벨 Javadoc |
| `gift/auth/AuthenticationResolver.java` | 클래스 레벨 Javadoc |
| `gift/auth/JwtProvider.java` | 클래스 레벨 Javadoc만 |
| `gift/auth/TokenResponse.java` | 클래스 레벨 Javadoc |
| `gift/auth/KakaoAuthController.java` | 블록 주석 |
| `gift/option/OptionController.java` | 블록 주석 |

### 1-3. `collect(Collectors.toList())` → `.toList()` 통일 [x]

`gift/option/OptionController.java`:
- `.collect(Collectors.toList())` → `.toList()`
- `import java.util.stream.Collectors` 제거

반환값이 응답 직렬화에만 사용되므로 불변 리스트 전환에 문제 없음.

### 1-4. HTTP 상태코드 숫자 → `HttpStatus` 상수 통일 [x]

| 파일 | 변경 |
|------|------|
| `gift/wish/WishController.java` | `401` → `HttpStatus.UNAUTHORIZED`, `403` → `HttpStatus.FORBIDDEN` |
| `gift/order/OrderController.java` | `401` → `HttpStatus.UNAUTHORIZED` |

각 파일에 `import org.springframework.http.HttpStatus` 추가.

### 1-5. `@RequestMapping(path = ...)` → `@RequestMapping("...")` 통일 [x]

| 파일 | 변경 |
|------|------|
| `gift/auth/KakaoAuthController.java` | `path = ` 제거 |
| `gift/option/OptionController.java` | `path = ` 제거 |

### 1-6. 에러 메시지 한국어 통일 [x]

| 파일 | 변경 전 → 변경 후 |
|------|-------------------|
| `gift/member/Member.java` | `"Amount must be greater than zero."` → `"충전 금액은 1 이상이어야 합니다."` |
| `gift/member/MemberController.java` | `"Email is already registered."` → `"이미 등록된 이메일입니다."` |
| `gift/member/MemberController.java` | `"Invalid email or password."` → `"이메일 또는 비밀번호가 올바르지 않습니다."` |
| `gift/member/AdminMemberController.java` | `"Email is already registered."` → `"이미 등록된 이메일입니다."` |
| `gift/member/AdminMemberController.java` | `"Member not found. id="` → `"회원이 존재하지 않습니다. id="` |

---

## Phase 2: 불필요한 코드 제거

### 2-1. `@ExceptionHandler` 중복 → `GlobalExceptionHandler`로 통합 [x]

3개 Controller에 동일한 `@ExceptionHandler(IllegalArgumentException.class)` 코드가 중복.

**신규 파일**: `gift/config/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

**제거 대상**:
| 파일 | 제거 내용 |
|------|-----------|
| `gift/product/ProductController.java` | `@ExceptionHandler` 메서드 + `ExceptionHandler` import |
| `gift/option/OptionController.java` | `@ExceptionHandler` 메서드 + `ExceptionHandler` import |
| `gift/member/MemberController.java` | `@ExceptionHandler` 메서드 + `ExceptionHandler` import |

### 2-2. 보류 항목 (이번 단계에서 건드리지 않음) [x]

- **OrderController의 WishRepository 미사용 주입**: Phase 3에서 OrderService 추출 시 함께 정리
- **Kotlin 플러그인**: 빌드 설정 변경은 위험도 높으므로 별도 작업으로 분리

---

## TDD 테스트 전략

- **Unit 테스트 (Mockito)**: Service 단위 테스트. Repository/외부 의존성 모두 Mock
- **Validator 테스트**: ProductNameValidator, OptionNameValidator 순수 함수 테스트 (Mock 불필요)
- 테스트 언어: Java (기존 프로덕션 코드와 동일)
- 예상 테스트 수: ~75개

### TDD 사이클 (서비스당)

1. **RED**: 컨트롤러에서 동작 목록 추출 → 테스트 메서드 작성 → 빈 서비스 스켈레톤 → 테스트 실패
2. **GREEN**: 컨트롤러 로직을 서비스로 이동 → 테스트 통과
3. **REFACTOR**: 컨트롤러를 서비스 의존으로 변경 → `./gradlew test` + `./gradlew compileJava` 확인

### 테스트 파일 구조

```
src/test/
├── java/gift/
│   ├── TestFixtures.java
│   ├── category/CategoryServiceTest.java
│   ├── product/
│   │   ├── ProductNameValidatorTest.java
│   │   └── ProductServiceTest.java
│   ├── member/MemberServiceTest.java
│   ├── option/
│   │   ├── OptionNameValidatorTest.java
│   │   └── OptionServiceTest.java
│   ├── wish/WishServiceTest.java
│   ├── auth/KakaoAuthServiceTest.java
│   └── order/OrderServiceTest.java
└── resources/
    └── application-test.properties
```

---

## Phase 3: 서비스 계층 추출 [x]

### 공통 설계 원칙

- `@Service` + `@Transactional(readOnly = true)` 클래스 기본
- 변경 메서드에만 `@Transactional` 추가
- 조회 실패 시 `NoSuchElementException`, 비즈니스 규칙 위반 시 `IllegalArgumentException`
- Controller는 HTTP 관심사만 (인증 확인, 응답 생성, 폼 렌더링)

### 사전 작업

#### 0-1. 테스트 설정 파일 생성

**신규**: `src/test/resources/application-test.properties`

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.flyway.enabled=false
spring.jpa.hibernate.ddl-auto=create-drop
jwt.secret=test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha
jwt.expiration=3600000
kakao.login.client-id=test-client-id
kakao.login.client-secret=test-client-secret
kakao.login.redirect-uri=http://localhost:8080/api/auth/kakao/callback
```

Flyway는 MySQL용 마이그레이션이므로 테스트에서 비활성화, H2 + JPA ddl-auto 사용.

#### 0-2. TestFixtures 유틸 생성

**신규**: `src/test/java/gift/TestFixtures.java`

Entity 생성 팩토리 메서드 모음 (Category, Product, Member, Option 등). 모든 테스트에서 공유.

#### 0-3. GlobalExceptionHandler 확장

Phase 3 시작 전에 핸들러 추가:

```java
@ExceptionHandler(NoSuchElementException.class)
public ResponseEntity<String> handleNoSuchElement(NoSuchElementException e) {
    return ResponseEntity.notFound().build();
}

@ExceptionHandler(IllegalStateException.class)
public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
}
```

기존 `orElse(null)` + null 체크 → 404 패턴이 `orElseThrow()` → `NoSuchElementException` → 404로 대체되어 외부 응답 동일.
기존 `status(403)` → `IllegalStateException` → 403으로 대체되어 외부 응답 동일.

### 3-1. CategoryService

**신규**: `gift/category/CategoryService.java`
**수정**: `gift/category/CategoryController.java` (Repository → Service 의존)

```java
@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    // findAll(): List<Category>
    // create(CategoryRequest): Category
    // update(Long id, CategoryRequest): Category  ← orElseThrow
    // delete(Long id): void
}
```

**테스트** (`CategoryServiceTest`, ~5개):

| 테스트 | 검증 내용 |
|--------|----------|
| `findAll_returnsAllCategories` | Repository 위임 + 응답 매핑 |
| `create_savesAndReturns` | save 호출 + 반환 |
| `update_existingCategory_updatesFields` | findById → update → save |
| `update_nonExistent_throwsNoSuchElementException` | orElseThrow 동작 |
| `delete_delegatesToRepository` | deleteById 호출 |

### 3-2. ProductService

**신규**: `gift/product/ProductService.java`
**수정**: `gift/product/ProductController.java`, `gift/product/AdminProductController.java`

```java
@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // findAll(Pageable): Page<Product>
    // findAll(): List<Product>           ← Admin 목록용
    // findById(Long id): Product         ← orElseThrow
    // create(ProductRequest): Product    ← validateName(allowKakao=false) + 카테고리 조회
    // update(Long id, ProductRequest): Product
    // delete(Long id): void
    // findCategoryById(Long id): Category ← Admin 폼용
    // findAllCategories(): List<Category> ← Admin 폼용
}
```

**AdminProductController 특이사항**: Admin은 `ProductNameValidator.validate(name, true)`로 "카카오" 허용 + 에러를 폼에 표시해야 한다. Admin Controller에서 직접 `ProductNameValidator.validate()`를 호출하여 에러 리스트를 폼에 전달하고, 검증 통과 시 Service의 저장 메서드를 호출하는 구조로 한다. 즉 Admin 검증은 Controller에 남기고, Service에는 별도 `saveProduct(name, price, imageUrl, category)` / `updateProduct(id, name, price, imageUrl, category)` 메서드를 둔다.

**테스트 — ProductNameValidatorTest** (~6개):

| 테스트 | 검증 내용 |
|--------|----------|
| `validate_validName_returnsEmpty` | 정상 이름 |
| `validate_exceedsMaxLength_returnsError` | 15자 초과 |
| `validate_invalidChars_returnsError` | 허용 외 특수문자 |
| `validate_containsKakao_returnsError` | "카카오" 포함 |
| `validate_containsKakao_allowTrue_noError` | allowKakao=true |
| `validate_blankName_returnsError` | 빈 문자열 |

**테스트 — ProductServiceTest** (~11개):

| 테스트 | 검증 내용 |
|--------|----------|
| `findAll_returnsPage` | Pageable 위임 |
| `findById_existing_returns` | 정상 조회 |
| `findById_nonExistent_throws` | NoSuchElementException |
| `create_validRequest_saves` | 검증 + 카테고리 조회 + 저장 |
| `create_invalidName_throws` | 이름 검증 실패 |
| `create_categoryNotFound_throws` | 카테고리 없음 |
| `create_kakaoName_allowFalse_throws` | API 경로 "카카오" 차단 |
| `create_kakaoName_allowTrue_succeeds` | Admin 경로 "카카오" 허용 |
| `update_happyPath_updates` | 전체 필드 업데이트 |
| `update_notFound_throws` | 존재하지 않는 상품 |
| `delete_delegatesToRepository` | 삭제 위임 |

### 3-3. MemberService

**신규**: `gift/member/MemberService.java`
**수정**: `gift/member/MemberController.java`, `gift/member/AdminMemberController.java`

```java
@Service
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    // findAll(): List<Member>
    // findById(Long id): Member           ← orElseThrow
    // register(String email, String pw): TokenResponse  ← 중복 검증 + 저장 + JWT
    // login(String email, String pw): TokenResponse     ← 조회 + 비밀번호 검증 + JWT
    // create(String email, String pw): Member           ← Admin용, 중복 검증 + 저장
    // update(Long id, String email, String pw): Member
    // chargePoint(Long id, int amount): void
    // delete(Long id): void
}
```

**테스트** (`MemberServiceTest`, ~11개):

| 테스트 | 검증 내용 |
|--------|----------|
| `register_newEmail_savesAndReturnsToken` | 중복 검증 + 저장 + JWT |
| `register_duplicateEmail_throws` | existsByEmail → IllegalArgumentException |
| `login_validCredentials_returnsToken` | findByEmail + 비밀번호 + JWT |
| `login_emailNotFound_throws` | 이메일 없음 |
| `login_wrongPassword_throws` | 비밀번호 불일치 |
| `findAll_returnsList` | Admin 목록 |
| `findById_existing_returns` | Admin 조회 |
| `findById_notFound_throws` | NoSuchElementException |
| `update_updatesFields` | email, password 변경 |
| `chargePoint_validAmount_updates` | chargePoint 호출 + 저장 |
| `delete_delegatesToRepository` | 삭제 위임 |

### 3-4. OptionService

**신규**: `gift/option/OptionService.java`
**수정**: `gift/option/OptionController.java`

```java
@Service
@Transactional(readOnly = true)
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    // findByProductId(Long productId): List<Option>  ← 상품 존재 확인 포함
    // create(Long productId, OptionRequest): Option  ← 이름 검증 + 상품 확인 + 중복 검증
    // delete(Long productId, Long optionId): void    ← 최소 1개 유지 + 소속 확인
}
```

**테스트 — OptionNameValidatorTest** (~5개):

| 테스트 | 검증 내용 |
|--------|----------|
| `validate_validName_returnsEmpty` | 정상 이름 |
| `validate_exceedsMaxLength_returnsError` | 50자 초과 |
| `validate_invalidChars_returnsError` | 허용 외 특수문자 |
| `validate_blankName_returnsError` | 빈 문자열 |
| `validate_validSpecialChars_returnsEmpty` | 허용 특수문자 `()[]+-&/_` |

**테스트 — OptionServiceTest** (~10개):

| 테스트 | 검증 내용 |
|--------|----------|
| `findByProductId_exists_returnsOptions` | 상품 확인 + 옵션 목록 |
| `findByProductId_productNotFound_throws` | NoSuchElementException |
| `create_happyPath_saves` | 이름 검증 + 상품 확인 + 중복 확인 + 저장 |
| `create_invalidName_throws` | 이름 검증 실패 |
| `create_productNotFound_throws` | 상품 없음 |
| `create_duplicateName_throws` | 동일 상품 내 중복 이름 |
| `delete_happyPath_deletes` | 정상 삭제 |
| `delete_onlyOneOption_throws` | 최소 1개 옵션 제약 |
| `delete_optionNotOwnedByProduct_throws` | 소속 확인 |
| `delete_productNotFound_throws` | 상품 없음 |

### 3-5. WishService

**신규**: `gift/wish/WishService.java`
**수정**: `gift/wish/WishController.java`

```java
@Service
@Transactional(readOnly = true)
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    // findByMemberId(Long memberId, Pageable): Page<Wish>
    // add(Long memberId, Long productId): AddResult  ← 상품 확인 + 중복 처리
    // remove(Long memberId, Long wishId): void       ← 존재 확인 + 소유권 검증
}
```

**addWish 201/200 분기 보존**: 기존 코드는 중복이면 200, 신규면 201을 반환한다. 이를 유지하기 위해 Service에서 `record AddResult(Wish wish, boolean created)`를 반환하고 Controller에서 분기한다.

```java
// WishService
public record AddResult(Wish wish, boolean created) {}

@Transactional
public AddResult add(Long memberId, Long productId) {
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다."));
    return wishRepository.findByMemberIdAndProductId(memberId, productId)
        .map(existing -> new AddResult(existing, false))
        .orElseGet(() -> new AddResult(wishRepository.save(new Wish(memberId, product)), true));
}

// WishController
var result = wishService.add(member.getId(), request.productId());
if (result.created()) {
    return ResponseEntity.created(URI.create("/api/wishes/" + result.wish().getId()))
        .body(WishResponse.from(result.wish()));
}
return ResponseEntity.ok(WishResponse.from(result.wish()));
```

`removeWish`의 기존 403 응답: 소유권 불일치 시 `IllegalStateException` 발생 → GlobalExceptionHandler가 403으로 변환.

**테스트** (`WishServiceTest`, ~7개):

| 테스트 | 검증 내용 |
|--------|----------|
| `findByMemberId_returnsPagedWishes` | Pageable 위임 |
| `add_newWish_returnsCreatedTrue` | AddResult(wish, created=true) |
| `add_duplicateWish_returnsCreatedFalse` | AddResult(existing, created=false) |
| `add_productNotFound_throws` | NoSuchElementException |
| `remove_happyPath_deletes` | 정상 삭제 |
| `remove_wishNotFound_throws` | NoSuchElementException |
| `remove_notOwner_throwsIllegalState` | 소유권 → IllegalStateException → 403 |

### 3-6. KakaoAuthService

**신규**: `gift/auth/KakaoAuthService.java`
**수정**: `gift/auth/KakaoAuthController.java`

```java
@Service
public class KakaoAuthService {
    private final KakaoLoginClient kakaoLoginClient;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    // loginOrRegister(String code): TokenResponse
    //   ← 토큰 교환 + 사용자 정보 조회 + 회원 조회/생성 + JWT 발급
}
```

KakaoAuthController는 `KakaoAuthService` + `KakaoLoginProperties` 의존. `/login` 엔드포인트의 리다이렉트 URL 구성은 HTTP 관심사이므로 Controller에 남김.

**테스트** (`KakaoAuthServiceTest`, ~5개):

| 테스트 | 검증 내용 |
|--------|----------|
| `loginOrRegister_existingMember_updatesTokenReturnsJwt` | 기존 회원 → 토큰 갱신 |
| `loginOrRegister_newMember_createsAndReturnsJwt` | 신규 회원 생성 |
| `loginOrRegister_savesKakaoAccessToken` | kakaoAccessToken 저장 검증 |
| `loginOrRegister_generatesJwtWithEmail` | JWT 이메일 일치 |
| `loginOrRegister_kakaoClientError_propagates` | 예외 전파 (삼키지 않음) |

Mock 대상: `KakaoLoginClient`, `MemberRepository`, `JwtProvider`

### 3-7. OrderService

**신규**: `gift/order/OrderService.java`
**수정**: `gift/order/OrderController.java`

```java
@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final KakaoMessageClient kakaoMessageClient;

    // findByMemberId(Long memberId, Pageable): Page<Order>
    // placeOrder(Member member, OrderRequest): Order
    //   ← 옵션 확인 + 재고 차감 + 포인트 차감 + 저장 + 카카오 알림
}
```

- `WishRepository` 주입 제거 (현재 미사용. wish cleanup은 미구현 상태 유지)
- `sendKakaoMessageIfPossible()` private 메서드를 OrderService로 이동
- Controller에는 `AuthenticationResolver` + `OrderService` 의존만 남김

**테스트** (`OrderServiceTest`, ~10개):

| 테스트 | 검증 내용 |
|--------|----------|
| `placeOrder_happyPath_completesAllSteps` | 전체 흐름 |
| `placeOrder_optionNotFound_throws` | 옵션 없음 |
| `placeOrder_insufficientStock_throws` | 재고 부족 |
| `placeOrder_insufficientPoints_throws` | 포인트 부족 |
| `placeOrder_savesOrderWithCorrectFields` | 주문 엔티티 필드 검증 |
| `placeOrder_calculatesCorrectPrice` | price × quantity |
| `placeOrder_withKakaoToken_sendsNotification` | 알림 전송 |
| `placeOrder_withoutKakaoToken_skipsNotification` | 알림 건너뜀 |
| `placeOrder_notificationFails_orderStillSucceeds` | 알림 실패해도 주문 성공 |
| `findByMemberId_returnsPagedOrders` | 페이징 위임 |

Mock 대상: `OrderRepository`, `OptionRepository`, `MemberRepository`, `KakaoMessageClient`

### Phase 3 실행 순서

```
1. GlobalExceptionHandler 확장 (NoSuchElementException, IllegalStateException)
2. CategoryService → CategoryController 수정
3. ProductService → ProductController, AdminProductController 수정
4. MemberService → MemberController, AdminMemberController 수정
5. OptionService → OptionController 수정
6. WishService → WishController 수정
7. KakaoAuthService → KakaoAuthController 수정
8. OrderService → OrderController 수정
```

---

## 검증 방법

각 서비스 추출 후:

1. `./gradlew test` — 해당 서비스 테스트 전체 통과 + 회귀 없음
2. `./gradlew compileJava` — 컴파일 통과

최종 완료 후:

3. `./gradlew test` — 전체 ~75개 테스트 통과
4. `./gradlew bootRun` — 애플리케이션 정상 기동 (H2 인메모리)
5. 주요 엔드포인트 수동 확인:
   - `GET /api/categories` → 200 + JSON 배열
   - `POST /api/products` 잘못된 이름 → 400 + 에러 메시지
   - `GET /api/products?page=0` → 200 + 페이지 응답
   - `GET /admin/products` → 200 + HTML
   - 존재하지 않는 리소스 → 404
