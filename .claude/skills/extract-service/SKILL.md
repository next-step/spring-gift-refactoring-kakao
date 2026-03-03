---
name: extract-service
description: Controller의 비즈니스 로직을 Service 계층으로 추출한다. 구조 변경만 수행하고 작동은 변경하지 않는다.
disable-model-invocation: true
---

# 3단계: 서비스 계층 추출 (구조 변경, 작동 변경 없음)

## 목표
Controller의 비즈니스 로직을 Service로 이동한다. Controller는 요청 검증과 위임만 담당하도록 얇게 만든다. **신규 기능을 추가하지 않는다.**

## 대상 범위
`src/main/java/gift/` 아래의 Controller 파일 → 각 도메인별 Service 클래스 생성

## 추출 대상 Service 목록

### 1. `MemberService`
- `MemberController.register()` → `MemberService.register(email, password)` : 이메일 중복 검증 + 회원 저장 + JWT 발급
- `MemberController.login()` → `MemberService.login(email, password)` : 회원 조회 + 비밀번호 검증 + JWT 발급
- `AdminMemberController.create()` → 이메일 중복 검증 + 회원 저장
- `AdminMemberController.update()` → 회원 조회 + 정보 업데이트
- `AdminMemberController.chargePoint()` → 회원 조회 + 포인트 충전
- `AdminMemberController.delete()` → 회원 삭제

### 2. `KakaoAuthService`
- `KakaoAuthController.callback()` → `KakaoAuthService.loginWithKakao(code)` : 토큰 교환 + 회원 자동 등록/갱신 + JWT 발급

### 3. `CategoryService`
- `CategoryController` 전체 CRUD → 조회, 생성, 수정, 삭제

### 4. `ProductService`
- `ProductController` + `AdminProductController` → 상품명 검증 + 카테고리 조회 + CRUD
- 두 컨트롤러의 중복 로직(상품명 검증, 카테고리 조회)을 하나의 Service로 통합

### 5. `OptionService`
- `OptionController` → 상품 존재 확인 + 옵션명 검증 + 중복 검증 + CRUD
- 삭제 시 "최소 1개 옵션" 비즈니스 규칙 포함

### 6. `WishService`
- `WishController` → 조회, 추가(중복 검증), 삭제(소유권 확인)

### 7. `OrderService`
- `OrderController.createOrder()` → 옵션 조회 + 재고 차감 + 포인트 차감 + 주문 저장 + 위시 정리 + 카카오 알림
- `OrderController.getOrders()` → 회원별 주문 조회

## 추출 규칙

### Controller에 남기는 것
- `@RequestMapping`, `@GetMapping` 등 HTTP 매핑
- `@Valid`, `@RequestBody` 등 요청 바인딩/검증
- `ResponseEntity` 생성 및 HTTP 상태 코드 결정
- Thymeleaf `Model` 조작 및 뷰 이름 반환

### Service로 옮기는 것
- Repository 호출
- 엔티티 조회 및 존재 여부 확인 (null 체크 → 예외)
- 비즈니스 검증 (중복 체크, 소유권 확인, 수량 검증 등)
- 엔티티 생성/수정/삭제 로직
- 외부 API 호출 (KakaoLoginClient, KakaoMessageClient)

### @Transactional 부여 기준
- 여러 엔티티를 변경하는 메서드: `OrderService.createOrder()`, `KakaoAuthService.loginWithKakao()`
- 단일 엔티티 CUD: 각 Service의 생성/수정/삭제 메서드
- 읽기 전용 조회: `@Transactional(readOnly = true)`

## 작업 순서
1. 도메인별로 하나씩 Service를 추출한다 (한 번에 한 도메인)
2. 각 Service 추출 후 `./gradlew build`로 빌드가 깨지지 않는지 확인한다
3. 기존 작동이 유지되는지 검증한다
4. 도메인별로 커밋 메시지를 제안한다

## 제약
- 기존 API 응답, HTTP 상태 코드, 동작을 변경하지 않는다
- 새로운 엔드포인트나 기능을 추가하지 않는다
- 테스트를 비활성화하거나 삭제하지 않는다
- 인증 처리 구조 변경(AOP/Interceptor 도입)은 이 단계에서 하지 않는다
