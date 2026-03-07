# 미사용 Kotlin 의존성 제거 작업 보고서

> 작업 일시: 2026-02-26

## 작업 목적 및 배경

`build.gradle.kts`에 Kotlin 플러그인과 의존성이 포함되어 있었으나, 프로젝트 전체가 Java로만 작성되어 있어 Kotlin 관련 설정이 완전히 불필요했다. Spring Initializr에서 Kotlin 템플릿으로 생성된 초기 설정이 그대로 남아있던 것이다. README.md 2-3항에서 계획된 작업이며, `docs/removal-analysis-unused-kotlin-dependencies.md` 분석 보고서를 기반으로 실행했다.

## 변경 사항 요약

### 수정된 파일

| 파일 | 변경 내용 |
|------|-----------|
| `build.gradle.kts` | Kotlin 플러그인 3개, ktlint 플러그인, Kotlin 의존성 3개, kotlin/allOpen/ktlint 설정 블록 제거. `java` 플러그인 추가. |

### 삭제된 파일/디렉토리

| 대상 | 설명 |
|------|------|
| `src/main/kotlin/` | `.gitkeep`만 존재하던 빈 Kotlin 소스 디렉토리 |
| `src/test/kotlin/` | `.gitkeep`만 존재하던 빈 Kotlin 테스트 디렉토리 |

### build.gradle.kts 제거 항목 상세

- **플러그인**: `kotlin("jvm")`, `kotlin("plugin.spring")`, `kotlin("plugin.jpa")`, `org.jlleitschuh.gradle.ktlint`
- **의존성**: `jackson-module-kotlin`, `kotlin-reflect`, `kotlin-test-junit5`
- **설정 블록**: `kotlin { compilerOptions { ... } }`, `allOpen { ... }`, `ktlint { ... }`
- **추가**: Kotlin 플러그인 제거로 인해 `java` 플러그인을 명시적으로 선언

## 적용된 규칙

- 분석 보고서(`docs/removal-analysis-unused-kotlin-dependencies.md`)의 권장 작업 순서를 따름
- 제거 후 `./gradlew clean build`로 빌드 및 전체 테스트 통과 확인

## 참고 사항

- Kotlin 플러그인이 암묵적으로 `java` 플러그인을 적용해주었으므로, 제거 후 `java` 플러그인을 명시적으로 추가했다.
- 향후 Kotlin 도입이 필요하면 플러그인과 의존성을 다시 추가해야 한다.
