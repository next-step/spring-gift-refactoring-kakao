# spring-gift-refactoring

레거시 코드를 안전하게 리팩토링하는 프로젝트입니다.

## 목표

**작동을 바꾸기 쉬운 상태를 만드는 것** - 구조 변경을 통해 변경 난이도를 낮추되 작동은 유지합니다.

---

## 기술 스택

- Java 21
- Spring Boot 3.5.9
- Gradle 8.14
- H2 Database
- JPA
- RestAssured (테스트)

---

## 실행 방법

```bash
# 테스트 실행
./gradlew test

# 애플리케이션 실행
./gradlew bootRun
```

---

## 문서

| 문서 | 설명 |
|------|------|
| [1단계 리팩토링 전략](docs/리팩토링-전략.md) | 4단계 접근법, 완료된 작업 체크리스트 |
| [2단계 리팩토링 전략](docs/step2/리팩토링-전략.md) | 5단계 접근법, 트랜잭션·서비스 추출·자동 검증 |
| [2단계 ADR](docs/step2/ADR.md) | 아키텍처 결정 기록 8개 |
| [2단계 테스트 전략](docs/step2/테스트-전략.md) | Phase별 테스트 먼저 접근법 |
| [자동 검증 전략](docs/step2/자동검증-전략.md) | Spotless + ArchUnit 도입 전략 |
| [AI 활용 기록](docs/AI-활용-기록.md) | 단계별 AI 활용 방식과 학습 내용 |
| [프로젝트 구조](docs/PROJECT_STRUCTURE.md) | 패키지 및 클래스 구조 |
| [테스트 전략](docs/TEST_STRATEGY.md) | 인수 테스트 설계 방침 |
| [Anti-pattern 분석](docs/anti-pattern.md) | 식별된 안티패턴과 수정 방안 |
| [스타일 분석](docs/style.md) | 코드 스타일 불일치 분석 |
| [미참조 코드 분석](docs/unreferenced.md) | 사용되지 않는 코드 분석 |
| [기타 코드 스멜](docs/etc.md) | 기타 코드 스멜 분석 |
