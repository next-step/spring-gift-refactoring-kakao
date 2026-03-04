# 기타 코드 스멜 분석 결과

기존 분석(anti-pattern, style, unreferenced)에서 다루지 않은 코드 스멜을 정리한 문서입니다.

---

## 1. 과도한 접근 범위 (Excessive Visibility)

패키지 내에서만 사용되는 클래스가 `public`으로 선언되어 있습니다.

### 발견 위치

| 파일 | 사용 범위 | 권장 접근 제어자 |
|------|-----------|-----------------|
| `ProductNameValidator.java` | product 패키지 내 | package-private |
| `OptionNameValidator.java` | option 패키지 내 | package-private |
| `KakaoLoginClient.java` | auth 패키지 내 | package-private |
| `KakaoMessageClient.java` | order 패키지 내 | package-private |

### 상세

```java
// 현재 (ProductNameValidator.java:7)
public class ProductNameValidator {

// 권장
class ProductNameValidator {  // package-private
```

**사용처 확인**:
- `ProductNameValidator`: `ProductController`, `AdminProductController` (모두 product 패키지)
- `OptionNameValidator`: `OptionController` (option 패키지)
- `KakaoLoginClient`: `KakaoAuthController` (auth 패키지)
- `KakaoMessageClient`: `OrderController` (order 패키지)

### 권장 조치

패키지 외부에서 사용할 계획이 없다면 package-private으로 변경하여 캡슐화를 강화합니다.

---

## 2. 내부 가변 컬렉션 노출 (Mutable Collection Exposure)

내부 가변 컬렉션을 직접 반환하여 캡슐화가 깨질 수 있습니다.

### 발견 위치

| 파일 | 라인 | 코드 |
|------|------|------|
| `Product.java` | 70-72 | `return options;` |

### 상세

```java
// 현재 (Product.java:70-72)
public List<Option> getOptions() {
    return options;  // 내부 리스트 직접 반환
}

// 권장
public List<Option> getOptions() {
    return Collections.unmodifiableList(options);
}
// 또는
public List<Option> getOptions() {
    return List.copyOf(options);
}
```

외부에서 `product.getOptions().clear()` 등을 호출하면 내부 상태가 변경됩니다.

### 권장 조치

- 읽기 전용 뷰 반환: `Collections.unmodifiableList()`
- 또는 방어적 복사: `List.copyOf()`
- JPA lazy loading을 고려하면 `unmodifiableList`가 적합

---

## 3. 하드코딩된 외부 API URL

외부 API URL이 코드에 하드코딩되어 있습니다.

### 발견 위치

| 파일 | 라인 | URL |
|------|------|-----|
| `KakaoLoginClient.java` | 28 | `https://kauth.kakao.com/oauth/token` |
| `KakaoLoginClient.java` | 37 | `https://kapi.kakao.com/v2/user/me` |
| `KakaoAuthController.java` | 42 | `https://kauth.kakao.com/oauth/authorize` |
| `KakaoMessageClient.java` | 23 | `https://kapi.kakao.com/v2/api/talk/memo/default/send` |

### 상세

```java
// 현재 (KakaoLoginClient.java:28)
.uri("https://kauth.kakao.com/oauth/token")

// 권장
@Value("${kakao.api.token-url}")
private String tokenUrl;
// ...
.uri(tokenUrl)
```

### 권장 조치

- **낮은 우선순위**: Kakao API URL은 안정적이므로 하드코딩도 허용 가능
- 설정 외부화 시 테스트/스테이징 환경 대응 용이

---

## 요약

| 코드 스멜 | 발생 횟수 | 심각도 |
|-----------|-----------|--------|
| 과도한 접근 범위 | 4건 | 낮음 |
| 가변 컬렉션 노출 | 1건 | 중 |
| 하드코딩된 URL | 4건 | 낮음 |

### 우선순위

1. **가변 컬렉션 노출** - 캡슐화 위반, 의도치 않은 상태 변경 가능
2. **과도한 접근 범위** - 캡슐화 약화, API 표면 증가
3. **하드코딩된 URL** - 안정적인 외부 API이므로 낮은 우선순위

---

## AI 활용 기록

이 문서는 Claude Code를 활용하여 작성되었습니다.
- **활용 방식**: 접근 제어자, 불변성, 하드코딩 패턴 탐색
- **검증 방법**: Grep으로 사용처 확인, 패키지 경계 분석
- **분석 범위**: auth, category, member, option, order, product, wish 패키지
