# 3단계: 불필요한 코드 제거 — 3-1. 미사용 코드 분석

## 목적

프로덕션 코드 전체를 정적 분석하여 미사용 항목을 식별하고 주석-동작 불일치를 파악한다.
이 보고서는 3-2 단계(제거)의 근거 자료로 사용된다.

## 분석 범위

- Java 소스 45개 파일 (`src/main/java/**/*.java`)
- 빌드 설정 (`build.gradle.kts`)
- 교차 참조 분석: 각 항목이 프로덕션/테스트 코드에서 실제로 참조되는지 확인

## 식별된 미사용 항목

### A. 미사용 빌드 설정 (Kotlin 관련)

프로젝트에 `.kt` 파일이 단 하나도 없음에도 Kotlin 관련 설정이 광범위하게 존재한다.

| # | 항목 | 파일 | 위치 | 판정 |
|---|------|------|------|------|
| 1 | `kotlin("jvm")` 플러그인 | `build.gradle.kts` | line 2 | 삭제 |
| 2 | `kotlin("plugin.spring")` 플러그인 | `build.gradle.kts` | line 3 | 삭제 |
| 3 | `kotlin("plugin.jpa")` 플러그인 | `build.gradle.kts` | line 4 | 삭제 |
| 4 | `jackson-module-kotlin` 의존성 | `build.gradle.kts` | line 31 | 삭제 |
| 5 | `kotlin-reflect` 의존성 | `build.gradle.kts` | line 32 | 삭제 |
| 6 | `kotlin-test-junit5` 의존성 | `build.gradle.kts` | line 42 | 삭제 |
| 7 | `kotlin { compilerOptions { ... } }` 블록 | `build.gradle.kts` | lines 51-55 | 삭제 |
| 8 | `allOpen { ... }` 블록 | `build.gradle.kts` | lines 57-61 | 삭제 |
| 9 | `ktlint { ... }` 블록 + 플러그인 | `build.gradle.kts` | line 7, lines 63-65 | 삭제 |

**근거**: 프로젝트 전체에 Kotlin 소스 파일(`.kt`)이 없다. Kotlin 플러그인, 의존성, 컴파일러 설정, ktlint 린터가 모두 불필요하다.

**4단계 충돌 여부**: 없음. 서비스 계층 추출은 Java로 수행한다.

### B. 미사용 필드/주입

| # | 항목 | 파일 | 위치 | 판정 |
|---|------|------|------|------|
| 11 | `WishRepository wishRepository` | `OrderController.java` | lines 25, 33, 39 | 삭제 |

**현재 상태**: 생성자에서 주입받지만 클래스 내 어떤 메서드에서도 호출하지 않는다.

**근거**: `OrderController`의 주문 생성 흐름에서 6단계(위시 정리)가 미구현 상태이며, 해당 필드는 미구현 로직의 흔적이다. 현재 동작에 영향을 주지 않으므로 제거해도 안전하다.

**4단계 충돌 여부**: 4단계에서 OrderService 추출 시 위시 정리 로직이 필요하면 그때 다시 주입하면 된다. 미사용 필드를 남겨두는 것보다 필요 시 추가하는 것이 깔끔하다.

### C. 주석-동작 불일치

| # | 항목 | 파일 | 위치 | 판정 |
|---|------|------|------|------|
| 12 | `// 6. cleanup wish` 주석 | `OrderController.java` | line 62 | TODO 주석으로 변경 |

**현재 상태**: 주문 생성 메서드에 7단계 흐름이 주석으로 기술되어 있으나, 6단계(위시 정리)에 해당하는 코드가 없다. `wishRepository`가 주입만 되고 사용되지 않는 것(#11)과 직접 연관된다.

**근거**: 구현되지 않은 동작을 기술하는 주석은 코드를 읽는 사람을 오도할 수 있다. 미구현 로직임을 명시하기 위해 `// TODO: cleanup wish` 형태로 변경하여 향후 구현이 필요함을 표시한다.

**4단계 충돌 여부**: 없음. 4단계에서 위시 정리 로직을 구현할 때 TODO를 해소하면 된다.

### D. 불필요한 어노테이션

| # | 항목 | 파일 | 위치 | 판정 |
|---|------|------|------|------|
| 13 | `@Autowired` (생성자) | `JwtProvider.java` | line 22 | 삭제 |
| 14 | `@Autowired` (생성자) | `AuthenticationResolver.java` | line 19 | 삭제 |
| 15 | `@Autowired` (생성자) | `MemberController.java` | line 27 | 삭제 |
| 16 | `@Autowired` (생성자) | `AdminMemberController.java` | line 23 | 삭제 |

**근거**: Spring 4.3+에서 생성자가 하나뿐인 클래스는 `@Autowired` 없이도 자동 주입된다. 프로젝트의 다른 클래스들(`OrderController`, `ProductController`, `CategoryController`, `OptionController`, `WishController`, `KakaoAuthController`, `KakaoLoginClient`)은 이미 `@Autowired` 없이 생성자 주입을 사용하고 있다. 일관성을 위해 제거한다.

**4단계 충돌 여부**: 없음. 동작이 동일하므로 서비스 추출에 영향 없다.

### E. 불필요한 import

| # | 항목 | 파일 | 위치 | 판정 |
|---|------|------|------|------|
| 17 | `import java.util.stream.Collectors` | `OptionController.java` | line 8 | 삭제 |

**현재 상태**: `.collect(Collectors.toList())`를 사용 중이다.

**근거**: Java 16+에서 `.toList()`로 대체 가능하며, 이 프로젝트는 Java 21을 사용한다. `.collect(Collectors.toList())` → `.toList()`로 변경하면 import가 불필요해진다. 반환 타입은 동일하게 `List<T>`이며, 동작 변경 없다.

**4단계 충돌 여부**: 없음.

## 보류 항목 (삭제하지 않음)

| 항목 | 파일 | 보류 사유 |
|------|------|-----------|
| `Product.getOptions()` | `Product.java` line 69 | 프로덕션/테스트 코드에서 직접 호출 없으나, JPA `@OneToMany` 필드의 getter이며 4단계(서비스 추출)에서 사용 예상 |
| `@author`/`@since` Javadoc | member/auth 패키지 일부 | 일부 클래스에만 존재하는 불일치이나, 미사용 코드가 아니라 스타일 문제 |

## 요약

| 카테고리                  | 건수 | 판정                           |
|-----------------------|------|------------------------------|
| A. 미사용 빌드 설정 (Kotlin) | 9 | 전체 삭제                        |
| B. 미사용 필드/주입          | 1 | 삭제                           |
| C. 주석-동작 불일치          | 1 | TODO 주석으로 변경                 |
| D. 불필요한 어노테이션         | 4 | 삭제                           |
| E. 불필요한 import        | 1 | 삭제                           |
| F. 보류                 | 2 | 보류                           |
| **합계**                | **19** | **삭제 15 / TODO 변경 1 / 보류 2** |
