# Plan: 단계 1 — 스타일 정리

## 목표
프로젝트 전반에 흩어진 스타일 불일치를 하나의 기준으로 통일한다.
작동(behavior)은 일체 변경하지 않는다.

## 범위 (수정 파일)

### 1-1. @author/@since Javadoc 제거
일부 파일(auth, member 패키지)에만 존재하는 `@author brian.kim` / `@since 1.0` 블록을 제거한다.
프로젝트 대다수 파일에 Javadoc이 없으므로 "없는 쪽"으로 통일.

- `auth/AuthenticationResolver.java` — 클래스 Javadoc 블록 제거
- `auth/JwtProvider.java` — 클래스 Javadoc 블록 제거
- `auth/TokenResponse.java` — 클래스 Javadoc 블록 제거
- `member/AdminMemberController.java` — 클래스 Javadoc 블록 제거
- `member/Member.java` — 클래스 Javadoc 블록 제거
- `member/MemberController.java` — 클래스 Javadoc 블록 제거
- `member/MemberRequest.java` — 클래스 Javadoc 블록 제거
- `member/MemberRepository.java` — 클래스 Javadoc 블록 제거

### 1-2. 불필요한 @Autowired 제거
생성자가 1개인 클래스에서 Spring이 자동 주입하므로 `@Autowired` 불필요.
프로젝트 대다수 클래스가 `@Autowired` 없이 생성자 주입 → "없는 쪽"으로 통일.

- `auth/AuthenticationResolver.java` — `@Autowired` 제거 + `import` 제거
- `auth/JwtProvider.java` — `@Autowired` 제거 + `import` 제거
- `member/AdminMemberController.java` — `@Autowired` 제거 + `import` 제거
- `member/MemberController.java` — `@Autowired` 제거 + `import` 제거

### 1-3. Entity 필드 빈 줄 패턴 통일
`Member.java`만 필드 사이에 빈 줄이 있음. 다른 엔티티(Product, Option, Order 등)는 빈 줄 없음 → "빈 줄 없는 쪽"으로 통일.

- `member/Member.java` — 필드 사이 빈 줄 제거

### 1-4. Collectors.toList() → .toList() 통일
`OptionController`만 `Collectors.toList()` 사용. 나머지(`CategoryController` 등)는 `.toList()` 사용 → `.toList()`로 통일.

- `option/OptionController.java` — `.collect(Collectors.toList())` → `.toList()`, `import Collectors` 제거

## 제약
- 작동 변경 금지
- 신규 기능 추가 금지
- 로직 변경 없음 (순수 포맷/어노테이션/import 변경만)

## 검증 명령
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew ktlintCheck
```
