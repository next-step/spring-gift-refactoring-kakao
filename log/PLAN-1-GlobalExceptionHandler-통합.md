# PLAN 1: GlobalExceptionHandler 통합 + Controller try-catch 제거

## SessionID: 5d21b45c-1886-40bc-a3b9-19da94f222d0

### 세션 정보
- 목적: PLAN 1번 — GlobalExceptionHandler에 NoSuchElementException 핸들러 통합, Controller try-catch 제거
- 범위: GlobalExceptionHandler, ProductController, OptionController, CategoryController, WishController, OrderController
- 근거 소스: 커밋 `e45717a`, `./gradlew test` 전체 통과

### 프롬프트 및 결과
| ID | 목적 | 프롬프트 원문(마스킹) | 기대 결과 | 실제 결과 | 판단(채택/기각/보류) | 이유(재현성/유지보수성/검증력/비용) | 근거 |
|----|------|------------------------|-----------|-----------|----------------------|--------------------------------------|------|
| B1 | PLAN 1번 구현 | "자 이제부터, PLAN에서 1번에 대한 항목에 대한 리팩토링을 해보자" | GlobalExceptionHandler 통합 + try-catch 제거 | 6개 파일 수정, 10개 try-catch 제거, 테스트 전체 통과 | 채택 | 구조 변경만 수행, 외부 응답 동일 유지, 테스트 검증 완료 | 커밋 `e45717a` |
| B2 | 영향 범위 분석 | "영향범위와 확장성 측면에서 괜찮은지" | Admin Controller 영향 분석 | @RestControllerAdvice가 Admin @Controller에도 적용되는 문제 식별 | 채택 | Admin에서 NoSuchElementException 발생 시 JSON 404 반환 → HTML 기대와 불일치 | 코드 분석 |
| B3 | 커스텀 예외 설명 | "커스텀 예외 도입은 어떻게 하는데?" | 커스텀 예외 개념 설명 | ResourceNotFoundException 도입 방법 설명, PLAN 5번에서 도입 예정으로 판단 | 보류 | PLAN 5번(에러 코드 수정)에서 함께 도입하는 것이 자연스러움 | 대화 내용 |
| B4 | Admin 영향 차단 | "1번 방식으로 해서 적용해줘" (annotations = RestController.class) | Admin Controller에 영향 없도록 제한 | @RestControllerAdvice(annotations = RestController.class) 적용, 테스트 통과 | 채택 | Admin Controller 격리, 확장성 확보, 비용 최소(1줄 변경) | 커밋 `e45717a` |

### 접근법 요약
| 접근법 | 적용 범위 | 결과 | 유지 여부 | 메모 |
|--------|-----------|------|-----------|------|
| GlobalExceptionHandler에 NoSuchElementException 핸들러 추가 | REST API 전체 | 성공 | 유지 | 10개 try-catch → 1개 핸들러로 통합 |
| annotations = RestController.class로 적용 범위 제한 | GlobalExceptionHandler | 성공 | 유지 | Admin @Controller에 영향 차단 |
| WishController의 IllegalStateException catch 유지 | WishController.removeWish | 성공 | 유지 | NoSuchElementException catch만 제거, IllegalStateException → 403은 유지 |

### 최종 가이드
- 재사용 프롬프트:
  - "영향범위와 확장성 측면에서 괜찮은지" → 변경 전 영향 분석 유도에 효과적
- 주의점:
  - @RestControllerAdvice는 기본적으로 모든 Controller에 적용됨 → annotations 파라미터로 범위 제한 필수
  - NoSuchElementException은 Java 표준 예외라 의도하지 않은 곳에서도 발생 가능 → PLAN 5번에서 커스텀 예외 도입으로 해결 예정
- 다음 세션 TODO:
  - PLAN 2번 구현: Order 도메인에 총 가격 계산 책임 이동
