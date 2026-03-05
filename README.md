# spring-gift-refactoring

## [Step1] 구현할 기능 목록

이번 단계의 목표: 작동을 바꾸기 쉬운 상태를 만든다.
구조 변경을 통해 변경 난이도를 낮추되 작동은 유지한다.

### 1단계: 인수 테스트 작성

> 목적: 리팩토링 안전망을 구축한다. 기존 작동을 그대로 검증하는 테스트만 작성한다.
> 이 단계에서는 프로덕션 코드를 수정하지 않는다.

#### 1-1. 테스트 범위 결정
- [x] 각 Controller가 처리하는 API 엔드포인트 목록 분석
- [x] API 엔드포인트별 사용자가 관찰 가능한 시나리오 분석
  - 엔드포인트별 정상/실패 시나리오 도출
  - 실제 동작 기준으로 기대값 결정 (주석이 아닌 코드 실행 결과 기준)
- [x] 기존 테스트 커버리지 확인: 어떤 엔드포인트가 테스트되고, 어떤 것이 빠져있는가
- [x] 인수 테스트에 반영할 시나리오 선정

#### 1-2. 인수 테스트 작성

> 한 번에 모든 시나리오를 작성하면 안되며, 각 도메인별로 시나리오를 작성하고 테스트가 통과된 후 사용자의 검토를 받아야 한다.

- [x] 각 시나리오별 인수 테스트 작성
    - [x] 전체 테스트 실행 → 통과 확인
- [x] (시나리오별 반복)

---

### 2단계: 스타일 정리

> 목적: 프로젝트 전반의 스타일 불일치를 일관되게 정리한다.
> 이 단계에서는 로직을 변경하지 않는다. 포매팅만 변경한다.

- [x] 스타일 포매터 선정
    - 대안 비교: IDE 기본 포매터 vs Checkstyle vs Spotless 등
- [x] 포매터 적용 (네이밍, import, 공백, 줄바꿈, 중괄호 등)
- [x] 전체 테스트 실행 → 통과 확인

---

### 3단계: 불필요한 코드 제거

> 목적: IDE 또는 정적 분석 도구가 "미사용"으로 표시하는 항목을 근거를 확인한 후 제거한다.
> 이 단계에서는 로직을 변경하지 않는다. 사용되지 않는 코드만 제거한다.

#### 3-1. 분석
- [x] IDE/정적 분석 도구 실행하여 미사용 항목 목록 작성
- [x] 주석-동작 불일치 지점 식별 (미사용 코드 주변의 주석이 현재 코드와 맞는지 확인)
    - 각 항목별: 주변 주석/TODO 확인, git blame 확인, 이후 단계 충돌 확인

#### 3-2. 제거

> 한 번에 모든 내용을 제거해서는 안되며, 각 도메인별로 작업한 후 사용자의 검토를 받아야 한다.

- [x] 보고서에서 "삭제" 판정을 받은 항목만 제거
- [x] 전체 테스트 실행 → 통과 확인

---

### 4단계: 서비스 계층 추출

> 목적: Controller의 비즈니스 로직을 Service로 이동한다.
> Controller는 요청 검증과 위임만 담당하도록 얇게 만든다.
> 이 단계에서는 신규 기능을 추가하지 않는다.

#### 4-1. 추출 계획
- [x] Controller에 포함된 비즈니스 로직 식별
    - Controller별 엔드포인트, 현재 로직, 이동 대상, Controller에 남길 것
    - 대안 비교: 메서드 단위 추출 vs 도메인 단위 추출 등

#### 4-2. Controller별 추출

> 한 번에 모든 도메인에 대해 작업해서는 안되며, 각 도메인별로 작업한 후 사용자의 검토를 받아야 한다.

- [x] CategoryController → CategoryService 추출
- [x] ProductController, AdminProductController → ProductService 추출
- [x] MemberController, AdminMemberController, KakaoAuthController → MemberService 추출
- [x] OptionController → OptionService 추출
- [x] WishController → WishService 추출
- [x] OrderController → OrderService 추출
- [x] 전체 테스트 실행 → 통과 확인

---

### 5단계: 구조 변경 — 의존성 정리 및 도메인 책임 이동

> 목적: 입력과 출력을 유지하면서 의존성과 책임 구조를 정리한다.
> 변경이 가능한 상태를 만들고, 변경 시 위험을 통제할 수 있는 구조를 확보한다.
> 이 단계에서는 작동을 변경하지 않는다. 구조만 변경한다.

#### 5-1. 예외 처리 중앙화 — @ControllerAdvice 도입

> 근거: 동일한 @ExceptionHandler가 5개 컨트롤러에 10회 중복되어 있다.
> 예외 응답 형식 변경 시 5곳을 수정해야 하며, 누락 시 컨트롤러마다 다른 응답을 반환하게 된다.

- [x] @ControllerAdvice 클래스 생성 (NoSuchElementException→404, IllegalArgumentException→400, IllegalStateException→403)
- [x] 5개 컨트롤러에서 @ExceptionHandler 메서드 10개 제거
- [x] 인수 테스트 단언문 정밀화 — 404 매핑 검증이 `≥400`으로 느슨했던 2개 시나리오를 `=404`로 정밀화
- [x] 전체 테스트 실행 → 통과 확인

#### 5-2. 인증 처리 추출 — HandlerMethodArgumentResolver 도입

> 근거: extractMember() + null 체크 + 401 반환 패턴이 OrderController 2회, WishController 3회, 총 5회 반복된다.
> 인증 방식 변경 시 5곳을 수정해야 하며, 새 엔드포인트 추가 시 인증 누락 위험이 있다.
> 전제 조건: 5-1 완료 (@ControllerAdvice에서 인증 실패 예외를 401로 매핑)

- [x] AuthenticationResolver를 HandlerMethodArgumentResolver로 전환
- [x] 인증 실패 전용 예외 생성 → @ControllerAdvice에 401 매핑 추가
- [x] WebMvcConfigurer에 리졸버 등록
- [x] OrderController, WishController에서 인증 인라인 코드 제거 → Member 파라미터 직접 수신
- [x] 전체 테스트 실행 → 통과 확인

#### 5-3. 크로스 패키지 Repository 참조 제거 — 서비스 위임

> 근거: OrderService가 OptionRepository·MemberRepository를, WishService가 ProductRepository를, OptionService가 ProductRepository를 직접 참조한다.
> 각 도메인에 Service가 이미 존재하는데도 Repository를 직접 호출하여 findById+orElseThrow 조회 패턴이 중복되고, Repository 변경 시 영향이 패키지를 넘는다.

- [x] OptionService에 findById(Long) 메서드 추가
- [x] OrderService → OptionService, MemberService로 위임 전환
- [x] WishService → ProductService로 위임 전환
- [x] OptionService → ProductService로 위임 전환
- [x] 전체 테스트 실행 → 통과 확인

#### 5-4. 외부 인프라 인터페이스 추출 — OrderMessageClient

> 근거: OrderService가 KakaoMessageClient 구체 클래스에 직접 의존한다.
> 도메인 서비스가 특정 벤더(Kakao)에 결합되어 있으며, 메시지 채널 변경 시 OrderService를 수정해야 한다.

- [x] OrderMessageClient 인터페이스 생성 (order 패키지)
- [x] KakaoMessageClient가 OrderMessageClient를 구현하도록 변경
- [x] OrderService의 의존을 KakaoMessageClient → OrderMessageClient로 변경
- [x] 전체 테스트 실행 → 통과 확인

#### 5-5. auth ↔ member 순환 참조 해소 — 의존성 역전

> 근거: auth → member와 member → auth가 순환한다. 두 패키지를 독립적으로 변경할 수 없다.
> auth 패키지에 인터페이스를 두면 member → auth 의존이 남아 순환이 해소되지 않는다.
> member 패키지에 TokenProvider 인터페이스를 배치하여 의존 방향을 역전시킨다 (DIP).

- [x] member 패키지에 TokenProvider 인터페이스 생성 (createToken 메서드)
- [x] JwtProvider가 member.TokenProvider를 구현하도록 변경
- [x] MemberService의 의존을 auth.JwtProvider → member.TokenProvider로 변경
- [x] TokenResponse를 auth → member 패키지로 이동 (MemberController → TokenResponse 순환도 해소)
- [x] 전체 테스트 실행 → 통과 확인

#### 5-6. 도메인 책임 이동 — Member.matchesPassword()

> 근거: 비밀번호 검증이 MemberService에서 getPassword()로 내부 상태를 꺼내 직접 비교한다.
> chargePoint(), deductPoint()와 달리 비밀번호 검증만 도메인 외부에 있어, 해싱 도입 등 검증 정책 변경 시 서비스를 수정해야 한다.

- [x] Member에 matchesPassword(String) 메서드 추가
- [x] MemberService.login()에서 직접 비교를 matchesPassword() 호출로 대체
- [x] "틀린 비밀번호로 로그인하면 실패한다" 인수 테스트 시나리오 추가
- [x] 전체 테스트 실행 → 통과 확인

#### 5-7. MemberService.register()에서 create() 재사용

> 근거: register()와 create()가 동일한 이메일 중복 검증 + 저장 로직을 중복한다.
> register()가 create()를 호출하고 토큰 생성만 추가하면 변경 지점이 1곳으로 수렴한다.
> 입력(email, password)과 출력(토큰)이 동일하게 유지되며, 내부 호출 구조만 바뀐다.

- [x] register()가 create()를 내부 호출하도록 변경
- [x] 전체 테스트 실행 → 통과 확인

---

### 6단계: 작동 변경 — 정책 수정, 검증 추가, 예외 처리 개선

> 목적: 입출력이 달라지는 변경을 수행한다.
> 정책 변경, 트랜잭션 경계 수정, 예외 처리 방식 변경, 도메인 검증 추가, DB 제약 추가 등.
> 각 변경에 대해 기존 인수 테스트를 수정하거나 새 시나리오를 추가한다.

#### 6-1. Authorization 헤더 누락 시 401 반환

> 근거: Authorization 헤더 누락 시 MissingRequestHeaderException → 400을 반환한다.
> 의미적으로 인증 자격 증명 누락은 401이 맞다 (RFC 7235). TODO 주석으로 인지된 상태.

- [x] AuthenticationResolver에서 MissingRequestHeaderException 대신 AuthenticationException을 던져 401 반환
- [x] 인수 테스트 수정 (인증 헤더 누락 시 400 → 401)
- [x] 전체 테스트 실행 → 통과 확인

#### 6-2. 트랜잭션에서 외부 API 호출 분리

> 근거: OrderService.createOrder()의 @Transactional 내에서 Kakao API를 호출한다.
> 외부 API 타임아웃 동안 DB 커넥션이 점유되며, 메시지 전송은 주문의 부수 효과이므로 트랜잭션 커밋 후로 분리한다.

- [x] OrderCreatedEvent 도메인 이벤트 클래스 생성
- [x] 메시지 전송을 @TransactionalEventListener(phase = AFTER_COMMIT)로 이동
- [x] OrderService.createOrder()에서 sendMessageIfPossible() 제거, 이벤트 발행으로 대체
- [x] 전체 테스트 실행 → 통과 확인

#### 6-3. Option.subtractQuantity 음수/영 검증 추가

> 근거: amount <= 0 검증이 없어 음수 전달 시 재고가 증가한다.
> Member.deductPoint()에는 동일한 방어가 있으나 Option에는 누락.

- [x] Option.subtractQuantity()에 amount <= 0 검증 추가
- [x] 전체 테스트 실행 → 통과 확인

#### 6-4. 삭제 시 FK 위반 처리 — Restrict + 예외 포착

> 근거: 하위 엔티티가 있는 상위 엔티티 삭제 시 DataIntegrityViolationException → 500 에러.
> DB의 FK 기본 정책(Restrict)에 맞춰 삭제 시도 후 FK 위반을 잡아 의미 있는 응답을 반환한다.
> 초기에 사전 검증(existsBy*) 방식을 적용했으나, 크로스 패키지 Repository 참조(5-3 원칙 위반)와 불필요한 추가 조회 문제로 예외 포착 방식으로 전환.

- [x] 각 Service의 delete()를 try { deleteById(); flush(); } catch (DataIntegrityViolationException) 패턴으로 변경
- [x] existsBy* 메서드 6개 제거, 크로스 패키지 Repository 주입 4건 제거
- [x] GlobalExceptionHandler에 DataIntegrityViolationException → 409 안전망 유지
- [x] 삭제 실패 시나리오 인수 테스트 추가
- [x] 전체 테스트 실행 → 통과 확인

#### 6-5. MethodArgumentNotValidException 핸들러 추가

> 근거: 9개 엔드포인트에서 @Valid를 사용하나 GlobalExceptionHandler에 핸들러가 없다.
> Bean Validation 실패 시 Spring 기본 응답과 IllegalArgumentException 응답의 형식이 다르다.

- [x] GlobalExceptionHandler에 MethodArgumentNotValidException → 400 핸들러 추가
- [x] 전체 테스트 실행 → 통과 확인

#### 6-6. wish 테이블 UNIQUE 제약 추가

> 근거: (member_id, product_id) UNIQUE 제약이 없어 동시 요청 시 중복 위시가 생성될 수 있다.
> 애플리케이션 레벨 중복 검사만으로는 race condition에 취약하다.

- [x] Flyway 마이그레이션으로 UNIQUE(member_id, product_id) 제약 추가
- [x] WishService에서 DB 제약 위반 예외 처리 보완
- [x] 전체 테스트 실행 → 통과 확인

#### 6-7. 에러 메시지 언어 통일

> 근거: Member.chargePoint()는 영어, deductPoint()는 한국어 등 에러 메시지 언어가 혼재한다.
> IllegalArgumentException 메시지가 API 응답에 노출되므로 일관성이 필요하다.

- [x] 프로젝트 전체 에러 메시지를 단일 언어로 통일
- [x] 전체 테스트 실행 → 통과 확인

#### 6-8. 에러 응답 본문 통일 — JSON `{"message": "..."}`

> 근거: 401/403/404는 body 없음, 400/409는 plain text String body로 에러 응답 형식이 불일관하다.
> 모든 핸들러가 동일한 JSON 구조를 반환해야 클라이언트가 성공/실패 응답을 동일한 방식으로 파싱할 수 있다.
> API 계약을 먼저 확립하여 이후 작업에서 발생하는 에러도 통일된 형식으로 반환한다.

- [x] ErrorResponse record 생성 (`public record ErrorResponse(String message) {}`)
- [x] GlobalExceptionHandler 6개 핸들러 반환 타입을 `ResponseEntity<ErrorResponse>`로 통일
- [x] 전체 테스트 실행 → 통과 확인

#### 6-9. 주문 시 위시 삭제

> 근거: 주문 생성 시 해당 상품에 대한 위시를 자동 삭제하지 않는다.
> WishRepository.findByMemberIdAndProductId()가 존재하지만 OrderService에서 호출하지 않는다.
> 작업 10 전에 OrderService를 최종 형태로 완성하여 잠금 범위 재조정을 방지한다.

- [x] WishRepository에 `deleteByMemberIdAndProductId()` 추가
- [x] WishService에 `removeWishByMemberIdAndProductId()` 메서드 추가
- [x] OrderService에서 주문 저장 후 위시 삭제 호출
- [x] 인수 테스트 시나리오 추가 (주문 시 위시리스트에서 해당 상품 삭제 검증)
- [x] 전체 테스트 실행 → 통과 확인

#### 6-10. 동시성 제어 — 비관적 잠금 (SELECT FOR UPDATE)

> 근거: Option.subtractQuantity()와 Member.deductPoint()에 read-then-write race condition이 존재한다.
> 동시 주문 시 재고/포인트가 음수가 될 수 있다. 비관적 잠금으로 도메인 모델/에러 메시지를 보존하면서 동시성을 제어한다.
> OrderService가 최종 형태일 때 적용하여 잠금 범위를 한 번에 확정한다.

- [ ] OptionRepository, MemberRepository에 `findByIdForUpdate()` + `@Lock(PESSIMISTIC_WRITE)` 추가
- [ ] OptionService, MemberService에 `findByIdForUpdate()` wrapper 추가
- [ ] OrderService에서 `findById()` → `findByIdForUpdate()` 변경 (2곳)
- [ ] V4 마이그레이션으로 CHECK 제약 추가 (quantity >= 0, point >= 0)
- [ ] 전체 테스트 실행 → 통과 확인

