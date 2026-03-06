# ADR-011: 트랜잭션 어노테이션 컨벤션

## 상태
- [x] 승인됨 (2026-03)

## 맥락
리뷰어가 CUD 메서드에도 `@Transactional`을 명시적으로 선언하는 것이 의도를 드러내는 데 유리하지 않냐고 질문했다. 또한 읽기-수정-쓰기 패턴에서 원자성 보장이 필요하다.

## 선택지

### Option A: 메서드 레벨 명시
```java
@Service
public class CategoryService {
    @Transactional(readOnly = true)
    public List<Category> findAll() { ... }

    @Transactional
    public Category create(...) { ... }
}
```
- 장점: 각 메서드 의도가 명확
- 단점: CUD 메서드에 붙이는 걸 깜빡할 수 있음

### Option B: 클래스 레벨 + 오버라이드 (선택)
```java
@Service
@Transactional
public class CategoryService {

    @Transactional(readOnly = true)  // 오버라이드
    public List<Category> findAll() { ... }

    // @Transactional 상속됨
    public Category create(...) { ... }
}
```
- 장점: CUD 메서드 누락 방지 (안전망), 읽기-수정-쓰기 패턴 원자성 보장
- 단점: 암묵적 동작에 의존

## 결정
**Option B** 채택: 클래스 레벨 `@Transactional` + 읽기 메서드만 `readOnly = true` 오버라이드

### 근거
1. **안전망**: 새 CUD 메서드 추가 시 자동으로 트랜잭션 적용
2. **원자성 보장**: 읽기-수정-쓰기 패턴 (update, chargePoint 등)에서 중간 실패 시 롤백
3. **일관성**: 모든 Service가 동일한 컨벤션 적용

## 적용 결과

| Service | 클래스 레벨 | readOnly 메서드 |
|---------|-------------|-----------------|
| OrderService | `@Transactional` | `getOrders()` |
| WishService | `@Transactional` | `getWishes()` |
| ProductService | `@Transactional` | `getAllProducts()`, `getProduct()`, `findAll()`, `findById()` |
| CategoryService | `@Transactional` | `getAllCategories()`, `findAll()`, `findById()` |
| MemberService | `@Transactional` | `login()`, `findAll()`, `findById()`, `existsByEmail()` |
| OptionService | `@Transactional` | `getOptions()` |
| KakaoAuthService | `@Transactional` | - |

## 결과
- 모든 Service가 동일한 트랜잭션 컨벤션 적용
- 81개 테스트 통과

## 관련 ADR
- [ADR-004: 트랜잭션 경계](004-transaction-boundary.md)
- [ADR-010: 읽기 전용 트랜잭션](010-readonly-transactions.md)
