# 스타일 불일치 분석 결과

코드베이스에서 발견된 스타일 불일치를 정리한 문서입니다.

---

## 1. 에러 메시지 한/영 혼용

가장 심각한 스타일 불일치입니다. 사용자에게 노출되는 에러 메시지가 한국어와 영어로 혼용되어 있습니다.

### 영어 메시지

| 파일 | 라인 | 메시지 |
|------|------|--------|
| `Member.java` | 51 | `"Amount must be greater than zero."` |
| `MemberController.java` | 36 | `"Email is already registered."` |
| `MemberController.java` | 47, 50 | `"Invalid email or password."` |
| `AdminMemberController.java` | 46 | `"Email is already registered."` |
| `AdminMemberController.java` | 57, 69, 81 | `"Member not found. id="` |

### 한국어 메시지

| 파일 | 라인 | 메시지 |
|------|------|--------|
| `Member.java` | 59 | `"차감 금액은 1 이상이어야 합니다."` |
| `Member.java` | 62 | `"포인트가 부족합니다."` |
| `Option.java` | 41 | `"차감할 수량이 현재 재고보다 많습니다."` |
| `OptionController.java` | 60 | `"이미 존재하는 옵션명입니다."` |
| `OptionController.java` | 81 | `"옵션이 1개인 상품은 옵션을 삭제할 수 없습니다."` |
| `OptionNameValidator.java` | 25, 30, 34 | 한국어 검증 메시지 |
| `ProductNameValidator.java` | 23, 28, 32, 36 | 한국어 검증 메시지 |
| `AdminProductController.java` | 54, 87 | `"카테고리가 존재하지 않습니다."` |
| `AdminProductController.java` | 62, 78 | `"상품이 존재하지 않습니다."` |

### 권장 통일안

**한국어로 통일** (사용자 대상 서비스이므로)

```java
// Before
throw new IllegalArgumentException("Amount must be greater than zero.");

// After
throw new IllegalArgumentException("금액은 1 이상이어야 합니다.");
```

---

## 2. 주석 스타일 불일치

### 현황

| 스타일 | 파일 |
|--------|------|
| Javadoc (`/** */`) | `AuthenticationResolver`, `JwtProvider`, `Member`, `MemberController`, `MemberRepository`, `MemberRequest`, `TokenResponse`, `AdminMemberController` |
| 블록 주석 (`/* */`) | `KakaoAuthController`, `OptionController`, `OptionNameValidator` |
| 인라인 주석 (`//`) | `OrderController`, `WishController`, `Member`, `Order` |
| 주석 없음 | `CategoryController`, `Category`, `Product`, `Option`, `Wish` 등 |

### 권장 통일안

- **클래스/public 메서드**: Javadoc (`/** */`) 사용
- **구현 내부 설명**: 인라인 주석 (`//`) 사용
- **블록 주석**: 사용하지 않음

---

## 3. `@Autowired` 사용 불일치

### 현황

| 사용 | 파일 |
|------|------|
| `@Autowired` 있음 | `AuthenticationResolver`, `JwtProvider`, `MemberController`, `AdminMemberController` |
| `@Autowired` 없음 | `KakaoAuthController`, `KakaoLoginClient`, `CategoryController`, `ProductController`, `OptionController`, `OrderController`, `WishController` |

### 권장 통일안

**`@Autowired` 생략** (Spring 4.3+ 단일 생성자는 자동 주입)

```java
// Before
@Autowired
public MemberController(MemberRepository memberRepository) { }

// After
public MemberController(MemberRepository memberRepository) { }
```

---

## 4. `var` vs 명시적 타입 선언

### 현황

| 스타일 | 파일 |
|--------|------|
| `var` 사용 | `OrderController`, `WishController` |
| 명시적 타입 | `CategoryController`, `ProductController`, `OptionController`, `MemberController` 등 |

### 권장 통일안

**명시적 타입 선언** (가독성과 IDE 지원)

```java
// Before
var member = authenticationResolver.extractMember(authorization);

// After
Member member = authenticationResolver.extractMember(authorization);
```

---

## 5. Stream 수집 방식 불일치

### 현황

| 스타일 | 파일 | 라인 |
|--------|------|------|
| `.toList()` | `CategoryController` | 30 |
| `.collect(Collectors.toList())` | `OptionController` | 43 |

### 권장 통일안

**`.toList()` 사용** (Java 16+, 불변 리스트 반환)

```java
// Before
.collect(Collectors.toList());

// After
.toList();
```

---

## 6. `@RequestMapping` path 속성

### 현황

| 스타일 | 파일 |
|--------|------|
| `path=` 명시 | `KakaoAuthController`, `OptionController` |
| `path=` 생략 | 나머지 모든 컨트롤러 |

### 권장 통일안

**`path=` 생략** (단일 값일 경우 value가 기본)

```java
// Before
@RequestMapping(path = "/api/products/{productId}/options")
@DeleteMapping(path = "/{optionId}")

// After
@RequestMapping("/api/products/{productId}/options")
@DeleteMapping("/{optionId}")
```

---

## 7. 예외 타입 불일치

### 현황

| 예외 타입 | 파일 |
|----------|------|
| `IllegalArgumentException` | `Member`, `MemberController`, `OptionController`, `AdminMemberController` |
| `NoSuchElementException` | `AdminProductController` |

### 권장 통일안

**`IllegalArgumentException` 통일** (또는 커스텀 예외 도입)

---

## 8. `final` 키워드 사용 불일치

### 현황

| 스타일 | 파일 |
|--------|------|
| 지역 변수에 `final` 사용 | `AuthenticationResolver`, `JwtProvider`, `MemberController`, `AdminMemberController` |
| `final` 미사용 | `CategoryController`, `ProductController`, `OrderController`, `WishController` 등 |

### 권장 통일안

**`final` 생략** (또는 일관되게 모두 사용)

---

## 9. 엔티티 필드 선언 빈 줄 불일치

### 현황

| 스타일 | 파일 |
|--------|------|
| 필드 사이 빈 줄 있음 | `Member.java` (19-26), `Option.java`, `Order.java` |
| 필드 사이 빈 줄 없음 | `Category.java` (12-16), `Product.java` (21-24) |

### 권장 통일안

**JPA 어노테이션이 붙은 필드는 빈 줄로 분리, 단순 필드는 연속 선언**

```java
// 권장
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@ManyToOne
@JoinColumn(name = "category_id")
private Category category;

private String name;
private int price;
private String imageUrl;
```

---

## 10. 주석 언어 vs 에러 메시지 언어 불일치

주석은 영어로 작성되어 있으나, 에러 메시지는 한국어인 경우가 있습니다.

### 현황

| 파일 | 주석 언어 | 에러 메시지 언어 |
|------|-----------|------------------|
| `OptionNameValidator.java` | 영어 (7-12) | 한국어 (25, 30, 34) |
| `OptionController.java` | 영어 (20-23) | 한국어 (60, 81) |
| `Member.java` | 영어 (56) | 한국어 (59, 62) + 영어 (51) |

### 권장 통일안

**주석과 에러 메시지 모두 한국어로 통일**

---

## 11. Validator 클래스 주석 유무 불일치

### 현황

| 파일 | 주석 |
|------|------|
| `OptionNameValidator.java` | 블록 주석 있음 (7-12) |
| `ProductNameValidator.java` | 주석 없음 |

### 권장 통일안

**둘 다 동일한 형식의 주석 추가** (또는 둘 다 생략)

---

## 12. 동일 목적 주석 문구 불일치

primitive FK를 설명하는 주석이 파일마다 다릅니다.

### 현황

| 파일 | 라인 | 주석 |
|------|------|------|
| `Order.java` | 24 | `// primitive FK` |
| `Wish.java` | 16 | `// primitive FK - no entity reference` |

### 권장 통일안

**동일한 문구로 통일**: `// primitive FK`

---

## 요약

| 불일치 항목 | 심각도 | 권장 통일안 |
|-------------|--------|-------------|
| 에러 메시지 한/영 혼용 | 높음 | 한국어로 통일 |
| 주석 언어 vs 에러 메시지 언어 | 높음 | 한국어로 통일 |
| 주석 스타일 | 중 | Javadoc + 인라인 |
| `var` vs 명시적 타입 | 중 | 명시적 타입 |
| 예외 타입 | 중 | `IllegalArgumentException` |
| 엔티티 필드 빈 줄 | 낮음 | 어노테이션 필드만 분리 |
| `@Autowired` | 낮음 | 생략 |
| Stream 수집 방식 | 낮음 | `.toList()` |
| `path=` 속성 | 낮음 | 생략 |
| `final` 키워드 | 낮음 | 생략 |
| Validator 주석 유무 | 낮음 | 통일 |
| 동일 목적 주석 문구 | 낮음 | 통일 |

---

## AI 활용 기록

이 문서는 Claude Code를 활용하여 작성되었습니다.
- **활용 방식**: 도메인별로 코드를 순회하며 스타일 불일치 식별
- **분석 범위**: auth, category, member, option, order, product, wish 패키지
