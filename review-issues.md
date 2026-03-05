# Step2 리팩터링 검토 항목

## 작동 변경 (증거 필요)

### D-1. Admin "카카오" 상품명 검증 버그
- **파일**: `AdminProductController.java:43`, `ProductService.java:39,69`
- **내용**: Admin에서 `validateName(name, allowKakao=true)` 통과 후, `create()` 내부 `validateNameOrThrow()`가 `allowKakao=false`로 재검증 → "카카오" 포함 이름 저장 불가
- **심각도**: 높음
- **검토 결과**: **수정 확정**. `ProductService.create()`에 `allowKakao` 전달 필요. Admin은 MD 협의 전제로 카카오 이름 허용이 설계 의도.

### B-1. Option.subtractQuantity() 음수/0 수량 미검증
- **파일**: `Option.java:45-50`
- **내용**: `amount <= 0` 검증 없음. 음수 전달 시 `this.quantity -= (-5)` → 재고 증가. `OrderRequest`의 `@Min(1)` 제약이 API 경로를 막지만, 도메인 객체 자체는 무방비
- **심각도**: 높음
- **검토 결과**: **수정 확정**. 도메인 객체는 자기 방어해야 함. `amount <= 0` 검증 추가.

### B-4. 상품 연결된 카테고리 삭제 시 500 오류
- **파일**: `CategoryService.java:42-45`
- **내용**: `product.category_id` FK 제약 존재. 상품이 연결된 카테고리 삭제 → `DataIntegrityViolationException` → `GlobalExceptionHandler` 미처리 → 500
- **심각도**: 중간
- **검토 결과**: **수정 확정**. 사전 검증으로 연관 상품 존재 시 명확한 에러 메시지 반환.

### B-5. 위시/주문 있는 상품 삭제 시 500 오류
- **파일**: `ProductService.java:64-67`
- **내용**: `Wish.product_id`, `Order.option_id` FK 존재. cascade 미설정. 위시/주문이 있는 상품 삭제 → FK 위반 → 500
- **심각도**: 중간
- **검토 결과**: **수정 확정**. 위시는 자동 삭제(상품과 함께 정리), 주문 이력 있는 상품은 삭제 금지.

### T-2. 이메일 중복 TOCTOU + 500 오류
- **파일**: `AdminMemberController.java:39-47`, `MemberService.java:31-36`
- **내용**: `existsByEmail()` (트랜잭션 A) → `register()` (트랜잭션 B) 사이에 동일 이메일 삽입 가능. `DataIntegrityViolationException` 미처리 → 500. DB `UNIQUE` 제약이 최종 방어선
- **심각도**: 낮음
- **검토 결과**: **수정 확정**. 컨트롤러의 중복 `existsByEmail` 제거, `MemberService.register()`의 예외를 활용.

### D-6. 트랜잭션 밖 지연 로딩 잠재 위험
- **파일**: `WishResponse.java:4-11`, `ProductResponse.java`
- **내용**: `open-in-view=false` 설정에서 컨트롤러의 `Page.map(WishResponse::from)`이 `Wish.product` 접근. 현재 `@ManyToOne` 기본값 EAGER로 동작하지만 명시 없음. LAZY 전환 시 즉시 오류
- **심각도**: 낮음 (잠재적)
- **검토 결과**: **제외**. `open-in-view` 설정이 실제로 존재하지 않음. Spring Boot 기본값 `true`로 OSIV 활성화 상태이므로 현재 위험 없음.

## 구조 변경 (작동 불변)

### D-2. AuthenticationResolver의 Repository 직접 의존
- **파일**: `AuthenticationResolver.java:14-32`
- **내용**: `auth` 패키지가 `MemberRepository`에 직접 의존. `MemberService`를 통해 접근해야 레이어 경계 준수. 또한 `@Transactional` 없이 DB 조회
- **검토 결과**: **수정 확정**. `MemberService`를 통해 접근하도록 변경.

### D-3. KakaoMessageClient 내 가격 계산 중복
- **파일**: `KakaoMessageClient.java:38-58`
- **내용**: `product.getPrice() * order.getQuantity()` 가격 계산이 인프라 레이어에 존재. `Option.calculateTotalPrice()`와 중복
- **검토 결과**: **수정 확정**. 가격 계산을 도메인에서 가져와 인프라 레이어의 중복 제거.

### D-5. ProductService/OptionService의 Repository 직접 의존
- **파일**: `ProductService.java:17,40-43`, `OptionService.java`
- **내용**: `ProductService`가 `CategoryRepository`에, `OptionService`가 `ProductRepository`에 직접 의존. 각각의 서비스를 통해 접근해야 레이어 경계 준수
- **검토 결과**: **수정 확정**. `CategoryService`, `ProductService`를 통해 접근하도록 변경.

### T-3. OptionService.delete() 비효율 쿼리
- **파일**: `OptionService.java:46-63`
- **내용**: `findByProductId()` 전체 목록 로딩 후 `size()` 비교. `COUNT` 쿼리로 대체 가능
- **검토 결과**: **수정 확정**. `COUNT` 쿼리로 대체.

### B-6. Option 생성자 수량 제약 부재
- **파일**: `Option.java` (생성자), `OptionRequest.java:7`
- **내용**: `@Min(1)`, `@Max(99_999_999)` 제약이 DTO에만 존재. 도메인 객체 생성자에 검증 없음. `SeedOptionController` 등 DTO 우회 경로에서 무효 수량 삽입 가능
- **검토 결과**: **수정 확정**. 생성자에 수량 검증 추가. B-1과 동일 원칙(도메인 자기 방어).

### B-7. WishService의 ProductRepository 직접 의존
- **파일**: `WishService.java:26-38`
- **내용**: `WishService`가 `ProductRepository`에 직접 의존. `ProductService`를 통해 접근하거나 `NoSuchElementException`을 던지는 방식이 더 명확
- **검토 결과**: **수정 확정**. `ProductService`를 통해 접근하도록 변경. D-5와 동일 원칙.

## 설계 판단 (현재 허용 가능)

### B-2. 옵션별 가격 차별화 불가
- **파일**: `Option.java:41-43`
- **내용**: `Option.calculateTotalPrice()`가 `product.getPrice()` 사용. 옵션별 가격 필드 없음
- **검토 결과**: **보류**. 현재 요구사항에 옵션별 가격 명세 없음.

### D-4. memberId primitive FK 비일관성
- **파일**: `Order.java:29-31`, `Wish.java:23`
- **내용**: `Wish`, `Order`는 `memberId`를 `Long`으로 저장. `Option`은 `@ManyToOne` 사용. 엔티티 매핑 비일관
- **검토 결과**: **보류**. 의도적 느슨한 결합일 수 있음.
