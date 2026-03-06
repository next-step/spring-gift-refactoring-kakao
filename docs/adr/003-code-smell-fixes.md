# ADR-003: 코드 스멜 수정 전략

## 상태
- [x] 승인됨 (2026-02)

## 맥락
코드 분석 단계에서 식별된 코드 스멜을 수정해야 한다. 수정 시 작동 변경을 최소화하면서 코드 품질을 개선해야 한다.

## 수정 항목

### Anti-pattern 수정

| 항목 | 수정 방식 | 비고 |
|------|----------|------|
| `orElse(null)` 후 null 체크 | Optional chain (map/filter) | AuthenticationResolver 제외 |
| Exception Swallowing | 최소 로깅 추가 (debug/warn) | |
| `ResponseEntity<?>` 와일드카드 | 서비스 추출 시 해결됨 | |
| 매직 넘버 HTTP 상태 코드 | `HttpStatus` 상수 사용 | |

### 스타일 통일

| 항목 | 수정 방식 |
|------|----------|
| 에러 메시지 | 한국어 통일 |
| `@Autowired` | 생략 (단일 생성자 자동 주입) |
| `var` | 명시적 타입 선언 |
| `path=` 속성 | 생략으로 통일 |

### 기타

| 항목 | 수정 방식 |
|------|----------|
| 가변 컬렉션 노출 | `Product.getOptions()` 삭제로 해결 |
| 과도한 접근 범위 | package-private으로 변경 |
| 미참조 코드 | 삭제 (`Order.getMemberId()`, `Product.getOptions()`) |

## 제외 사항

### ~~AuthenticationResolver.extractMember()~~ → 해결됨

~~제외 사유: 작동 변경에 해당~~

**Step 2에서 해결됨** - [ADR-006](006-domain-responsibility.md) 참조
- `Optional<Member>` 반환으로 변경
- OrderController, WishController Optional 체이닝으로 수정

### 하드코딩된 Kakao API URL
**제외 사유:**
- 낮은 우선순위 (Kakao API URL은 안정적)
- 설정 외부화의 실질적 이점 제한적

## 결과
- 모든 수정 후 75개 테스트 통과
- 코드 가독성 및 일관성 개선

## 관련 문서
- [분석 문서](../archive/analysis/)

## 관련 ADR
- [ADR-001: 리팩토링 전략](001-refactoring-strategy.md)
- [ADR-002: 서비스 계층 추출](002-service-layer-extraction.md)
