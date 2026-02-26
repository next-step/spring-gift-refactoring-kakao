---
name: write-test
description: 현재 작동을 보존하기 위한 테스트 코드를 작성한다. TDD 루프(Red → Green → Refactor)의 기반이 되는 테스트를 만든다.
disable-model-invocation: true
---

# 테스트 코드 작성

## 목표
리팩터링 전 현재 작동을 검증하는 테스트를 작성한다. 이 테스트가 통과하는 상태에서 구조 변경을 진행해야 "변경 후 전체 테스트 통과" 원칙을 지킬 수 있다.

## 테스트 환경
- **프레임워크**: JUnit 5 + Spring Boot Test
- **언어**: Java
- **위치**: `src/test/java/gift/` (기존 소스 패키지 구조와 동일)
- **테스트 설정**: `src/test/resources/application.properties` (필요 시 생성)
- **DB**: H2 인메모리 (runtimeOnly 의존성 존재)
- **검증 명령**: `./gradlew test`

## 테스트 작성 원칙

### TDD 루프
1. **Red**: 실패하는 테스트를 먼저 작성한다
2. **Green**: 테스트가 통과하는 최소한의 코드를 작성한다
3. **Refactor**: 테스트가 통과하는 상태에서 구조를 정리한다

### 한 번에 한 테스트
- 테스트를 한 개 작성하고 `./gradlew test`로 확인한다
- 통과를 확인한 후 다음 테스트로 넘어간다

## 테스트 대상 및 유형

### 1. 단위 테스트 (Unit Test) — 먼저 작성

Entity 비즈니스 로직을 검증한다. 외부 의존성 없이 순수 Java로 작성한다.

**Member**
| 메서드 | 정상 케이스 | 에러 케이스 |
|--------|-----------|-----------|
| `chargePoint(amount)` | 양수 금액 충전 → 포인트 증가 | 0 이하 → IllegalArgumentException |
| `deductPoint(amount)` | 잔액 이내 차감 → 포인트 감소 | 0 이하 → IllegalArgumentException |
| `deductPoint(amount)` | 잔액과 동일 금액 차감 → 0 | 잔액 초과 → IllegalArgumentException |

**Option**
| 메서드 | 정상 케이스 | 에러 케이스 |
|--------|-----------|-----------|
| `subtractQuantity(amount)` | 재고 이내 차감 → 수량 감소 | 재고 초과 → IllegalArgumentException |
| `subtractQuantity(amount)` | 재고와 동일 수량 차감 → 0 | |

**Validator**
| 클래스 | 정상 케이스 | 에러 케이스 |
|--------|-----------|-----------|
| `ProductNameValidator` | 허용 문자 15자 이내 → 빈 리스트 | 16자 이상 / 특수문자 / "카카오" 포함 → 에러 리스트 |
| `OptionNameValidator` | 허용 문자 50자 이내 → 빈 리스트 | 51자 이상 / 특수문자 → 에러 리스트 |

### 2. 통합 테스트 (Integration Test) — 단위 테스트 이후

`@SpringBootTest` + `MockMvc`로 컨트롤러 엔드포인트를 검증한다.

**MemberController** (`/api/members`)
- POST `/register` → 201 + TokenResponse
- POST `/register` 중복 이메일 → 400
- POST `/login` 정상 → 200 + TokenResponse
- POST `/login` 잘못된 비밀번호 → 400

**CategoryController** (`/api/categories`)
- GET `/` → 200 + 목록
- POST `/` → 201
- PUT `/{id}` → 200
- PUT `/{id}` 존재하지 않는 ID → 404
- DELETE `/{id}` → 204

**ProductController** (`/api/products`)
- GET `/` → 200 + Page
- POST `/` → 201
- POST `/` 이름 검증 실패 → 400
- PUT `/{id}` → 200
- DELETE `/{id}` → 204

**OptionController** (`/api/products/{productId}/options`)
- GET `/` → 200 + 목록
- POST `/` → 201
- POST `/` 중복 옵션명 → 400
- DELETE `/{optionId}` → 204
- DELETE 마지막 옵션 → 400

**WishController** (`/api/wishes`) — 인증 필요
- GET `/` 인증 있음 → 200
- GET `/` 인증 없음 → 401
- POST `/` → 201
- DELETE `/{id}` 본인 위시 → 204
- DELETE `/{id}` 타인 위시 → 403

**OrderController** (`/api/orders`) — 인증 필요
- GET `/` → 200
- POST `/` → 201 (재고 차감 + 포인트 차감 확인)
- POST `/` 인증 없음 → 401

## 작업 순서
1. `src/test/resources/application.properties` 테스트 설정 생성 (H2, Flyway)
2. Entity 단위 테스트 작성 → `./gradlew test`
3. Validator 단위 테스트 작성 → `./gradlew test`
4. Controller 통합 테스트 작성 (도메인별 하나씩) → `./gradlew test`
5. 전체 테스트 통과 확인 → `./gradlew test`

## 제약
- 테스트를 위해 프로덕션 코드를 변경하지 않는다
- 테스트를 `@Disabled`로 비활성화하지 않는다
- 각 테스트는 독립적으로 실행 가능해야 한다 (테스트 간 순서 의존 금지)
