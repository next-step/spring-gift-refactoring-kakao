---
name: refactor-service
description: 리팩터링 Phase 3 Service Layer 추출. 사용자가 "Service 추출", "Controller 분리", "계층 분리", "Service Layer" 등을 언급하면 이 스킬을 사용한다.
---

# Phase 3: Service Layer 추출

Controller에 있는 비즈니스 로직을 Service 계층으로 분리한다.
Controller는 **요청 수신 + 응답 반환**만 담당하도록 얇게(Thin) 유지한다.

---

## 추출 순서

의존성이 적은 도메인부터 진행한다:

```
1. CategoryService   (의존성 없음, 가장 단순)
2. MemberService     (인증 로직 포함)
3. ProductService    (Category 의존)
4. OptionService     (Product 의존)
5. WishService       (Member + Product 의존)
6. OrderService      (Member + Option + 외부 API 의존, 가장 복잡)
```

---

## 추출 절차 (도메인 하나당)

```
1. README.md에 작업 항목 확인
2. ./gradlew test → 현재 상태 확인
3. Service 클래스 생성 (빈 클래스 + @Service)
4. Controller의 비즈니스 로직을 Service로 이동
   - Repository 호출 → Service로
   - 데이터 가공/검증 로직 → Service로
   - DTO 변환 → Service로
5. Controller에서 Service 주입 및 호출로 변경
6. ./gradlew test → 동작 유지 확인
7. 커밋
```

---

## 템플릿

- Service 구조: `service-template.java` 참조
- Controller 구조: `controller-template.java` 참조

---

## Controller에 남겨야 할 것
- `@RequestMapping`, `@GetMapping` 등 라우팅 어노테이션
- `@Valid`, `@RequestBody` 등 요청 바인딩
- `ResponseEntity` 반환 (HTTP 상태 코드 결정)
- `@ExceptionHandler` (컨트롤러 레벨 예외 처리)

## Service로 이동해야 할 것
- Repository 호출 (`findAll`, `findById`, `save`, `delete`)
- 비즈니스 검증 로직 (중복 체크, 권한 확인, 재고 확인 등)
- 데이터 가공 (Entity → DTO 변환)
- 트랜잭션이 필요한 복합 연산 (주문 처리: 재고 차감 + 포인트 차감)
- 외부 API 호출 (Kakao 메시지 전송)

---

## 커밋 예시
```
refactor(category): CategoryService 생성 및 비즈니스 로직 이동
refactor(product): ProductService 생성 및 Controller에서 로직 분리
refactor(order): OrderService 생성 및 주문 처리 로직 이동
```

## 주의사항
- 한 커밋에 하나의 도메인 Service만 추출
- 로직 이동 과정에서 기능을 추가하거나 변경하지 않음
- `@Transactional`은 Service 메서드에 필요한 경우만 추가
- Controller의 `@ExceptionHandler`는 이 단계에서 옮기지 않음 (별도 커밋)
