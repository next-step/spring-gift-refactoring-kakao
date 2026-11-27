# 사용자 스토리 (User Stories)

컨트롤러(API와 관리자 화면)를 읽고, 사용자가 할 수 있는 행동을 "누가 무엇을 왜 한다"로 정리한 문서다.

- 기준 브랜치: `M32k`
- 작성일: 2026-11-22
- **누가**는 비회원, 회원, 관리자 중 하나로 적는다.
  - 회원: `Authorization` 헤더의 로그인 토큰을 확인하는 기능
  - 관리자: `/admin` 화면과, 관리자용으로 보이는 상품, 카테고리, 옵션의 등록, 수정, 삭제 API. 단, 실제 코드에는 관리자 권한 확인이 없다(비고 참고).
  - 비회원: 로그인 토큰 없이 쓰도록 만든 기능
- **왜**는 코드에 목적이 적혀 있을 때만 쓰고 근거를 비고에 붙인다. 코드로 알 수 없으면 "왜: 확인 필요"라고 적는다.
- **확인한 응답**에는 요청으로 직접 확인한 결과만 적는다. 확인하지 않은 API는 "아직 확인 안 함"으로 둔다.
- **인수 조건** 본문은 `src/test/resources/features/`의 feature 파일에만 있다. 여기에는 feature 파일 위치와 시나리오 번호만 적는다. 지금 인수 조건이 있는 스토리는 1, 7, 15, 16, 17번이다.
- 비고의 "open-questions N번"은 [`docs/open-questions.md`](open-questions.md)의 항목 번호다.

---

## 1. 비회원은 이메일과 비밀번호로 회원 가입을 한다 (왜: 확인 필요)
- API: `POST /api/members/register`
- 확인한 응답:
  - 새 이메일: 201, token
  - 이미 가입한 이메일: 400
- 비고: 가입하면 바로 로그인 토큰을 준다

### 인수 조건

인수 조건: `src/test/resources/features/member.feature` (1-1~1-3)

## 2. 비회원은 이메일과 비밀번호로 로그인한다 (왜: 확인 필요)
- API: `POST /api/members/login`
- 확인한 응답:
  - 성공: 200, token
  - 비밀번호 틀림, 없는 이메일: 400, 본문 "Invalid email or password." (둘이 같다)
- 비고: 비밀번호를 평문으로 비교한다(open-questions 7번)

### 인수 조건

인수 조건: `src/test/resources/features/login.feature` (2-1~2-3)

## 3. 비회원은 카카오 계정으로 로그인한다 (왜: 확인 필요)
- API: `GET /api/auth/kakao/login`, `GET /api/auth/kakao/callback`
- 확인한 응답: 카카오 동의 후 콜백에서 200, `{"token": ...}` (로그인 토큰)
- 비고: 처음이면 자동으로 가입된다. 단서: scope에 `talk_message`가 있고, 받은 카카오 액세스 토큰을 주문 알림 발송에 쓴다

## 4. 비회원은 상품 목록과 상세를 본다 (왜: 확인 필요)
- API: `GET /api/products`, `GET /api/products/{id}`
- 확인한 응답:
  - `GET /api/products`: 200
  - `GET /api/products/{id}`: 200, 상품명과 가격
  - 쪽 크기 2: content 2개, 전체 6개, 3쪽
  - 없는 상품: 404(본문 없음)
- 비고: 없음

### 인수 조건

인수 조건: `src/test/resources/features/product-view.feature` (4-1~4-4)

## 5. 비회원은 카테고리 목록을 본다 (왜: 확인 필요)
- API: `GET /api/categories`
- 확인한 응답: 200, 카테고리 id·이름·색·이미지 URL·설명
- 비고: 없음

### 인수 조건

인수 조건: `src/test/resources/features/category-list.feature` (5-1)

## 6. 비회원은 상품의 옵션 목록을 본다 (왜: 확인 필요)
- API: `GET /api/products/{productId}/options`
- 확인한 응답: 200, 옵션 id·이름·재고 / 없는 상품: 404
- 비고: 없음

### 인수 조건

인수 조건: `src/test/resources/features/option-list.feature` (6-1~6-2)

## 7. 관리자는 상품을 등록한다 (왜: 확인 필요)
- API: `POST /api/products`
- 확인한 응답: 201 (로그인 토큰 없이도 성공)
- 비고: 실제로는 로그인 토큰 없이 누구나 할 수 있다(open-questions 1번). "카카오"가 들어간 상품명은 거부한다

### 인수 조건

인수 조건: `src/test/resources/features/product.feature` (7-1~7-3)

## 8. 관리자는 상품을 수정한다 (왜: 확인 필요)
- API: `PUT /api/products/{id}`
- 확인한 응답:
  - 200 (로그인 토큰 없이도 성공), 다시 조회하면 바뀐 상품명과 가격
  - 16자 상품명·카카오 상품명: 400과 메시지
  - 없는 카테고리·없는 상품: 404(본문 없음)
  - 없는 상품 + 16자 상품명: 400(상품명 검사가 먼저)
- 비고: 실제로는 로그인 토큰 없이 누구나 할 수 있다(open-questions 18번)

### 인수 조건

인수 조건: `src/test/resources/features/product-update.feature` (8-1~8-6)

## 9. 관리자는 상품을 삭제한다 (왜: 확인 필요)
- API: `DELETE /api/products/{id}`
- 확인한 응답: 204, 다시 조회하면 404
- 비고: 실제로는 로그인 토큰 없이 누구나 할 수 있다(open-questions 11번)

### 인수 조건

인수 조건: `src/test/resources/features/product-delete.feature` (9-1~9-4)

## 10. 관리자는 카테고리를 등록, 수정, 삭제한다 (왜: 확인 필요)
- API: `POST /api/categories`, `PUT /api/categories/{id}`, `DELETE /api/categories/{id}`
- 확인한 응답:
  - `POST /api/categories`: 201 (로그인 토큰 없이도 성공)
  - `PUT /api/categories/{id}`: 200 (로그인 토큰 없이도 성공), 목록에 바뀐 이름
  - `DELETE /api/categories/{id}`: 204
  - 빈 이름 등록: 400
  - 같은 이름 등록: 500
  - 없는 카테고리 수정: 404
  - 없는 카테고리 + 빈 이름 수정: 400
  - 상품이 있는 카테고리 삭제: 500
  - 없는 카테고리 삭제: 204
- 비고: 등록, 수정, 삭제 모두 로그인 토큰 없이 누구나 할 수 있다(open-questions 15번)

### 인수 조건

인수 조건: `src/test/resources/features/category-manage.feature` (10-1~10-9)

## 11. 관리자는 상품에 옵션을 등록한다 (왜: 확인 필요)
- API: `POST /api/products/{productId}/options`
- 확인한 응답: 201 + Location (로그인 토큰 없이도 성공) / 같은 상품 안 중복 옵션명: 400 / 허용 안 되는 문자: 400 / 없는 상품: 404 / 없는 상품 + 틀린 옵션명: 400
- 비고: 실제로는 로그인 토큰 없이 누구나 할 수 있다(open-questions 16번). 같은 상품 안에서 옵션명 중복은 막는다

### 인수 조건

인수 조건: `src/test/resources/features/option-create.feature` (11-1~11-6)

## 12. 관리자는 상품의 옵션을 삭제한다 (왜: 확인 필요)
- API: `DELETE /api/products/{productId}/options/{optionId}`
- 확인한 응답: 204 / 옵션이 1개뿐인 상품: 400 / 다른 상품의 옵션·없는 옵션·없는 상품: 404 / 옵션 1개 상품 + 없는 옵션: 400 / 주문이 있는 옵션: 500
- 비고: 실제로는 로그인 토큰 없이 누구나 할 수 있다(open-questions 17번). 옵션이 1개뿐이면 삭제할 수 없다(`OptionService.java:11` 주석은 규칙만 적고 이유는 없다)

### 인수 조건

인수 조건: `src/test/resources/features/option-delete.feature` (12-1~12-7)

## 13. 회원은 자기 위시 목록을 본다 (왜: 확인 필요)
- API: `GET /api/wishes`
- 확인한 응답:
  - 200, 내 위시 목록
  - 틀린 로그인 토큰: 401
  - 로그인 토큰 헤더 없음: 400
- 비고: 없음

### 인수 조건

인수 조건: `src/test/resources/features/wish-list.feature` (13-1~13-3)

## 14. 회원은 상품을 위시에 담는다 (왜: 확인 필요)
- API: `POST /api/wishes`
- 확인한 응답:
  - 처음 담기: 201
  - 같은 상품 다시 담기: 200, 같은 위시 id. 목록에는 한 번만 있음
  - 없는 상품: 404(본문 없음)
- 비고: 이미 담았으면 기존 위시를 200으로 돌려준다

### 인수 조건

인수 조건: `src/test/resources/features/wish-add.feature` (14-1~14-3)

## 15. 회원은 자기 위시를 삭제한다 (왜: 확인 필요)
- API: `DELETE /api/wishes/{id}`
- 확인한 응답:
  - 남의 위시면 403, 위시는 그대로
  - 자기 위시: 204, 목록에서 빠짐
  - 없는 위시: 404(본문 없음)
- 비고: 남의 위시는 삭제할 수 없다(open-questions 5번)

### 인수 조건

인수 조건: `src/test/resources/features/wish.feature` (15-1~15-3)

## 16. 회원은 포인트로 상품 옵션을 주문한다 (왜: 확인 필요)
- API: `POST /api/orders`
- 확인한 응답:
  - 성공: 201, 재고와 포인트 차감
  - 포인트 부족: 500, 상태 그대로(open-questions 3번, ADR 0001)
  - 포인트 3,000,000인 user2가 맥북 1개 주문: 500, 재고는 10에서 9로 줄어듦 (초기 데이터에서 직접 확인) (4단계 전, ADR 0001로 바뀜)
  - 4단계 뒤 같은 요청: 500, 재고 10 그대로 (초기 데이터에서 직접 확인, ADR 0001)
  - 재고 부족: 500, 상태 그대로(open-questions 12번)
- 비고: "선물이 도착했어요" 주문 알림을 주문한 본인에게 보낸다(open-questions 14번). 주문 금액은 옵션이 아니라 상품 가격으로 계산한다(open-questions 2번). 주문 알림은 아웃박스에 저장해 커밋 뒤 보내고, 실패하면 5번까지 다시 보낸다(ADR 0002).

### 인수 조건

인수 조건: `src/test/resources/features/order.feature` (16-1~16-7)
인수 조건: `src/test/resources/features/order-notification.feature` (16-8~16-14)

## 17. 회원은 자기 주문 내역을 본다 (왜: 확인 필요)
- API: `GET /api/orders`
- 확인한 응답: 200, 성공한 주문만 보이고 실패한 주문은 없음
- 비고: 없음

### 인수 조건

인수 조건: `src/test/resources/features/order-history.feature` (17-1~17-4)

## 18. 관리자는 상품 목록 화면을 본다 (왜: 확인 필요)
- API: `GET /admin/products`
- 확인한 응답: 화면이 열림
- 비고: 없음

## 19. 관리자는 담당 MD와 협의한 "카카오" 상품명을 쓰려고, "카카오"가 들어간 상품명으로도 상품을 등록한다
- API: `GET /admin/products/new`, `POST /admin/products`
- 확인한 응답:
  - 관리자 화면: "카카오" 상품명으로 등록 성공
  - 비교: 같은 상품명을 `POST /api/products`로 보내면 400("담당 MD와 협의한 경우에만" 메시지)
- 비고: 근거는 `ProductNameValidator.java:34` 메시지와 `AdminProductController.java:45`의 `allowKakao = true`. API는 막고 관리자 화면은 허용한다(open-questions 20번)

## 20. 관리자는 담당 MD와 협의한 "카카오" 상품명을 쓰려고, 상품을 수정할 때도 "카카오"가 들어간 상품명을 쓴다
- API: `GET /admin/products/{id}/edit`, `POST /admin/products/{id}/edit`
- 확인한 응답: "카카오" 상품명을 유지한 채 302, 다시 조회하면 바뀐 값
- 비고: 근거는 `AdminProductController.java:78`

## 21. 관리자는 상품을 삭제한다 (왜: 확인 필요)
- API: `POST /admin/products/{id}/delete`
- 확인한 응답: 302, 다시 조회하면 404
- 비고: 없음

## 22. 관리자는 회원 목록과 포인트를 본다 (왜: 확인 필요)
- API: `GET /admin/members`
- 확인한 응답: 회원 목록과 포인트 (로그인 토큰 없이 열림)
- 비고: 로그인 없이 열린다(open-questions 13번)

## 23. 관리자는 회원을 등록한다 (왜: 확인 필요)
- API: `GET /admin/members/new`, `POST /admin/members`
- 확인한 응답: 302
- 비고: 없음

## 24. 관리자는 회원의 이메일과 비밀번호를 수정한다 (왜: 확인 필요)
- API: `GET /admin/members/{id}/edit`, `POST /admin/members/{id}/edit`
- 확인한 응답: 302, 목록에 바뀐 이메일
- 비고: 실제로는 로그인 없이 누구나 할 수 있다(open-questions 22번). 이미 삭제한 회원 id로 수정하면 404가 아니라 500이 온다(open-questions 21번)

## 25. 관리자는 회원 포인트를 충전한다 (왜: 확인 필요)
- API: `POST /admin/members/{id}/charge-point`
- 확인한 응답:
  - 10000: 302, 충전 성공
  - 0, -100: 500, 실패 뒤 포인트는 그대로
- 비고: 실제로는 로그인 없이 누구나 할 수 있다(open-questions 22번). 단서: `Member.java:55` 주석 "point deduction for order payment"에서 포인트는 주문 결제에 쓰인다. 0이나 음수는 500이 온다(open-questions 19번)

## 26. 관리자는 회원을 삭제한다 (왜: 확인 필요)
- API: `POST /admin/members/{id}/delete`
- 확인한 응답: 302, 목록에서 사라짐
- 비고: 실제로는 로그인 없이 누구나 할 수 있다(open-questions 22번)

---

## 코드에 없는 행동

- 회원이 자기 포인트를 조회하는 기능은 없다. 포인트는 관리자 화면(22번)에서만 보인다(open-questions 4번).
