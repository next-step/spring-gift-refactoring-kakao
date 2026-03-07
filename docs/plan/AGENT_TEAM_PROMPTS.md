# Agent Team Prompts - Step 2 작업 실행

> 작성일: 2026-03-05
> 목표: TODO_STEP2의 5개 작업을 에이전트 팀으로 분담하여 실행한다.

---

## 팀 구성

| 에이전트 | 담당 작업 | 유형 |
|---------|----------|------|
| agent-1-transaction | 작업 1: 트랜잭션 경계 세우기 | 작동 변경 |
| agent-2-wish-cleanup | 작업 2: 주문 시 위시리스트 정리 | 작동 변경 |
| agent-3-category-filter | 작업 3: 카테고리별 상품 필터링 | 작동 변경 |
| agent-4-domain-price | 작업 4: 주문 금액 계산 도메인 이동 | 구조 변경 |
| agent-5-validate-dedup | 작업 5: validateName 중복 제거 | 구조 변경 |

---

## 공통 지침

```
## 공통 규칙
- 프로젝트 루트: /Users/max.716/Documents/GitHub/spring-gift-refactoring-kakao
- Java Spring Boot 프로젝트 (패키지: gift)
- 테스트는 @SpringBootTest + MockMvc 기반 통합 테스트 패턴을 사용한다.
- KakaoMessageClient는 @MockitoBean으로 모킹한다.
- 기존 테스트가 깨지면 안 된다. 변경 후 반드시 `./gradlew test`로 전체 테스트를 실행한다.

## 작업 흐름 (2단계로 진행)
### [Phase 1] 실행 계획 보고서 생성
- 변경할 파일 목록과 각 파일의 변경 내용을 구체적으로 명시
- 새로 추가할 테스트 케이스 설명
- 바꾸지 않는 것 명시
- `docs/reports/` 디렉토리에 계획 보고서를 md로 작성
- 작성 완료 후 팀 리더에게 메시지를 보낸다
- **팀 리더는 사용자에게 계획 보고서 검토를 요청한다**
- **사용자가 승인할 때까지 Phase 2로 진행하지 않는다**

### [Phase 2] 실행 (사용자 승인 후)
- 사용자의 승인을 받은 뒤, 팀 리더가 해당 에이전트에게 실행을 지시한다
- 계획 보고서 대로 코드를 변경한다
- 테스트를 작성하고 `./gradlew test`로 전체 테스트를 실행한다
- 테스트가 통과하면 작업 보고서를 `docs/reports/`에 작성한다
- 작업 완료 후 팀 리더에게 완료 메시지를 보낸다
```

---

## Agent 1: 트랜잭션 경계 세우기

```
# 작업 1: 트랜잭션 경계 세우기

## 목표
OrderService.createOrder()에 @Transactional을 추가하여 주문의 원자성을 보장한다.

## 현황 분석
- `src/main/java/gift/service/OrderService.java`
  - createOrder()가 3개 독립 DB 저장 수행: 재고 차감, 포인트 차감, 주문 저장
  - @Transactional 없음 → 중간 실패 시 부분 반영 발생
  - kakaoNotificationService.sendOrderNotification()은 외부 API 호출

## 변경 대상 파일
1. `src/main/java/gift/service/OrderService.java`
   - createOrder()에 @Transactional 추가
   - findByMemberId()에 @Transactional(readOnly = true) 추가
   - 주의: kakao 알림은 트랜잭션 커밋 후 best-effort로 유지
     - 방법: 알림 호출을 트랜잭션 범위 밖으로 분리하거나,
       트랜잭션 내에서 호출하되 실패해도 롤백되지 않도록 현재 try-catch 구조 유지

2. `src/test/java/gift/controller/OrderControllerTest.java` (새 테스트 추가)
   - 테스트: 포인트 부족 시 주문 실패 → 재고가 원래대로 유지됨을 재조회로 확인
   - 기존 createOrderInsufficientPoints 테스트는 상태코드만 확인하므로,
     새 테스트에서 재고 롤백까지 검증

## 바꾸지 않는 것
- 카카오 알림 로직 (KakaoNotificationService)
- 기존 테스트의 동작

## 커밋 메시지
feat(order): 트랜잭션 경계 추가로 주문 원자성 보장
```

---

## Agent 2: 주문 시 위시리스트 정리

```
# 작업 2: 누락된 작동 구현 - 주문 시 위시리스트 정리

## 목표
주문 시 해당 상품이 회원의 위시리스트에 있으면 자동 제거한다.

## 현황 분석
- `src/main/java/gift/service/OrderService.java` - 주문 생성 시 위시 정리 로직 없음
- `src/main/java/gift/service/WishService.java` - removeByMemberIdAndProductId 메서드 없음
- `src/main/java/gift/repository/WishRepository.java` - findByMemberIdAndProductId는 있으나 deleteByMemberIdAndProductId 없음
- 주문 흐름 7단계 중 6단계 "cleanup wish"가 미구현 상태

## 변경 대상 파일
1. `src/main/java/gift/repository/WishRepository.java`
   - deleteByMemberIdAndProductId(Long memberId, Long productId) 추가

2. `src/main/java/gift/service/WishService.java`
   - removeByMemberIdAndProductId(Long memberId, Long productId) 메서드 추가
   - 위시에 없으면 아무 일도 하지 않음 (예외 없이 정상 진행)

3. `src/main/java/gift/service/OrderService.java`
   - WishService 의존성 추가 (생성자 주입)
   - createOrder()에서 주문 저장 후, 위시리스트 정리 호출
   - option.getProduct().getId()로 productId를 얻어 wishService.removeByMemberIdAndProductId() 호출

4. `src/test/java/gift/controller/OrderControllerTest.java` (새 테스트 추가)
   - 테스트 1: 위시리스트에 상품 추가 → 해당 상품 주문 → 위시리스트에서 제거됨 재조회 확인
   - 테스트 2: 위시리스트에 없는 상품 주문 → 정상 처리됨 확인 (이미 기존 테스트가 커버)

## 바꾸지 않는 것
- 기존 위시 CRUD API (WishController, WishService의 기존 메서드)
- 위시리스트에 없는 상품을 주문하는 기존 흐름

## 의존성 주의
- 작업 1(트랜잭션)이 먼저 완료되어야 위시 정리가 트랜잭션 안에 포함됨
- OrderService에 WishService 주입 시 순환 의존성 확인 필요 (WishService -> ProductService, OrderService -> WishService -> ProductService: 순환 없음)

## 커밋 메시지
feat(order): 주문 시 위시리스트 자동 정리 구현
```

---

## Agent 3: 카테고리별 상품 필터링

```
# 작업 3: 누락된 작동 구현 - 상품 목록 카테고리별 필터링

## 목표
GET /api/products?categoryId=1 호출 시 해당 카테고리의 상품만 반환하도록 구현한다.

## 현황 분석
- `src/main/java/gift/controller/ProductController.java` - getProducts()에 categoryId 파라미터 없음
- `src/main/java/gift/service/ProductService.java` - findAll(Pageable)이 전체 조회만 수행
- `src/main/java/gift/repository/ProductRepository.java` - 카테고리 필터링 쿼리 메서드 없음

## 변경 대상 파일
1. `src/main/java/gift/repository/ProductRepository.java`
   - Page<Product> findByCategoryId(Long categoryId, Pageable pageable) 추가

2. `src/main/java/gift/service/ProductService.java`
   - findAll(Pageable pageable) → findAll(Long categoryId, Pageable pageable)로 변경
   - categoryId가 null이면 전체 조회, 값이 있으면 카테고리 필터링
   - 기존 findAll(Pageable) 시그니처를 유지하면서 오버로딩하거나,
     categoryId 파라미터를 추가하여 null 허용

3. `src/main/java/gift/controller/ProductController.java`
   - getProducts()에 @RequestParam(required = false) Long categoryId 추가
   - productService.findAll(categoryId, pageable) 호출

4. `src/test/java/gift/controller/ProductControllerTest.java` (새 테스트 추가)
   - 테스트 1: 카테고리 A, B 각각에 상품 등록 → categoryId=A로 조회 → A 카테고리 상품만 반환 확인
   - 테스트 2: categoryId 없이 조회 → 전체 상품 반환 확인

## 바꾸지 않는 것
- categoryId 없이 호출하는 기존 동작 (전체 조회 유지)
- 기존 상품 CRUD 동작
- AdminProductController (별도 관리자 컨트롤러)

## 커밋 메시지
feat(product): 카테고리별 상품 목록 필터링 구현
```

---

## Agent 4: 주문 금액 계산 도메인 이동

```
# 작업 4: 도메인 책임 되찾기 (1) - 주문 금액 계산을 도메인으로 이동

## 목표
가격 계산(price * quantity)을 서비스에서 Option 도메인 객체로 이동한다.

## 현황 분석
- `src/main/java/gift/service/OrderService.java` (line 39)
  - `var price = option.getProduct().getPrice() * quantity;`
  - 디미터 법칙 위반 (열차 충돌 호출: option -> product -> price)
  - 가격 계산 책임이 서비스에 존재

## 변경 대상 파일
1. `src/main/java/gift/model/Option.java`
   - calculateTotalPrice(int quantity) 메서드 추가
   ```java
   public int calculateTotalPrice(int quantity) {
       return product.getPrice() * quantity;
   }
   ```

2. `src/main/java/gift/service/OrderService.java`
   - 기존: `var price = option.getProduct().getPrice() * quantity;`
   - 변경: `var price = option.calculateTotalPrice(quantity);`

3. `src/test/java/gift/model/OptionTest.java` (새 테스트 추가)
   - Option.calculateTotalPrice() 단위 테스트
   - 정상 계산 확인 (가격 10000, 수량 3 → 30000)

## 바꾸지 않는 것
- 계산 결과 (가격 x 수량)는 동일
- 기존 OrderControllerTest 전체 통과 필수

## 주의사항
- 이것은 구조 변경이므로 작동이 바뀌면 안 됨
- 기존 OptionTest.java가 이미 존재하므로 해당 파일에 테스트 추가

## 커밋 메시지
refactor(option): 주문 금액 계산을 Option 도메인으로 이동
```

---

## Agent 5: validateName 중복 제거

```
# 작업 5: 도메인 책임 되찾기 (2) - validateName 중복 제거

## 목표
ProductService와 OptionService에 중복된 validateName 메서드를 NameValidator로 통합한다.

## 현황 분석
- `src/main/java/gift/service/ProductService.java` (lines 61-66)
  ```java
  private void validateName(String name) {
      List<String> errors = NAME_VALIDATOR.validate(name);
      if (!errors.isEmpty()) {
          throw new IllegalArgumentException(String.join(", ", errors));
      }
  }
  ```

- `src/main/java/gift/service/OptionService.java` (lines 69-74)
  ```java
  private void validateName(String name) {
      List<String> errors = NAME_VALIDATOR.validate(name);
      if (!errors.isEmpty()) {
          throw new IllegalArgumentException(String.join(", ", errors));
      }
  }
  ```

- 두 메서드가 95% 동일 (에러 수집 → 예외 변환 패턴 반복)

## 변경 대상 파일
1. `src/main/java/gift/common/NameValidator.java`
   - validateOrThrow(String name) 메서드 추가
   ```java
   public void validateOrThrow(String name) {
       List<String> errors = validate(name);
       if (!errors.isEmpty()) {
           throw new IllegalArgumentException(String.join(", ", errors));
       }
   }
   ```

2. `src/main/java/gift/service/ProductService.java`
   - private void validateName(String name) 메서드 삭제
   - 호출부를 NAME_VALIDATOR.validateOrThrow(name)으로 변경 (create, update 메서드 내)

3. `src/main/java/gift/service/OptionService.java`
   - private void validateName(String name) 메서드 삭제
   - 호출부를 NAME_VALIDATOR.validateOrThrow(name)으로 변경 (create 메서드 내)

## 바꾸지 않는 것
- 검증 규칙 (maxLength, allowedCharacters, noKakao) 변경 없음
- 예외 메시지 형식 동일
- NameValidator.validate() 메서드는 그대로 유지 (validateOrThrow만 추가)

## 증거
- 기존 NameValidatorTest 전체 통과
- 기존 ProductControllerTest, OptionControllerTest 전체 통과

## 커밋 메시지
refactor(validator): validateName 중복 제거
```

---

## 의존성 및 실행 순서

```
작업 4, 5 (구조 변경) --- 독립적, 병렬 실행 가능
작업 1 (트랜잭션)     --- 독립적, 병렬 실행 가능
작업 3 (카테고리 필터) --- 독립적, 병렬 실행 가능
작업 2 (위시 정리)    --- 작업 1에 의존 (트랜잭션 안에 위시 정리가 포함되어야 함)
```

- **병렬 가능**: 작업 1, 3, 4, 5는 서로 다른 파일을 수정하므로 동시 진행 가능
- **순차 필요**: 작업 2는 작업 1(트랜잭션) 완료 후 진행 (OrderService.java 수정이 겹침)
