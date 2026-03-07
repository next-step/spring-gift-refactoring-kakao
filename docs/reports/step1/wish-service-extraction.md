# WishService 추출 + HandlerMethodArgumentResolver 도입 작업 보고서

> 작업 일시: 2026-02-26

## 작업 목적 및 배경

README.md 3-5 항목에 따라 `WishController`에 직접 구현된 비즈니스 로직을 `WishService`로 추출하고, 인증 보일러플레이트(`extractMember` + null 체크)를 `HandlerMethodArgumentResolver`로 대체한다.

### 기존 문제
- `WishController`에 인증 확인, 상품 조회, 위시 중복 확인, 소유권 검증 로직이 모두 혼재
- 인증 보일러플레이트가 WishController(3곳) + OrderController(2곳) = 총 5곳에서 반복
- `OrderController`에 `WishRepository`가 주입되어 있으나 미사용

## 변경 사항 요약

### 신규 파일 (6개)

| 파일 | 설명 |
|------|------|
| `gift/auth/LoginMember.java` | `@LoginMember` 커스텀 어노테이션 (파라미터용) |
| `gift/auth/LoginMemberArgumentResolver.java` | `@LoginMember Member member` 파라미터를 Authorization 헤더로부터 해석 |
| `gift/exception/UnauthorizedException.java` | 인증 실패 시 401 응답을 위한 예외 |
| `gift/exception/ForbiddenException.java` | 권한 부족 시 403 응답을 위한 예외 |
| `gift/config/WebConfig.java` | `WebMvcConfigurer` — ArgumentResolver 등록 |
| `gift/service/WishService.java` | 위시리스트 비즈니스 로직 (CRUD + 소유권 검증) |

### 수정 파일 (3개)

| 파일 | 변경 내용 |
|------|-----------|
| `gift/exception/GlobalExceptionHandler.java` | `UnauthorizedException` → 401, `ForbiddenException` → 403 핸들러 추가 |
| `gift/controller/WishController.java` | `WishRepository`, `ProductRepository`, `AuthenticationResolver` 의존 → `WishService` 단일 의존으로 변경. `@RequestHeader("Authorization")` → `@LoginMember Member member`로 변경 |
| `gift/controller/OrderController.java` | `AuthenticationResolver` + `WishRepository` 의존 제거. `@RequestHeader("Authorization")` → `@LoginMember Member member`로 변경 (2곳) |

## WishService 메서드 구성

| 메서드 | 설명 |
|--------|------|
| `findByMemberId(Long, Pageable)` | 회원별 위시 목록 조회 (페이징) |
| `findByMemberIdAndProductId(Long, Long)` | 회원+상품 기준 위시 조회 (중복 확인용) |
| `addWish(Long, Long)` | 상품 존재 검증 + 위시 생성 |
| `removeWish(Long, Long)` | 위시 조회 + 소유권 검증 + 삭제 |
| `deleteByMemberIdAndProductId(Long, Long)` | OrderService 연동용 (주문 시 위시 정리) |

## 인증 보일러플레이트 제거 (HandlerMethodArgumentResolver)

### 기존 패턴 (5곳에서 반복)
```java
@RequestHeader("Authorization") String authorization
// ...
var member = authenticationResolver.extractMember(authorization);
if (member == null) {
    return ResponseEntity.status(401).build();
}
```

### 변경 후
```java
@LoginMember Member member
// ArgumentResolver가 자동으로 인증 처리. 실패 시 UnauthorizedException → 401
```

## 적용된 규칙 및 컨벤션

- **구조 변경만 수행**: 기존 HTTP 응답 동작(200/201/204/401/403/404) 그대로 유지
- **WishController.addWish 200 vs 201 보존**: 중복 위시 → 200, 신규 생성 → 201 구분 유지 (컨트롤러에서 먼저 확인 후 분기)
- **GlobalExceptionHandler 401/403**: 기존 Controller에서 `ResponseEntity<Void>`를 반환하던 것과 동일하게 body 없이 반환
- **OrderController WishRepository 제거**: 미사용 의존성 정리 (분석 보고서에서 확인)

## 참고 사항

- `@LoginMember`는 `Member.class`를 파라미터 타입으로 기대. 다른 타입에는 적용되지 않음
- `LoginMemberArgumentResolver`는 기존 `AuthenticationResolver`에 위임하여 JWT 파싱 및 회원 조회를 수행
