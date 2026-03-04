# Step2 리팩터링 완성하기 — 작업 계획

## Step1 완료 항목 (구조 변경, 작동 불변)

- [x] S1: GlobalExceptionHandler에 IllegalArgumentException 핸들러 추가
- [x] S2: Thymeleaf Product 목록 페이지에 카테고리 표시 추가
- [x] S3: OptionService.createRaw 제거 및 SeedOptionController 수정
- [x] S4: BEARER_PREFIX 상수 중복 제거
- [x] S5: 카카오 API URL을 application.properties로 외부화
- [x] S6: public API 주석을 javadoc 스타일로 변경
- [x] S7: 리뷰어 피드백 반영

## Step2 구조 변경 (작동 불변, `refactor:` 커밋)

- [ ] S8: Option에 calculateTotalPrice 추가 및 OrderService에서 사용
- [ ] S9: WishService에 removeByMemberAndProduct 메서드 추가
- [ ] S10: OrderService의 MemberRepository를 MemberService로 전환
- [ ] S11: 카카오 메시지 전송 실패 시 경고 로깅 추가

## Step2 작동 변경 (증거 필수, `fix:`/`feat:`/`test:` 커밋)

- [ ] B1: NoSuchElementException 응답 코드를 404로 변경
- [ ] B2: 주문 시 위시 자동 정리 기능 구현
- [ ] B3: 트랜잭션 경계 검증 — 포인트 부족 시 재고 원복 확인

## ADR (Architecture Decision Records)

- [ ] ADR-001: NoSuchElementException → 404 변경 근거
- [ ] ADR-002: 주문 시 위시 정리 전략 (동기 vs 비동기)

## 과제 요구사항 대응표

| 요구사항 | 대응 커밋 | 증거 |
|---------|----------|------|
| 트랜잭션 경계 세우기 | B3 | 포인트 부족 시 재고 rollback을 API 재조회로 검증 |
| 누락된 작동 구현 | B2 | 주문 후 위시 목록 API 재조회로 삭제 확인 |
| 도메인 책임 되찾기 (1) | S8 | calculateTotalPrice 캡슐화 → 호출부 단순화 |
| 도메인 책임 되찾기 (2) | S10 | MemberRepository → MemberService → 계층 의존 정리 |
| 구조/작동 분리 | S8~S11 / B1~B3 | 커밋 라벨로 분리 |
| ADR | B1, B2 | ADR-001, ADR-002 |
