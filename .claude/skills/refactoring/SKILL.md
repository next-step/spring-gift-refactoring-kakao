---
name: refactoring
description: 리팩터링 전체 흐름 오버뷰. 사용자가 "리팩터링", "리팩터링 순서", "전체 흐름" 등을 언급하면 이 스킬을 사용한다. 각 Phase의 상세 절차는 Phase별 스킬을 참조한다.
---

# 리팩터링 절차 가이드

리팩터링은 **외부 동작을 변경하지 않으면서** 코드 내부 구조를 개선하는 작업이다.
모든 리팩터링은 인수테스트가 통과하는 상태에서 시작하고, 통과하는 상태로 끝난다.

---

## 핵심 원칙

1. **구조 변경과 동작 변경을 절대 섞지 않는다** — 한 커밋에 하나의 관심사만
2. **한 번에 한 파일, 한 가지 변경** — 대량 변경 금지
3. **매 커밋마다 테스트 통과** — `./gradlew test` 확인
4. **README.md 먼저 업데이트** — 작업 전 체크리스트 정의, 작업 후 체크

---

## 전체 흐름

```
Phase 0: 인수테스트 작성 → 안전망 확보         (/acceptance-test)
         ↓
Phase 1: 스타일 정리 → 가독성 확보             (/refactor-style)
         ↓ (./gradlew test 통과)
Phase 2: 미사용 코드 제거 → 불필요한 코드 정리  (/refactor-cleanup)
         ↓ (./gradlew test 통과)
Phase 3: Service Layer 추출 → 계층 분리 완료    (/refactor-service)
         ↓ (./gradlew test 통과)
Phase 4: 에러 처리 구조화 → 예외 체계 통합      (/setup-error-handling)
         ↓ (./gradlew test 통과)
       완료
```

각 Phase 사이에 반드시 **테스트 전체 통과**를 확인한다.
Phase를 건너뛰거나 순서를 바꾸지 않는다.
각 Phase의 상세 절차는 괄호 안의 스킬을 참조한다.
