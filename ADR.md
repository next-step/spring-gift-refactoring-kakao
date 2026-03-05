# Architecture Decision Records

## ADR-001: 트랜잭션 경계에서 TransactionTemplate 사용

### 상태
채택

### 맥락
외부 API 호출(카카오 토큰 교환, 메시지 전송)이 `@Transactional` 메서드 안에 있어 DB 커넥션이 불필요하게 점유되는 문제가 있었다. 외부 호출을 트랜잭션 밖으로 분리해야 했다.

### 선택지
1. **별도 빈 추출**: DB 쓰기 로직을 새 클래스(`KakaoMemberWriter` 등)로 분리하고 `@Transactional` 부여
2. **TransactionTemplate**: 기존 클래스 내에서 `TransactionTemplate.execute()`로 트랜잭션 범위를 명시적으로 제어
3. **Self-injection**: 자기 자신의 프록시를 주입받아 내부 메서드의 `@Transactional`이 동작하게 함

### 결정
**TransactionTemplate**을 선택했다.

### 근거
- 별도 빈 추출은 클래스 수가 증가하고, 하나의 논리 흐름이 두 클래스로 분산되어 가독성이 떨어진다.
- Self-injection은 순환 참조를 유발하고 Spring의 의도된 사용 방식이 아니다.
- TransactionTemplate은 기존 클래스 구조를 유지하면서도 트랜잭션 범위를 코드에서 명시적으로 보여준다. 읽는 사람이 "어디까지가 트랜잭션인지" 즉시 파악할 수 있다.

### 적용 대상
- `KakaoAuthService.loginWithKakao()` — 외부 HTTP 호출 후 DB 저장
- `OrderService.createOrder()` — DB 쓰기 후 best-effort 카카오 메시지 전송

---

## ADR-002: 관리자 카카오 상품명 허용을 allowKakao 파라미터로 해결

### 상태
채택

### 맥락
`ProductService.create()`는 항상 `allowKakao=false`로 상품명을 검증하여 "카카오" 포함 상품명을 차단했다. 관리자 컨트롤러는 `allowKakao=true`로 먼저 검증한 뒤 서비스를 호출했지만, 서비스가 다시 `allowKakao=false`로 검증하여 결국 차단되는 버그가 있었다. `validateNameWithKakao()` 메서드가 정의되어 있었지만 어디에서도 호출되지 않았다.

### 선택지
1. **allowKakao 파라미터 오버로드**: `create()`/`update()`에 `boolean allowKakao` 파라미터를 추가한 오버로드 메서드 제공
2. **별도 메서드**: `createForAdmin()`/`updateForAdmin()` 메서드를 별도로 생성
3. **관리자 전용 서비스**: `AdminProductService`를 별도로 분리

### 결정
**allowKakao 파라미터 오버로드**를 선택했다.

### 근거
- 기존 4파라미터 `create()`/`update()`는 하위 호환을 유지하며 `allowKakao=false`가 기본값이 된다.
- 별도 메서드나 서비스 분리는 비즈니스 로직 중복이 발생하고, 관리자/API의 차이가 "카카오 허용 여부" 한 가지뿐이므로 과잉이다.
- 미사용이던 `validateNameWithKakao()` 퍼블릭 메서드를 제거하고, `validateName(name, allowKakao)` 프라이빗 메서드로 통합하여 검증 경로를 단일화했다.

### 결과
- REST API: `create(name, price, imageUrl, categoryId)` → `allowKakao=false` (기존 동작 유지)
- Admin: `create(name, price, imageUrl, categoryId, true)` → `allowKakao=true` (카카오 허용)

---

## ADR-003: 도메인 로직 이동 기준 — Entity 필드로 완결되는 것만 이동

### 상태
채택

### 맥락
Service에 도메인 로직이 누수되어 있었다. getter로 필드를 꺼내 비교·계산·판단하는 코드가 Service에 흩어져 있었고, 일부는 중복되었다. 이를 Entity로 이동하되, 이동 범위의 기준이 필요했다.

### 선택지
1. **최소 이동**: Entity 자신의 필드만으로 완결되는 순수 로직만 이동
2. **적극 이동**: Value Object(Money, Points 등) 도입까지 포함하여 도메인 모델을 풍부하게 재설계

### 결정
**최소 이동**을 선택했다.

### 근거
- Value Object 도입은 엔티티 구조, 영속성 매핑, 테스트 전반에 영향을 미치는 구조 변경이다. "작동을 유지하면서 책임만 이동"하는 이번 단계의 범위를 넘는다.
- Repository 쿼리가 필요한 검증(중복 체크, 존재 확인)은 Service에 남겨야 한다. Entity가 Repository에 의존하면 안 된다.
- 이동 기준을 명확히 하면 향후에도 "이건 Entity, 이건 Service"라는 판단을 일관되게 내릴 수 있다.

### 적용 결과

| Entity | 메서드 | 이동한 로직 | 개선 증거 |
|--------|--------|------------|----------|
| Member | `matchesPassword()` | null 체크 + 비밀번호 비교 | 호출부 분기 감소 |
| Member | `hasKakaoAccessToken()` | null 체크 → 의미 있는 질의 메서드 | 분기·조건 감소 |
| Option | `belongsTo(productId)` | 상품 소속 확인 | 중복 제거 (2곳 → 1곳) |
| Option | `calculateTotalPrice(quantity)` | 가격 × 수량 계산 | 디미터 법칙 위반 제거 |
| Wish | `isOwnedBy(memberId)` | 소유권 확인 | 호출부 단순화 |

### 이동하지 않은 것과 이유
- 이메일 중복 확인 (`memberRepository.existsByEmail`) → Repository 쿼리 필요
- 옵션명 중복 확인 (`optionRepository.existsByProductIdAndName`) → Repository 쿼리 필요
- 상품/카테고리 존재 확인 → Repository 쿼리 필요
