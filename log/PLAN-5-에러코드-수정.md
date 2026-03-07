# PLAN 5: 에러 코드 수정

## SessionID: 5d21b45c-1886-40bc-a3b9-19da94f222d0

### 세션 정보
- 목적: REST API 상태 코드 정정 (Part A) + Admin Controller 예외 처리 추가 (Part B)
- 범위: GlobalExceptionHandler, MemberService, OptionService, AdminMemberController, AdminProductController
- 근거 소스: 커밋 `474efa9`~`b729162`, `./gradlew test` 전체 통과

### 프롬프트 및 결과
| ID | 목적 | 프롬프트 원문(마스킹) | 기대 결과 | 실제 결과 | 판단(채택/기각/보류) | 이유(재현성/유지보수성/검증력/비용) | 근거 |
|----|------|------------------------|-----------|-----------|----------------------|--------------------------------------|------|
| C1 | Part A 플랜 | "PARTA부터 보자. 어떻게 할건지 구체적으로 말해봐. 기존에 있었던 공용 에러 핸들러와의 적용범위는?" | 커스텀 예외 도입 계획 | RuntimeException 직접 상속, IllegalArgumentException은 catch-all로 유지하는 설계 제시 | 채택 | 독립적 매핑으로 우선순위 혼란 방지 | 대화 내용 |
| C2 | Part A 구현 | "PART A만 적용해서 커밋 나눠서 진행해" | 4개 커밋으로 분리 구현 | 커스텀 예외 생성 → 핸들러 추가 → 예외 타입 교체 → 테스트 수정, 전체 통과 | 채택 | 단계적 적용으로 각 커밋이 독립적으로 revert 가능 | 커밋 `474efa9`~`f190a16` |
| C3 | 커밋 메시지 검토 | "커밋 메시지 fix가 맞아?" / "커밋 스킬 보고 판단해봐" | 올바른 prefix 판별 | 예외 타입 교체(작동 변경)는 `refactor`가 아니라 `fix` | 채택 | logical-commit 스킬 기준: 예외 타입 변경 = 작동 변경 → refactor 불가 | 스킬 문서 |
| C4 | Part B 플랜 | "PART B 플랜 세워줘" | Admin 예외 처리 방안 | try-catch 방식 채택 (ControllerAdvice 방식은 리다이렉트 경로 구분 불가) | 채택 | Admin Controller 2개뿐, 메서드별 리다이렉트 경로가 다름 | 코드 분석 |
| C5 | Part B 커밋 분리 | "커밋 메시지 fix가 맞아?" | feat/fix 분리 필요 식별 | NoSuchElementException(미구현→feat) + IllegalArgumentException(PLAN1에서 깨짐→fix) 분리 | 채택 | logical-commit 스킬: 서로 다른 이유의 변경은 분리 | 스킬 문서 |

### 접근법 요약
| 접근법 | 적용 범위 | 결과 | 유지 여부 | 메모 |
|--------|-----------|------|-----------|------|
| 커스텀 예외(RuntimeException 상속) + GlobalExceptionHandler 매핑 | REST API | 성공 | 유지 | IllegalArgumentException과 독립적으로 동작 |
| Admin Controller에 try-catch 직접 추가 | Admin | 성공 | 유지 | ControllerAdvice보다 리다이렉트 경로 제어에 유리 |
| 커밋 prefix를 logical-commit 스킬 기준으로 검증 | 전체 | 성공 | 유지 | 작동 변경 판별 체크리스트 활용 |

### 최종 가이드
- 재사용 프롬프트:
  - "커밋 스킬 보고 판단해봐" → prefix 판별에 스킬 문서 기준 적용 유도
  - "기존에 있었던 공용 에러 핸들러와의 적용범위는?" → 변경 영향 분석
- 주의점:
  - `refactor`는 외부 동작이 동일할 때만 사용 (예외 타입 변경 = 작동 변경 → fix)
  - feat과 fix가 같은 파일에 섞이면 커밋을 쪼개야 함
  - PLAN 1에서 Admin 제외하면서 IllegalArgumentException 처리가 악화된 부작용 발생 — 구조 변경 시 부작용 추적 필요
- 다음 세션 TODO:
  - PLAN 6번 구현: 카카오 알림을 트랜잭션 밖으로 분리
