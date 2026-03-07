# 2-1. 미사용 @Autowired 제거 작업 보고서

> 작업 일시: 2026-02-26

## 작업 목적 및 배경

- README.md 2-1 항목 "미사용 @Autowired 제거"의 선정된 대안 C를 실행
- Spring 4.3+ 에서 생성자가 1개뿐이면 `@Autowired`는 자동 적용되므로 불필요
- 프로젝트 내 13개 Spring Bean 중 4개만 `@Autowired`를 사용하고 있어 일관성이 깨져 있었음
- 초기 설정 커밋(`55ca9e43`)에서 습관적으로 추가된 코드로 판단

## 변경 사항 요약

### 수정된 파일 (4개)

| 파일 | 제거 내용 |
|------|-----------|
| `src/main/java/gift/auth/JwtProvider.java` | `@Autowired` 어노테이션 + import 제거 |
| `src/main/java/gift/auth/AuthenticationResolver.java` | `@Autowired` 어노테이션 + import 제거 |
| `src/main/java/gift/controller/MemberController.java` | `@Autowired` 어노테이션 + import 제거 |
| `src/main/java/gift/controller/AdminMemberController.java` | `@Autowired` 어노테이션 + import 제거 |

### 주요 변경 내용

- 각 파일에서 `@Autowired` 어노테이션 행 제거
- 각 파일에서 `import org.springframework.beans.factory.annotation.Autowired;` 제거
- 모든 파일이 이미 `final` 필드를 사용하고 있어 어노테이션 제거만으로 충분
- 테스트 코드의 필드 주입용 `@Autowired`는 Spring Boot 테스트 표준 패턴이므로 제거 대상에서 제외

### README.md 업데이트

- 2-1 항목에서 대안 A, B, C를 제거하고 선정된 방안으로 교체

## 적용된 규칙 및 컨벤션

- **Spring 4.3+ 단일 생성자 규칙**: 생성자가 1개인 클래스는 `@Autowired` 없이도 자동으로 의존성 주입
- **구조 변경만 수행**: 작동 변경 없음 (리팩토링)
- **커밋 컨벤션**: `refactor: 미사용 @Autowired 어노테이션 제거` (커밋 `c280382`)

## 검증

- `./gradlew test` 전체 통과 (BUILD SUCCESSFUL)
- 구조 변경만 수행, 작동 변경 없음 확인

## 참고 사항

- 사전 분석 보고서: `docs/analysis/autowired.md`
- 프로젝트 내 나머지 9개 Spring Bean은 이미 `@Autowired` 없이 동작하고 있었으므로, 이번 작업으로 프로젝트 전체의 DI 스타일이 통일됨
