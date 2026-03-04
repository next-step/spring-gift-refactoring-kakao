# 구조 변경 리팩터링 분석 보고서

> 분석 일자: 2026-03-04
> 분석 범위: src/main/java/gift/ 전체 (8개 패키지, 50개 파일)

## 1. 분석 목적

서비스 계층 추출(Step 4)이 완료된 현재 코드베이스에서 **구조 변경**이 필요한 지점을 식별한다.
구조 변경이란 입력과 출력이 유지되는 상태에서의 코드 이동, 메서드 추출, 계층 분리, 의존성 정리를 의미한다.

리팩터링의 목적은 **변경이 가능한 상태**를 만들고, **변경 시 위험을 통제**하는 것이다.
각 작업 항목은 이 목적에 부합하는지 구체적 근거와 함께 검증한다.

## 2. 현재 패키지 의존성 그래프

작업 항목의 근거를 이해하기 위해, 현재 패키지 간 의존 관계를 먼저 정리한다.

```
auth ──→ member    (AuthenticationResolver → MemberRepository,
                    KakaoAuthService → MemberService)

member ──→ auth    (MemberService → JwtProvider)

product ──→ category  (ProductService → CategoryService)

option ──→ product    (OptionService → ProductRepository,
                       Option entity → Product entity)

order ──→ option      (OrderService → OptionRepository)
order ──→ member      (OrderService → MemberRepository)
order ──→ product     (KakaoMessageClient 시그니처에 Product 사용)

wish ──→ product      (WishService → ProductRepository,
                       Wish entity → Product entity)
```

주목할 점:
- `auth ↔ member` 순환이 존재한다
- 4개의 서비스가 자기 패키지가 아닌 Repository를 직접 참조한다
- 외부 HTTP API 클라이언트 2개가 구체 클래스로 주입된다

---

## 3. 작업 항목

### 작업 1: 예외 처리 중앙화 — @ControllerAdvice 도입

#### 근거

5개 컨트롤러에 동일한 `@ExceptionHandler` 메서드가 10개 반복된다.

| 예외 | HTTP 상태 | 위치 |
|------|----------|------|
| `NoSuchElementException` | 404 | `CategoryController.java:54`, `ProductController.java:62`, `OptionController.java:52`, `OrderController.java:54`, `WishController.java:73` |
| `IllegalArgumentException` | 400 | `ProductController.java:67`, `OptionController.java:57`, `OrderController.java:59`, `MemberController.java:40` |
| `IllegalStateException` | 403 | `WishController.java:78` |

모든 컨트롤러에서 동일 예외에 대해 동일한 응답을 반환한다. 예외가 없다. 그럼에도 10개의 메서드가 각 컨트롤러에 분산되어 있다.

#### 문제

**변경 시 위험을 통제할 수 없다.** 예외 응답 형식을 변경하거나 새로운 예외 매핑을 추가할 때, 5개 컨트롤러를 빠짐없이 수정해야 한다. 한 곳이라도 누락되면 동일한 예외인데 컨트롤러마다 다른 응답을 반환하게 된다.

또한 새 컨트롤러를 추가할 때마다 동일한 핸들러를 복사해야 하므로, 코드가 증가할수록 누락 위험도 비례하여 증가한다.

#### ADR-001: 예외 처리 전략

**선택지**

| 선택지 | 설명 | 장점 | 단점 |
|--------|------|------|------|
| A. 현행 유지 | 각 컨트롤러에 개별 핸들러 | 컨트롤러별 커스터마이징 가능 | 10개 중복, 누락 위험 |
| B. @ControllerAdvice | 전역 예외 핸들러 1개 | 변경 지점 1곳, 중복 제거 | 컨트롤러별 예외 분기가 어려움 |
| C. 베이스 컨트롤러 상속 | 부모 클래스에 공통 핸들러 | 선택적 오버라이드 가능 | 단일 상속 제약, 클래스 결합도 증가 |

**결정: B안 — @ControllerAdvice**

- 현재 10개 핸들러 중 동일 예외에 대해 다른 응답을 반환하는 경우가 **0건**이다. 컨트롤러별 커스터마이징 필요성이 없으므로 A안의 장점이 실현되지 않는다.
- Spring `@ControllerAdvice`는 프레임워크 표준이며, 필요 시 특정 컨트롤러에 개별 `@ExceptionHandler`를 작성하면 우선 적용된다. B안의 단점은 실질적으로 없다.
- C안은 Java 단일 상속 제약이 있고, 이 문제 해결을 위해 상속 계층을 도입하는 것은 과도하다.

**규칙**: 예외-HTTP 상태 매핑은 `@ControllerAdvice` 1개에서 관리한다. 특정 컨트롤러에서 다른 응답이 필요한 경우에만 해당 컨트롤러에 개별 핸들러를 작성한다.

**검증**: 인수 테스트 17개 전체 통과. 특히 4XX 에러 시나리오가 동일한 HTTP 상태를 반환하는지 확인.

#### 작업 내용

1. `@ControllerAdvice` 클래스 생성 — `NoSuchElementException → 404`, `IllegalArgumentException → 400`, `IllegalStateException → 403` 매핑
2. 5개 컨트롤러에서 `@ExceptionHandler` 메서드 10개 제거
3. 인수 테스트 실행

**영향 범위**: CategoryController, ProductController, OptionController, OrderController, WishController, MemberController

---

### 작업 2: 인증 처리 추출 — HandlerMethodArgumentResolver 도입

#### 근거

인증이 필요한 컨트롤러 메서드에서 동일한 패턴이 5회 반복된다.

```java
// OrderController.java:32-35, :43-46
// WishController.java:34-37, :45-48, :64-67
var member = authenticationResolver.extractMember(authorization);
if (member == null) {
    return ResponseEntity.status(401).build();
}
```

이 패턴은 3가지 요소로 구성된다:
1. `@RequestHeader("Authorization") String authorization` 파라미터 선언
2. `authenticationResolver.extractMember(authorization)` 호출
3. null 체크 + 401 반환

세 요소가 묶여서 5곳에 복사되어 있다.

#### 문제

**변경이 가능한 상태가 아니다.** 인증 방식을 변경(예: 헤더 이름 변경, 토큰 형식 변경, 인증 실패 응답 본문 추가)하려면 5곳을 모두 수정해야 한다. 수정 누락 시 일부 엔드포인트만 새 인증 방식을 적용하게 되어, 동일 API 내에서 인증 동작이 불일치한다.

또한 인증이 필요한 새 엔드포인트를 추가할 때마다 이 패턴을 복사해야 하므로, 누락(인증 없이 노출)의 위험이 있다.

#### ADR-002: 인증 중복 제거 전략

**선택지**

| 선택지 | 설명 | 장점 | 단점 |
|--------|------|------|------|
| A. 현행 유지 | 각 메서드에서 인라인 인증 | 명시적, 흐름이 한눈에 보임 | 5회 중복, 누락 위험 |
| B. HandlerMethodArgumentResolver | Spring MVC 인자 리졸버로 Member 자동 주입 | 파라미터 선언만으로 인증 적용, 프레임워크 표준 | 인증 실패 시 예외 → @ControllerAdvice 연계 필요 |
| C. 인터셉터 + 어노테이션 | `@Authenticated` 어노테이션 + HandlerInterceptor | 선언적 인증, AOP 스타일 | 어노테이션 + 인터셉터 + 리졸버 3개 구현 필요, 과도한 복잡성 |

**결정: B안 — HandlerMethodArgumentResolver**

- 현재 `AuthenticationResolver`가 이미 인증 로직을 캡슐화하고 있다(`AuthenticationResolver.java:23-31`). 이것을 `HandlerMethodArgumentResolver` 인터페이스로 전환하면, 컨트롤러 메서드가 `Member member`를 파라미터로 바로 받을 수 있다.
- B안의 단점("@ControllerAdvice 연계 필요")은 작업 1에서 이미 `@ControllerAdvice`를 도입하므로 해소된다. 인증 실패 시 전용 예외를 던지고 `@ControllerAdvice`에서 401로 변환하면 된다. **작업 1이 작업 2의 전제 조건이다.**
- C안은 B안과 동일한 결과를 달성하지만 구현 복잡도가 더 높다. 어노테이션 없이도 "파라미터에 `Member` 타입이 있으면 인증"이라는 규칙으로 충분하다.
- 이 변경은 **구조 변경**이다. 입력(Authorization 헤더)과 출력(인증된 Member 또는 401)이 동일하게 유지되며, 코드의 위치만 이동한다.

**규칙**: 인증이 필요한 엔드포인트는 컨트롤러 메서드 파라미터에 `Member`를 선언한다. `HandlerMethodArgumentResolver`가 Authorization 헤더에서 Member를 추출하고, 인증 실패 시 전용 예외를 던진다.

**검증**: 인수 테스트 17개 전체 통과. 인증이 필요한 주문/위시 시나리오가 동일하게 동작하는지 확인.

#### 작업 내용

1. `AuthenticationResolver`를 `HandlerMethodArgumentResolver` 인터페이스 구현으로 전환
2. 인증 실패 시 던질 전용 예외 클래스 생성 → `@ControllerAdvice`에 401 매핑 추가
3. `WebMvcConfigurer`에 리졸버 등록
4. OrderController, WishController에서 인증 인라인 코드 제거 — 파라미터로 `Member` 직접 수신
5. 인수 테스트 실행

**영향 범위**: AuthenticationResolver, OrderController, WishController, @ControllerAdvice
**전제 조건**: 작업 1 완료

---

### 작업 3: 크로스 패키지 Repository 참조 제거 — 서비스 위임

#### 근거

4개의 서비스가 자기 패키지가 아닌 다른 패키지의 Repository를 직접 참조한다.

| 서비스 | 외부 Repository | 근거 |
|--------|----------------|------|
| OrderService | OptionRepository | `OrderService.java:6` (`import gift.option.OptionRepository`) |
| OrderService | MemberRepository | `OrderService.java:4` (`import gift.member.MemberRepository`) |
| WishService | ProductRepository | `WishService.java:4` (`import gift.product.ProductRepository`) |
| OptionService | ProductRepository | `OptionService.java:4` (`import gift.product.ProductRepository`) |
| AuthenticationResolver | MemberRepository | `AuthenticationResolver.java:4` (`import gift.member.MemberRepository`) |

각 도메인에 이미 Service 계층이 존재한다. 그럼에도 서비스가 타 도메인의 Repository를 직접 호출한다. 이로 인해 `findById + orElseThrow` 조회 패턴이 중복된다.

예시 — `OrderService.java:38-40`과 `OptionService`에 이미 존재하는(또는 추가 가능한) `findById` 메서드:

```java
// OrderService.java:38-40 — OptionRepository 직접 호출
Option option = optionRepository
        .findById(optionId)
        .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. id=" + optionId));
```

같은 패턴이 `OrderService`에서 MemberRepository에 대해(`OrderService.java:45-47`), `WishService`에서 ProductRepository에 대해(`WishService.java:28-30`), `OptionService`에서 ProductRepository에 대해(`OptionService.java:57-60`) 반복된다.

#### 문제

**도메인 경계가 모호하다.** Repository는 도메인의 내부 구현이다. 다른 도메인의 Repository를 직접 사용하면:

1. **조회 로직이 분산된다.** 예를 들어 Option 조회 시 예외 메시지를 변경하려면 `OptionService`뿐 아니라 `OrderService`도 수정해야 한다. 변경 지점이 2곳 이상이 된다.
2. **Repository 시그니처 변경의 영향이 패키지를 넘는다.** `OptionRepository`의 메서드를 변경하면 `option` 패키지뿐 아니라 `order` 패키지도 영향을 받는다.

#### ADR-003: 크로스 패키지 데이터 접근 정책

**선택지**

| 선택지 | 설명 | 장점 | 단점 |
|--------|------|------|------|
| A. 현행 유지 | Repository 직접 참조 | 단순, 간접 계층 없음 | 도메인 경계 모호, 조회 로직 중복 |
| B. 서비스 위임 | 타 도메인 접근 시 해당 Service를 통해 접근 | 도메인 캡슐화, `findById` 중복 제거 | 서비스 간 의존 발생, 간접 계층 추가 |

**결정: B안 — 서비스 위임**

- `OrderService.createOrder()`에서 `optionRepository.findById()` 호출은 `OptionService.findById()`로 대체 가능하다. 이미 `ProductService.findById()`, `CategoryService.findById()`, `MemberService.findById()` 등 유사한 메서드가 존재하며, `OptionService`에도 `findById` 메서드 추가만 하면 된다.
- 서비스 위임의 단점인 "간접 계층 추가"는 실질적으로 메서드 1개 호출이 추가되는 것이다. 반면 얻는 이점은 도메인별 조회 로직(예외 메시지, 검증, 로깅 등) 단일화다.
- 트랜잭션 경계: 호출하는 서비스의 `@Transactional` 내에서 호출되므로 Spring 기본 `PROPAGATION_REQUIRED`에 의해 동일 트랜잭션에 참여한다. 추가적인 트랜잭션 문제가 없다.

**규칙**: 서비스는 자기 패키지의 Repository만 직접 참조한다. 타 도메인 데이터가 필요하면 해당 도메인의 Service를 통해 접근한다.

**예외**: `AuthenticationResolver`는 인증 인프라 컴포넌트이므로 이 규칙을 즉시 적용하지 않는다. 작업 2에서 `HandlerMethodArgumentResolver`로 전환될 때 함께 정리한다.

**검증**: 인수 테스트 17개 전체 통과.

#### 작업 내용

1. `OptionService`에 `findById(Long)` 메서드 추가
2. `OrderService`에서 `OptionRepository`, `MemberRepository` 의존 제거 → `OptionService`, `MemberService`로 대체
3. `WishService`에서 `ProductRepository` 의존 제거 → `ProductService`로 대체
4. `OptionService`에서 `ProductRepository` 의존 제거 → `ProductService`로 대체
5. 사용하지 않게 된 import 및 필드 제거
6. 인수 테스트 실행

**영향 범위**: OrderService, WishService, OptionService, (OptionService에 메서드 추가)

---

### 작업 4: 외부 인프라 인터페이스 추출 — OrderMessageClient

#### 근거

`OrderService`가 `KakaoMessageClient` 구체 클래스에 직접 의존한다.

```java
// OrderService.java:18
private final KakaoMessageClient kakaoMessageClient;
```

`KakaoMessageClient`는 Kakao REST API(`https://kapi.kakao.com/v2/api/talk/memo/default/send`)에 HTTP 호출을 수행하는 외부 인프라 컴포넌트다(`KakaoMessageClient.java:22-29`).

한편 `KakaoLoginClient`도 구체 클래스로 `KakaoAuthService`에 주입되지만(`KakaoAuthService.java:8`), `KakaoAuthService` 자체가 Kakao 전용 서비스이므로 Kakao 클라이언트에 직접 의존하는 것이 자연스럽다.

#### 문제

**OrderService가 특정 벤더(Kakao)에 결합되어 있다.** OrderService는 "주문 후 메시지를 보낸다"는 비즈니스 로직을 담당하는데, "Kakao로 보낸다"는 인프라 구현 세부사항까지 알고 있다. 메시지 전송 채널을 변경(예: SMS, 이메일, 다른 메신저)하려면 `OrderService`를 수정해야 한다.

또한 `OrderService`의 단위 테스트를 작성할 때, Kakao API의 구체적인 동작을 모킹해야 한다. 인터페이스가 있으면 "메시지가 전송되었는가"라는 행위 수준의 검증이 가능하다.

#### ADR-004: 외부 인프라 클라이언트 추상화 전략

**선택지**

| 선택지 | 설명 | 장점 | 단점 |
|--------|------|------|------|
| A. 현행 유지 | 구체 클래스 의존 | 단순, 변경 없음 | 벤더 결합, 채널 교체 시 서비스 수정 |
| B. 개별 인터페이스 추출 | KakaoMessageClient에 대해 인터페이스 정의 | 벤더 비의존, 테스트 용이 | 구현이 1개뿐인 인터페이스 |
| C. 역할 기반 인터페이스 | `MessageSender` 등 범용 인터페이스 | 벤더 중립적 | 과도한 일반화, 추상화 수준 결정이 어려움 |

**결정: B안 — 개별 인터페이스 추출 (KakaoMessageClient만)**

- `OrderService`는 도메인 서비스이다. "Kakao"라는 벤더명이 도메인 서비스의 필드로 존재하는 것은 계층 위반이다. `OrderMessageClient`라는 인터페이스를 정의하고 `KakaoMessageClient`가 이를 구현하면, `OrderService`는 "메시지를 보내는 행위"에만 의존한다.
- `KakaoLoginClient`는 추출하지 않는다. `KakaoAuthService`가 Kakao 전용 서비스이므로 Kakao 구현체에 직접 의존하는 것이 자연스럽다. 다른 OAuth 제공자가 추가되면 별도의 `{Provider}AuthService`를 만든다.
- C안은 현재 메시지 전송 채널이 Kakao 1개뿐인 상태에서 과도한 일반화다.

**규칙**: 도메인 서비스는 외부 인프라 구현체가 아닌 인터페이스에 의존한다. 인터페이스 이름은 벤더명을 포함하지 않는다. 단, 벤더 전용 서비스(예: KakaoAuthService)는 해당 벤더 구현체에 직접 의존할 수 있다.

**검증**: 인수 테스트 17개 전체 통과.

#### 작업 내용

1. `OrderMessageClient` 인터페이스 생성 (order 패키지) — `sendToMe(String accessToken, Order order, Product product)` 메서드 정의
2. `KakaoMessageClient`가 `OrderMessageClient`를 구현하도록 변경
3. `OrderService`의 의존을 `KakaoMessageClient` → `OrderMessageClient`로 변경
4. 인수 테스트 실행

**영향 범위**: OrderService, KakaoMessageClient, order 패키지

---

### 작업 5: auth ↔ member 순환 참조 해소

#### 근거

auth 패키지와 member 패키지가 서로를 참조하는 순환 구조다.

```
auth → member:  AuthenticationResolver.java:4  (import gift.member.MemberRepository)
                KakaoAuthService.java:3         (import gift.member.MemberService)

member → auth:  MemberService.java:3            (import gift.auth.JwtProvider)
```

auth가 member를 알고, member가 auth를 안다.

#### 문제

**두 패키지를 독립적으로 변경할 수 없다.** auth 패키지의 내부 구조를 변경하면 member 패키지가 영향받고, 그 역도 성립한다. 순환 참조가 있으면 변경의 영향 범위를 예측할 수 없다.

구체적으로: `JwtProvider`의 메서드 시그니처를 변경하면 `MemberService`가 영향받는데, `MemberService`는 `KakaoAuthService`가 의존하고 있어 auth 패키지에 다시 영향이 돌아온다. 한 방향의 변경이 양 패키지를 순회하게 된다.

#### ADR-005: auth ↔ member 순환 해소 전략

**순환의 원인 분석**

순환을 끊으려면 두 방향 중 하나를 제거해야 한다.
- `auth → member` 방향: AuthenticationResolver가 회원을 조회하고, KakaoAuthService가 회원 로그인을 수행한다. 인증이 회원에 의존하는 것은 자연스럽다. **제거 불가.**
- `member → auth` 방향: MemberService가 JwtProvider를 호출하여 토큰을 생성한다. 회원 서비스가 토큰 생성이라는 인증 인프라에 의존한다. **이 방향을 끊어야 한다.**

`member → auth`를 끊으려면, MemberService가 auth 패키지의 어떤 것도 import하지 않아야 한다. 인터페이스를 auth 패키지에 두면 `member → auth` 의존이 여전히 존재하므로 순환이 해소되지 않는다.

**선택지**

| 선택지 | 설명 | 장점 | 단점 |
|--------|------|------|------|
| A. 현행 유지 | 순환 허용 | 변경 없음 | 패키지 독립성 부재, 변경 영향 순회 |
| B. 의존성 역전 — member에 인터페이스 배치 | member 패키지에 `TokenProvider` 인터페이스를 정의하고, auth의 `JwtProvider`가 이를 구현 | 순환 완전 해소, 의존이 auth → member 한 방향으로 정리 | "토큰 생성"이라는 인증 개념을 member 패키지가 소유 |
| C. 공용 패키지 도입 | `common` 패키지에 `TokenProvider` 인터페이스 배치, auth와 member 모두 common에 의존 | 순환 해소, 인터페이스가 중립적 위치에 존재 | 인터페이스 1개를 위해 새 패키지 도입, 향후 dump 패키지화 위험 |
| D. 패키지 통합 | auth와 member를 하나의 패키지(예: account)로 합침 | 순환이 정의상 소멸, 강결합 현실을 구조에 반영 | 패키지 크기 증가(13개 파일), JWT·OAuth·회원 CRUD 관심사 혼합 |

**결정: B안 — 의존성 역전 (member 패키지에 인터페이스)**

- `MemberService`는 토큰 생성의 **소비자**다. 소비자가 자신이 필요한 인터페이스를 정의하고, 제공자(auth)가 이를 구현하는 것은 의존성 역전 원칙(DIP)의 표준 적용이다.
- B안의 단점인 "member가 토큰 개념을 소유한다"는 우려는, 인터페이스가 member의 관점에서 정의된다고 보면 자연스럽다. member는 "내가 토큰을 생성할 수 있는 수단이 필요하다"를 인터페이스로 선언할 뿐이고, JWT인지 다른 방식인지는 모른다.
- C안은 인터페이스 1개를 위해 새 패키지를 도입한다. 현재 프로젝트에 공용 패키지가 없으므로 패키지 구조의 선례를 만들게 된다. 향후 공유할 인터페이스가 늘어나면 재검토할 수 있지만, 현 시점에서는 과도하다.
- D안은 auth 패키지의 7개 파일과 member 패키지의 6개 파일을 합쳐 13개 파일이 되며, JWT 인프라, OAuth 클라이언트, 회원 CRUD라는 서로 다른 관심사가 한 패키지에 혼합된다. 순환을 해소하기 위해 응집도를 희생하는 것은 적절하지 않다.

변경 후 의존성:

```
auth → member:  AuthenticationResolver → MemberService    (유지)
                KakaoAuthService → MemberService           (유지)
                JwtProvider implements member.TokenProvider (신규 — auth가 member의 인터페이스 구현)

member → auth:  (없음 — 순환 해소)
```

**검증**: 인수 테스트 17개 전체 통과.

#### 작업 내용

1. member 패키지에 `TokenProvider` 인터페이스 생성 — `createToken(String email)` 메서드
2. `JwtProvider`가 `TokenProvider`를 구현하도록 변경
3. `MemberService`의 의존을 `gift.auth.JwtProvider` → `gift.member.TokenProvider`로 변경 (같은 패키지 내 인터페이스)
4. 인수 테스트 실행

**영향 범위**: member 패키지 (인터페이스 추가), auth 패키지 (JwtProvider implements 추가), MemberService (import 변경)

---

### 작업 6: 도메인 책임 이동 — 비밀번호 검증을 Member로

#### 근거

비밀번호 일치 검증이 `MemberService`에 위치한다.

```java
// MemberService.java:33
if (member.getPassword() == null || !member.getPassword().equals(password)) {
    throw new IllegalArgumentException("Invalid email or password.");
}
```

`Member` 엔티티는 `chargePoint()`, `deductPoint()` 등 자신의 비즈니스 규칙을 메서드로 캡슐화하고 있다(`Member.java:48-63`). 그런데 비밀번호 검증만 서비스에 노출되어 있다. 서비스가 `member.getPassword()`로 내부 상태를 꺼내서 직접 비교하고 있다.

#### 문제

**비밀번호 검증 정책을 변경할 때 변경 지점이 도메인 외부에 있다.** 향후 비밀번호 해싱(BCrypt 등)을 도입하면, 비교 로직이 `password.equals(input)`에서 `passwordEncoder.matches(input, hashed)`로 바뀌어야 한다. 이 변경이 `MemberService`에서 이루어져야 한다는 것은, Member 도메인의 핵심 정책이 서비스 계층에 유출되어 있음을 의미한다.

`Member.chargePoint()`와 `Member.deductPoint()`가 금액 검증을 도메인 내부에서 수행하는 것처럼, 비밀번호 검증도 `Member.matchesPassword(String)`로 캡슐화하면 일관성이 확보된다.

이 작업에는 ADR이 필요 없다. 선택지 간 트레이드오프가 없으며, "도메인 로직은 도메인 객체에 위치한다"는 원칙의 직접 적용이다.

#### 작업 내용

1. `Member`에 `matchesPassword(String rawPassword)` 메서드 추가 — null 체크 포함
2. `MemberService.login()`에서 `getPassword()` 직접 비교를 `member.matchesPassword(password)`로 대체
3. 인수 테스트 실행

**영향 범위**: Member, MemberService

---

## 4. 현행 유지 항목

분석 결과 구조 변경이 불필요하거나, 이번 작업 범위 밖인 항목을 정리한다.

| 항목 | 현재 상태 | 사유 |
|------|----------|------|
| "최소 1개 옵션" 규칙의 서비스 위치 | `OptionService.java:42-45`에서 DB 조회 후 검증 | DB 조회가 필요하므로 서비스 레벨이 적절. 도메인으로 이동하려면 Product가 options를 항상 로딩해야 하며, 성능 트레이드오프가 있다 |
| Product, Order, Wish, Category 데이터 홀더 | `update()` 메서드만 존재, 도메인 로직 없음 | 현재 요구사항에서 복잡한 도메인 로직이 없다. 과도한 추상화 방지 |
| Validator 구조 중복 | `ProductNameValidator`와 `OptionNameValidator`의 정규식 동일 | 2개뿐이며 독립적으로 진화할 가능성이 있다. 공통화하면 한쪽 변경이 다른 쪽에 영향. 세 번째 Validator 등장 시 재검토 |
| AdminProductController 이중 검증 | 서비스 호출 전 직접 검증 + 서비스 내부 재검증 | `allowKakao` 플래그는 정책 변경(작동 변경)에 해당하므로 이번 범위 밖 |
| KakaoLoginClient 추상화 | KakaoAuthService → KakaoLoginClient 구체 의존 | KakaoAuthService가 Kakao 전용이므로 구체 의존이 자연스럽다. 다른 OAuth 제공자 추가 시 별도 서비스 생성 |

---

## 5. 작업 순서 요약

| 순서 | 작업 | 전제 조건 | 영향 범위 |
|------|------|----------|----------|
| 1 | 예외 처리 중앙화 (@ControllerAdvice) | 없음 | 전 컨트롤러 |
| 2 | 인증 처리 추출 (HandlerMethodArgumentResolver) | 작업 1 | OrderController, WishController, AuthenticationResolver |
| 3 | 크로스 패키지 Repository 참조 제거 | 없음 | OrderService, WishService, OptionService |
| 4 | 외부 인프라 인터페이스 추출 (OrderMessageClient) | 없음 | OrderService, KakaoMessageClient |
| 5 | auth ↔ member 순환 해소 (의존성 역전 — member에 TokenProvider 인터페이스) | 없음 | member 패키지, auth 패키지, MemberService |
| 6 | 도메인 책임 이동 (Member.matchesPassword) | 없음 | Member, MemberService |

**순서 근거**
- 작업 1 → 2: `@ControllerAdvice`가 있어야 `HandlerMethodArgumentResolver`의 인증 실패 예외를 처리할 수 있다.
- 작업 3~6: 서로 독립적이다. 영향 범위가 넓은 것(크로스 패키지 의존, 3개 서비스 수정)을 먼저, 좁은 것(단일 메서드 이동)을 마지막에 배치한다.

**검증 전략**: 각 작업 완료 후 인수 테스트 17개 시나리오를 실행하여, 입력과 출력이 변경되지 않았음을 확인한다.