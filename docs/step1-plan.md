# Step 1 계획: 구조 변경

작동을 바꾸기 쉬운 상태를 만든다. 구조 변경을 통해 변경 난이도를 낮추되 작동은 유지한다. 이 단계에서는 신규 기능을 추가하지 않는다.

---

## 0단계: 테스트 코드 작성

리팩터링 전 현재 작동을 보호하는 테스트를 먼저 작성한다. 테스트가 통과하는 상태에서만 구조 변경을 시작한다.

### 단위 테스트

| 대상 | 테스트 내용 |
|------|------------|
| `Member.chargePoint()` | 정상 충전, 0 이하 금액 예외 |
| `Member.deductPoint()` | 정상 차감, 0 이하 금액 예외, 잔액 부족 예외 |
| `Option.subtractQuantity()` | 정상 차감, 재고 초과 예외 |
| `ProductNameValidator` | 정상 이름, 15자 초과, 허용 문자 외, "카카오" 포함 |
| `OptionNameValidator` | 정상 이름, 50자 초과 |

### 통합 테스트

RestAssured + `@Sql`(setup-data.sql, cleanup.sql) 기반 Given/When/Then 스타일.

| 컨트롤러 | 테스트 내용 |
|----------|------------|
| `MemberController` | 회원가입, 로그인, 중복 이메일, 잘못된 비밀번호 |
| `CategoryController` | 카테고리 CRUD |
| `ProductController` | 상품 CRUD, "카카오" 포함 상품명 거부 |
| `OptionController` | 옵션 CRUD, 존재하지 않는 상품 |
| `WishController` | 위시 조회/추가/삭제, 중복 추가, 존재하지 않는 위시 삭제 |
| `OrderController` | 주문 생성, 인증 없음, 존재하지 않는 옵션, 재고 부족, 포인트 부족 |

---

## 1단계: 스타일 정리

프로젝트 전반의 스타일 불일치를 찾아 일관되게 정리한다. 작동이 바뀌지 않아야 한다.

### 변경 내용

| 항목 | 대상 파일 | 내용 |
|------|----------|------|
| `@Autowired` 제거 | MemberController, CategoryController, ProductController, OptionController | Spring 4.3+ 단일 생성자 규칙에 따라 불필요한 `@Autowired` 제거 |
| `var` → 명시적 타입 | WishController, OrderController, KakaoMessageClient | `var` 선언을 명시적 타입 선언으로 통일 |
| 로컬 `final` 제거 | 프로젝트 전반 | 필드의 `private final`은 유지, 로컬 변수의 `final`은 제거하여 일관성 확보 |
| 에러 메시지 한글 통일 | MemberController 등 | `"Email is already registered."` 등 영어 예외 메시지를 한글로 변환 |
| 필드 빈 줄 정리 | Member | 어노테이션 없는 필드 사이 불필요한 빈 줄 제거 |
| `/* */` → `/** */` | OptionController | 클래스 레벨 블록 주석을 JavaDoc으로 변환 |
| import 정렬 | 프로젝트 전반 | `gift` → `jakarta` → `io/com` → `org` → `java` 순서로 그룹 간 빈 줄 추가 |

---

## 2단계: 불필요한 코드 제거

IDE 또는 정적 분석 도구가 "미사용"으로 표시하는 항목을 제거한다. 삭제 전에 반드시 근거를 확인한다.

### 삭제 전 확인 기준

- 주변 주석 또는 TODO에 의도가 있는가
- git blame으로 누가 왜 추가했는가
- 이후 단계(작동 변경)와 충돌하지 않는가

### 변경 내용

| 항목 | 대상 | 근거 |
|------|------|------|
| 미사용 메서드 제거 | `Product.getOptions()` | 프로젝트 어디에서도 호출되지 않음을 grep으로 확인. `@OneToMany` 관계는 엔티티에 유지되므로 JPA 동작에 영향 없음 |
| 자명한 주석 제거 | `// primitive FK`, `// point deduction for order payment` 등 | 필드명/메서드명으로 충분히 의도가 드러나는 주석 |
| `Collectors.toList()` 간소화 | `Stream.collect(Collectors.toList())` 사용부 | Java 16+ 내장 `Stream.toList()`로 대체, 미사용 `Collectors` import 제거 |

---

## 3단계: 서비스 계층 추출

Controller에 있는 비즈니스 로직을 Service로 이동한다. Controller는 요청 검증과 위임만 담당하도록 얇게 만든다.

### 추출 대상

| Service | 추출 원본 | 역할 |
|---------|----------|------|
| `MemberService` | MemberController, AdminMemberController | 회원 등록/로그인/CRUD, JWT 발급 |
| `KakaoAuthService` | KakaoAuthController | 카카오 OAuth 토큰 교환 + 회원 자동 등록 + JWT 발급 |
| `CategoryService` | CategoryController | 카테고리 CRUD |
| `ProductService` | ProductController, AdminProductController | 상품명 검증 + 카테고리 조회 + 상품 CRUD |
| `OptionService` | OptionController | 옵션명 검증 + 중복 검증 + 최소 1개 규칙 + CRUD |
| `WishService` | WishController | 위시 조회/추가(중복 검증)/삭제(소유권 확인) |
| `OrderService` | OrderController | 재고 차감 + 포인트 차감 + 주문 저장 + 카카오 알림 |

### 추출 규칙

- Controller에는 HTTP 매핑, 요청 바인딩, `ResponseEntity` 생성만 남긴다.
- Repository 호출과 비즈니스 검증은 모두 Service로 이동한다.
- `@Transactional`은 작동 변경에 해당하므로 이 단계에서는 추가하지 않는다.
- 원본 Controller의 주석(order flow, check product 등)은 Service에 그대로 옮긴다.

---

## 코드 리뷰 반영

코드 리뷰에서 지적된 사항을 수정했다.

| 항목 | 내용 | 원인 |
|------|------|------|
| `@Transactional` 제거 | 서비스 추출 시 임의로 추가한 `@Transactional`을 제거 | 작동 변경 없는 구조 정리 원칙 위반 |
| `@Valid` 복원 | WishController에서 누락된 `@Valid` 애노테이션 복원 | 서비스 추출 시 실수로 누락 |
| 미사용 의존성 제거 | OrderService의 `WishRepository`, ProductService의 `validateNameWithKakao()` | 아직 구현하지 않은 기능의 의존성을 미리 추가한 과잉 |
| 주석 복원 | order flow, check product 등 주석을 서비스에 복원 | 추출 과정에서 주석이 누락됨 |
| 카카오 설정 기본값 | `${KAKAO_CLIENT_ID:}` 형태로 빈 기본값 추가 | `.env` 없는 환경에서 부팅 실패 |
| 경계 조건 테스트 추가 | 재고 부족, 포인트 부족, 카카오 상품명, 존재하지 않는 리소스 등 | 리뷰 과정에서 누락된 실패 케이스 발견 |

---

## 검증

매 단계 완료 후 `./gradlew test` 전체 통과 확인.
