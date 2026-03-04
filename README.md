# spring-gift-refactoring

## Step2 리팩토링 요약

### 리팩토링 계획

미션 요구사항을 3가지로 분류하고, 구조 변경과 작동 변경을 분리하여 총 7개 커밋으로 나누어 진행했다.

| 분류      | 해결 대상                                         | 커밋             |
|---------|-----------------------------------------------|----------------|
| 트랜잭션 경계 | OrderService.createOrder()의 3개 save가 원자적이지 않음 | #1(구조), #2(작동) |
| 누락된 작동  | 카카오 메시지 실패 무시, 검증 누락                          | #3, #5, #7     |
| 도메인 책임  | 검증/판단 로직이 Controller에 위치                      | #4(구조), #6(구조) |

### 커밋 내역

#### Commit 1 [구조] OrderService cross-domain Repository 제거

- OrderService가 OptionRepository, MemberRepository를 직접 사용하던 것을 OptionService, MemberService로 교체
- 다른 도메인의 영속성 로직은 해당 도메인의 Service를 통해 접근하는 원칙 적용

#### Commit 2 [작동] @Transactional 적용 + OrderServiceTest + ADR

- createOrder()에 @Transactional 추가하여 재고 차감/포인트 차감/주문 저장을 원자적으로 수행
- 단위 테스트: 성공 시 전체 수행, 포인트 부족 시 주문 미생성, 카카오 실패해도 주문 성공
- ADR: 트랜잭션 내부 카카오 API 호출의 트레이드오프 문서화

#### Commit 3 [작동] 카카오 메시지 전송 실패 로깅

- `catch (Exception ignored) {}` → `log.warn()`으로 변경
- 실패 시 orderId, memberId, 예외 내용이 로그에 기록됨

#### Commit 4 [구조] 상품 이름 검증을 Controller에서 Service로 이동

- AdminProductController에서 직접 호출하던 ProductNameValidator를 ProductService 내부로 이동
- saveProduct(), updateProduct()가 자체적으로 검증 수행
- Controller는 예외 catch로 에러 표시만 담당

#### Commit 5 [작동] ProductServiceTest 추가

- 15자 초과 이름 거부, 관리자 "카카오" 허용, API "카카오" 거부, 특수문자 거부 테스트
- save 호출 여부로 상태 검증

#### Commit 6 [구조] Wish 중복 체크를 Controller에서 Service로 이동

- WishController의 중복 확인 분기 → WishService.addWish()로 통합
- Controller 9줄 분기 → 1줄 호출로 단순화

#### Commit 7 [작동] WishServiceTest 추가

- 위시 없으면 새로 생성, 이미 있으면 기존 반환(save 미호출), 타인 위시 삭제 거부 테스트

#### Commit 8 [작동] OptionService, MemberService @Transactional 적용

- read-then-write 패턴의 메서드에 @Transactional 누락 → 추가
  - OptionService: subtractQuantity(), delete()
  - MemberService: deductPoint(), chargePoint(), registerOrUpdateKakaoMember()
- Cucumber 시나리오에 실패 후 재고 재조회 검증 추가 (롤백 증거)

#### Commit 9 [구조] OptionService, WishService, AuthenticationResolver cross-domain Repository 제거

- Commit 1에서 OrderService에 적용한 원칙을 나머지에도 일관 적용
  - OptionService: ProductRepository → ProductService
  - WishService: ProductRepository → ProductService
  - AuthenticationResolver: MemberRepository → MemberService
- ProductService에 getById(), MemberService에 findByEmail() 추가

#### Commit 10 [구조] 가격 계산을 Order 도메인으로 이동

- OrderService, KakaoMessageClient에 중복된 `price * quantity` 계산 → Order.getTotalPrice()로 통합
- KakaoMessageClient.sendToMe()에서 Product 파라미터 제거 (Order를 통해 접근)
- 호출부 단순화: OrderService.sendKakaoMessageIfPossible() 파라미터 3개 → 2개

#### Commit 11 [구조] IllegalArgumentException 핸들러를 GlobalExceptionHandler로 통합

- OptionController, ProductController, MemberController에 중복된 @ExceptionHandler 3건 제거
- GlobalExceptionHandler에 IllegalArgumentException → 400 Bad Request 핸들러 1건 추가

---

## AI 활용 과정

### 사용 도구

- Claude Code (CLI)

### 활용 흐름

#### 1단계: 프로젝트 현황 파악

리팩토링 전에 AI로 전체 프로젝트를 탐색시켜 구조, 도메인 관계, 개선 포인트를 빠르게 파악했다.
이 단계에서 cross-domain Repository 접근, 트랜잭션 미설정, 검증 위치 불일치 등을 식별할 수 있었다.

#### 2단계: 개선 포인트별 접근 방식 검토

식별된 문제마다 AI에게 "왜 문제인지", "어떤 방식으로 고칠 수 있는지" 선택지를 물어보고, 트레이드오프를 비교한 뒤 방향을 정했다.
예를 들어 OrderService의 cross-domain Repository 문제는 기존에 KakaoAuthService에서 적용한 패턴과 동일하게 Service 위임 방식을 선택했고,
@Transactional 적용 시 카카오 API 호출이 트랜잭션 안에 포함되는 문제는 ADR로 트레이드오프를 기록했다.

#### 3단계: 계획 수립 후 단계별 실행

미션 요구사항(구조/작동 분리, 테스트 증거)을 AI에게 제약 조건으로 주고 커밋 계획을 수립했다.
각 커밋마다 AI가 코드 수정 → 컴파일 확인 → 테스트 통과까지 검증한 뒤, 커밋 메시지를 제안하면 직접 커밋하는 방식으로 변경을 통제했다.

#### 4단계: 변경 의도 확인

AI가 작성한 코드에 대해 "이게 왜 필요한지", "이 테스트가 실질적으로 무엇을 보장하는지" 질문하며 이해도를 확보했다.
예를 들어 단위 테스트에서 mock 반환값을 넣는 것이 실제로 의미 있는 검증인지, 로깅 추가가 왜 작동 변경인지 등을 확인했다.

### AI 활용 원칙

1. **계획 먼저** — 바로 코드를 쓰지 않고 AI에게 계획을 세우게 한 뒤 검토
2. **커밋은 직접** — AI는 수정과 검증까지, 커밋 판단은 사람이
3. **매 단계 검증** — 커밋마다 컴파일 + 테스트로 기존 동작 보장
4. **왜를 질문** — AI의 변경을 무조건 수용하지 않고, 의도를 확인하며 학습
