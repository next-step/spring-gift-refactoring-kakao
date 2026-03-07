# ADR-3: Admin 메서드 분리 방식

## 상태

승인됨

## 맥락

`ProductService`에 REST API용 메서드(`create`, `update`)와 Admin 전용 메서드(`createFromAdmin`, `updateFromAdmin`)가 혼재한다.
두 경로의 차이점:

| 항목 | REST API 경로 | Admin 경로 |
|------|-------------|-----------|
| 입력 | `ProductRequest` DTO | 개별 파라미터 |
| 검증 위치 | **Service** (`validateName`) | **Controller** (`ProductNameValidator.validate`) |
| 카카오 허용 | `allowKakao=false` | `allowKakao=true` |
| 에러 처리 | 예외 throw → ControllerAdvice | 에러 리스트 반환 → 폼 재렌더링 |

Validator 호출 위치가 분산되어 있어, 검증 로직 수정 시 두 곳을 모두 변경해야 한다.

## 선택지

### 1. AdminProductService 분리

- Admin 전용 Service 클래스를 생성하여 `createFromAdmin`/`updateFromAdmin`을 이동
- 장점: ProductService가 REST API 전용으로 깔끔해짐
- 단점: Repository 의존성 중복, 2개 메서드를 위한 클래스 생성은 과도한 추상화

### 2. Validator 호출을 Service 계층으로 통일

- `createFromAdmin`/`updateFromAdmin`에 `validateName(name, true)` 추가
- AdminProductController는 try-catch 패턴으로 전환 (AdminMemberController와 동일)
- 장점: 검증이 항상 Service에서 수행, Admin 컨트롤러 패턴 통일
- 단점: 에러 표시 형식이 List → String으로 변경 (Admin UI 미세 변경)

### 3. 현재 구조 유지

- 변경하지 않음
- 장점: 변경 위험 없음
- 단점: 문제 C, E가 해소되지 않음

## 결정

**선택지 2: Validator 호출을 Service 계층으로 통일**

## 근거

1. **일관성**: 모든 비즈니스 검증이 Service 계층에서 수행되는 단일 패턴
2. **Admin 컨트롤러 패턴 통일**: AdminMemberController와 AdminProductController가 동일한 try-catch 패턴 사용
3. **유지보수**: 검증 로직 수정 시 Service만 변경하면 됨
4. **별도 Service 불필요**: 2개 메서드를 위해 클래스를 생성하는 것은 과도함

## 결과

- `createFromAdmin`/`updateFromAdmin`에 검증 로직 추가 (throw IllegalArgumentException)
- AdminProductController에서 직접 Validator 호출 제거, try-catch로 전환
- Product 템플릿의 에러 표시가 List → 단일 String으로 변경 (AdminMember와 동일)
- `populateNewForm`/`populateEditForm` 시그니처 간소화
