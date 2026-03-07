# CLAUDE.md

## 문서 디렉토리 구조

```
docs/
├── adr/         # Architecture Decision Records (트레이드오프가 있는 설계 결정)
├── analysis/    # 분석 보고서 (리팩토링/삭제 전 분석)
├── reports/     # 작업 보고서 (완료된 작업 결과, step별 하위 폴더)
│   ├── step1/   # Step 1 작업 보고서
│   └── step2/   # Step 2 작업 보고서
├── plan/        # 계획 및 요구사항 (REQUIREMENT.md, TODO.md)
└── log/         # 프롬프트 기록 (PROMPT.md)
```

- 파일명 규칙: 도메인 기반 이름 사용 (예: `category-service.md`, `kotlin-dependencies.md`)

## 프롬프트 기록 규칙

- 사용자가 프롬프트를 입력할 때마다, 해당 프롬프트 내용을 `docs/log/PROMPT.md` 파일에 기록한다.
- 기록 형식: 각 프롬프트를 코드 블록(```)으로 감싸서 순서대로 추가한다.
- 이미 기록된 프롬프트는 수정하지 않고, 새로운 프롬프트만 아래에 추가한다.

## 작업 문서화 규칙

- 큰 단위의 작업(리팩토링, 기능 추가, 아키텍처 변경 등)을 완료하면 별도의 `.md` 파일로 문서화한다.
- 문서 파일명: `docs/reports/` 디렉토리 아래에 작업 내용을 나타내는 이름으로 생성한다. (예: `docs/reports/style-refactoring.md`)
- 문서에 포함할 내용:
  - 작업 목적 및 배경
  - 변경 사항 요약 (수정된 파일, 주요 변경 내용)
  - 적용된 규칙이나 컨벤션
  - 참고 사항
