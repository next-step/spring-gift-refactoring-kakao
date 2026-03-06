# ADR-002: 서비스 계층 추출 전략

## 상태
- [x] 승인됨 (2026-02)

## 맥락
Controller에 비즈니스 로직이 포함되어 있어 테스트와 유지보수가 어렵다. Controller를 얇게 만들고 비즈니스 로직을 Service 계층으로 이동해야 한다.

## 선택지

### 1. Controller 로직을 그대로 유지
- 장점: 변경 없음
- 단점: 테스트 어려움, 책임 혼재

### 2. 전체 재설계
- 장점: 깔끔한 구조
- 단점: 작동 변경 위험, 시간 소요

### 3. 로직 이동만 수행 (선택)
- 장점: 작동 변경 없이 구조만 개선
- 단점: 기존 설계의 문제점 일부 유지

## 결정
**로직 이동만 수행**한다. Controller의 비즈니스 로직을 Service로 이동하되, 로직 자체는 변경하지 않는다.

### Controller 책임
- HTTP 요청/응답 처리
- 인증 확인 (AuthenticationResolver)
- Service 호출 및 결과 반환

### Service 책임
- 비즈니스 로직 수행
- Repository 호출
- 트랜잭션 관리 (향후)

## 추출된 Service
| Service | Controller | 비고 |
|---------|------------|------|
| MemberService | MemberController, AdminMemberController | |
| CategoryService | CategoryController | |
| ProductService | ProductController, AdminProductController | |
| OptionService | OptionController | |
| WishService | WishController | |
| OrderService | OrderController | |
| KakaoAuthService | KakaoAuthController | |

## 제외 사항
- `AuthenticationResolver.extractMember()`의 `orElse(null)` 패턴은 유지
  - 사유: null 반환이 "인증 실패"를 의미하는 정상 흐름
  - 변경 시 메서드 시그니처와 모든 호출자 수정 필요 (작동 변경)

## 근거
- 구조 변경만 수행하여 기존 테스트가 그대로 통과함
- 각 Service 추출 후 테스트 실행으로 검증

## 결과
- 6개 Service 클래스 생성
- Controller는 위임만 담당
- 향후 트랜잭션 추가 용이

## 관련 ADR
- [ADR-001: 리팩토링 전략](001-refactoring-strategy.md)
