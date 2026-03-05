# ADR-1: 인증 로직 추출 방식

> Phase 1 관련

## 상태
제안됨

## 맥락
OrderController(2곳)와 WishController(3곳)에서 동일한 인증 코드가 5회 반복된다. Spring MVC에서 컨트롤러 진입 전에 공통 로직을 실행하는 방법은 여러 가지가 있다.

## 선택지

### 방법 1: `HandlerMethodArgumentResolver` + 커스텀 어노테이션

```java
// 컨트롤러 메서드 시그니처가 이렇게 변한다
public ResponseEntity<?> getOrders(@LoginMember Member member, Pageable pageable)
```

| 항목 | 평가 |
|------|------|
| 변경 범위 | 중간 — 어노테이션 1개, Resolver 1개, WebMvcConfig 1개 생성 |
| Spring MVC 관용성 | 높음 — `@PathVariable`, `@RequestBody`와 동일한 패턴 |
| 컨트롤러 변경 | `@RequestHeader` 제거 + `@LoginMember Member` 추가 |
| 테스트 영향 | 없음 — 인수 테스트는 HTTP 헤더를 보내므로 동일하게 동작 |
| 인증 실패 처리 | Resolver에서 예외 발생 → `@ControllerAdvice`에서 401 매핑 |

### 방법 2: Servlet Filter

```java
// Filter에서 인증 후 request attribute에 저장
request.setAttribute("loginMember", member);
```

| 항목 | 평가 |
|------|------|
| 변경 범위 | 중간 — Filter 1개, FilterRegistration 1개 |
| Spring MVC 관용성 | 낮음 — `request.getAttribute()` 캐스팅 필요 |
| 컨트롤러 변경 | `@RequestHeader` 제거 + `request.getAttribute()` 추가 |
| 테스트 영향 | 없음 |
| 인증 실패 처리 | Filter에서 직접 401 응답 |

### 방법 3: Spring Security

| 항목 | 평가 |
|------|------|
| 변경 범위 | **매우 큼** — SecurityFilterChain, UserDetailsService, 설정 클래스 다수 |
| Spring MVC 관용성 | 최고 — 산업 표준 |
| 컨트롤러 변경 | `@AuthenticationPrincipal` 사용 |
| 테스트 영향 | **높음** — Security 설정에 따라 기존 테스트 실패 가능 |
| 인증 실패 처리 | Security가 자동 처리 |

### 방법 4: AOP (`@Aspect`)

| 항목 | 평가 |
|------|------|
| 변경 범위 | 중간 — Aspect 1개, 어노테이션 1개 |
| Spring MVC 관용성 | 낮음 — 컨트롤러 파라미터로 Member를 받는 방법이 부자연스러움 |
| 컨트롤러 변경 | 메서드에 어노테이션 추가, Member는 별도로 주입 필요 |
| 테스트 영향 | 낮음 |
| 인증 실패 처리 | Aspect에서 예외 발생 |

## 결정
**방법 1: `HandlerMethodArgumentResolver`** 선택

## 근거
1. **Spring MVC 관용적**: `@PathVariable`, `@RequestBody`와 동일한 패턴으로, 컨트롤러 메서드 시그니처만 보면 인증이 필요한지 알 수 있다
2. **변경 범위 적절**: 3개 파일 생성, 2개 컨트롤러 수정으로 완료
3. **컨트롤러가 깔끔해짐**: `@LoginMember Member member`로 인증된 회원을 바로 받을 수 있다
4. **테스트 영향 없음**: HTTP 레벨에서 동일하게 동작
5. **Spring Security 도입 대비 불필요**: 현재 프로젝트 규모에서 Security는 과도함

## 결과
- `@LoginMember` 어노테이션, `LoginMemberArgumentResolver`, `WebMvcConfig` 3개 파일이 추가된다
- OrderController, WishController에서 `AuthenticationResolver` 의존성이 제거된다
- 향후 인증 로직 변경 시 `LoginMemberArgumentResolver` 한 곳만 수정하면 된다
