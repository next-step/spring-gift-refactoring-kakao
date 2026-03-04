# 미참조 코드 분석 결과

코드베이스에서 발견된 참조되지 않는 코드를 정리한 문서입니다.

---

## 분석 기준

삭제 전 반드시 확인한 사항:
1. 주변 주석 또는 TODO에 의도가 있는가
2. git blame으로 누가 왜 추가했는가
3. 이후 단계(작동 변경)와 충돌하지 않는가

---

## 1. `OrderController.wishRepository` - 미사용 의존성

### 발견 위치

| 파일 | 라인 | 코드 |
|------|------|------|
| `OrderController.java` | 26 | `private final WishRepository wishRepository;` |
| `OrderController.java` | 41 | `this.wishRepository = wishRepository;` |

### 근거 확인

**주석/TODO 의도**:
```java
// order flow:
// 1. auth check
// 2. validate option
// 3. subtract stock
// 4. deduct points
// 5. save order
// 6. cleanup wish    ← 의도 명시됨
// 7. send kakao notification
```

**git blame**:
```
55ca9e43 (wotjd243 2026-02-18) feat: set up the project
```
- 초기 프로젝트 설정 시 추가됨
- 주문 흐름의 6단계 구현을 위해 의도적으로 주입됨

**향후 단계 충돌**:
- "cleanup wish" 기능 구현 시 필요
- 삭제 시 작동 변경 단계에서 다시 추가해야 함

### 판정: 삭제 불가

주석에 명시된 의도가 있으며, 미완성 기능의 흔적입니다. 삭제하지 않고 작동 변경 단계에서 구현하거나, 기능이 불필요하다면 주석과 함께 삭제해야 합니다.

---

## 2. `Order.getMemberId()` - 미사용 Getter

### 발견 위치

| 파일 | 라인 | 코드 |
|------|------|------|
| `Order.java` | 49-51 | `public Long getMemberId() { return memberId; }` |

### 근거 확인

**주석/TODO 의도**: 없음

**git blame**:
```
55ca9e43 (wotjd243 2026-02-18) feat: set up the project
```
- 초기 프로젝트 설정 시 엔티티 표준 getter로 추가됨

**향후 단계 충돌**:
- 주문 조회 시 회원 정보 표시 기능에 필요할 수 있음
- `OrderResponse`에 memberId 추가 시 필요

### 판정: 삭제 예정

현재 사용되지 않으며, 필요 시 재추가가 용이합니다. 미사용 코드 제거 원칙에 따라 삭제합니다.

---

## 3. `Product.getOptions()` - 미사용 Getter

### 발견 위치

| 파일 | 라인 | 코드 |
|------|------|------|
| `Product.java` | 70-72 | `public List<Option> getOptions() { return options; }` |

### 근거 확인

**주석/TODO 의도**: 없음

**git blame**:
```
55ca9e43 (wotjd243 2026-02-18) feat: set up the project
```
- 초기 프로젝트 설정 시 양방향 관계의 getter로 추가됨

**향후 단계 충돌**:
- 상품 상세 조회 시 옵션 목록 표시 기능에 필요할 수 있음
- 현재는 `OptionRepository.findByProductId()`로 조회 중

### 판정: 삭제 예정

현재 사용되지 않으며, 옵션 조회는 `OptionRepository.findByProductId()`로 수행 중입니다. 필요 시 재추가가 용이하므로 삭제합니다.

---

## 요약

| 미참조 코드 | 주석 의도 | 향후 충돌 | 판정 |
|-------------|-----------|-----------|------|
| `OrderController.wishRepository` | ✓ 있음 | ✓ 충돌 | **삭제 불가** |
| `Order.getMemberId()` | ✗ 없음 | ✗ 없음 | **삭제 예정** |
| `Product.getOptions()` | ✗ 없음 | ✗ 없음 | **삭제 예정** |

### 결론

- **삭제 예정**: 엔티티 getter 2건 (미사용, 재추가 용이)
- **삭제 불가**: `OrderController.wishRepository` (의도된 미완성 기능)

---

## AI 활용 기록

이 문서는 Claude Code를 활용하여 작성되었습니다.
- **활용 방식**: 도메인별 코드 순회 및 Grep으로 참조 여부 확인
- **검증 방법**: git blame으로 추가 맥락 확인, 주석 분석
- **분석 범위**: auth, category, member, option, order, product, wish 패키지
