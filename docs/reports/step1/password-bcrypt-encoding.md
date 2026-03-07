# 비밀번호 BCrypt 해싱 적용

## 작업 목적 및 배경

기존 코드에서 비밀번호가 평문(plain text)으로 저장되고 `String.equals()`로 비교되고 있었다.
이는 DB가 유출될 경우 모든 사용자의 비밀번호가 노출되는 보안 취약점이므로, BCrypt 해싱을 적용하여 안전하게 저장/비교하도록 수정했다.

## 변경 사항 요약

### 1. 의존성 추가
- **`build.gradle.kts`**: `spring-security-crypto` 의존성 추가
  - `spring-boot-starter-security` 대신 crypto만 추가하여 Security Filter Chain 자동 구성을 방지
- **`settings.gradle.kts`**: Gradle 툴체인 자동 다운로드를 위한 `foojay-resolver-convention` 플러그인 추가

### 2. Bean 등록
- **`src/main/java/gift/config/PasswordEncoderConfig.java`** (신규 생성)
  - `BCryptPasswordEncoder`를 `PasswordEncoder` Bean으로 등록

### 3. 서비스 코드 수정
- **`src/main/java/gift/service/AuthService.java`**
  - `register()`: `passwordEncoder.encode(password)`로 해싱 후 저장
  - `login()`: `passwordEncoder.matches(password, member.getPassword())`로 비교
- **`src/main/java/gift/service/MemberService.java`**
  - `create()`: 해싱 후 저장
  - `update()`: 해싱 후 저장

### 4. 데이터 수정
- **`src/main/resources/db/migration/V2__Insert_default_data.sql`**: 기본 데이터의 평문 비밀번호를 BCrypt 해시로 변경
  - `admin1234`, `password1`, `password2` -> BCrypt 해시 (`$2a$10$...`)
- **`src/test/resources/sql/member-data.sql`**: 테스트 데이터의 평문 비밀번호를 BCrypt 해시로 변경

### 5. 테스트 코드 수정
- **`src/test/java/gift/controller/MemberControllerTest.java`**
  - `PasswordEncoder` 주입 추가
  - `memberRepository.save()` 직접 호출 시 `passwordEncoder.encode()`로 비밀번호 해싱

## 적용된 규칙

| 항목 | 적용 내용 |
|------|-----------|
| 해싱 알고리즘 | BCrypt (cost factor 10, 기본값) |
| 비밀번호 저장 | `passwordEncoder.encode()` 사용 |
| 비밀번호 비교 | `passwordEncoder.matches()` 사용 (timing-attack safe) |
| 의존성 범위 | `spring-security-crypto`만 사용 (전체 Spring Security 미사용) |

## 참고 사항

- `Member` 엔티티의 `password` 필드는 `varchar(255)`이므로 BCrypt 해시(60자)를 저장하기에 충분하다.
- 카카오 로그인 사용자(`password == null`)는 기존과 동일하게 비밀번호 로그인이 차단된다.
- 운영 환경에서 기존 평문 비밀번호가 있는 경우, 별도의 데이터 마이그레이션이 필요하다.
