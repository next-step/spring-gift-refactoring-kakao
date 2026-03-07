# Password 일급 객체 도입 트레이드오프 분석

## 배경

BCrypt 비밀번호 해싱을 적용한 이후, "Password를 일급 객체로 만들어 책임을 위임해보는 것도 좋겠다"는 피드백을 받았다. 현재 프로젝트에서 이 접근법이 실제로 적절한지 트레이드오프를 분석한다.

## 현재 비밀번호 처리 경로

비밀번호 인코딩이 발생하는 곳은 **2곳**이다:

| 위치 | 메서드 | 용도 |
|------|--------|------|
| `AuthService` | `register()`, `login()` | 회원가입 시 인코딩, 로그인 시 비교 |
| `MemberService` | `create()`, `update()` | 관리자 회원 생성/수정 시 인코딩 |

```java
// AuthService.register()
new Member(email, passwordEncoder.encode(password))

// AuthService.login()
passwordEncoder.matches(password, member.getPassword())

// MemberService.create()
new Member(email, passwordEncoder.encode(password))

// MemberService.update()
member.update(email, passwordEncoder.encode(password))
```

## 3가지 방식 비교

### 방식 1: String 그대로 사용 (현재)

Service 계층에서 `PasswordEncoder`를 직접 호출하고, `Member`는 `String password`만 보관한다.

```java
// Service
new Member(email, passwordEncoder.encode(password));

// Member
private String password;
```

**장점:**
- 구조가 단순하고 코드량이 적다
- 도메인 모델(`Member`)이 인프라(`PasswordEncoder`)에 전혀 의존하지 않는다
- JPA 매핑이 자연스럽다 (`String` → `VARCHAR`)
- 비밀번호 처리 경로가 2곳뿐이라 중복 부담이 크지 않다

**단점:**
- Service마다 `passwordEncoder.encode(password)` 호출을 잊으면 평문이 저장될 수 있다
- "비밀번호"라는 도메인 개념이 타입으로 드러나지 않는다
- 비밀번호 관련 규칙(길이 검증 등)이 생기면 여러 곳에 흩어질 수 있다

---

### 방식 2: Password 일급 객체 + PasswordEncoder 내장

`Password` 객체가 생성 시점에 `PasswordEncoder`를 받아 스스로 해싱한다.

```java
public class Password {
    private final String encodedValue;

    public Password(String rawPassword, PasswordEncoder encoder) {
        validate(rawPassword);
        this.encodedValue = encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, this.encodedValue);
    }

    private void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
    }
}
```

**장점:**
- 비밀번호 검증·해싱 로직이 한 곳에 응집된다
- Service에서 `encode()` 호출을 깜빡할 수 없다 (생성자에서 강제)
- `matches()` 책임도 Password 객체가 가진다

**단점:**
- **도메인 객체가 인프라(PasswordEncoder)에 의존한다** — `Member → Password → PasswordEncoder` 의존 체인이 형성된다
- JPA `@Embeddable` 매핑 시 `PasswordEncoder`를 주입할 방법이 없다 (DB에서 엔티티를 로드할 때 `PasswordEncoder`를 전달할 수 없음)
- `AttributeConverter`나 `@PostLoad` 같은 우회 방법이 필요해 복잡도가 올라간다
- 테스트할 때마다 `PasswordEncoder` 모킹이 필요하다

---

### 방식 3: Password 일급 객체 (값만 감싸기)

`Password`는 인코딩된 문자열만 감싸고, 인코딩 자체는 Service나 팩토리에서 수행한다.

```java
@Embeddable
public class Password {
    @Column(name = "password")
    private String value;

    protected Password() {}

    // 이미 인코딩된 값으로 생성
    public static Password fromEncoded(String encodedValue) {
        return new Password(encodedValue);
    }

    private Password(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
```

```java
// Service에서 사용
Password password = Password.fromEncoded(passwordEncoder.encode(rawPassword));
new Member(email, password);
```

**장점:**
- 타입 안전성 확보 — `String`과 `Password`를 실수로 혼용할 수 없다
- 도메인 모델이 인프라에 의존하지 않는다
- JPA `@Embeddable` 매핑이 자연스럽다

**단점:**
- `String`을 감싸기만 하는 래퍼 — 추가 행위가 거의 없다
- `encode()` 호출을 잊는 실수는 여전히 Service에서 발생할 수 있다
- 비밀번호 처리 경로가 2곳뿐인 현재 상황에서 실질적 이득이 크지 않다
- 클래스 하나가 더 생기는 코드 복잡도 증가

---

## 비교 요약

| 기준 | 방식 1 (String) | 방식 2 (Password+Encoder) | 방식 3 (값 래퍼) |
|------|:-:|:-:|:-:|
| 구현 복잡도 | 낮음 | 높음 | 중간 |
| 도메인 순수성 | 순수 | 위반 | 순수 |
| 인코딩 누락 방지 | X | O | X |
| JPA 매핑 자연스러움 | O | X | O |
| 타입 안전성 | X | O | O |
| 테스트 용이성 | O | 모킹 필요 | O |

## 현재 프로젝트 상황 분석

1. **비밀번호 처리 경로가 2곳**뿐이다. 중복으로 인한 실수 확률이 낮다.
2. **비밀번호 검증 규칙이 없다.** 길이·복잡도 검증이 필요해지면 일급 객체의 이점이 커지지만, 현재는 그런 요구사항이 없다.
3. **JPA 엔티티 기반 프로젝트**라 `@Embeddable` 매핑이 자연스러운 방식이 유리하다.
4. 방식 2는 도메인 → 인프라 의존 문제와 JPA 매핑 문제가 동시에 발생하여 현재 프로젝트에는 과하다.

## 결론

**현재는 방식 1(String 그대로)을 유지하는 것이 적절하다.**

- 비밀번호 처리 경로가 2곳뿐이므로 일급 객체 도입의 실질적 이득이 작다.
- 방식 2는 JPA 매핑과 도메인 순수성 문제를 동시에 야기하므로 이 프로젝트에는 맞지 않는다.
- 방식 3은 타입 안전성을 제공하지만, 추가 행위 없이 래퍼만 두는 것은 현재 규모에서 과잉 설계다.

**도입을 재검토할 시점:**
- 비밀번호 검증 규칙(길이, 복잡도, 이력 관리 등)이 추가될 때
- 비밀번호를 다루는 경로가 3곳 이상으로 늘어날 때
- 비밀번호 관련 버그(평문 저장 등)가 실제로 발생했을 때

이 시점이 오면 **방식 3(값 래퍼)에 검증 로직을 추가**하는 방향이 가장 현실적이다.
