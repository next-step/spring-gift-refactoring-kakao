# Anti-Pattern 분석 결과

코드베이스에서 발견된 anti-pattern을 정리한 문서입니다.

---

## 1. `orElse(null)` 후 null 체크

Optional을 사용하면서 null을 반환하고 다시 null 체크를 하는 패턴입니다.
Optional의 목적(null 안전성)을 훼손합니다.

### 발견 위치

| 파일 | 라인 | 수정 여부 |
|------|------|----------|
| `AuthenticationResolver.java` | 29 | ✅ 수정됨 (Step 2에서 Optional 반환으로 변경) |
| `CategoryController.java` | 46 | ✅ 수정됨 |
| `OptionController.java` | 37, 54, 74, 84 | ✅ 수정됨 |
| `OrderController.java` | 81 | ✅ 수정됨 |
| `ProductController.java` | 41, 52, 69, 74 | ✅ 수정됨 |
| `WishController.java` | 63, 69, 90 | ✅ 수정됨 |

### AuthenticationResolver ~~제외 사유~~ → 해결됨

~~`AuthenticationResolver.extractMember()`는 의도적으로 수정하지 않았습니다.~~

**Step 2에서 해결됨** - [ADR-006](../../adr/006-domain-responsibility.md) 참조

```java
// 변경 후: Optional 반환으로 명시적 처리 강제
public Optional<Member> extractMember(String authorization) {
    // ...
    return memberRepository.findByEmail(email);
}

// Controller: Optional 체이닝
return authenticationResolver.extractMember(authorization)
    .map(member -> { /* 로직 */ })
    .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
```

### 예시

```java
// Before (anti-pattern)
Product product = productRepository.findById(id).orElse(null);
if (product == null) {
    return ResponseEntity.notFound().build();
}

// After (권장)
Product product = productRepository.findById(id)
    .orElseThrow(() -> new NotFoundException("Product not found"));
```

---

## 2. Exception Swallowing

예외를 catch하고 무시하거나 단순히 null을 반환하는 패턴입니다.
디버깅과 문제 추적을 어렵게 만듭니다.

### 발견 위치

| 파일 | 라인 | 설명 |
|------|------|------|
| `AuthenticationResolver.java` | 30-31 | `catch (Exception e) { return null; }` |
| `OrderController.java` | 111-112 | `catch (Exception ignored) {}` |

### 예시

```java
// Before (anti-pattern)
try {
    // ...
} catch (Exception e) {
    return null;
}

// After (권장) - 최소한 로깅
try {
    // ...
} catch (Exception e) {
    log.warn("Failed to process: {}", e.getMessage());
    return Optional.empty();
}
```

---

## 3. `ResponseEntity<?>` 와일드카드 타입

제네릭 와일드카드를 사용하면 타입 안전성이 떨어지고, API 문서화가 어려워집니다.

### 발견 위치

| 파일 | 라인 |
|------|------|
| `OrderController.java` | 48, 70 |

### 예시

```java
// Before (anti-pattern)
public ResponseEntity<?> getOrders(...) { }

// After (권장)
public ResponseEntity<Page<OrderResponse>> getOrders(...) { }
```

---

## 4. 매직 넘버 HTTP 상태 코드

숫자 리터럴 대신 `HttpStatus` 상수를 사용하는 것이 가독성과 유지보수성에 좋습니다.

### 발견 위치

| 파일 | 라인 | 코드 |
|------|------|------|
| `OrderController.java` | 55, 77 | `status(401)` |
| `WishController.java` | 45, 59, 87 | `status(401)` |
| `WishController.java` | 96 | `status(403)` |

### 예시

```java
// Before (anti-pattern)
return ResponseEntity.status(401).build();
return ResponseEntity.status(403).build();

// After (권장)
return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
```

---

## 요약

| Anti-Pattern | 발생 횟수 | 심각도 |
|--------------|-----------|--------|
| `orElse(null)` 후 null 체크 | 12회 | 중 |
| Exception Swallowing | 2회 | 높음 |
| `ResponseEntity<?>` 와일드카드 | 2회 | 중 |
| 매직 넘버 HTTP 상태 코드 | 6회 | 낮음 |

---

## AI 활용 기록

이 문서는 Claude Code를 활용하여 작성되었습니다.
- **활용 방식**: 도메인별로 코드를 순회하며 anti-pattern 식별
- **분석 범위**: auth, category, member, option, order, product, wish 패키지
