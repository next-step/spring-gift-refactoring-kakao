# spring-gift-refactoring

## 리팩토링 목표

> 구조 변경을 통해 변경 난이도를 낮추되 작동은 유지한다.

## 판단 근거 및 논의 기록

### 1. 현황 분석

#### 프로젝트 구조
- Spring Boot 3.5.9 / Java 21 / JPA / Flyway / Kakao OAuth2
- 도메인: auth, category, member, option, order, product, wish
- REST API 18개 + Admin 페이지 13개 = 총 31개 엔드포인트

#### 발견된 문제점

**미사용 코드**

| 항목 | 위치 | 이유 |
|------|------|------|
| `OrderController.wishRepository` | `OrderController.java:26` | 주입만 되고 사용 안 됨. 주석에 "6. cleanup wish"가 있으나 미구현 |

`Product.getOptions()`와 `Order.getMemberId()`는 현재 호출처가 없으나, 추후 확장성을 고려하여 제거하지 않는다.

**Service 계층 부재**
- Service 클래스가 프로젝트에 하나도 없음
- 모든 Controller가 Repository를 직접 호출
- 특히 `OrderController`는 Repository 4개 + Client 1개를 직접 의존

**Controller에 비즈니스 로직 집중**
- 인증 확인, 검증, 재고 차감, 포인트 차감, 주문 저장, 외부 API 호출이 모두 Controller에 존재

### 2. 왜 Service 계층을 도입하는가?

| 문제 | Service 도입 후 |
|------|-----------------|
| 비즈니스 로직 변경 시 Controller를 직접 수정해야 함 | Service만 수정하면 됨 |
| REST / Admin 간 로직 재사용 불가 (복붙) | 같은 Service를 공유 |
| 비즈니스 로직만 단위 테스트하기 어려움 | Service 단위 테스트 가능 |
| 트랜잭션 경계가 불명확함 | `@Transactional`로 명확하게 제어 |

### 3. 왜 @Transactional을 Controller가 아닌 Service에 붙이는가?

Controller에 붙이면 동작은 하지만, 트랜잭션 범위가 불필요하게 넓어진다.

```
// Controller에 @Transactional 붙인 경우
@Transactional 시작
  1. 인증 확인          ← DB 불필요
  2. 옵션 검증          ← DB
  3. 재고 차감          ← DB
  4. 포인트 차감        ← DB
  5. 주문 저장          ← DB
  6. 카카오 알림 전송    ← 외부 API 호출 (느림)
@Transactional 끝
→ 카카오 API가 3초 걸리면 DB 커넥션을 3초간 점유. 트래픽 몰리면 커넥션 풀 고갈.

// Service에 @Transactional 붙인 경우
Controller:
  인증 확인
  orderService.createOrder()   ← @Transactional (DB 작업만)
  카카오 알림 전송              ← 트랜잭션 밖
→ 트랜잭션 범위를 필요한 만큼만 제어 가능.
```

**단, 성능에 크게 문제가 없는 단순 CRUD는 클래스 레벨에 `@Transactional`을 붙이고, 필요한 메서드만 override 한다.**

### 4. @ExceptionHandler를 왜 Controller 안에 두는가?

- 예외 처리는 각 도메인의 책임이므로 Controller 안에 둔다.
- `@ControllerAdvice`에 패키지 지정이 가능하지만, 현재 exception handler가 많지 않아서 중앙에서 관리할 필요성이 없다.

### 5. 리팩토링 순서 결정

1. **미사용 코드 제거** — 안 쓰는 메서드/필드를 조기에 찾아서 없애기
2. **인수 테스트 작성 (먼저)** — 유닛 테스트는 리팩토링 과정에서 깨지므로, 사용자 행동 기반의 인수 테스트를 먼저 작성하여 안전망 확보
3. **구조 변경 (Service 계층 도입)** — 작동 유지하면서 구조만 변경
4. **스타일 통일** — 구조 변경이 끝난 후에 진행

> 유닛 테스트를 지금 작성하면 리팩토링 과정에서 깨진다. 인수 테스트는 사용자 행동 주도이므로 구조가 바뀌어도 유지된다.

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

| 항목 | 변경 내용 | 이유 |
|------|-----------|------|
| `var` → 명시적 타입 | 12곳에서 `var`를 `Member`, `Product`, `Optional<Wish>` 등 명시적 타입으로 변경 | 일관성이 없어서 제거 |
| `final` 로컬 변수 제거 | `AuthenticationResolver`, `JwtProvider`, `AdminMemberController`의 `final` 7곳 제거 | 일관성이 없어서 삭제 |
| `@Autowired` 제거 | 생성자가 하나인 클래스에서 `@Autowired` 어노테이션 및 미사용 import 제거 | Spring에서 권장하지 않아서 되도록 안 쓰려고 제거 |
| lint format | import 순서, 공백 등 포매팅 통일 | 통일성을 위해 |

### 4. E2E 인수 테스트 추가
RestAssured 기반 인수 테스트를 전 API에 대해 작성했다.

- `CategoryAcceptanceTest`
- `MemberAcceptanceTest`
- `ProductAcceptanceTest`
- `OptionAcceptanceTest`
- `WishAcceptanceTest`
- `OrderAcceptanceTest`

### 5. @ExceptionHandler 유지
`MemberController`, `OptionController`, `ProductController`에 동일한 `@ExceptionHandler`가 존재하지만, 예외 처리는 각 도메인의 책임이므로 Controller 안에 유지한다. `@ControllerAdvice`에 패키지 지정이 가능하지만, 현재 handler가 많지 않아 중앙 관리할 필요성이 없다.
