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
- step1에서 했으면 좋았겠지만, 빠뜨려서 step2에서라도 추가했습니다. 

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

---

## 4. [TDD] 전체 도메인 단위 테스트 작성

### 4-1. 배경

예외 체계 전환, Service 분리, 트랜잭션 정책 적용 이후 도메인(비즈니스) 로직이 서비스 계층에 집중되었다.  
구조 변경 이후 회귀를 빠르게 감지하고, 이후 동작 변경 단계에서 안전하게 리팩토링하기 위해 도메인 단위 테스트를 보강했다.
+ step1에서 빠뜨린 단위 테스트도 함께 추가

### 4-2. 수정사항

| 항목 | 내용 |
|---|---|
| member | `MemberTest`, `MemberServiceTest`, `AdminMemberServiceTest` 추가 |
| auth | `KakaoAuthServiceTest`, `AuthenticationResolverTest` 추가 |
| product | `ProductTest`, `ProductServiceTest` 추가 |
| category | `CategoryTest`, `CategoryServiceTest` 추가 |
| option | `OptionTest`, `OptionServiceTest` 추가 |
| order | `OrderServiceTest` 추가 |
| wish | `WishServiceTest` 추가 |
| 검증 범위 | 도메인 검증 로직, 서비스 예외 흐름, 저장소 상호작용, 주요 성공/실패 시나리오 검증 |

### 4-3. 기대효과

1. 서비스 계층 변경 시 의도치 않은 동작 변화를 테스트로 차단할 수 있다.
2. 2단계 동작 변경(트랜잭션/비즈니스 로직 확장) 시 안전한 리팩토링 기반을 확보할 수 있다.

---

## 5. [구조 변경 + 동작 변경] AdminProductController Repository 의존 분리

### 5-1. 배경

step1에서 대부분 Controller의 Repository 직접 의존을 Service로 분리했지만, `AdminProductController`는 누락되어 있었다.  
구조 일관성을 맞추기 위해 step2에서 해당 누락분을 보완했다.

### 5-2. 수정사항

| 항목 | 내용 |
|---|---|
| Controller 의존성 정리 | `AdminProductController`에서 `ProductRepository`, `CategoryRepository` 직접 주입 제거 |
| Service 분리 | `AdminProductService` 신설 후 상품/카테고리 조회, 생성, 수정, 삭제 책임 이동 |
| 트랜잭션 정책 | `AdminProductService`에 `@Transactional(readOnly = true)` 적용, 쓰기 메서드에 `@Transactional` 명시 |
| Lombok 적용 | `AdminProductController`, `AdminProductService` 모두 `@RequiredArgsConstructor` 기반 생성자 주입 유지 |
| 단위 테스트 | `AdminProductServiceTest` 추가(조회/수정/삭제/예외 시나리오 검증) |

### 5-3. 기대효과

1. Controller는 요청/응답 흐름에 집중하고, 비즈니스/데이터 접근 로직은 Service로 일관되게 관리할 수 있다.
2. step1에서 누락된 의존성 분리를 보완해 전체 도메인 구조 기준을 맞출 수 있다.
3. Admin 상품 관리 로직의 회귀를 단위 테스트로 빠르게 검증할 수 있다.

---

## 6. [구조 변경] OAuth/메시지 클라이언트 Provider 확장 구조 전환

### 6-1. 배경

기존에는 인증/메시지 서비스가 Kakao 구현체를 직접 의존하고 있어, Google/Line 같은 Provider 확장 시 서비스 코드를 수정해야 했다.  
외부 연동 구현 교체 영향을 최소화하기 위해 인터페이스 + Registry 기반 구조로 전환했다.

### 6-2. 수정사항

| 항목 | 내용 |
|---|---|
| Provider 식별자 | `ExternalProvider` enum 추가 (`KAKAO`, `GOOGLE`, `LINE`) |
| OAuth 포트 | `OAuthClient`, `OAuthUserInfo`, `OAuthClientRegistry` 추가 |
| 메시지 포트 | `MessageClient`, `MessageClientRegistry` 추가 |
| Kakao 어댑터화 | `KakaoLoginClient`가 `OAuthClient` 구현, `KakaoMessageClient`가 `MessageClient` 구현 |
| Auth 서비스 일반화 | `KakaoAuthService` -> `OAuthService`로 전환 (`provider` 인자 기반 처리) |
| Auth 컨트롤러 일반화 | `KakaoAuthController` -> `OAuthController`로 전환, 경로를 `/api/auth/{provider}/login`, `/api/auth/{provider}/callback`으로 변경 |
| 예외 보강 | `AuthErrorCode.INVALID_OAUTH_PROVIDER`, `InvalidOAuthProviderException` 추가 |
| 테스트 반영 | `KakaoAuthServiceTest` -> `OAuthServiceTest`로 전환 및 Registry 기반 목킹으로 수정 |

### 6-3. 기대효과

1. 신규 Provider 추가 시 서비스 코드 변경을 최소화하고 구현체 추가 중심으로 확장할 수 있다.
2. 외부 API 변경 영향이 Provider별 어댑터 내부로 제한되어 유지보수가 쉬워진다.
3. 테스트가 벤더 구현 세부사항보다 인터페이스 계약 검증에 집중된다.

---

## 7. [구조 변경] 디렉토리 과밀 해소를 위한 패키지 분리

### 7-1. 배경

도메인별 파일 수가 증가하면서 하나의 디렉토리에 Controller, Service, DTO, Exception, Repository, Entity가 함께 섞여 있었다.  
이로 인해 파일 탐색 비용이 커지고, 변경 시 책임 경계를 빠르게 파악하기 어려운 문제가 있었다.

### 7-2. 수정사항

| 항목 | 내용 |
|---|---|
| 분리 기준 | 단일 패키지 집합에서 레이어 기준(`controller`, `service`, `dto`, `exception`, `repository`, `entity`)으로 분리 |
| Auth 정리 | OAuth 관련 포트/레지스트리(`oauth`)와 인증 책임 영역을 분리해 탐색 경로 단순화 |
| 도메인 확장성 | 도메인별 파일 증가를 전제하고, 신규 기능 추가 시 동일 규칙으로 패키지 확장 가능하도록 기준 고정 |
| 공통 원칙 | 비즈니스 로직은 Service, 입출력 모델은 DTO, 예외는 Exception 패키지로 관리하는 규칙 명시 |

### 7-3. 기대효과

1. 같은 책임의 파일이 가까운 위치에 모여 코드 탐색 속도가 빨라진다.
2. 변경 영향 범위를 패키지 단위로 파악할 수 있어 유지보수성이 향상된다.
3. 파일 수가 늘어나도 구조가 무너지지 않아 도메인 확장 시 일관성을 유지할 수 있다.

---

## 8. [동작 변경] 주문 완료 시 위시 자동 정리

### 8-1. 배경

주문 흐름에 “구매 완료 시 위시 제거” 단계가 누락되어, 구매 이후에도 동일 상품이 위시 목록에 남아 있었다.  
주문 결과와 위시 상태를 일치시키기 위해 주문 트랜잭션 내에서 위시 정리 단계를 추가했다.

### 8-2. 수정사항

| 항목 | 내용 |
|---|---|
| Repository 확장 | `WishRepository`에 `deleteByMemberIdAndProductId(memberId, productId)` 추가 |
| 주문 로직 반영 | `OrderService#createOrder`에서 주문 저장 후 위시 자동 삭제 단계 추가 |
| 트랜잭션 정책 | 위시 삭제 실패 시 예외 전파로 주문/재고/포인트 변경이 함께 롤백되도록 유지 |
| 단위 테스트 | `OrderServiceTest`에 위시 삭제 호출 검증 및 삭제 실패 예외 케이스 추가 |

### 8-3. 기대효과

1. 구매 완료 후 위시 목록이 자동 정리되어 사용자 상태와 데이터가 일치한다.
2. 주문/위시를 같은 트랜잭션으로 묶어 정합성을 보장할 수 있다.
3. 위시 정리 회귀를 단위 테스트로 빠르게 검증할 수 있다.

---

## 9. [구조 변경] Wish 소유권 검증 책임의 도메인 이동

### 9-1. 배경

`WishService`에서 소유자 검증을 `if (!wish.getMemberId().equals(member.getId()))`로 직접 처리하고 있었다.  
이 패턴은 다른 서비스/유스케이스에서 동일하게 반복될 가능성이 높아, 권한 판단 규칙이 서비스 계층에 분산될 위험이 있었다.

### 9-2. 수정사항

| 항목 | 내용 |
|---|---|
| 도메인 메서드 추가 | `Wish` 엔티티에 `assertOwner(Long memberId)` 추가 |
| 서비스 단순화 | `WishService#removeWish`의 직접 비교/예외 throw 제거 후 `wish.assertOwner(member.getId())` 호출로 변경 |
| 예외 정책 유지 | 소유자가 아닐 때 기존과 동일하게 `ForbiddenException` 발생 |
| 테스트 보강 | `WishTest` 추가 (`assertOwner` 성공/실패 케이스 검증) |

### 9-3. 기대효과

1. 소유권 판단 규칙이 도메인으로 응집되어 중복과 분산을 줄일 수 있다.
2. 서비스는 인증 추출, 조회, 삭제 같은 흐름 조립에 집중할 수 있다.
3. 권한 검증 규칙 변경 시 `Wish` 도메인 단일 지점 수정으로 영향 범위를 축소할 수 있다.

---

## 10. [구조 변경] AdminProductController 에러 폼 세팅 중복 제거

### 10-1. 배경

`AdminProductController`의 `populateNewFormError`, `populateEditForm`는 에러/입력값/카테고리 목록을 모델에 넣는 로직이 거의 동일했다.  
동일 세팅이 두 메서드에 반복되어 변경 시 누락 가능성이 있어, 공통화가 필요했다.

### 10-2. 수정사항

| 항목 | 내용 |
|---|---|
| 공통 메서드 추출 | `populateFormCommon(Model, errors, name, price, imageUrl, categoryId)` 추가 |
| new 폼 에러 메서드 정리 | `populateNewFormError`에서 공통 메서드 호출로 변경 |
| edit 폼 에러 메서드 정리 | `populateEditForm`에서 공통 메서드 호출 후, 차이점인 `product`만 별도 세팅 |
| 검증 | `AdminProductServiceTest` 실행으로 빌드/테스트 정상 통과 확인 |

### 10-3. 기대효과

1. 폼 에러 모델 세팅 중복이 제거되어 코드 가독성과 유지보수성이 향상된다.
2. 공통 속성 변경 시 한 곳만 수정하면 되어 누락/불일치 위험을 줄일 수 있다.
3. new/edit 폼의 차이점이 명확해져 테스트와 코드 리뷰 포인트가 단순해진다.
