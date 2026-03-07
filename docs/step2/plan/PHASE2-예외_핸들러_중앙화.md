# Phase 2: 예외 핸들러 중앙화 — 세부 실행 계획

> 유형: **구조 변경** (외부 동작 불변)
> 상태: 계획 수립

---

## 1. 문제 정의

MemberController, ProductController, OptionController 3곳에서 **동일한 `@ExceptionHandler`가 반복**된다.

```java
// 3곳에서 완전히 동일한 코드
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
}
```

**영향받는 파일**:

| 파일 | 핸들러 | 처리하는 예외 |
|------|--------|--------------|
| `MemberController.java` (라인 34-37) | `handleIllegalArgument` | `IllegalArgumentException` → 400 |
| `ProductController.java` (라인 71-74) | `handleIllegalArgument` | `IllegalArgumentException` → 400 |
| `OptionController.java` (라인 65-68) | `handleIllegalArgument` | `IllegalArgumentException` → 400 |

**핸들러가 없는 Controller**:

| 파일 | 현재 상태 |
|------|----------|
| `OrderController.java` | 핸들러 없음 — `IllegalArgumentException` 발생 시 500 |
| `WishController.java` | 핸들러 없음 |
| `CategoryController.java` | 핸들러 없음 |
| `AdminProductController.java` | `@Controller` (Thymeleaf) — 핸들러 없음 |
| `AdminMemberController.java` | `@Controller` (Thymeleaf) — try-catch로 직접 처리 |
| `KakaoAuthController.java` | 핸들러 없음 |

---

## 2. 동작 변경 위험 분석

> 구조 변경에서 의도치 않은 동작 변경이 발생하지 않는지 사전 점검한다.

### 핵심 위험: `@RestControllerAdvice` 적용 범위

`@RestControllerAdvice`를 **범위 제한 없이** 생성하면, 현재 핸들러가 없는 Controller에도 적용된다.

| Controller | 현재 동작 | 무제한 Advice 적용 후 | 변경 여부 |
|------------|----------|---------------------|-----------|
| MemberController | `IllegalArgumentException` → 400 | → 400 | **동일** |
| ProductController | `IllegalArgumentException` → 400 | → 400 | **동일** |
| OptionController | `IllegalArgumentException` → 400 | → 400 | **동일** |
| **OrderController** | `IllegalArgumentException` → **500** | → **400** | **작동 변경!** |
| **CategoryController** | 핸들러 없음 | → 400 | **잠재적 변경** |
| **AdminProductController** | `IllegalArgumentException` → **500** (에러 페이지) | → **400** (JSON) | **작동 변경!** |
| AdminMemberController | try-catch로 직접 처리 | 영향 없음 (예외가 전파되지 않음) | 동일 |

**OrderController의 위험 경로**:
```
OrderController.createOrder()
  → OrderService.create()
    → Option.subtractQuantity()  // throws IllegalArgumentException("차감할 수량이 현재 재고보다 많습니다.")
    → Member.deductPoint()       // throws IllegalArgumentException("포인트가 부족합니다.")
```

현재 이 예외들은 처리되지 않아 500이 반환되지만, 전역 Advice가 적용되면 400으로 바뀐다. 이는 **Phase 5(B3)에서 의도적으로 수행할 작동 변경**이므로, Phase 2에서는 발생하면 안 된다.

### 해결: `assignableTypes`로 범위 제한

```java
@RestControllerAdvice(assignableTypes = {
    MemberController.class,
    ProductController.class,
    OptionController.class
})
```

이렇게 하면 **현재 핸들러가 있는 3개 Controller에만** 적용되어, 기존 동작이 정확히 보존된다.

### 적용 범위 제한 후 동작 매핑

| Controller | 현재 동작 | 변경 후 동작 | 변경 여부 |
|------------|----------|------------|-----------|
| MemberController | `IllegalArgumentException` → 400 | → 400 (Advice 적용) | **동일** |
| ProductController | `IllegalArgumentException` → 400 | → 400 (Advice 적용) | **동일** |
| OptionController | `IllegalArgumentException` → 400 | → 400 (Advice 적용) | **동일** |
| OrderController | `IllegalArgumentException` → 500 | → 500 (Advice 미적용) | **동일** |
| AdminProductController | `IllegalArgumentException` → 500 | → 500 (Advice 미적용) | **동일** |

**모든 케이스에서 기존 HTTP 응답 코드가 보존된다.**

---

## 3. 생성/수정할 파일 목록

### 새로 생성 (1개)

| 파일 | 역할 |
|------|------|
| `src/main/java/gift/config/GlobalExceptionHandler.java` | `@RestControllerAdvice` — 예외 핸들러 중앙 관리 |

### 수정 (3개)

| 파일 | 변경 내용 |
|------|----------|
| `MemberController.java` | `@ExceptionHandler` 메서드 제거, import 정리 |
| `ProductController.java` | `@ExceptionHandler` 메서드 제거, import 정리 |
| `OptionController.java` | `@ExceptionHandler` 메서드 제거, import 정리 |

### 변경 없음

| 파일 | 이유 |
|------|------|
| OrderController, WishController 등 | Advice 적용 범위 밖 |
| 인수 테스트 전체 | HTTP 레벨 동작 동일 — 테스트 수정 불필요 |

---

## 4. 구현 단계 (Step-by-Step)

### Step 1: `GlobalExceptionHandler` 생성

```java
// src/main/java/gift/config/GlobalExceptionHandler.java
@RestControllerAdvice(assignableTypes = {
    MemberController.class,
    ProductController.class,
    OptionController.class
})
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

**확인**: 컴파일 가능한지 빌드 확인

---

### Step 2: MemberController에서 `@ExceptionHandler` 제거

**변경 전**:
```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
}
```

**변경 후**: 메서드 삭제 + 미사용 import 제거

**확인**: 컴파일 확인

---

### Step 3: ProductController에서 `@ExceptionHandler` 제거

MemberController와 동일한 패턴.

**확인**: 컴파일 확인

---

### Step 4: OptionController에서 `@ExceptionHandler` 제거

MemberController와 동일한 패턴.

**확인**: 컴파일 확인

---

### Step 5: 전체 테스트 실행

```bash
./gradlew test
```

**기대 결과**: 모든 20개 시나리오 통과

---

### Step 6: diff 검토

```bash
git diff
```

**체크리스트**:
```
□ MemberController에서 @ExceptionHandler가 제거되었는가?
□ ProductController에서 @ExceptionHandler가 제거되었는가?
□ OptionController에서 @ExceptionHandler가 제거되었는가?
□ GlobalExceptionHandler에 assignableTypes 범위 제한이 있는가?
□ 새로 생성된 파일은 GlobalExceptionHandler 1개뿐인가?
□ OrderController, WishController 등 다른 Controller는 변경되지 않았는가?
□ 인수 테스트 파일은 변경되지 않았는가?
□ 구조 변경 외에 작동 변경이 섞이지 않았는가?
```

---

### Step 7: 커밋

```
refactor(config): ExceptionHandler를 ControllerAdvice로 중앙화

MemberController, ProductController, OptionController에서
반복되던 @ExceptionHandler를 GlobalExceptionHandler로 이동한다.

- assignableTypes로 적용 범위를 기존 3개 Controller로 제한
- 핸들러가 없던 Controller(Order, Wish 등)에 영향 없음
- 기존 HTTP 응답 코드 완전 보존
```

---

## 5. 롤백 계획

문제 발생 시:
1. `git stash` 또는 `git checkout .` 으로 전체 변경 취소
2. 원인 분석 후 재시도

---

## 6. Phase 2 완료 조건

```
□ GlobalExceptionHandler 생성 완료 (assignableTypes 범위 제한 포함)
□ MemberController에서 @ExceptionHandler 제거 완료
□ ProductController에서 @ExceptionHandler 제거 완료
□ OptionController에서 @ExceptionHandler 제거 완료
□ 전체 인수 테스트(20개 시나리오) 통과
□ diff 검토 완료 — 구조 변경만 포함
□ 커밋 완료
□ CURRENT_PROJECT_STRUCTURE.md 업데이트
```

---

## 7. 후속 작업과의 관계

Phase 2 완료 후 `GlobalExceptionHandler`는 **확장 지점**이 된다:

| 후속 Phase | 변경 내용 | GlobalExceptionHandler 변경 |
|-----------|----------|---------------------------|
| Phase 3 (B1) | null → 예외 전환 | 새 예외 핸들러 추가 |
| Phase 5 (B3) | Order 예외 처리 개선 | `assignableTypes` 확장 또는 제거 |

Phase 5에서 `assignableTypes` 제한을 제거하면, 모든 Controller에 핸들러가 적용되는 **의도적 작동 변경**이 된다. 이를 위해 Phase 2에서는 제한을 두고, Phase 5에서 명시적으로 확장한다.
