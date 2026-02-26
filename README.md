# spring-gift-refactoring

## 리팩토링 진행 내역

### 1. Service 계층 추출
Controller에 몰려 있던 비즈니스 로직을 Service로 분리했다.

- `CategoryController` → `CategoryService`
- `MemberController` → `MemberService`
- `ProductController` → `ProductService`
- `OptionController` → `OptionService`
- `WishController` → `WishService`
- `OrderController` → `OrderService`

### 2. 주석 복원
원본 코드(`55ca9e4`)에 있던 주석들이 리팩토링 과정에서 누락되어 복원했다.

- `OrderController` — `// auth check`, order flow 개요 주석(1~7단계)
- `OrderService` — `// validate option`
- `WishController` — `// check auth`
- `WishService` — `// check product`, `// check duplicate`
- `MemberController` — Javadoc (`@author`, `@since`)
- `OptionController` — 블록 주석 (옵션 제약조건 설명)

### 3. 코드 스타일 통일
프로젝트 전체를 스캔하여 일관성이 깨진 부분을 정리했다.

| 항목 | 변경 내용 |
|------|-----------|
| `var` → 명시적 타입 | 12곳에서 `var`를 `Member`, `Product`, `Optional<Wish>` 등 명시적 타입으로 통일 |
| `final` 로컬 변수 제거 | `AuthenticationResolver`, `JwtProvider`, `AdminMemberController`의 불필요한 `final` 7곳 제거 |
| `@Autowired` 제거 | 생성자가 하나인 클래스에서 불필요한 `@Autowired` 어노테이션 및 미사용 import 제거 |
| lint format | import 순서, 공백 등 포매팅 통일 |

### 4. E2E 인수 테스트 추가
RestAssured 기반 인수 테스트를 전 API에 대해 작성했다.

- `CategoryAcceptanceTest`
- `MemberAcceptanceTest`
- `ProductAcceptanceTest`
- `OptionAcceptanceTest`
- `WishAcceptanceTest`
- `OrderAcceptanceTest`

### 5. 미반영 사항
- `@ExceptionHandler` 중복: `MemberController`, `OptionController`, `ProductController`에 동일한 핸들러가 존재한다. `@ControllerAdvice`로 통합 가능하나, 다른 컨트롤러에 의도치 않은 영향을 줄 수 있어 보류 중이다.
