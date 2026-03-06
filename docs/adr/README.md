# Architecture Decision Records (ADR)

프로젝트의 주요 기술적 의사결정을 기록합니다.

## 목록

| ADR | 제목 | 상태 |
|-----|------|------|
| [001](001-refactoring-strategy.md) | 리팩토링 전략 | 승인됨 |
| [002](002-service-layer-extraction.md) | 서비스 계층 추출 전략 | 승인됨 |
| [003](003-code-smell-fixes.md) | 코드 스멜 수정 전략 | 승인됨 |
| [004](004-transaction-boundary.md) | 트랜잭션 경계 설정 | 승인됨 |
| [005](005-wishlist-cleanup-on-order.md) | 주문 완료 시 위시리스트 정리 | 승인됨 |
| [006](006-domain-responsibility.md) | 도메인 책임 되찾기 | 승인됨 |
| [007](007-controller-readability.md) | Controller 가독성 개선 | 승인됨 |
| [008](008-boundary-value-tests.md) | 경계값 테스트 추가 | 승인됨 |
| [009](009-wishlist-edge-case-test.md) | 위시리스트 엣지 케이스 테스트 | 승인됨 |
| [010](010-readonly-transactions.md) | 읽기 전용 트랜잭션 | 승인됨 |
| [011](011-transactional-convention.md) | 트랜잭션 어노테이션 컨벤션 | 승인됨 |
| [012](012-price-calculation-safety.md) | 가격 계산 안전성 강화 | 승인됨 |
| [013](013-service-unit-tests.md) | 서비스 레벨 단위 테스트 추가 | 승인됨 |
| [014](014-global-exception-handler.md) | 전역 예외 처리 통합 | 승인됨 |
| [015](015-authenticated-member-resolver.md) | HandlerMethodArgumentResolver로 인증 보일러플레이트 제거 | 승인됨 |
| [016](016-kakao-message-after-commit.md) | 카카오 메시지 전송을 트랜잭션 밖으로 분리 | 승인됨 |
| [017](017-pessimistic-lock-for-stock.md) | 비관적 락으로 동시 주문 시 Overselling 방지 | 승인됨 |

## ADR 작성 기준

다음 조건 중 하나라도 해당하면 ADR을 작성합니다:
- 선택지가 2개 이상이고 트레이드오프가 있었던 경우
- 팀이 반복해서 따라야 할 규칙이나 경계를 정한 경우
- 테스트 전략과 검증 방식이 결정의 핵심이었던 경우

## 템플릿

새 ADR 작성 시 [000-template.md](000-template.md)를 참고하세요.
