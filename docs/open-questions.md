# 확인 필요 목록 (Open Questions)

리팩터링 전에 "의도된 작동인지, 고쳐야 할 작동인지" 결정이 필요한 항목을 모은 문서다.
여기서는 작동을 기록만 하고, 어떻게 고칠지는 정하지 않는다.

- 기준 브랜치: `M32k`
- 작성일: 2026-11-22
- 코드 위치는 `파일:줄` 형식으로 적는다.
- 2단계에서 인수 테스트로 지금 작동을 고정한 항목에는 **인수 테스트** 줄에 시나리오 번호를 적는다. 고정했다고 결정된 것은 아니다. 상태는 그대로 미결정이다.
- "이유가 코드에 있는지"는 주석, 메시지, 이름 등 **작동의 의도를 설명하는 근거**가 코드에 있는지를 뜻한다. 작동을 만드는 코드의 위치와는 따로 적는다.

---

## A. 요청으로 직접 확인한 작동 (curl, 브라우저)

### 1. 상품 등록이 로그인 토큰 없이 성공한다

- **작동**: `POST /api/products`를 `Authorization` 헤더 없이 보내도 201 Created로 상품이 등록된다.
- **확인한 방법**: curl로 로그인 토큰 없이 요청을 보냄.
- **이유가 코드에 있는지**: 없음.
  - `ProductController.createProduct`(`src/main/java/gift/product/ProductController.java:42-50`)는 `@RequestHeader("Authorization")`를 받지 않고 `AuthenticationResolver`도 주입받지 않는다.
  - 프로젝트에 Spring Security 설정이나 공통 인증 필터, 인터셉터가 없다. 그래서 인증은 컨트롤러마다 직접 확인하는 방식이고, 이 컨트롤러에는 그 확인이 없다.
  - 인증을 일부러 뺐다는 주석은 없다.
- **인수 테스트**: `src/test/resources/features/product.feature` 7-1~7-3 (로그인 토큰 없이 등록하는 지금 작동)
- **상태**: 미결정

### 2. 주문 금액이 옵션이 아니라 상품 가격 기준으로 계산된다

- **작동**: 주문 금액은 옵션과 관계없이 `상품 가격 × 주문 수량`으로 계산된다. 옵션에는 가격 필드가 없어서, 옵션이 달라도(예: 맥북 "스페이스 블랙 / M1 Pro"와 "실버 / M1 Max") 같은 금액이 차감된다.
- **확인한 방법**: user1(포인트 5,000,000)이 맥북 옵션(재고 10)을 1개 주문하자 201이 오고, 재고는 9, 포인트는 1,640,000이 되었다(curl로 주문, 포인트는 `/admin/members`에서 확인). 줄어든 포인트 3,360,000원은 상품 가격과 같다. 옵션별 가격이 없다는 점은 코드로 확인했다.
- **이유가 코드에 있는지**: 없음. 상품 가격으로 계산하는 이유를 설명하는 주석은 없다.
  - 금액 계산: `option.getProduct().getPrice() * request.quantity()`(`src/main/java/gift/order/OrderService.java:61`). 주문 알림에 적히는 주문 금액도 같은 방식으로 계산한다(`src/main/java/gift/order/OrderNotificationMessage.java:10`).
  - `Option` 엔티티(`src/main/java/gift/option/Option.java:16-28`)에는 `id`, `product`, `name`, `quantity`만 있고 가격 필드가 없다.
  - 초기 데이터: 맥북 3,360,000원(`V2__Insert_default_data.sql:9`), 맥북 옵션 2개(`:38-40`), user1 포인트 5,000,000(`:24`).
- **상태**: 미결정

### 3. 포인트가 부족해서 500이 와도 재고는 줄어든다

- **작동**: 2번에 이어 같은 주문을 다시 하면 포인트가 부족해 500이 온다. 그런데 재고는 9에서 8로 줄어든다.
- **확인한 방법**: curl로 같은 주문을 두 번째로 보낸 뒤 재고를 확인함.
  - 초기 데이터의 user2(포인트 3,000,000)로 한 번에 주문해도 같은 작동
- **이유가 코드에 있는지**: 없음. 작동을 만드는 코드는 다음과 같다.
  - 재고를 줄이고 바로 저장한다: `option.subtractStock(...)` 다음에 `optionRepository.save(option)`(`OrderService.java:57-58`).
  - 그다음 포인트를 줄일 때 예외가 난다: `member.deductPoint(totalPrice)`(`OrderService.java:62`)에서 `throw new IllegalArgumentException("포인트가 부족합니다.")`(`src/main/java/gift/member/Member.java:60-62`).
  - `createOrder`와 `OrderController`에 `@Transactional`이 없다. 프로젝트 전체에도 `@Transactional`이 없어서, 앞에서 저장한 재고 변경이 되돌려지지 않는다. (4단계 전. 지금은 ADR 0001로 createOrder에 있다)
  - 500인 이유: `OrderController`에는 `@ExceptionHandler`가 없고, 프로젝트에 `@ControllerAdvice`도 없다. 그래서 `IllegalArgumentException`이 400으로 바뀌지 않는다. `ProductController`와 `MemberController`에는 컨트롤러별 400 처리기가 있다.
- **인수 테스트**: `src/test/resources/features/order.feature` 16-2 (500, 재고·포인트·주문 내역 그대로)
- **상태**: 4단계에서 고침, ADR 0001

### 4. 회원이 자기 포인트를 조회하는 API가 없다

- **작동**: 회원용 API로는 자기 포인트를 볼 수 없다. 관리자 화면 `/admin/members`에서만 볼 수 있다.
- **확인한 방법**: 브라우저로 `/admin/members`를 확인했고, 회원용 조회 API는 찾지 못함.
- **이유가 코드에 있는지**: 없음.
  - `MemberController`(`src/main/java/gift/member/MemberController.java`)에는 `/register`, `/login`만 있다.
  - 포인트를 보여 주는 곳은 관리자 템플릿 `templates/member/list.html:27`, `templates/member/edit.html:15`뿐이다.
  - 포인트 충전도 관리자 전용이다: `POST /admin/members/{id}/charge-point`(`src/main/java/gift/member/AdminMemberController.java:67-75`).
- **상태**: 미결정

### 5. 다른 회원의 위시를 삭제하려고 하면 403이 온다

- **작동**: user1의 로그인 토큰으로 user2의 위시(id 3)를 삭제하면 403이 오고, user2의 위시는 그대로 남는다.
- **확인한 방법**: curl로 user1의 로그인 토큰을 넣어 `DELETE /api/wishes/3`을 보낸 뒤 위시가 남아 있는지 확인함.
- **이유가 코드에 있는지**: 소유자 확인 코드는 있지만, 의도를 설명하는 주석은 없다.
  - `if (!wish.getMemberId().equals(memberId)) return RemoveResult.FORBIDDEN`(`src/main/java/gift/wish/WishService.java:61-63`), 403 응답은 `case FORBIDDEN -> ResponseEntity.status(403)`(`src/main/java/gift/wish/WishController.java:71`).
  - 확인 순서는 401(인증), 404(위시 없음), 403(소유자 아님) 순서다(401은 `WishController.java:64-67`, 위시 확인과 소유자 확인은 `WishService.java:56-63`, 응답은 `WishController.java:69-73`). 그래서 다른 사람의 위시 id가 있는지 없는지가 404와 403의 차이로 드러난다.
  - 초기 데이터에서 위시 id 3은 member 3(user2)의 것이다(`V2__Insert_default_data.sql:32-33`).
- **인수 테스트**: `src/test/resources/features/wish.feature` 15-1 (403, 위시는 그대로)
- **상태**: 미결정 (현재 작동을 정책으로 확정할지, 404로 숨길지 결정 필요)

### 6. 오류 메시지 언어가 섞여 있다 (한국어와 영어)

- **작동**: 상품명 검증 오류 메시지는 한국어인데, 다른 곳에는 영어 메시지가 있다.
- **확인한 방법**: 한국어 메시지는 요청으로, 영어 메시지는 코드로 확인함.
- **이유가 코드에 있는지**: 없음. 메시지 언어에 대한 규칙이나 주석이 없다.
  - 한국어: `ProductNameValidator`(`src/main/java/gift/product/ProductNameValidator.java:21,26,30,34`), `Option.subtractStock`(`src/main/java/gift/option/Option.java:40`), `Member.deductPoint`(`Member.java:58,61`)
  - 영어: `Member.chargePoint`의 `"Amount must be greater than zero."`(`Member.java:50`), `MemberService`의 `"Email is already registered."`, `"Invalid email or password."`(`MemberService.java:18,28,31`), `AdminMemberController`의 `"Member not found. id=..."`(`AdminMemberController.java:52,61,71`)
  - 같은 `Member` 클래스 안에서도 충전 메시지는 영어, 차감 메시지는 한국어다(`Member.java:50` vs `:58`).
- **상태**: 미결정

### 7. 초기 데이터의 비밀번호가 평문으로 저장돼 있다

- **작동**: 초기 데이터의 비밀번호가 `password1` 그대로 저장돼 있다.
- **확인한 방법**: 초기 데이터 SQL 파일을 읽어 확인함.
- **이유가 코드에 있는지**: 없음.
  - `V2__Insert_default_data.sql:21-26`: `'admin1234'`, `'password1'`, `'password2'`가 그대로 들어간다.
  - 로그인도 평문을 그대로 비교한다: `member.getPassword().equals(password)`(`MemberService.java:30`). 회원 가입(`MemberService.java:21`)과 관리자 화면의 등록, 수정(`AdminMemberController.java:44,62`)도 받은 값을 그대로 저장한다.
  - `PasswordEncoder`나 해시 처리는 프로젝트에 없다.
- **상태**: 미결정

### 8. 주문 흐름 주석의 6단계 "위시 정리"가 구현되지 않았다

> 처음에는 B 섹션(코드로만 확인)이었고, 요청으로 확인해서 이 섹션으로 옮겼다. 번호는 그대로 두었다.

- **작동**: 주석에는 `6. cleanup wish`가 있지만 `createOrder` 본문에는 위시를 정리하는 코드가 없다. 주문 저장 후 바로 주문 알림으로 넘어간다.
- **확인한 방법**: 직접 curl로 확인함: 맥북을 주문한 user1의 위시에 맥북이 그대로 남아 있음
- **이유가 코드에 있는지**: 의도(주석)만 있고, 왜 빠졌는지에 대한 설명은 없다.
  - 주석 `// 6. cleanup wish`(`OrderService.java:45`)
  - 본문의 `// save order`(`:58`) 다음이 바로 `// best-effort kakao notification`(`:61`)이다.
  - `WishRepository`(`src/main/java/gift/wish/WishRepository.java`)에는 조회 메서드만 있고, 회원과 상품으로 삭제하는 전용 메서드는 없다.
- **상태**: 미결정 (주석이 맞는지, 구현이 맞는지 결정 필요)

### 11. 상품 삭제가 로그인 토큰 없이 성공한다

> 처음에는 C-1(AI 추정)이었고, 요청으로 확인해서 이 섹션으로 옮겼다. B 섹션 항목끼리 번호로 서로 가리키고 있어서, 번호를 다시 매기지 않고 11번으로 붙였다.

- **작동**: `DELETE /api/products/{id}`를 `Authorization` 헤더 없이 보내도 204 No Content가 오고, 상품이 실제로 삭제된다.
- **확인한 방법**: curl로 로그인 토큰 없이 `DELETE /api/products/7`을 보내 204를 받았다. 그 뒤 같은 상품을 조회하자 404가 왔다.
- **이유가 코드에 있는지**: 없음.
  - `ProductController.deleteProduct`(`src/main/java/gift/product/ProductController.java:62-66`)는 `@RequestHeader("Authorization")`를 받지 않고, 인증 확인 없이 바로 `productService.deleteProduct(id)`를 부르고, 서비스가 `productRepository.deleteById(id)`를 호출한다(`src/main/java/gift/product/ProductService.java:60`).
  - 1번과 원인이 같다. 공통 인증 장치가 없고, 이 컨트롤러에는 인증 확인이 없다. 인증을 일부러 뺐다는 주석도 없다.
- **상태**: 미결정

### 12. 재고보다 많이 주문하면 500이 오지만 재고와 포인트는 그대로다

> 처음에는 C-2(AI 추정)였고, 요청으로 확인해서 이 섹션으로 옮겼다.

- **작동**: 재고보다 많은 주문 수량으로 주문하면 400이 아니라 500이 온다. 그 뒤에도 재고와 포인트는 바뀌지 않는다.
  - **3번과의 차이**: 3번(포인트 부족)은 500이 와도 재고가 줄어든 채로 남는다. 이 경우(재고 부족)는 아무 상태도 바뀌지 않는다. 같은 500이어도 실패한 단계에 따라 데이터가 남기도 하고 남지 않기도 한다.
- **확인한 방법**: user2의 로그인 토큰으로 일반 감귤 옵션(재고 50)을 51개 주문하자 500이 왔다. 그 뒤 재고는 50, user2 포인트는 3,000,000 그대로였다. 이때 주문 금액은 25,000 × 51 = 1,275,000원이라 포인트는 충분했다. 따라서 실패 원인은 재고 부족이다(`V2__Insert_default_data.sql:17,26,49-50`).
- **이유가 코드에 있는지**: 없음. 작동을 만드는 코드는 다음과 같다.
  - 500인 이유: `Option.subtractStock`이 `IllegalArgumentException("차감할 수량이 현재 재고보다 많습니다.")`를 던진다(`src/main/java/gift/option/Option.java:39-41`). 3번과 마찬가지로 `OrderController`에는 이 예외를 400으로 바꾸는 처리기가 없다.
  - 상태가 남지 않는 이유: `subtractStock`은 재고를 줄이기 전에 수량을 확인하고 예외를 던진다(`Option.java:39-42`). 그래서 그 뒤의 `optionRepository.save(option)`(`OrderService.java:58`)와 포인트 차감(`:62`)까지 가지 않는다. 3번은 재고를 저장한 뒤에 실패하고, 이 경우는 저장하기 전에 실패한다.
- **인수 테스트**: `src/test/resources/features/order.feature` 16-3 (500, 재고와 포인트 그대로)
- **상태**: 미결정

### 13. 관리자 화면(/admin/members)이 로그인 없이 열린다

> 처음에는 C-3(AI 추정)이었고, 요청으로 확인해서 이 섹션으로 옮겼다. 회원 수정, 포인트 충전, 회원 삭제는 22번에 있다.

- **작동**: 로그인하지 않고 로그인 토큰도 없이 `/admin/members`가 열린다. 4번에서 "관리자 화면에서만 볼 수 있다"고 한 회원 포인트도, 이 화면을 통해 누구나 볼 수 있다.
- **확인한 방법**: 브라우저에서 로그인 토큰 없이 `/admin/members`에 접속하자 화면이 열렸다.
- **이유가 코드에 있는지**: 없음.
  - `AdminMemberController.list`(`src/main/java/gift/member/AdminMemberController.java:26-30`)는 인증이나 권한 확인 없이 `memberRepository.findAll()`을 화면에 넘긴다.
  - 프로젝트에 공통 인증 장치(Spring Security, 필터, 인터셉터)가 없다. 관리자 화면을 일부러 열어 뒀다는 주석도 없다.
- **상태**: 미결정

### 14. "선물이 도착했어요" 주문 알림을 주문한 본인에게 보낸다

> 처음에는 B 섹션(코드로만 확인)이었고, 요청으로 확인해서 이 섹션으로 옮겼다. 번호는 그대로 두었다.

- **작동**: 주문하면 카카오톡으로 주문 알림이 "🎁 선물이 도착했어요!"라는 문구로 나간다. 그런데 받는 사람은 선물을 받을 사람이 아니라 주문한 회원 본인이다.
- **확인한 방법**: 직접 확인함. 카카오 로그인으로 가입한 회원으로 주문했더니, 주문한 본인의 카카오톡에 "선물이 도착했어요" 메시지가 왔다.
- **이유가 코드에 있는지**: 없음. 선물 문구를 본인에게 보내는 이유를 설명하는 주석은 없다.
  - 메시지 문구는 받는 사람 입장의 선물 알림이다. `"🎁 선물이 도착했어요!"`(`src/main/java/gift/order/OrderNotificationMessage.java:16`), 버튼 `"선물 확인하기"`(`:18`).
  - 보내는 곳은 "나에게 보내기" API다. 4단계 전 메서드 이름이 `sendToMe`였고, 지금은 `send` 위 주석에 남아 있다(`KakaoMessageClient.java:15`). 주소는 `/v2/api/talk/memo/default/send`(`:23`)이다. 주문한 회원의 카카오 액세스 토큰으로 보낸다(`OrderNotificationDispatcher.java:35-37`).
  - 주문할 때 받는 사람 정보를 넘기지 않는다. `new Order(option, member.getId(), quantity, message)`(`OrderService.java:66`)
  - 참고로 초기 주문 데이터의 메시지는 받는 사람에게 쓰는 말이다. 예: `'생일 축하해! 🎉'`, `'엄마 감사합니다 ❤️'`(`V2__Insert_default_data.sql:55,59`)
- **상태**: 미결정 (본인 알림이 의도인지, 받는 사람에게 보내는 기능이 빠진 것인지 결정 필요)

### 15. 카테고리 등록, 수정, 삭제가 로그인 토큰 없이 성공한다

> 15~17번은 B 섹션 14번 다음 번호로 매겼다.

- **작동**: `POST /api/categories`, `PUT /api/categories/{id}`, `DELETE /api/categories/{id}`를 `Authorization` 헤더 없이 보내도 성공한다. 등록은 201 Created, 수정은 200 OK, 삭제는 204 No Content가 오고, 실제로 반영된다.
- **확인한 방법**: 직접 curl로 확인함.
  - 등록: 로그인 토큰 없이 카테고리를 등록하자 201과 id 6이 왔다.
  - 수정: 로그인 토큰 없이 `PUT`을 보내자 200이 왔다. 그 뒤 목록에 바뀐 이름이 나왔다.
  - 삭제: 로그인 토큰 없이 id 6을 삭제하자 204가 왔다. 그 뒤 목록에서 사라졌다.
  - 이전 확인에서 등록한 테스트 카테고리 id 4도 로그인 토큰 없이 삭제했고, 204가 왔다. 지금 카테고리 목록에는 초기 데이터 id 1~3만 있다.
- **이유가 코드에 있는지**: 없음.
  - `CategoryController`의 등록 `createCategory`(`src/main/java/gift/category/CategoryController.java:31-36`), 수정 `updateCategory`(`:38-46`), 삭제 `deleteCategory`(`:48-52`)는 모두 `Authorization` 헤더를 받지 않고 인증도 확인하지 않는다.
  - 1번과 원인이 같다. 공통 인증 장치가 없고, 인증을 일부러 뺐다는 주석도 없다.
- **인수 테스트**: `src/test/resources/features/category-manage.feature` 10-1, 10-2, 10-4 (로그인 토큰 없이 등록, 수정, 삭제되는 지금 작동)
- **상태**: 미결정

### 16. 옵션 등록이 로그인 토큰 없이 성공한다

- **작동**: `POST /api/products/{productId}/options`를 `Authorization` 헤더 없이 보내도 201 Created로 옵션이 등록된다.
- **확인한 방법**: 직접 curl로 확인함. 로그인 토큰 없이 상품 1에 옵션을 등록하자 201이 왔다.
- **이유가 코드에 있는지**: 없음.
  - `OptionController.createOption`(`src/main/java/gift/option/OptionController.java:34-44`)은 `Authorization` 헤더를 받지 않고 인증도 확인하지 않는다.
  - 1번과 원인이 같다. 인증을 일부러 뺐다는 주석은 없다. 클래스 주석(`src/main/java/gift/option/OptionService.java:10-13`)에는 옵션 규칙만 있다.
- **인수 테스트**: `src/test/resources/features/option-create.feature` 11-1 (로그인 토큰 없이 등록되는 지금 작동)
- **상태**: 미결정

### 17. 옵션 삭제가 로그인 토큰 없이 성공한다

- **작동**: `DELETE /api/products/{productId}/options/{optionId}`를 `Authorization` 헤더 없이 보내도 204 No Content가 오고, 옵션이 실제로 삭제된다.
- **확인한 방법**: 직접 curl로 확인함. 16번에서 등록한 옵션을 로그인 토큰 없이 삭제하자 204가 왔다. 그 뒤 상품 1의 옵션 목록에서 사라졌다.
  - 등록한 옵션을 지웠기 때문에, 삭제할 때 상품 1에는 옵션이 3개 있었다. 그래서 "옵션이 1개뿐이면 삭제할 수 없다"는 규칙(`OptionService.java:64-66`)에는 걸리지 않았다.
- **이유가 코드에 있는지**: 없음.
  - `OptionController.deleteOption`(`OptionController.java:46-53`)은 `Authorization` 헤더를 받지 않고 인증도 확인하지 않는다.
  - 1번과 원인이 같다. 인증을 일부러 뺐다는 주석은 없다.
- **인수 테스트**: `src/test/resources/features/option-delete.feature` 12-1 (로그인 토큰 없이 삭제되는 지금 작동)
- **상태**: 미결정

### 18. 상품 수정이 로그인 토큰 없이 성공한다

> 처음에는 C-1(AI 추정)이었고, 요청으로 확인해서 이 섹션으로 옮겼다.

- **작동**: `PUT /api/products/{id}`를 `Authorization` 헤더 없이 보내도 200 OK가 오고, 상품이 실제로 수정된다.
- **확인한 방법**: 직접 curl로 확인함. 로그인 토큰 없이 `PUT`을 보내자 200이 왔다. 그 뒤 다시 조회하자 바뀐 상품명과 가격이 나왔다.
- **이유가 코드에 있는지**: 없음.
  - `ProductController.updateProduct`(`src/main/java/gift/product/ProductController.java:52-60`)는 `Authorization` 헤더를 받지 않고 인증도 확인하지 않는다.
  - 1번, 11번과 원인이 같다. 인증을 일부러 뺐다는 주석은 없다.
- **인수 테스트**: `src/test/resources/features/product-update.feature` 8-1 (로그인 토큰 없이 수정되는 지금 작동)
- **상태**: 미결정

### 19. 관리자 화면에서 포인트를 0이나 음수로 충전하면 500이 온다

- **작동**: 관리자 화면의 포인트 충전에 0이나 음수를 넣으면 500이 온다. 회원 포인트는 바뀌지 않는다. 양수(10000)는 정상적으로 충전되고 302로 목록에 돌아간다.
- **확인한 방법**: 직접 curl로 확인함. 포인트 충전에 10000을 넣으면 302로 성공했다. 0과 -100을 넣으면 500이 왔고, 실패 뒤 포인트는 그대로였다.
- **이유가 코드에 있는지**: 0 이하를 막는 규칙은 있지만, 500으로 응답하는 이유는 없다.
  - 규칙: `Member.chargePoint`가 0 이하이면 `IllegalArgumentException("Amount must be greater than zero.")`를 던진다(`src/main/java/gift/member/Member.java:48-51`). 왜 0 이하를 막는지 설명하는 주석은 없다.
  - 500인 이유: `AdminMemberController.chargePoint`(`src/main/java/gift/member/AdminMemberController.java:67-75`)에는 이 예외를 처리하는 코드가 없다. 컨트롤러에 `@ExceptionHandler`가 없고, 프로젝트에 `@ControllerAdvice`도 없다. 그래서 오류 메시지를 화면에 보여 주지 못하고 500이 된다. 같은 컨트롤러의 회원 등록은 오류를 화면에 보여 준다(`AdminMemberController.java:39-42`).
  - 포인트가 그대로인 이유: 포인트를 더하기 전에 예외가 나서(`Member.java:49-52`), 그 뒤의 `memberRepository.save(member)`(`AdminMemberController.java:73`)까지 가지 않는다.
- **상태**: 미결정

### 20. 같은 "카카오" 상품명을 API는 막고 관리자 화면은 허용한다

- **작동**: "카카오"가 들어간 상품명으로 등록하면, `POST /api/products`는 400을 돌려주지만 관리자 화면(`POST /admin/products`)은 등록에 성공한다. 관리자 화면에서는 "카카오" 상품명을 유지한 채 수정해도 성공한다.
- **확인한 방법**: API는 직접 curl로, 관리자 화면은 직접 브라우저로 확인함.
  - API(curl): 400과 함께 "담당 MD와 협의한 경우에만" 메시지가 왔다.
  - 관리자 화면(브라우저): 같은 상품명으로 등록에 성공했다. "카카오" 상품명을 유지한 채 수정해도 302가 왔고, 다시 조회하자 바뀐 값이 나왔다.
- **이유가 코드에 있는지**: 부분적으로 있다.
  - 막는 이유는 메시지에 있다: `"\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다."`(`src/main/java/gift/product/ProductNameValidator.java:33-35`)
  - 다르게 처리하는 코드: API는 `validate(name)`(`src/main/java/gift/product/ProductService.java:64`)를 호출하고, 이 메서드는 `allowKakao = false`로 검사한다(`ProductNameValidator.java:13-15`). 관리자 화면은 `validate(name, true)`로 허용한다(`src/main/java/gift/product/AdminProductController.java:45,78`).
  - 관리자 화면이 "MD와 협의한 경우"에 해당한다는 설명은 코드에 없다. 관리자 화면에서 등록하면 협의를 거친 것으로 보는지는 확인이 필요하다.
  - 참고: API의 상품 등록과 수정은 로그인 토큰 없이 된다(1번, 18번). `AdminProductController`에도 인증 확인 코드가 없다. 그래서 "카카오" 상품명 말고는 API와 관리자 화면 사이에 권한 차이가 없을 것으로 보인다. 관리자 상품 화면이 로그인 없이 열리는지는 요청으로 확인하지 않았다.
- **인수 테스트**: `src/test/resources/features/product-update.feature` 8-3 (API로 수정할 때 "카카오" 상품명을 막는 지금 작동)
- **상태**: 미결정

### 21. 관리자 화면에서 이미 삭제한 회원을 수정하면 404가 아니라 500이 온다

- **작동**: 관리자 화면에서 이미 삭제한 회원 id로 수정 요청을 보내면 404가 아니라 500이 온다.
- **확인한 방법**: 직접 curl로 확인함. 회원을 삭제한 뒤, 그 회원 id로 관리자 화면의 수정 요청을 보내자 500이 왔다.
- **이유가 코드에 있는지**: 없음. 작동을 만드는 코드는 다음과 같다.
  - `AdminMemberController.update`가 회원을 찾지 못하면 `IllegalArgumentException("Member not found. id=...")`를 던진다(`src/main/java/gift/member/AdminMemberController.java:59-61`).
  - 이 컨트롤러에는 `@ExceptionHandler`가 없고, 프로젝트에 `@ControllerAdvice`도 없다. 그래서 404로 바뀌지 않고 500이 된다.
  - 수정 화면 열기(`AdminMemberController.java:50-52`)와 포인트 충전(`:69-71`)도 같은 방식으로 회원을 찾는다. 그래서 없는 회원 id로 요청하면 똑같이 500이 될 것으로 보이지만, 요청으로는 확인하지 않았다.
- **상태**: 미결정

### 22. 관리자 화면의 회원 수정, 포인트 충전, 회원 삭제가 로그인 없이 성공한다

> 처음에는 C-3(AI 추정)이었고, 요청으로 확인해서 이 섹션으로 옮겼다. 목록 화면이 열리는 것은 13번에 있다.

- **작동**: 로그인하지 않고 로그인 토큰도 없이 관리자 화면의 회원 수정(`POST /admin/members/{id}/edit`), 포인트 충전(`POST /admin/members/{id}/charge-point`), 회원 삭제(`POST /admin/members/{id}/delete`) 요청을 보내도 302로 성공하고, 실제로 반영된다.
- **확인한 방법**: 직접 curl로 확인함. 로그인하지 않고 로그인 토큰도 없이 요청을 보냈다.
  - 회원 수정: 302가 왔다. 그 뒤 목록에 바뀐 이메일이 나왔다.
  - 포인트 충전: 10000을 넣자 302가 오고 충전되었다.
  - 회원 삭제: 302가 왔다. 그 뒤 목록에서 사라졌다.
- **이유가 코드에 있는지**: 없음.
  - `AdminMemberController`의 수정(`src/main/java/gift/member/AdminMemberController.java:57-65`), 충전(`:67-75`), 삭제(`:77-81`)에는 인증이나 권한 확인이 없다.
  - 13번과 원인이 같다. 공통 인증 장치가 없고, 관리자 기능을 일부러 열어 뒀다는 주석도 없다.
  - 참고: 누구나 회원의 이메일과 비밀번호를 바꾸고, 포인트를 충전하고, 회원을 삭제할 수 있다.
- **상태**: 미결정

### 24. 위시에 담긴 상품이나 주문이 있는 옵션을 가진 상품을 삭제하면 500이 온다

> 23번은 B 섹션(코드로만 확인)에 있다. 이 항목은 요청으로 확인해서 A 섹션에 24번으로 붙였다.

- **작동**: 위시에 담긴 상품(또는 주문이 있는 옵션을 가진 상품)을 삭제하면 500이 온다. 상품, 옵션, 위시는 그대로다.
- **확인한 방법**: 직접 curl로 확인함. 앱을 새로 켜서 초기 데이터로 되돌린 뒤 상품 1(user1의 위시 1에 들어 있음)을 삭제하자 500(Spring 기본 오류 JSON)이 왔다. 그 뒤 상품 1, 옵션 1·2(재고 10, 5), user1의 위시 2개가 모두 그대로였다.
- **이유가 코드에 있는지**: 없음. 작동을 만드는 코드는 다음과 같다.
  - `wish.product_id`, `orders.option_id` 외래 키에 `ON DELETE`가 없다(`src/main/resources/db/migration/V1__Initialize_project_tables.sql:35,55`).
  - `ProductController.deleteProduct`(`src/main/java/gift/product/ProductController.java:63-64`)와 `ProductService.deleteProduct`는 위시나 주문이 있는지 확인하지 않고 바로 `productRepository.deleteById(id)`를 부른다(`src/main/java/gift/product/ProductService.java:59-60`).
  - 이 예외를 처리하는 곳이 없어서 500이 된다. `ProductController`의 `@ExceptionHandler`는 `IllegalArgumentException`만 처리하고, 프로젝트에 `@ControllerAdvice`도 없다.
  - 옵션은 `Product.options`의 `cascade`로 함께 지워지려 하지만(`src/main/java/gift/product/Product.java:30`), 삭제가 실패하면서 함께 되돌려진다.
- **인수 테스트**: `src/test/resources/features/product-delete.feature` 9-3 (500, 상품·옵션·위시 그대로)
- **상태**: 미결정

### 25. 주문이 있는 옵션을 삭제하면 500이 온다

- **작동**: 주문에 쓰인 옵션을 삭제하면 500이 온다. 옵션은 그대로다.
- **확인한 방법**: 직접 curl로 확인함. 앱을 새로 켜서 초기 데이터로 되돌린 뒤 상품 2의 옵션 3(user1의 주문 1에 쓰임)을 삭제하자 500이 왔다. 그 뒤 상품 2의 옵션 목록에 옵션 3, 4가 그대로 있었다.
- **이유가 코드에 있는지**: 없음. 작동을 만드는 코드는 다음과 같다.
  - `orders.option_id` 외래 키에 `ON DELETE`가 없다(`src/main/resources/db/migration/V1__Initialize_project_tables.sql:55`).
  - `OptionController.deleteOption`(`src/main/java/gift/option/OptionController.java:46-53`)이 부르는 `OptionService.deleteOption`은 주문이 있는지 확인하지 않고 바로 `optionRepository.delete(option)`를 부른다(`src/main/java/gift/option/OptionService.java:73`).
  - 이 예외를 처리하는 곳이 없어서 500이 된다. `OptionController`의 `@ExceptionHandler`는 `IllegalArgumentException`만 처리하고(`OptionController.java:55`), 프로젝트에 `@ControllerAdvice`도 없다.
  - 24번(상품 삭제)과 원인이 같다.
- **인수 테스트**: `src/test/resources/features/option-delete.feature` 12-7 (500, 옵션 그대로)
- **상태**: 미결정

### 26. 같은 이름의 카테고리를 등록하면 500이 온다

- **작동**: 이미 있는 카테고리 이름으로 등록하면 500이 온다. 카테고리 목록은 그대로다.
- **확인한 방법**: 직접 curl로 확인함. 앱을 새로 켜서 초기 데이터로 되돌린 뒤 이름 "전자기기"(카테고리 1의 이름)로 등록하자 500이 왔다. 그 뒤 카테고리 목록은 1~3 그대로였다. 실패한 등록이 번호 4를 써 버려서, 다음에 등록한 카테고리는 번호 5가 되었다.
- **이유가 코드에 있는지**: 없음. 작동을 만드는 코드는 다음과 같다.
  - `category.name`에 `unique` 제약이 있다(`src/main/resources/db/migration/V1__Initialize_project_tables.sql:4`).
  - `CategoryController.createCategory`(`src/main/java/gift/category/CategoryController.java:31-36`)가 부르는 `CategoryService.createCategory`는 같은 이름이 있는지 확인하지 않고 바로 `categoryRepository.save(...)`를 부른다(`src/main/java/gift/category/CategoryService.java:20`).
  - 이 예외를 처리하는 곳이 없어서 500이 된다. `CategoryController`에는 `@ExceptionHandler`가 없고, 프로젝트에 `@ControllerAdvice`도 없다.
  - 옵션명은 같은 상품 안에서 중복을 미리 확인해 400으로 돌려주는데(`OptionService`의 "이미 존재하는 옵션명입니다."), 카테고리 이름은 그런 확인이 없다.
- **인수 테스트**: `src/test/resources/features/category-manage.feature` 10-7 (500, 목록 그대로)
- **상태**: 미결정

### 27. 상품이 있는 카테고리를 삭제하면 500이 온다

- **작동**: 상품이 속한 카테고리를 삭제하면 500이 온다. 카테고리는 그대로다.
- **확인한 방법**: 직접 curl로 확인함. 앱을 새로 켜서 초기 데이터로 되돌린 뒤 카테고리 1(상품 1, 2가 속함)을 삭제하자 500이 왔다. 그 뒤 카테고리 목록은 1~3 그대로였다.
- **이유가 코드에 있는지**: 없음. 작동을 만드는 코드는 다음과 같다.
  - `product.category_id` 외래 키에 `ON DELETE`가 없다(`src/main/resources/db/migration/V1__Initialize_project_tables.sql:17`).
  - `CategoryController.deleteCategory`(`src/main/java/gift/category/CategoryController.java:48-52`)가 부르는 `CategoryService.deleteCategory`는 상품이 있는지 확인하지 않고 바로 `categoryRepository.deleteById(id)`를 부른다(`src/main/java/gift/category/CategoryService.java:35`).
  - 이 예외를 처리하는 곳이 없어서 500이 된다. `CategoryController`에는 `@ExceptionHandler`가 없고, 프로젝트에 `@ControllerAdvice`도 없다.
  - 24번(상품 삭제), 25번(옵션 삭제)과 원인이 같다.
- **인수 테스트**: `src/test/resources/features/category-manage.feature` 10-5 (500, 카테고리 그대로)
- **상태**: 미결정

### 28. 없는 카테고리를 삭제해도 404가 아니라 204가 온다

- **작동**: 없는 카테고리 번호로 삭제해도 204가 온다. 카테고리 목록은 그대로다.
- **확인한 방법**: 직접 curl로 확인함. 앱을 새로 켜서 초기 데이터로 되돌린 뒤 카테고리 999를 삭제하자 204가 왔다. 그 뒤 카테고리 목록은 1~3 그대로였다.
- **이유가 코드에 있는지**: 없음. 작동을 만드는 코드는 다음과 같다.
  - `CategoryController.deleteCategory`(`src/main/java/gift/category/CategoryController.java:48-52`)가 부르는 `CategoryService.deleteCategory`는 카테고리가 있는지 확인하지 않고 바로 `categoryRepository.deleteById(id)`를 부른다(`src/main/java/gift/category/CategoryService.java:35`).
  - Spring Data JPA 3의 `deleteById`는 없는 번호여도 예외를 던지지 않는다. 그래서 삭제할 것이 없어도 204가 된다.
  - 상품 삭제(9-4)와 같은 이유다. `ProductService.deleteProduct`도 확인 없이 `deleteById`를 불러서, 없는 상품 999를 삭제해도 204가 온다. 반면 수정은 없는 카테고리에 404를 돌려준다(10-3).
- **인수 테스트**: `src/test/resources/features/category-manage.feature` 10-6 (204, 목록 그대로)
- **상태**: 미결정

---

## B. 코드를 읽어서 확인한 작동 (요청으로는 아직 확인 안 함)

### 9. OrderController가 WishRepository를 주입받지만 사용하지 않는다

- **작동**: `wishRepository` 필드를 선언하고 생성자에서 주입받지만, 어떤 메서드에서도 쓰지 않는다.
- **확인한 방법**: 코드 읽기. 파일 안에서 `wishRepository`는 선언(`OrderController.java:26`), 생성자 매개변수(`:34`), 대입(`:41`)에만 나온다.
- **이유가 코드에 있는지**: 직접적인 설명은 없다. 8번의 "위시 정리" 단계에 쓰려고 넣은 것으로 보이지만, 이것은 추정이다.
- **상태**: 3단계에서 지움, 근거는 05-service-layer/tasks.md 작동 변경 후보

### 10. 주문 알림 발송이 실패해도 주문은 성공한다

- **작동**: 주문 알림 발송에서 나는 모든 예외를 잡고 무시한다. 로그도 남기지 않는다. 주문은 201로 성공한다. 회원에게 카카오 액세스 토큰이 없으면 발송을 시도하지 않는다.
- **확인한 방법**: 코드 읽기 (4단계 전 `OrderService`의 `sendOrderNotificationIfPossible`. ADR 0002로 없어졌다)
- **이유가 코드에 있는지**: 부분적으로 있다. (아래는 4단계 전 코드)
  - 주석 `// best-effort kakao notification`과 메서드 이름 `sendOrderNotificationIfPossible`에서 실패해도 괜찮다는 의도가 보인다.
  - 하지만 `catch (Exception ignored) {}`는 로그도 남기지 않는다. 실패를 기록하지 않는 이유는 코드에 없다.
  - 발송은 `KakaoMessageClient.sendToMe`에서 하고, 재시도는 없다.
- **인수 테스트**: `src/test/resources/features/order-notification.feature` 16-9(실패해도 201, 시도 횟수와 마지막 오류를 남기고 재시도), 16-12(다섯 번 실패하면 "실패")
- **상태**: 4단계에서 고침, ADR 0002

### 23. JWT_SECRET 환경 변수가 없으면 저장소에 있는 예시 비밀 키로 로그인 토큰을 만든다

- **작동**: `JWT_SECRET` 환경 변수를 넣지 않고 실행하면, `application.properties`에 적힌 예시 기본값이 로그인 토큰의 서명 키가 된다. 로그인 토큰을 만들 때와 확인할 때 모두 이 키를 쓴다.
- **확인한 방법**: 코드를 읽어 확인함
  - `jwt.secret=${JWT_SECRET:...}`(`src/main/resources/application.properties:2`)
  - `JwtProvider` 생성자가 `jwt.secret`으로 서명 키를 만든다(`src/main/java/gift/auth/JwtProvider.java:21-24`).
- **이유가 코드에 있는지**: 없음. 예시 값을 기본값으로 둔 이유를 설명하는 주석이 없다. `KAKAO_CLIENT_ID`와 `KAKAO_CLIENT_SECRET`은 기본값이 비어 있는데, `JWT_SECRET`만 기본값이 들어 있다.
- **상태**: 미결정

> 8번, 14번은 요청으로 확인해서 A 섹션으로 옮겼다.

---

## C. AI 추정 (위 목록에 없던 내용)

> 아래는 위 항목의 관련 파일을 읽다가 발견한 내용이다. **요청으로는 확인하지 않았고**, 사용자가 처음 준 목록에도 없다. 확인 대상인지부터 판단해 줘야 한다.

> 지금 남은 AI 추정 항목은 없다. 모두 요청으로 확인해서 A 섹션으로 옮겼다.
> - C-1은 A 섹션 11번(상품 삭제), 18번(상품 수정)으로 옮겼다.
> - C-2는 A 섹션 12번(재고 부족 주문)으로 옮겼다.
> - C-3은 A 섹션 13번(회원 목록 화면), 22번(회원 수정, 충전, 삭제)으로 옮겼다.
