# Phase 1: 인증 로직 추출 — 세부 실행 계획

> 유형: **구조 변경** (외부 동작 불변)
> 상태: 계획 수립

---

## 1. 문제 정의

OrderController(2곳)와 WishController(3곳)에서 동일한 인증 코드가 5회 반복된다.

```java
// 5곳에서 반복되는 동일 패턴
var member = authenticationResolver.extractMember(authorization);
if (member == null) {
    return ResponseEntity.status(401).build();
}
```

**영향받는 파일과 메서드**:

| 파일 | 메서드 | 라인 |
|------|--------|------|
| `OrderController.java` | `getOrders()` | `@RequestHeader` + null 체크 |
| `OrderController.java` | `createOrder()` | `@RequestHeader` + null 체크 |
| `WishController.java` | `getWishes()` | `@RequestHeader` + null 체크 |
| `WishController.java` | `addWish()` | `@RequestHeader` + null 체크 |
| `WishController.java` | `removeWish()` | `@RequestHeader` + null 체크 |

**현재 의존 구조**:
```
OrderController  ──→ AuthenticationResolver ──→ JwtProvider
WishController   ──→ AuthenticationResolver ──→ MemberRepository
```

---

## 2. 의사결정

> **[ADR-1: 인증 로직 추출 방식](../adr/ADR1-인증_로직_추출_방식.md)** 참조
>
> 결정: `HandlerMethodArgumentResolver` + `@LoginMember` 커스텀 어노테이션 방식 채택

---

## 3. 동작 변경 위험 분석

> 구조 변경에서 의도치 않은 동작 변경이 발생하지 않는지 사전 점검한다.

### 현재 동작 매핑

| 상황 | 현재 동작 | 이유 |
|------|----------|------|
| 유효한 토큰 | 200 (정상) | extractMember()가 Member 반환 |
| 유효하지 않은 토큰 | 401 | extractMember()가 null 반환 → Controller에서 401 |
| Authorization 헤더 누락 | 400 | `@RequestHeader`가 required=true(기본값)이므로 Spring이 400 반환 |

### ArgumentResolver 적용 후 예상 동작

Resolver에서 **현재 동작을 정확히 재현**하여 동작 변경을 방지한다.

| 상황 | 변경 후 동작 | Resolver 처리 |
|------|------------|---------------|
| 유효한 토큰 | 200 (정상) | Member 반환 → **동일** |
| 유효하지 않은 토큰 | 401 | `ResponseStatusException(UNAUTHORIZED)` → **동일** |
| Authorization 헤더 누락 | **400** | `ResponseStatusException(BAD_REQUEST)` → **동일** |

```java
// 헤더 누락 → 기존과 동일하게 400 유지
if (authorization == null) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Required request header 'Authorization' is not present");
}

// 토큰 무효 → 기존과 동일하게 401 유지
if (member == null) {
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
}
```

### 위험 평가

**모든 케이스에서 기존 HTTP 응답 코드가 보존된다.**
- 헤더 누락: 400 → 400 (동일)
- 유효하지 않은 토큰: 401 → 401 (동일)
- 유효한 토큰: 200 → 200 (동일)

구조 변경 원칙을 준수하여 **작동 변경이 발생하지 않는다.**

---

## 4. 생성/수정할 파일 목록

### 새로 생성 (3개)

| 파일 | 역할 |
|------|------|
| `src/main/java/gift/auth/LoginMember.java` | 커스텀 어노테이션 |
| `src/main/java/gift/auth/LoginMemberArgumentResolver.java` | 인증 로직 실행 + Member 반환 |
| `src/main/java/gift/config/WebMvcConfig.java` | ArgumentResolver 등록 |

### 수정 (2개)

| 파일 | 변경 내용 |
|------|----------|
| `src/main/java/gift/order/OrderController.java` | `@RequestHeader` → `@LoginMember`, AuthenticationResolver 의존성 제거 |
| `src/main/java/gift/wish/WishController.java` | `@RequestHeader` → `@LoginMember`, AuthenticationResolver 의존성 제거 |

### 변경 없음

| 파일 | 이유 |
|------|------|
| `AuthenticationResolver.java` | 기존 클래스 그대로 유지 — Resolver에서 호출 |
| `JwtProvider.java` | 변경 없음 |
| 인수 테스트 전체 | HTTP 레벨 동작 동일 — 테스트 수정 불필요 |

---

## 5. 구현 단계 (Step-by-Step)

### Step 1: `@LoginMember` 어노테이션 생성

```java
// src/main/java/gift/auth/LoginMember.java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMember {
}
```

**확인**: 컴파일 가능한지 빌드 확인

---

### Step 2: `LoginMemberArgumentResolver` 생성

```java
// src/main/java/gift/auth/LoginMemberArgumentResolver.java
@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final AuthenticationResolver authenticationResolver;

    // 생성자 주입

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ..., NativeWebRequest webRequest, ...) {
        var authorization = webRequest.getHeader("Authorization");
        if (authorization == null) {
            // 기존 @RequestHeader 누락 시 Spring이 400을 반환하던 동작을 보존
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Required request header 'Authorization' is not present");
        }

        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            // 기존 Controller에서 401을 반환하던 동작을 보존
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return member;
    }
}
```

**의사결정**: 예외 타입으로 `ResponseStatusException`을 사용한다.
- 이유: Phase 2에서 `@ControllerAdvice`를 도입하기 전이므로, Spring 기본 제공 예외로 401을 바로 매핑할 수 있다
- Phase 2 이후에 커스텀 예외로 전환할 수 있지만, 이 단계에서는 최소 변경

**확인**: 컴파일 확인

---

### Step 3: `WebMvcConfig` 생성 및 Resolver 등록

```java
// src/main/java/gift/config/WebMvcConfig.java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginMemberArgumentResolver loginMemberArgumentResolver;

    // 생성자 주입

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginMemberArgumentResolver);
    }
}
```

**확인**: 애플리케이션 구동 확인 (`./gradlew bootRun` 또는 테스트)

---

### Step 4: OrderController 수정

**변경 전**:
```java
public class OrderController {
    private final OrderService orderService;
    private final AuthenticationResolver authenticationResolver;  // 제거 대상

    @GetMapping
    public ResponseEntity<?> getOrders(
        @RequestHeader("Authorization") String authorization,  // 제거
        Pageable pageable
    ) {
        var member = authenticationResolver.extractMember(authorization);  // 제거
        if (member == null) {                                              // 제거
            return ResponseEntity.status(401).build();                     // 제거
        }
        ...
    }
}
```

**변경 후**:
```java
public class OrderController {
    private final OrderService orderService;
    // AuthenticationResolver 의존성 제거됨

    @GetMapping
    public ResponseEntity<?> getOrders(@LoginMember Member member, Pageable pageable) {
        var orders = orderService.findByMemberId(member.getId(), pageable).map(OrderResponse::from);
        return ResponseEntity.ok(orders);
    }
}
```

**확인**: 컴파일 확인

---

### Step 5: WishController 수정

OrderController와 동일한 패턴으로 3개 메서드 수정.

**확인**: 컴파일 확인

---

### Step 6: 전체 테스트 실행

```bash
./gradlew test
```

**기대 결과**: 모든 20개 시나리오 통과

---

### Step 7: diff 검토

```bash
git diff
```

**체크리스트**:
```
□ OrderController에서 인증 관련 코드가 모두 제거되었는가?
□ WishController에서 인증 관련 코드가 모두 제거되었는가?
□ AuthenticationResolver 클래스는 변경되지 않았는가?
□ 새로 생성된 파일은 LoginMember, LoginMemberArgumentResolver, WebMvcConfig 3개뿐인가?
□ 인수 테스트 파일은 변경되지 않았는가?
□ 구조 변경 외에 작동 변경이 섞이지 않았는가?
```

---

### Step 8: 커밋

```
refactor(auth): 인증 로직을 ArgumentResolver로 추출

OrderController, WishController에서 5회 반복되던 인증 코드를
LoginMemberArgumentResolver로 추출한다.

- @LoginMember 어노테이션 + ArgumentResolver 방식 채택 (ADR-1)
- 컨트롤러에서 AuthenticationResolver 의존성 제거
- 기존 HTTP 응답 코드 완전 보존 (헤더 누락 400, 토큰 무효 401)
```

---

## 6. 롤백 계획

문제 발생 시:
1. `git stash` 또는 `git checkout .` 으로 전체 변경 취소
2. 원인 분석 후 재시도

이 Phase의 변경은 모두 새 파일 생성 + 기존 파일 수정이므로, `git checkout .`으로 즉시 원복 가능하다.

---

## 7. Phase 1 완료 조건

```
□ LoginMember 어노테이션 생성 완료
□ LoginMemberArgumentResolver 생성 완료
□ WebMvcConfig 생성 및 Resolver 등록 완료
□ OrderController에서 인증 중복 코드 제거 완료
□ WishController에서 인증 중복 코드 제거 완료
□ 전체 인수 테스트(20개 시나리오) 통과
□ diff 검토 완료 — 구조 변경만 포함
□ 커밋 완료
□ CURRENT_PROJECT_STRUCTURE.md 업데이트
```
