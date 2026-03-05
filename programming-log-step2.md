# step2 리팩토링 로그

## 1. [작동 변경] 예외처리

### 1-1. 배경

Service 계층 도입 이후 예외 처리가 Controller별 `@ExceptionHandler`와 `IllegalArgumentException` 중심으로 분산되어 있었다.  
이로 인해 에러 응답 정책과 메시지 관리 지점이 흩어져, 변경 시 일관성을 유지하기 어려웠다.

### 1-2. 수정사항

| 항목 | 내용 |
|---|---|
| 공통 구조 추가 | `BaseErrorCode`, `BaseException`, `ErrorResponse`, `GlobalExceptionHandler` |
| auth 도메인 | `AuthErrorCode` 추가, `AuthenticationException`/`ForbiddenException`을 `BaseException` 기반으로 전환 |
| member 도메인 | `MemberErrorCode`, `MemberException` 추가, `MemberService`/`AdminMemberService`/`Member` 예외 전환 |
| product 도메인 | `ProductErrorCode`, `ProductException` 추가, `ProductService`/`ProductNameValidator` 예외를 `ProductException`으로 전환 |
| category 도메인 | `CategoryErrorCode`, `CategoryException` 추가, `CategoryController`/`CategoryService` 예외를 `CategoryException`으로 전환 |
| option 도메인 | `OptionErrorCode`, `OptionException` 추가, `Option`/`OptionService` 예외를 `OptionException`으로 전환 |
| order 도메인 | `OrderErrorCode`, `OrderException` 추가, `OrderService` 예외를 `OrderException`으로 전환 |
| wish 도메인 | `WishErrorCode`, `WishException` 추가, `WishService` 예외를 `WishException`으로 전환 |
| 컨트롤러 정리 | `MemberController`, `ProductController`, `OptionController`, `OrderController`, `WishController` 로컬 `@ExceptionHandler` 제거(전역 처리로 이관) |
| 전역 매핑 확장 | `GlobalExceptionHandler`에 도메인 예외 매핑을 확장하고 `ErrorResponse(status, message)` 형식으로 통일 |

### 1-3. 기대효과

1. 예외 응답 정책을 전역에서 통합 관리할 수 있다.
2. 도메인별 에러코드로 상태코드/메시지 의도가 명확해지고, 응답 바디 포맷이 일관된다.
3. Controller는 요청/응답 흐름에 집중하고, 예외 처리 중복을 줄일 수 있다.

---

## 2. [구조 변경] lombok 적용

### 2-1. 배경

엔티티 클래스에 getter/기본 생성자 보일러플레이트가 반복되어 코드량이 증가하고 가독성이 떨어졌다.  
JPA 요구사항(기본 생성자)은 유지하면서 반복 코드를 줄이기 위해 Lombok을 도입했다.

### 2-2. 수정사항

| 항목 | 내용 |
|---|---|
| 의존성 추가 | `build.gradle.kts`에 `lombok` (`compileOnly`, `annotationProcessor`, `testCompileOnly`, `testAnnotationProcessor`) 추가 |
| 엔티티 적용 | `Category`, `Product`, `Option`, `Order`, `Wish`, `Member`에 `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 적용 |
| DI 적용 | Service/Controller/Component에 `@RequiredArgsConstructor` 적용, 수동 생성자 제거 |
| 적용 대상 | `CategoryController`, `CategoryService`, `ProductController`, `ProductService`, `AdminProductController`, `MemberController`, `MemberService`, `AdminMemberController`, `AdminMemberService`, `OptionController`, `OptionService`, `OrderController`, `OrderService`, `WishController`, `WishService`, `KakaoAuthController`, `KakaoAuthService`, `AuthenticationResolver` |
| 코드 정리 | 수동 getter / protected 기본 생성자 / 단순 DI 생성자 제거 |

### 2-3. 기대효과

1. 엔티티 보일러플레이트를 줄여 코드 가독성과 유지보수성을 높일 수 있다.
2. JPA 제약(`protected` 기본 생성자)은 유지하면서 표현을 단순화할 수 있다.
3. DI 관련 생성자 보일러플레이트를 줄이면서 생성자 주입의 장점을 유지할 수 있다.

---

## 3. [동작 변경] Service 트랜잭션 정책 적용

### 3-1. 배경

Service 계층에 읽기/쓰기 작업이 혼재되어 있었지만 트랜잭션 정책이 명시되지 않아, 조회와 변경의 경계가 코드에 드러나지 않았다.  
조회 성능 최적화와 데이터 일관성을 위해 Service 단위 트랜잭션 기준을 명확히 적용했다.

### 3-2. 수정사항

| 항목 | 내용 |
|---|---|
| 기본 정책 | Service 클래스에 `@Transactional(readOnly = true)` 적용 |
| 쓰기 메서드 오버라이드 | 변경 메서드에 `@Transactional` 명시 (`create`, `save`, `update`, `delete`, `handleCallback`, `createOrder`, `addWish`, `removeWish` 등) |
| 적용 서비스 | `CategoryService`, `ProductService`, `OptionService`, `OrderService`, `MemberService`, `AdminMemberService`, `WishService`, `KakaoAuthService` |

### 3-3. 기대효과

1. 조회 경로는 readOnly 트랜잭션으로 불필요한 변경 감지를 줄일 수 있다.
2. 쓰기 경로는 명시적 트랜잭션으로 데이터 정합성을 보장하기 쉬워진다.
3. 메서드 단위 트랜잭션 의도가 코드에 드러나 유지보수성이 높아진다.
