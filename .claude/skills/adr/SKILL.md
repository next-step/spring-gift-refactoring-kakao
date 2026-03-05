---
name: adr
description: 프로젝트의 ADR(Architecture Decision Record) 문서를 생성, 수정, 관리하는 스킬
allowed-tools: Bash, Read, Write, Edit, Glob, Grep
---

프로젝트의 아키텍처 결정사항을 `./docs/adr/` 디렉토리에 마크다운 파일로 기록하고 관리한다.

## 활성화 조건

사용자가 다음과 같은 요청을 할 때 이 스킬을 실행:
- "ADR 작성해줘" / "ADR 추가해줘"
- "아키텍처 결정 기록해줘"
- "ADR 목록 보여줘" / "ADR 상태 변경해줘"
- "이 결정 기록으로 남겨줘"

## 핵심 원칙

- **변경 불가(Immutable):** 한 번 작성된 ADR의 Context, Decision, Consequences는 수정하지 않는다.
- **상태만 변경 가능:** Status 필드만 예외적으로 변경할 수 있다 (예: `Proposed` → `Accepted`).
- **대체(Supersede):** 결정이 바뀌면 기존 ADR의 상태를 `Superseded by ADR NNN`으로 변경하고, 새 ADR을 작성한다.
- **가볍게 작성:** 한두 페이지 내외로 핵심만 기록한다.
- **한국어 작성:** 모든 내용은 한국어로 작성한다.

## 파일 명명 규칙

```
./docs/adr/NNN-<제목>.md
```

- 번호는 3자리 zero-padding, 순차 증가 (예: `001`, `002`, `003`)
- 제목은 한글 또는 영어 소문자, 하이픈(`-`)으로 연결
- 예시: `001-배포-환경-선택.md`, `002-인증-방식-결정.md`

## 문서 템플릿

```markdown
# ADR NNN: <제목>

> 작성일: YYYY-MM-DD
> 상태: Proposed

## Context (맥락)

[어떤 문제 상황이었는지, 어떤 제약 사항이 있었는지 설명]

## Decision (결정)

[최종적으로 무엇을 하기로 결정했는지 기술]

## Consequences (결과)

### Pros
- [이점 1]

### Cons
- [단점 1]
```

### 상태(Status) 종류

| 상태 | 설명 |
|------|------|
| `Proposed` | 제안됨 (초기 상태) |
| `Accepted` | 승인됨 (적용 중) |
| `Deprecated` | 폐기됨 (더 이상 유효하지 않음) |
| `Superseded by ADR NNN` | 다른 ADR로 대체됨 |

## 수행 절차

### 1. ADR 생성

1. `./docs/adr/` 디렉토리 존재 여부를 확인하고, 없으면 생성한다.
2. 기존 ADR 파일 목록을 확인하여 다음 번호를 결정한다.
3. 사용자와 대화하여 Context, Decision, Consequences 내용을 파악한다.
4. 템플릿에 맞춰 파일을 생성한다.
5. 생성된 파일 내용을 사용자에게 보여주고 확인을 받는다.

### 2. ADR 상태 변경

1. 대상 ADR 파일을 읽는다.
2. `> 상태:` 줄만 새 상태로 변경한다.
3. **Context, Decision, Consequences 내용은 절대 수정하지 않는다.**

### 3. ADR 대체 (Supersede)

1. 기존 ADR의 상태를 `Superseded by ADR NNN`으로 변경한다.
2. 새 ADR을 생성하며, Context에 기존 ADR 번호를 참조한다.

### 4. ADR 조회

1. `./docs/adr/` 내 모든 파일을 읽는다.
2. 번호, 제목, 상태를 표로 요약하여 보여준다.

## 주의사항

- `./docs/adr/` 디렉토리가 없으면 먼저 생성한다.
- 현재 날짜는 `date` 명령어로 확인한다.
- 기존 ADR의 본문(Context, Decision, Consequences)은 어떤 경우에도 수정하지 않는다.
- ADR 번호가 이미 존재하는 번호와 겹치지 않도록 확인한다.
