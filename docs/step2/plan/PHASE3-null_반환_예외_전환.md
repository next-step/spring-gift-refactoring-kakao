# Phase 3: null 반환을 예외로 전환 — 세부 실행 계획

> 유형: **작동 변경** (예외 전환으로 에러 응답 경로 변경)
> 상태: 계획 수립

---

## 1. 문제 정의

Service에서 `orElse(null)` → Controller에서 null 체크 → 404 패턴이 5개 도메인에 걸쳐 반복된다.
동시에 일부 Service 메서드는 `orElseThrow()`로 예외를 던지고 있어 전략이 혼재한다.

### 수정 대상 (null 반환 → 예외 전환)

| 도메인 | Service 메서드 | Controller 메서드 | 현재 HTTP 응답 |
|--------|---------------|-------------------|---------------|
| category | `update()` | `updateCategory()` | 404 |
| product | `findById()` | `getProduct()` | 404 |
| product | `create()` | `createProduct()` | 404 |
| product | `update()` | `updateProduct()` | 404 |
| option | `findByProductId()` | `getOptions()` | 404 |
| option | `create()` | `createOption()` | 404 |
| option | `delete()` | `deleteOption()` | 404 |
| order | `create()` | `createOrder()` | 404 |
| wish | `add()` | `addWish()` | 404 |

### 변경 불필요

| 도메인 | 이유 |
|--------|------|
| member | 이미 예외 방식 구현됨 (`orElseThrow`) |
| wish `remove()` | Enum 기반 반환 (null 아님) |
| product `createFromAdmin()` / `updateFromAdmin()` | 이미 예외 방식 구현됨 |

---

## 2. 의사결정

> **[ADR-2: 예외 처리 전략](../adr/ADR2-예외_처리_전략.md)** 참조
>
> 결정: `NoSuchElementException`을 사용한다.

---

## 3. 동작 변경 분석

### HTTP 응답 변화

| 상황 | 변경 전 | 변경 후 | 변경 여부 |
|------|--------|--------|-----------|
| 엔티티 존재 | 200 (정상) | 200 (정상) | **동일** |
| 엔티티 미존재 | 404 (Controller null 체크) | 404 (핸들러가 예외 캐치) | **동일** |

**HTTP 상태 코드는 보존된다.** 다만 에러 처리 경로가 변경된다:
- 변경 전: Service → null → Controller null 체크 → `ResponseEntity.notFound().build()`
- 변경 후: Service → `NoSuchElementException` → 핸들러 → `ResponseEntity.notFound().build()`

### 핸들러 적용 범위 위험

`GlobalExceptionHandler`에 핸들러를 추가하면 `assignableTypes` 제한 때문에 3개 Controller에만 적용된다.
그러나 Phase 3는 **모든 REST Controller**에 적용되어야 한다.

**해결**: 별도의 `@RestControllerAdvice(annotations = RestController.class)`를 사용하여
모든 `@RestController`에 적용하되, `@Controller`(Admin)에는 미적용한다.

| Controller | 타입 | 핸들러 적용 |
|------------|------|-----------|
| CategoryController | `@RestController` | **적용** |
| ProductController | `@RestController` | **적용** |
| OptionController | `@RestController` | **적용** |
| OrderController | `@RestController` | **적용** |
| WishController | `@RestController` | **적용** |
| MemberController | `@RestController` | 적용 (영향 없음 — NoSuchElementException 미발생) |
| KakaoAuthController | `@RestController` | 적용 (영향 없음 — NoSuchElementException 미발생) |
| AdminProductController | `@Controller` | **미적용** |
| AdminMemberController | `@Controller` | **미적용** |

### AdminProductController 영향

`ProductService.findById()`가 예외를 던지게 되면, AdminProductController의 null 체크가 **데드 코드**가 된다:

```java
// 변경 전: findById()가 null 반환 → 이 코드가 실행됨
var product = productService.findById(id);
if (product == null) {
    throw new NoSuchElementException("상품이 존재하지 않습니다.");
}

// 변경 후: findById()가 예외를 던짐 → if문에 도달하지 않음
```

AdminProductController의 데드 코드 제거는 **해당 도메인 커밋에 포함**한다.

---

## 4. 생성/수정할 파일 목록

### 새로 생성 (1개)

| 파일 | 역할 |
|------|------|
| `src/main/java/gift/config/NotFoundExceptionHandler.java` | `NoSuchElementException` → 404 핸들러 |

### 수정 — 도메인별

#### Category (2개)

| 파일 | 변경 내용 |
|------|----------|
| `CategoryService.java` | `update()`: `orElse(null)` → `orElseThrow()` |
| `CategoryController.java` | `updateCategory()`: null 체크 제거 |

#### Product (3개)

| 파일 | 변경 내용 |
|------|----------|
| `ProductService.java` | `findById()`, `create()`, `update()`: `orElse(null)` → `orElseThrow()` |
| `ProductController.java` | `getProduct()`, `createProduct()`, `updateProduct()`: null 체크 제거 |
| `AdminProductController.java` | `editForm()`, `update()`: 데드 코드(null 체크 + throw) 제거 |

#### Option (2개)

| 파일 | 변경 내용 |
|------|----------|
| `OptionService.java` | `findByProductId()`, `create()`, `delete()`: `orElse(null)` → `orElseThrow()` |
| `OptionController.java` | `getOptions()`, `createOption()`, `deleteOption()`: null 체크 제거 |

#### Order (2개)

| 파일 | 변경 내용 |
|------|----------|
| `OrderService.java` | `create()`: `orElse(null)` → `orElseThrow()` |
| `OrderController.java` | `createOrder()`: null 체크 제거 |

#### Wish (1개)

| 파일 | 변경 내용 |
|------|----------|
| `WishService.java` | `add()`: `orElse(null)` → `orElseThrow()` |
| `WishController.java` | `addWish()`: null 체크 제거 |

---

## 5. 구현 단계 (Step-by-Step)

### Step 1: NotFoundExceptionHandler 생성 (구조 변경)

```java
@RestControllerAdvice(annotations = RestController.class)
public class NotFoundExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.notFound().build();
    }
}
```

**확인**: 컴파일 + 테스트 통과 (아직 아무 Service도 변경하지 않았으므로 동작 불변)
**커밋**: `refactor(config): NoSuchElementException 핸들러 추가`

---

### Step 2: Category 도메인 전환

**CategoryService.update()** 변경:
```java
// 변경 전
var category = categoryRepository.findById(id).orElse(null);
if (category == null) {
    return null;
}

// 변경 후
var category = categoryRepository.findById(id)
    .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + id));
```

**CategoryController.updateCategory()** null 체크 제거.

**확인**: 테스트 통과
**커밋**: `feat(category): null 반환을 예외로 전환`

---

### Step 3: Product 도메인 전환

**ProductService** — `findById()`, `create()`, `update()` 변경.
**ProductController** — 3개 메서드 null 체크 제거.
**AdminProductController** — 데드 코드 제거.

**확인**: 테스트 통과
**커밋**: `feat(product): null 반환을 예외로 전환`

---

### Step 4: Option 도메인 전환

**OptionService** — `findByProductId()`, `create()`, `delete()` 변경.
**OptionController** — 3개 메서드 null 체크 제거.

**확인**: 테스트 통과
**커밋**: `feat(option): null 반환을 예외로 전환`

---

### Step 5: Order 도메인 전환

**OrderService.create()** 변경.
**OrderController.createOrder()** null 체크 제거.

**확인**: 테스트 통과
**커밋**: `feat(order): null 반환을 예외로 전환`

---

### Step 6: Wish 도메인 전환

**WishService.add()** 변경.
**WishController.addWish()** null 체크 제거.

**확인**: 테스트 통과
**커밋**: `feat(wish): null 반환을 예외로 전환`

---

### Step 7: 전체 테스트 실행 및 최종 diff 검토

```bash
./gradlew test
```

**체크리스트**:
```
□ 모든 인수 테스트(20개 시나리오) 통과
□ 모든 Service에서 orElse(null) 패턴이 제거되었는가?
□ 모든 REST Controller에서 null 체크 분기가 제거되었는가?
□ AdminProductController의 데드 코드가 제거되었는가?
□ NotFoundExceptionHandler의 적용 범위가 @RestController로 제한되어 있는가?
□ GlobalExceptionHandler는 변경되지 않았는가?
```

---

## 6. 롤백 계획

도메인별 커밋이므로, 특정 도메인에서 문제가 발생하면 해당 커밋만 되돌린다:
```bash
git revert <commit-hash>
```

---

## 7. Phase 3 완료 조건

```
□ ADR-2 작성 완료
□ NotFoundExceptionHandler 생성 완료
□ CategoryService/Controller null 패턴 제거 완료
□ ProductService/Controller/AdminProductController null 패턴 제거 완료
□ OptionService/Controller null 패턴 제거 완료
□ OrderService/Controller null 패턴 제거 완료
□ WishService/Controller null 패턴 제거 완료
□ 전체 인수 테스트(20개 시나리오) 통과
□ 최종 diff 검토 완료
□ CURRENT_PROJECT_STRUCTURE.md 업데이트
```
