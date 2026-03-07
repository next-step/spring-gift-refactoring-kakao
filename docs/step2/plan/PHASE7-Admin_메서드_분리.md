# Phase 7: Admin 메서드 분리 — 세부 실행 계획

> 유형: **구조 변경** (Validator 호출 위치 통일, Admin 에러 표시 형식 미세 변경)
> 상태: 계획 수립
> 관련 ADR: [ADR-3: Admin 메서드 분리 방식](../adr/ADR3-Admin_메서드_분리_방식.md)

---

## 1. 문제 정의

1. `ProductService`에 Admin 전용 메서드가 검증 없이 존재 — 검증은 Controller에서 수행
2. Validator 호출 위치가 분산 — REST API는 Service, Admin은 Controller
3. Admin 에러 처리 패턴이 불일치 — Product은 에러 리스트, Member은 예외 catch

---

## 2. 동작 변경 분석

| 상황 | 변경 전 | 변경 후 |
|------|--------|--------|
| Admin 상품 생성 (정상) | 201 리다이렉트 | 동일 |
| Admin 상품 생성 (이름 오류) | 에러 **리스트** 표시 | 에러 **단일 메시지** 표시 |
| REST API 상품 생성 | 동일 | 동일 |

Admin 에러 표시 형식만 미세하게 변경 (List → String). 검증 규칙 자체는 동일.

---

## 3. 변경 내용

### 수정 파일 (4개)

| 파일 | 변경 |
|------|------|
| `ProductService.java` | `createFromAdmin`/`updateFromAdmin`에 검증 추가, `validateName` 오버로드 |
| `AdminProductController.java` | Validator 직접 호출 제거, try-catch 패턴으로 전환 |
| `templates/product/new.html` | `errors` (List) → `error` (String) |
| `templates/product/edit.html` | `errors` (List) → `error` (String) |

---

## 4. 구현 단계

### Step 1: ProductService — validateName 오버로드 + Admin 메서드에 검증 추가

```java
private void validateName(String name, boolean allowKakao) {
    var errors = ProductNameValidator.validate(name, allowKakao);
    if (!errors.isEmpty()) {
        throw new IllegalArgumentException(String.join(", ", errors));
    }
}
```

### Step 2: AdminProductController — try-catch 패턴으로 전환

AdminMemberController와 동일한 패턴으로 통일.

### Step 3: Product 템플릿 — 에러 표시 형식 변경

`errors` (List, th:each) → `error` (String, th:text)

### Step 4: 컴파일 확인 + 전체 테스트 실행 + diff 검토 + 커밋

---

## 5. 완료 조건

```
□ ProductService.createFromAdmin/updateFromAdmin에 검증 추가
□ AdminProductController에서 Validator 직접 호출 제거
□ Product 템플릿 에러 표시 통일 (Member와 동일 패턴)
□ 전체 인수 테스트(20개 시나리오) 통과
□ diff 검토 완료
□ 커밋 완료
```
