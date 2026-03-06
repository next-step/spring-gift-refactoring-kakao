# 개발 계획

> REQUIREMENT.md 기반 작업 순서. 모든 단계는 **구조 변경만** 수행하며 작동을 변경하지 않는다.
> 각 단계 완료 후 전체 테스트를 통과해야 다음 단계로 넘어간다.

## 0단계: 준비

1. README.md에 구현할 기능 목록과 구현 전략 정리
2. 전체 테스트 실행하여 현재 상태가 Green인지 확인
3. AI 도구 활용 계획이 있다면 PROMPT.md에 프롬프트 내용 방식 기록 준비

- [ ] README.md 기능 목록 작성
- [ ] README.md 구현 전략 정리
- [ ] 전체 테스트 Green 확인

> **규칙**: README.md 체크리스트에 다음 작업이 적혀 있어야 코드 수정을 시작한다.

---

## 1단계: 스타일 정리 (구조 변경, 작동 변경 없음)

프로젝트 전반의 스타일 불일치를 찾아 일관되게 정리한다.

### 작업 절차

1. 스타일 불일치 항목을 먼저 **목록으로 정리**한다 (바로 고치지 않는다)
2. 항목별로 **한 번에 한 조각씩** 수정한다
3. 수정할 때마다 **작동이 바뀌지 않았는지** 확인한다 (전체 테스트 실행)
4. 커밋은 **목적 1개** 단위로 한다 (git diff를 보고 30초 안에 설명 가능해야 함)

### 체크리스트

- [ ] 코드 포맷팅 통일 (들여쓰기, 빈 줄, 중괄호 스타일 등)
- [ ] import 정리 (미사용 import 제거, 순서 통일)
- [ ] 네이밍 컨벤션 점검 (메서드명, 변수명 일관성)
- [ ] 전체 테스트 통과 확인

---

## 2단계: 불필요한 코드 제거 (구조 변경, 작동 변경 없음)

### 작업 절차

1. IDE 또는 정적 분석 도구로 **"미사용" 항목을 식별**한다
2. 삭제 후보마다 아래 **3가지 근거를 반드시 확인**한 뒤 삭제 여부를 결정한다:
   - 주변 주석 또는 TODO에 **의도가 있는가?**
   - `git blame`으로 **누가, 왜 추가했는가?**
   - 이후 단계(서비스 계층 추출)와 **충돌하지 않는가?**
3. 근거가 확인된 항목만 **한 번에 하나씩** 제거한다
4. 제거할 때마다 전체 테스트를 실행하여 **작동 변경이 없는지** 확인한다

### 체크리스트

- [ ] 미사용 필드, 메서드, 클래스 식별
- [ ] 각 항목별 삭제 근거 확인 (주석/TODO, git blame, 이후 단계 충돌)
- [ ] 근거 확인된 항목 제거
- [ ] 미사용 의존성 확인 (build.gradle.kts)
- [ ] 불필요한 주석 정리
- [ ] 전체 테스트 통과 확인

---

## 3단계: 서비스 계층 추출 (구조 변경, 작동 변경 없음)

### 작업 절차 (도메인마다 동일하게 반복)

1. Controller의 비즈니스 로직을 **식별**한다 (요청 검증/위임이 아닌 것)
2. Service 클래스를 **생성**한다
3. 비즈니스 로직을 Service로 **이동**한다 (이 단계에서 신규 기능을 추가하지 않는다)
4. Controller는 **요청 검증과 위임만** 담당하도록 변경한다
5. 전체 테스트를 실행하여 **기존 작동이 유지되는지** 확인한다
6. git diff를 보고 **30초 안에 커밋 의도를 설명할 수 있는지** 검증한다
7. 설명 불가능하면 커밋을 **더 쪼갠다**

> **규칙**: 한 번에 하나의 도메인만 작업한다. 의존 관계가 적은 것부터 시작한다.

### 3-1. CategoryService 추출

- [ ] CategoryController 비즈니스 로직 식별
- [ ] CategoryService 생성 및 로직 이동
- [ ] CategoryController → 요청 검증 + 위임만 남기기
- [ ] 전체 테스트 통과 확인

### 3-2. ProductService 추출

- [ ] ProductController 비즈니스 로직 식별
- [ ] ProductService 생성 및 로직 이동
- [ ] ProductController → 요청 검증 + 위임만 남기기
- [ ] AdminProductController → ProductService 위임으로 변경
- [ ] 전체 테스트 통과 확인

### 3-3. OptionService 추출

- [ ] OptionController 비즈니스 로직 식별
- [ ] OptionService 생성 및 로직 이동
- [ ] OptionController → 요청 검증 + 위임만 남기기
- [ ] 전체 테스트 통과 확인

### 3-4. MemberService 추출

- [ ] MemberController 비즈니스 로직 식별
- [ ] MemberService 생성 및 로직 이동
- [ ] MemberController → 요청 검증 + 위임만 남기기
- [ ] 전체 테스트 통과 확인

### 3-5. WishService 추출

- [ ] WishController 비즈니스 로직 식별
- [ ] WishService 생성 및 로직 이동
- [ ] WishController → 요청 검증 + 위임만 남기기
- [ ] 전체 테스트 통과 확인

### 3-6. OrderService 추출

- [ ] OrderController 비즈니스 로직 식별 (주문 플로우: 인증 → 검증 → 재고 차감 → 포인트 차감 → 저장 → 위시 정리 → 카카오 메시지)
- [ ] OrderService 생성 및 로직 이동
- [ ] OrderController → 요청 검증 + 위임만 남기기
- [ ] 전체 테스트 통과 확인

---

## 모든 단계 공통 규칙 (프로그래밍 요구 사항)

| 규칙 | 설명 |
|------|------|
| **README.md 먼저** | 코드 수정 전 README.md 체크리스트에 다음 작업이 적혀 있어야 한다 |
| **한 번에 한 조각** | "다음 변경 1개"처럼 범위를 제한한다 |
| **구조/작동 분리** | 한 커밋에 구조 변경과 작동 변경을 섞지 않는다 |
| **TDD 루프** | 변경 후 전체 테스트 통과가 최소 요구 사항이다 |
| **커밋 = 목적 1개** | git diff를 보고 30초 안에 설명할 수 없으면 더 쪼갠다 |
| **AI 산출물 검증** | AI는 초안을 만들 뿐, 의도하지 않은 변경이 없는지 반드시 확인한다 |
| **AI 활용 기록** | AI 도구를 활용했다면 README.md에 활용 방식, 수정 내용, 학습 내용을 기록한다 |

---

## 🚀 2단계: 리팩터링 완성하기 (작동 변경 포함)

> 작동 변경을 안전하게 수행하고, 그 결과를 증거로 보여준다.
> 구조 변경 커밋과 작동 변경 커밋을 분리한다.

### 4단계: 트랜잭션 경계 세우기

**대상 식별 결과:**

| Service 메서드 | 복수 쓰기 작업 | 우선순위 |
|---|---|---|
| `OrderService.createOrder()` | 재고 차감 → 포인트 차감 → 주문 저장 (3개 쓰기) | HIGH |
| `KakaoAuthService.processCallback()` | 회원 조회/생성 → 토큰 갱신 (외부 API + DB) | MEDIUM |

#### 4-1. OrderService.createOrder() 트랜잭션 적용

- **현재 문제**: 재고 차감 후 포인트 차감 실패 시 재고만 줄어든 채로 남음
- **변경**: `@Transactional` 추가, 카카오 알림은 트랜잭션 밖에서 처리
- [ ] `OrderService.createOrder()`에 `@Transactional` 적용
- [ ] 중간 실패 시 롤백 검증 테스트 작성 (포인트 부족 시 재고 원복 확인)
- [ ] 전체 테스트 통과 확인

#### 4-2. KakaoAuthService.processCallback() 트랜잭션 적용

- **현재 문제**: 회원 저장 실패 시 외부 API 호출은 이미 완료된 상태
- **변경**: DB 쓰기 부분에 `@Transactional` 적용
- [ ] `KakaoAuthService.processCallback()`에 `@Transactional` 적용
- [ ] 전체 테스트 통과 확인

---

### 5단계: 누락된 작동 구현

**대상 식별 결과:**

#### 5-1. 상품 옵션 수정 API 미구현

- **API 명세**: `PUT /api/products/{productId}/options/{optionId}` — "상품 옵션 수정"
- **현재 상태**: OptionController에 PUT 엔드포인트 없음, OptionService에 update() 없음
- [ ] `Option.update(String name, int quantity)` 모델 메서드 추가
- [ ] `OptionService.update()` 메서드 추가
- [ ] `OptionController` PUT 엔드포인트 추가
- [ ] 옵션 수정 테스트 작성 (상태 재조회로 검증)
- [ ] 전체 테스트 통과 확인

#### 5-2. 테스트 검증 강화 — 예외만 확인하는 테스트를 상태 검증으로 개선

- **현재 문제**: 예외 발생 여부만 확인하고, 상태가 변하지 않았는지 검증하지 않음
- **대상 테스트**:
  - `OptionTest` — 차감 실패 시 재고가 그대로인지 검증 누락 (2건)
  - `MemberTest` — 포인트 차감/충전 실패 시 포인트가 그대로인지 검증 누락 (5건)
- [ ] `OptionTest`: 예외 후 `option.getQuantity()` 불변 검증 추가
- [ ] `MemberTest`: 예외 후 `member.getPoint()` 불변 검증 추가
- [ ] 전체 테스트 통과 확인

---

### 6단계: 도메인 책임 되찾기

**대상 식별 결과:**

#### 6-1. 디미터 법칙 위반 해소 — DTO 변환에서 엔티티 체인 접근 제거

- **현재 문제**: DTO의 `from()` 메서드가 `wish.getProduct().getName()` 등으로 엔티티 내부를 탐색
- **대상 파일**:
  - `WishResponse.from()` — `wish.getProduct().getId/getName/getPrice/getImageUrl` (4회)
  - `ProductResponse.from()` — `product.getCategory().getId()` (1회)
  - `OrderResponse.from()` — `order.getOption().getId()` (1회)
  - `OptionService.delete()` — `option.getProduct().getId()` (1회)
- **개선 방향**: 엔티티에 필요한 값을 반환하는 메서드 추가 (예: `Wish.getProductId()`, `Product.getCategoryId()`)
- [ ] 엔티티에 위임 메서드 추가
- [ ] DTO 변환 코드에서 체인 접근 제거
- [ ] 전체 테스트 통과 확인

#### 6-2. 주문 금액 계산 책임 정리

- **현재 문제**: `Option.calculatePrice()`가 `product.getPrice()`를 호출하여 가격 계산 — Option이 Product 가격 정책을 알아야 함
- **개선 방향**: 주문 금액 계산은 `Order` 생성 시점에 총액을 받거나, 별도 계산 로직으로 분리
- [ ] 가격 계산 책임 위치 결정 (ADR 후보)
- [ ] 변경 적용
- [ ] 전체 테스트 통과 확인

---

### ADR 작성 판단 기준

- [ ] 선택지가 2개 이상이고 트레이드오프가 있었던 경우 → ADR 작성
- [ ] 팀이 반복해서 따라야 할 규칙이나 경계를 정한 경우 → ADR 작성
- [ ] 테스트 전략과 검증 방식이 결정의 핵심이었던 경우 → ADR 작성

**ADR 후보:**
- 트랜잭션 경계에서 카카오 알림을 트랜잭션 안/밖 어디에 둘지
- `Option.calculatePrice()` vs 서비스 레벨 가격 계산 vs Order 엔티티 내부 계산
