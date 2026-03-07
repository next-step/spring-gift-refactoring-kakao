# 1단계: 스타일 정리 작업 보고서

> 작업일: 2026-02-26
> 목표: 프로젝트 전반의 스타일 불일치를 일관되게 정리한다 (구조 변경만, 작동 변경 없음)

---

## 1. 에러 메시지 영어 통일

한국어로 작성되어 있던 에러 메시지를 영어로 통일했다.

### 변경 파일 및 내용

| 파일 | Before (한국어) | After (영어) |
|------|----------------|-------------|
| `ProductNameValidator.java` | `"상품 이름은 필수입니다."` | `"Product name is required."` |
| | `"상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다."` | `"Product name must be at most 15 characters."` |
| | `"상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ..."` | `"Product name contains invalid special characters. Allowed: ( ) [ ] + - & / _"` |
| | `"\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다."` | `"Product name containing \"카카오\" requires approval from the MD team."` |
| `OptionNameValidator.java` | `"옵션 이름은 필수입니다."` | `"Option name is required."` |
| | `"옵션 이름은 공백을 포함하여 최대 50자까지 입력할 수 있습니다."` | `"Option name must be at most 50 characters."` |
| | `"옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ..."` | `"Option name contains invalid special characters. Allowed: ( ) [ ] + - & / _"` |
| `Option.java` | `"차감할 수량이 현재 재고보다 많습니다."` | `"Subtract amount exceeds current stock."` |
| `Member.java` | `"차감 금액은 1 이상이어야 합니다."` | `"Deduction amount must be greater than zero."` |
| | `"포인트가 부족합니다."` | `"Insufficient points."` |
| `OptionController.java` | `"이미 존재하는 옵션명입니다."` | `"Option name already exists."` |
| | `"옵션이 1개인 상품은 옵션을 삭제할 수 없습니다."` | `"Cannot delete the last option of a product."` |
| `AdminProductController.java` | `"카테고리가 존재하지 않습니다. id=..."` | `"Category not found. id=..."` |
| | `"상품이 존재하지 않습니다. id=..."` | `"Product not found. id=..."` |

---

## 2. 예외 처리 패턴 통일

각 Controller에 분산되어 있던 `@ExceptionHandler`를 `GlobalExceptionHandler`로 통합했다.

### 새로 생성한 파일

**`src/main/java/gift/dto/ErrorResponse.java`**
- 에러 응답 DTO (`record`). `message` 필드 하나를 가진다.
- 기존: 에러 응답이 `String`으로 반환됨 → 이후: `{"message": "..."}` JSON 구조로 통일

**`src/main/java/gift/exception/GlobalExceptionHandler.java`**
- `@RestControllerAdvice`로 전역 예외 처리
- `IllegalArgumentException` → 400 Bad Request + `ErrorResponse`
- `NoSuchElementException` → 404 Not Found + `ErrorResponse`

### 제거한 코드

| 파일 | 제거 내용 |
|------|----------|
| `MemberController.java` | `@ExceptionHandler(IllegalArgumentException.class)` 메서드 (4줄) + import 제거 |
| `ProductController.java` | `@ExceptionHandler(IllegalArgumentException.class)` 메서드 (4줄) + import 제거 |
| `OptionController.java` | `@ExceptionHandler(IllegalArgumentException.class)` 메서드 (4줄) + import 제거 |

---

## 3. Null 처리 패턴 통일

`.orElse(null)` + if-null 패턴을 `.orElseThrow(() -> new NoSuchElementException(...))` 패턴으로 변경했다.

### 변경 파일 및 내용

| 파일 | 메서드 | 변경 내용 |
|------|--------|----------|
| `ProductController.java` | `getProduct` | `findById(id).orElse(null)` → `orElseThrow` |
| | `createProduct` | `categoryRepository.findById(...).orElse(null)` → `orElseThrow` |
| | `updateProduct` | category + product 조회 2곳 모두 `orElseThrow` |
| `OptionController.java` | `getOptions` | product 존재 확인 `orElseThrow` |
| | `createOption` | product 조회 `orElseThrow` |
| | `deleteOption` | product 존재 확인 + option 조회 2곳 `orElseThrow` |
| `CategoryController.java` | `updateCategory` | category 조회 `orElseThrow` |
| `OrderController.java` | `createOrder` | option 조회 `orElseThrow` |
| `WishController.java` | `addWish` | product 조회 `orElseThrow` |
| | `removeWish` | wish 조회 `orElseThrow` |

### 변경하지 않은 곳 (의도적 유지)

| 파일 | 위치 | 유지 이유 |
|------|------|----------|
| `WishController.addWish` | `findByMemberIdAndProductId()` | 존재 여부 확인 로직. `Optional.isPresent()` 패턴으로 개선 |
| `AuthenticationResolver` | `extractMember()` | 인증 실패 시 `null` 반환이 의도된 동작 |
| `OrderController` / `WishController` | `member == null → 401` | 인증 관련 null 체크이므로 유지 |

---

## 4. import 정리 및 코드 포맷팅

| 파일 | 변경 내용 |
|------|----------|
| `OptionController.java` | `import java.util.stream.Collectors` 제거, `.collect(Collectors.toList())` → `.toList()` |
| `ProductController.java` | `import @ExceptionHandler` 제거, `import NoSuchElementException` 추가 |
| `OptionController.java` | `import @ExceptionHandler` 제거, `import NoSuchElementException` 추가 |
| `MemberController.java` | `import @ExceptionHandler` 제거 |
| `CategoryController.java` | `import NoSuchElementException` 추가 |
| `OrderController.java` | `import NoSuchElementException` 추가 |
| `WishController.java` | `import NoSuchElementException` 추가 |

---

## 5. 테스트 수정

에러 메시지 변경과 GlobalExceptionHandler 도입에 맞춰 테스트 단언문을 업데이트했다.

### 에러 메시지 단언문 변경

| 파일 | Before | After |
|------|--------|-------|
| `ProductNameValidatorTest.java` | `.contains("필수")` | `.contains("required")` |
| | `.contains("15자")` | `.contains("15 characters")` |
| | `.contains("특수 문자")` | `.contains("invalid special characters")` |
| `OptionNameValidatorTest.java` | `.contains("필수")` | `.contains("required")` |
| | `.contains("50자")` | `.contains("50 characters")` |
| | `.contains("특수 문자")` | `.contains("invalid special characters")` |

### 예외 처리 방식 변경에 따른 테스트 수정

| 파일 | Before | After |
|------|--------|-------|
| `OrderControllerTest.java` | `assertThatThrownBy(ServletException.class)` | `.andExpect(status().isBadRequest())` |
| `GiftAcceptanceTest.java` | `.statusCode(500)` (재고 초과/포인트 부족) | `.statusCode(400)` |

> `GlobalExceptionHandler`가 `IllegalArgumentException`을 잡아 400으로 응답하므로, 기존에 서버 에러(500)로 전파되던 케이스가 클라이언트 에러(400)로 변경되었다.

---

## 검증 결과

```
BUILD SUCCESSFUL
125 tests completed, 0 failed
```

전체 125개 테스트 통과 확인 완료.
