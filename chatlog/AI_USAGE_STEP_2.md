# STEP 2: 리팩터링 완성 - AI 사용 기록

## 개요

이 문서는 리팩터링 완성 단계에서 AI를 어떻게 활용했는지를 기록한다.

Step 2의 핵심은 **작동 변경을 안전하게 수행하고, 테스트로 증명하는 것**이다.

1. **트랜잭션 경계 세우기** — 복합 쓰기 작업의 원자성 보장
2. **누락된 작동 구현** — TODO로 남아있던 위시리스트 자동 제거 완성
3. **도메인 책임 되찾기** — 검증/판단 로직을 올바른 위치로 이동

---

## 1단계: 프로젝트 현황 분석 및 Step 2 계획 수립

### 프롬프트

> Step 1 같이 진행한 상태가 현재 프로젝트 상태이고 이제 step-2 진행해야 하는 상태야. 프로젝트 전체적으로 한 번 다 둘러봐.

### AI 활용 방식

- 4개의 병렬 에이전트를 실행하여 전체 프로젝트를 동시에 분석:
  1. 프로젝트 디렉토리 구조 전체 탐색 (75+ 파일)
  2. 문서 체계 전문 읽기 (README, docs/, chatlog/)
  3. Git 히스토리 분석 (main vs step-2 브랜치 diff)
  4. 전체 소스 코드 읽기 (엔티티, 서비스, 컨트롤러, 설정 파일)

### 분석 결과

Step 1에서 완료된 상태:
- 7개 서비스 레이어 추출 완료 (Controller → Service → Repository 3계층)
- 25개 Cucumber BDD 시나리오 (gift 6, member 5, option 5, product 3, wish 6)
- `@Transactional`은 원본 동작 보존을 위해 의도적으로 제거된 상태 (commit 596884a)

Step 2에서 수행할 작업 식별:

| 작업 | 종류 | 증거 |
|------|------|------|
| `@Transactional` 추가 (특히 `OrderService.createOrder`) | 작동 변경 | 포인트 부족 시 재고 롤백 테스트 |
| 주문 후 위시리스트 자동 삭제 | 작동 변경 | 주문 → 위시 제거 확인 테스트 |
| 위시 중복 체크를 서비스로 이동 | 구조 변경 | 기존 테스트 재통과 |
| 상품 이름 검증을 서비스로 이동 | 구조 변경 | 기존 테스트 재통과 |

### 산출물

| 파일 | 내용 |
|------|------|
| `README.md` | Step 2 리팩터링 계획 추가 (작동 변경 2건 + 구조 변경 2건) |
| `chatlog/AI_USAGE_STEP_2.md` | 본 문서 (AI 활용 기록 시작) |

---

## 2단계: OrderService.createOrder 트랜잭션 경계 설정

### 프롬프트

> `OrderService.createOrder()` — 재고 차감 + 포인트 차감 + 주문 저장이 원자적으로 처리되어야 함

### 변경 전/후 정의

- **무엇을 바꾸는가**: `createOrder()`에 `@Transactional` 추가 → 포인트 부족 시 재고 차감이 롤백됨
- **무엇을 바꾸지 않는가**: 정상 주문 흐름 (재고 차감 + 포인트 차감 + 주문 저장 + 카카오 알림)
- **무엇이 이를 증명하는가**: `OrderServiceTest` 2개 테스트

### AI 활용 방식

1. **테스트 먼저 작성** (Red):
   - `OrderServiceTest.createOrder_insufficientPoints_rollbacksStock()` — 포인트 1000원, 상품 500원 × 3개 = 1500원 주문 시도 → 포인트 부족 예외 → 재고/포인트/주문 모두 원래 상태 확인
   - `OrderServiceTest.createOrder_success()` — 정상 주문 성공 시 재고/포인트 차감 확인
2. **테스트 실행으로 버그 확인**: `@Transactional` 없는 상태에서 롤백 테스트 실패 (재고 10 → 7로 차감됨, 포인트만 실패)
3. **최소 변경 적용** (Green): `OrderService.java`에 `@Transactional` import + 어노테이션 2줄만 추가
4. **git diff 확인**: 프로덕션 코드 변경이 2줄(import + 어노테이션)뿐인지 확인

### 산출물

| 파일 | 변경 | 종류 |
|------|------|------|
| `OrderServiceTest.java` | 신규 — 2개 테스트 (롤백 증명 + 정상 주문) | 테스트 |
| `OrderService.java` | `@Transactional` 추가 (import 1줄 + 어노테이션 1줄) | 작동 변경 |

### 테스트 결과

| 테스트 | @Transactional 전 | @Transactional 후 |
|--------|-------------------|-------------------|
| 포인트 부족 시 재고 롤백 | **FAIL** (재고 10→7) | **PASS** (재고 10 유지) |
| 정상 주문 성공 | PASS | PASS |

---
