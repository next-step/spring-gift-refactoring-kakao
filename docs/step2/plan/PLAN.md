# Step 2 리팩터링 실행 계획

> 작성일: 2026-03-04
> 기반: 리팩터링 피드백 5대 원칙

---

## 0. 미션의 핵심 이해

이 미션은 "코드를 예쁘게 만드는 것"이 아니라 **변경을 통제하는 방법을 훈련**하는 것이다.

핵심 질문:
- 이 변경이 구조 변경인가, 작동 변경인가?
- diff만 보고 변경 의도를 설명할 수 있는가?
- 이 커밋은 하나의 의도를 설명할 수 있는가?

---

## 1. 현재 상태 진단

### 1.1 Step 1에서 완료된 것
- Service 계층 추출 (Controller → Service 로직 이동)
- 코드 스타일 일관성 정리
- 미사용 코드 제거
- Cucumber BDD 인수 테스트 20개 시나리오

### 1.2 Step 1 피드백에서 지적된 문제
| 지적 사항 | 구체적 사례 |
|-----------|-----------|
| 구조 변경과 작동 변경이 섞임 | AdminMemberController에서 서비스 추출(구조) + 예외처리 방식 변경(작동)을 한 커밋에 수행 |
| AI 결과를 그대로 수용한 흔적 | WishRepository 미사용 변수가 스타일 작업을 거쳤음에도 남아 있음 |
| 변경 근거 기록 부재 | "왜 그렇게 했는가"에 대한 ADR이 없음 |

---

## 2. 현재 코드의 문제 식별 (원칙 4: 해결책보다 문제 정의가 먼저다)

> "좋은 요청은 해결책이 아니라 관찰할 수 있는 문제로 시작한다."

아래는 현재 코드에서 **관찰 가능한 문제**를 정의한 것이다. 해결 방법은 이 단계에서 정하지 않는다.

### 문제 A: 인증 로직이 컨트롤러에 중복되어 있다

**관찰**: OrderController와 WishController에서 동일한 인증 코드가 총 5회 반복된다.

```java
// OrderController - 2회 반복
var member = authenticationResolver.extractMember(authorization);
if (member == null) {
    return ResponseEntity.status(401).build();
}

// WishController - 3회 반복 (동일 패턴)
```

**위험**: 인증 관련 변경이 필요하면 5곳을 모두 수정해야 하고, 누락 가능성이 있다.

### 문제 B: 예외 처리 전략이 일관되지 않다

**관찰**: 에러 상황에서 각 계층의 처리 방식이 제각각이다.

| 계층 | 패턴 | 예시 |
|------|------|------|
| Service | `orElse(null)` 후 null 반환 | `ProductService.findById()` |
| Service | `orElseThrow()` 후 예외 | `ProductService.createFromAdmin()` |
| Service | enum 반환 | `WishService.WishDeleteResult` |
| Controller | null 체크 → 404 | `ProductController.getProduct()` |
| Controller | `@ExceptionHandler` | `ProductController`, `OptionController`, `MemberController`에만 존재 |
| Controller | 없음 | `OrderController`, `WishController`에는 예외 핸들러 없음 |

**위험**: 같은 유형의 에러가 컨트롤러에 따라 다른 HTTP 상태 코드(400 vs 500)로 응답된다.

### 문제 C: Service에 Admin 전용 메서드가 혼재한다

**관찰**: `ProductService`에 REST API용 메서드와 Admin 전용 메서드가 공존한다.

```java
// REST API용 - null 반환 방식
public Product create(ProductRequest request) { ... return null; }

// Admin 전용 - 예외 발생 방식
public Product createFromAdmin(String name, int price, ...) { ... orElseThrow(); }
```

**위험**: 같은 Service 내에서 에러 처리 전략이 다르므로 호출자가 혼란할 수 있다.

### 문제 D: 트랜잭션 경계가 없다

**관찰**: `OrderService.create()`에서 재고 차감 → 포인트 차감 → 주문 저장이 하나의 트랜잭션으로 묶이지 않는다.

```java
option.subtractQuantity(request.quantity());  // DB 저장
optionRepository.save(option);
member.deductPoint(price);                     // DB 저장 — 여기서 실패하면?
memberRepository.save(member);
var saved = orderRepository.save(...);         // DB 저장
```

**위험**: 포인트 차감 실패 시 재고만 빠져나가는 데이터 불일치가 발생한다.

### 문제 E: Validator 호출 위치가 분산되어 있다

**관찰**: `ProductNameValidator`가 두 군데에서 호출된다.
- `ProductService.create()` → `validateName(request.name())` (REST API)
- `AdminProductController.create()` → `ProductNameValidator.validate(name, true)` (Admin)

**위험**: 검증 로직 수정 시 두 곳을 모두 변경해야 한다. Admin은 `isAdmin=true` 파라미터로 "카카오" 제한을 우회하는데, 이 분기가 컨트롤러에 노출되어 있다.

### 문제 F: OrderService가 외부 메시징 구현체에 직접 의존한다

**관찰**: `OrderService`가 `KakaoMessageClient`(구체 클래스)에 직접 의존하고 있다.

```java
// OrderService.java
private final KakaoMessageClient kakaoMessageClient;

// 사용처
kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
```

`KakaoMessageClient`는 카카오 API URL(`https://kapi.kakao.com/...`)이 하드코딩되어 있고, 카카오 전용 템플릿 형식(`object_type: "text"`)에 종속되어 있다.

```java
// KakaoMessageClient.java — 카카오 API에 강하게 결합
restClient.post()
    .uri("https://kapi.kakao.com/v2/api/talk/memo/default/send")
    .header("Authorization", "Bearer " + accessToken)
    .body(params)
    .retrieve()
    .toBodilessEntity();
```

**위험**:
- 알림 채널이 변경되면(카카오 → 슬랙, SMS, 이메일 등) OrderService를 수정해야 한다
- 단위 테스트에서 카카오 API 호출을 대체(Mock)하기 어렵다 — 인터페이스가 없으므로 구체 클래스를 Mock해야 한다
- OrderService의 책임이 "주문 생성"과 "알림 발송"이 혼재한다

---

## 3. 구조 변경 vs 작동 변경 분류 (원칙 2)

> **핵심**: 이 두 가지를 절대 한 커밋에 섞지 않는다.

### 구조 변경 (외부 동작이 바뀌지 않는 변경)
| # | 변경 | 설명 | 검증 방법 |
|---|------|------|-----------|
| S1 | 인증 로직 추출 | 5곳의 중복 인증 코드를 한 곳으로 이동 | 기존 인수 테스트 전체 통과 |
| S2 | `@ExceptionHandler` 중앙화 | 3개 컨트롤러의 개별 핸들러를 `@RestControllerAdvice`로 이동 | 기존 인수 테스트 전체 통과 |
| S3 | Admin 메서드 정리 | `createFromAdmin`/`updateFromAdmin`을 별도 구조로 분리 | Admin 페이지 수동 테스트 + 기존 테스트 통과 |
| S4 | Validator 호출 위치 통일 | 검증 로직 호출 지점을 일관되게 정리 | 기존 인수 테스트 전체 통과 |
| S5 | 메시지 클라이언트 인터페이스 추출 | `KakaoMessageClient` 구체 클래스 의존을 인터페이스로 분리 | 기존 인수 테스트 전체 통과 |

### 작동 변경 (외부 동작이 바뀌는 변경)
| # | 변경 | 설명 | 검증 방법 |
|---|------|------|-----------|
| B0 | 헤더 누락 응답 코드 변경 | Authorization 헤더 누락 시 400 → 401 | 인수 테스트 추가 또는 기존 테스트 확인 |
| B1 | null 반환 → 예외 전환 | Service의 `orElse(null)` 패턴을 예외로 변경 | HTTP 응답 코드 변경을 인수 테스트로 검증 |
| B2 | `@Transactional` 추가 | OrderService.create()에 트랜잭션 경계 설정 | 실패 시나리오 테스트 추가 |
| B3 | OrderController 예외 처리 추가 | 현재 500으로 반환되는 에러를 400으로 변경 | 인수 테스트 기대값 수정 (500 → 400) |

---

## 4. 실행 순서와 고려사항 (원칙 3: 작고 검증 가능한 단계로 진행한다)

> "변경 → 테스트 실행 → diff 확인 → 커밋"

### Phase 0: 안전망 확인 (원칙 1)

**테스트 통과 상태에서 시작한다.**

```
./gradlew test   ← 이 명령이 통과하지 않으면 리팩터링을 시작하지 않는다
```

고려사항:
- 현재 인수 테스트가 모두 통과하는지 먼저 확인한다
- 실패하는 테스트가 있으면 리팩터링 전에 먼저 고친다
- 테스트가 안전망이 되려면 "지금 동작하는 상태"를 기록해야 한다

---

### Phase 1: 구조 변경 — 인증 로직 추출 (S1)

**문제**: 인증 코드가 OrderController(2회), WishController(3회)에 중복되어 있다.

**변경 전략**:
1. 인증 로직을 추출할 방법을 결정한다 (ADR 작성)
2. 추출한다
3. 기존 테스트가 모두 통과하는지 확인한다
4. diff를 확인한다 — 동작이 바뀐 부분이 없는지 점검

**의사결정 포인트 (ADR 필요)**:
- 방법 1: `HandlerMethodArgumentResolver`로 `@RequestHeader` 대신 커스텀 어노테이션 사용
- 방법 2: Servlet Filter로 인증 처리
- 방법 3: Spring Security 도입
- 방법 4: AOP(`@Aspect`)로 인증 처리

**고려사항**:
- Spring Security는 강력하지만 도입 범위가 크다 → 이 단계에서 적절한가?
- ArgumentResolver는 Spring MVC에 자연스럽고 변경 범위가 작다
- "구조 변경"이므로 **기존 HTTP 응답 코드를 완전히 보존한다** (헤더 누락 400, 토큰 무효 401)
- 추출 후 OrderController와 WishController에서 인증 관련 코드가 사라져야 한다

**커밋 단위**:
```
refactor(auth): 인증 로직을 ArgumentResolver로 추출
```

---

### Phase 1-B: 작동 변경 — 헤더 누락 시 응답 코드 변경

**문제**: Authorization 헤더가 누락되면 현재 400(Bad Request)을 반환하지만, 의미적으로 401(Unauthorized)이 더 정확하다.

**변경 전략**:
1. Phase 1에서 생성한 `LoginMemberArgumentResolver`의 헤더 누락 예외를 400 → 401로 변경한다
2. 기존 인수 테스트에서 이 케이스를 테스트하지 않으므로, 필요시 테스트를 추가한다

**고려사항**:
- 명확한 **작동 변경**이다 — HTTP 응답 코드가 바뀐다
- Phase 1(구조 변경)과 반드시 분리하여 커밋한다
- diff에 `BAD_REQUEST → UNAUTHORIZED` 변경 한 줄만 포함되어야 한다

**커밋 단위**:
```
feat(auth): Authorization 헤더 누락 시 응답 코드를 400에서 401로 변경
```

---

### Phase 2: 구조 변경 — 예외 핸들러 중앙화 (S2)

**문제**: `@ExceptionHandler`가 ProductController, OptionController, MemberController 3곳에 개별 정의되어 있다.

**변경 전략**:
1. `@RestControllerAdvice` 클래스를 생성한다
2. 3곳의 `@ExceptionHandler`를 이동한다
3. 기존 테스트가 모두 통과하는지 확인한다
4. diff를 확인한다 — 각 컨트롤러에서 핸들러만 제거되었는지 점검

**고려사항**:
- 이동만 하고, 새로운 예외 타입 추가나 응답 형식 변경은 하지 않는다
- 현재 `IllegalArgumentException → 400` 매핑을 그대로 유지한다
- Admin 컨트롤러(`@Controller`)와 REST 컨트롤러(`@RestController`)의 예외 처리 범위가 겹치지 않는지 확인한다

**커밋 단위**:
```
refactor(common): ExceptionHandler를 ControllerAdvice로 중앙화
```

---

### Phase 3: 작동 변경 — null 반환을 예외로 전환 (B1)

> ⚠️ 여기부터는 **작동 변경**이다. 외부 동작(HTTP 응답)이 바뀔 수 있다.

**문제**: Service에서 `orElse(null)` → Controller에서 null 체크 → 404 패턴이 반복되고, 예외 기반 처리와 혼재한다.

**변경 전략**:
1. Service에서 `orElse(null)` 대신 `orElseThrow()`를 사용하도록 변경한다
2. Controller의 null 체크 분기를 제거한다
3. Phase 2에서 만든 `@ControllerAdvice`에 새 예외 핸들러를 추가한다
4. **인수 테스트의 기대값을 수정한다** (필요한 경우)

**고려사항**:
- 이 변경은 HTTP 응답을 바꿀 수 있다 (예: 이전에 404였던 것이 500이 될 수 있음)
- 따라서 **인수 테스트를 먼저 수정하고** 코드를 변경해야 한다
- 커스텀 예외 클래스를 도입할지, 기존 예외(`NoSuchElementException`)를 활용할지 결정한다 (ADR 필요)
- 모든 Service를 한 번에 바꾸지 않고, 도메인별로 나눠서 변경한다

**커밋 단위** (도메인별 분리):
```
feat(category): CategoryService null 반환을 예외로 전환
feat(product): ProductService null 반환을 예외로 전환
feat(option): OptionService null 반환을 예외로 전환
```

---

### Phase 4: 작동 변경 — 트랜잭션 경계 추가 (B2)

**문제**: OrderService.create()에서 재고 차감 → 포인트 차감 → 주문 저장이 트랜잭션으로 묶이지 않는다.

**변경 전략**:
1. `@Transactional`을 OrderService.create()에 추가한다
2. 실패 시나리오 테스트를 추가한다 (포인트 부족 시 재고가 복구되는지)

**고려사항**:
- `@Transactional` 추가는 **작동 변경**이다 — 실패 시 롤백되는 동작이 새로 생긴다
- 현재 인수 테스트에서 `order.feature`의 "재고보다 많은 수량 → 500", "포인트 부족 → 500" 시나리오가 통과하는지 확인한다
- 트랜잭션 추가 후에도 같은 테스트가 통과해야 한다 (응답 코드는 동일)

**커밋 단위**:
```
feat(order): OrderService.create()에 트랜잭션 경계 추가
```

---

### Phase 5: 작동 변경 — OrderController 예외 처리 개선 (B3)

**문제**: OrderController에 `@ExceptionHandler`가 없어서 비즈니스 예외가 500으로 응답된다.

**변경 전략**:
1. Phase 2의 `@ControllerAdvice`에 Order 관련 예외 매핑을 추가한다
2. 인수 테스트의 기대값을 변경한다 (500 → 400)

**고려사항**:
- 이것은 명확한 **작동 변경**이다 — API 응답 코드가 바뀐다
- 인수 테스트를 먼저 수정한다 (Red → Green 사이클)
- 프론트엔드나 다른 클라이언트에 영향이 있을 수 있음을 인지한다

**커밋 단위**:
```
feat(order): 비즈니스 예외를 400으로 응답하도록 변경
test(order): 주문 실패 응답 코드 기대값 수정 (500 → 400)
```

---

### Phase 6: 구조 변경 — 메시지 클라이언트 인터페이스 추출 (S5)

**문제**: `OrderService`가 `KakaoMessageClient`(구체 클래스)에 직접 의존하여, 알림 채널 변경이나 테스트 시 대체가 어렵다.

**변경 전략**:
1. 알림 발송 역할을 정의하는 인터페이스를 추출한다 (ADR 작성)
2. `KakaoMessageClient`가 해당 인터페이스를 구현하도록 변경한다
3. `OrderService`가 인터페이스에만 의존하도록 변경한다
4. 기존 테스트가 모두 통과하는지 확인한다

**의사결정 포인트 (ADR 필요)**:
- 인터페이스 이름과 위치: `MessageClient` / `NotificationClient` / `OrderMessageSender`
- 인터페이스를 어느 패키지에 둘 것인가: `order` 패키지(사용처 기준) vs 별도 `notification` 패키지
- 메서드 시그니처를 카카오에 독립적으로 설계할 수 있는가 (`sendToMe(accessToken, order, product)` → 더 추상적인 형태?)

**고려사항**:
- 이것은 **순수 구조 변경**이다 — 인터페이스를 끼워넣을 뿐 동작은 바뀌지 않는다
- 런타임에는 기존과 동일하게 `KakaoMessageClient`가 주입된다 (Spring DI가 자동 해결)
- 인터페이스 메서드 시그니처를 어느 수준까지 추상화할지가 핵심이다
  - **최소 변경**: 현재 `sendToMe(String accessToken, Order order, Product product)` 시그니처를 그대로 인터페이스로 올린다
  - **추상화**: `send(NotificationMessage message)` 같은 범용 시그니처로 변경한다 → 이 경우 **작동 변경이 섞일 위험**이 있다
- 원칙 2를 지키려면 **최소 변경(시그니처 유지) 후 인터페이스 추출만** 이 단계에서 수행한다. 시그니처 추상화는 별도 Phase로 분리한다.

**커밋 단위**:
```
refactor(order): 메시지 발송을 인터페이스로 추상화
```

---

### Phase 7: 구조 변경 — Admin 메서드 분리 (S3, S4)

**문제**: ProductService에 Admin 전용 메서드가 혼재하고, Validator 호출 위치가 분산되어 있다.

**고려사항**:
- Phase 1~6을 완료한 후에 진행한다
- 이전 변경으로 예외 처리 패턴이 통일되어 있으므로 더 수월하다
- Admin 전용 Service를 분리할지, 기존 Service를 정리할지 결정한다 (ADR 필요)

---

## 5. ADR (Architecture Decision Record) 작성 가이드

> "왜 그렇게 했는가"를 기록하는 문서

### ADR 템플릿

```markdown
# ADR-{번호}: {제목}

## 상태
제안됨 / 승인됨 / 폐기됨

## 맥락
어떤 문제 상황인가? 왜 결정이 필요한가?

## 선택지
1. 방법 A — 장점 / 단점
2. 방법 B — 장점 / 단점

## 결정
어떤 방법을 선택했는가?

## 근거
왜 그 방법을 선택했는가?

## 결과
이 결정으로 인해 앞으로 어떤 영향이 생기는가?
```

### 이 미션에서 ADR이 필요한 결정

| ADR | 의사결정 | 선택지 |
|-----|---------|--------|
| ADR-1 | 인증 로직 추출 방식 | ArgumentResolver / Filter / Spring Security / AOP |
| ADR-2 | 예외 처리 전략 | 커스텀 예외 계층 / 표준 예외 활용 / HTTP 상태별 예외 |
| ADR-3 | Admin 서비스 분리 방식 | 별도 Service 클래스 / 기존 Service에 통합 유지 |
| ADR-4 | 메시지 클라이언트 인터페이스 설계 | 최소 변경(시그니처 유지) / 범용 추상화 / 패키지 위치 |

---

## 6. 각 단계의 체크리스트 (원칙 5: diff 검토는 필수다)

매 커밋 전에 아래를 확인한다:

```
□ 테스트가 통과하는가?
□ 이 커밋은 구조 변경만 포함하는가, 작동 변경만 포함하는가?
□ diff에 의도하지 않은 변경이 없는가?
□ 커밋 메시지 한 줄로 변경 의도를 설명할 수 있는가?
□ 변경 범위가 예상한 경계 안에 있는가?
```

---

## 7. 작업 흐름 요약

```
Phase 0: 테스트 통과 확인 (안전망)
    │
    ▼
Phase 1:   [구조] 인증 로직 추출         ← ADR-1 작성
    │       테스트 통과 확인 → diff 확인 → 커밋
    ▼
Phase 1-B: [작동] 헤더 누락 응답 코드 400 → 401
    │       diff 확인 → 커밋
    ▼
Phase 2:   [구조] ExceptionHandler 중앙화
    │     테스트 통과 확인 → diff 확인 → 커밋
    ▼
Phase 3: [작동] null → 예외 전환       ← ADR-2 작성
    │     테스트 수정 → 코드 변경 → 테스트 통과 확인 → diff 확인 → 커밋
    ▼
Phase 4: [작동] 트랜잭션 추가
    │     테스트 통과 확인 → diff 확인 → 커밋
    ▼
Phase 5: [작동] Order 예외 처리 개선
    │     테스트 수정 → 코드 변경 → 테스트 통과 확인 → diff 확인 → 커밋
    ▼
Phase 6: [구조] 메시지 클라이언트 인터페이스 추출  ← ADR-4 작성
    │     테스트 통과 확인 → diff 확인 → 커밋
    ▼
Phase 7: [구조] Admin 메서드 분리       ← ADR-3 작성
          테스트 통과 확인 → diff 확인 → 커밋
```

---

## 8. 피해야 할 패턴 (피드백 기반)

| 패턴 | 구체적 예시 | 대안 |
|------|-----------|------|
| 구조+작동 혼합 | 서비스 추출하면서 예외 처리 방식도 변경 | Phase를 나눠서 별도 커밋 |
| 한 번에 대수술 | 모든 Service의 null → 예외를 한 커밋에 | 도메인별로 나눠서 커밋 |
| AI 결과 무검증 수용 | 생성된 코드를 diff 없이 커밋 | 매 변경마다 diff 확인 |
| 테스트 없이 변경 | "나중에 테스트 추가하겠다" | 테스트 먼저 수정, 코드 나중 |
| 변경 이유 미기록 | "왜 ArgumentResolver를 선택했는가?" 설명 불가 | ADR 작성 |

---

## 9. 학습 포인트

이 미션을 통해 훈련하는 것:

1. **문제를 먼저 정의하는 습관** — "리팩터링해 줘"가 아니라 "이 코드에서 관찰되는 문제는 X이고, 위험은 Y이다"
2. **변경의 종류를 구분하는 능력** — 이 diff가 구조 변경인지 작동 변경인지 판단
3. **작은 단위로 검증하는 규율** — 변경 → 테스트 → diff → 커밋 사이클
4. **의사결정을 기록하는 습관** — ADR로 "왜"를 남긴다
5. **AI를 도구로 쓰는 방법** — 작은 단위로 요청하고, 결과를 검증하고, diff를 설명할 수 있어야 한다
