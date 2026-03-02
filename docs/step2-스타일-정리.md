# 2단계: 스타일 정리 — 의사결정 보고서

## 목적

프로젝트 전반의 스타일 불일치를 자동 포매터로 일관되게 정리한다.
로직 변경 없이 포매팅만 변경한다.

## 발견된 스타일 불일치

| 항목 | 현상 |
|---|---|
| import 순서 | Auth/Member 모듈과 Product/Category 모듈의 import 순서가 다름 |
| 들여쓰기 | 메서드 체이닝의 continuation indent가 파일마다 다름 (4칸 vs 8칸) |
| 빈 줄 | 클래스 선언 후 빈 줄, 필드 사이 빈 줄 등이 불일치 |
| 미사용 import | 일부 파일에 미사용 import 존재 |

## 포매터 선정: Spotless

### 대안 비교

| 기준 | Spotless | Checkstyle | IDE 포매터 |
|---|---|---|---|
| 자동 포매팅 | O (`spotlessApply`) | X (검사만) | O (수동) |
| CI 강제 | O (`spotlessCheck`) | O | X |
| 재현 가능성 | O (버전 고정) | O | X (IDE 설정 의존) |
| Gradle 통합 | 네이티브 플러그인 | 플러그인 | 해당 없음 |

### 선정 사유

**Spotless**를 선정했다. 자동 수정(`spotlessApply`) + CI 강제(`spotlessCheck`) + Gradle 네이티브 통합을 모두 지원하는 유일한 옵션이다.

- **Checkstyle 제외**: 위반을 검출하지만 자동 수정하지 못한다. 개발자가 수동으로 고쳐야 하므로 생산성이 떨어진다.
- **IDE 포매터 제외**: 개발자마다 IDE 설정이 다를 수 있어 재현 가능성이 없다. CI에서 강제할 수 없다.

### 포매팅 엔진: palantir-java-format

| 기준 | palantir-java-format | google-java-format |
|---|---|---|
| 들여쓰기 | 4칸 | 2칸 |
| 줄 너비 | 120자 | 100자 |

현재 코드베이스가 4칸 들여쓰기를 사용하므로 **palantir**(4칸)을 선택하여 diff를 최소화했다.

## 적용 범위

- `src/main/java/**/*.java` — 프로덕션 Java 소스
- `src/test/java/**/*.java` — 테스트 Java 소스
- `removeUnusedImports()` — 미사용 import 자동 제거

### 범위 외 (이 단계에서 수정하지 않음)

- `final` 키워드 일관성 (의미적 변경)
- `@Autowired` 어노테이션 유무 (의미적 변경)
- `var` vs 명시적 타입 (의미적 변경)
- 에러 메시지 언어 혼용 (내용 변경)
- 예외 타입 불일치 (동작 변경)

## 적용 결과

- 변경 파일: Java 소스 50개 + 설정/문서 4개
- `spotlessCheck` — BUILD SUCCESSFUL
- `test` — BUILD SUCCESSFUL (단위 테스트 전체 통과)
- `cucumberTest` — BUILD SUCCESSFUL (인수 테스트 17개 시나리오 전체 통과)
