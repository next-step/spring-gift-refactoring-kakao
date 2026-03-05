# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 작업 원칙

### 계획 우선
- 계획 파일(`README.md` 체크리스트 등)에 다음 작업이 명시되어 있어야 코드 수정을 시작한다.
- "다음 변경 1개"처럼 범위를 제한하고, 지금 할 일이 명확한 상태에서만 진행한다.

### TDD 루프 유지
- Red → Green → Refactor 순서로 진행한다.
- 최소 요구 사항: **변경 후 전체 테스트 통과** (`./gradlew test`).
- 테스트를 회피하거나 비활성화하지 않는다. 그런 흔적이 보이면 즉시 되돌린다.
- **작동 변경은 반드시 증거와 함께 제출한다.** 예외 발생 여부만 확인하는 것으로 충분하지 않다. 상태를 재조회하거나 결과를 관찰 가능한 방식으로 검증해야 한다.

### 구조 변경과 작동 변경 분리
- 구조 변경(리팩터링)은 구조만, 작동 변경(기능 추가/수정)은 작동만 다룬다.
- 한 커밋에는 둘 중 하나만 담는다.
- 구조 변경 커밋과 작동 변경 커밋을 섞지 않는다.

### 과잉 금지
- 요청하지 않은 기능, 불필요한 추상화, 반복·복잡도 증가를 만들지 않는다.
- AI 산출물은 초안일 뿐이다. 의도하지 않은 변경이 있으면 즉시 제거한다.

### 커밋 규칙
- 커밋은 목적 1개, 설명 가능한 단일 논리 단위로 구성한다.
- `git diff`를 보고 30초 안에 커밋 의도를 설명할 수 없으면 더 쪼갠다.

### 스타일
- 스타일 이슈는 도구로 해결한다: IDE 포매터, 린터 등 우선 사용.

### ADR (Architecture Decision Record)
- ADR은 선택이 아니라, 다음 조건 중 하나라도 해당하면 작성한다:
  - 선택지가 2개 이상이고 트레이드오프가 있었던 경우
  - 팀이 반복해서 따라야 할 규칙이나 경계를 정한 경우
  - 테스트 전략과 검증 방식이 결정의 핵심이었던 경우

## 빌드 및 개발 명령어

```bash
# 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "fully.qualified.ClassName"

# 특정 테스트 메서드 실행
./gradlew test --tests "fully.qualified.ClassName.methodName"
```

## 아키텍처

**기술 스택:** Spring Boot 3.5.9, Java 21, Gradle (Kotlin DSL)

**데이터베이스:** H2 (개발/테스트), MySQL (운영). Flyway로 마이그레이션 관리 (`src/main/resources/db/migration/`)

**인증:** JWT 토큰 (jjwt 라이브러리) + 카카오 OAuth2 로그인. 인증이 필요한 엔드포인트는 `AuthenticationResolver`가 `Authorization` 헤더에서 회원 정보를 추출한다.

### 도메인 구조 (`gift/` 패키지)

| 패키지      | 설명 |
|------------|------|
| `auth`     | JWT 발급/검증, 카카오 OAuth2 로그인 흐름, 인증 리졸버 |
| `member`   | 회원 계정 (이메일/비밀번호, 카카오 토큰, 포인트 잔액) |
| `category` | 상품 카테고리 |
| `product`  | 상품 (카테고리에 소속, 여러 옵션 보유). 커스텀 이름 검증기 (최대 15자, 허용 문자 제한, "카카오" 기본 차단) |
| `option`   | 상품 옵션 (재고 수량 관리). 커스텀 이름 검증기 (최대 50자) |
| `order`    | 선물 주문 — 포인트 차감, 재고 차감, 카카오톡 알림 전송 (실패 시 무시) |
| `wish`     | 회원 위시리스트 (회원 ↔ 상품) |

각 패키지는 **Entity → Repository → Controller + Request/Response DTO** 구조를 따른다.

### 컨트롤러 유형
- **REST API 컨트롤러** (`/api/...`) — 클라이언트 앱용 JSON 엔드포인트
- **관리자 MVC 컨트롤러** (`/admin/...`) — Thymeleaf 서버 렌더링 페이지 (회원, 상품 관리)

### 주요 흐름
- **주문 생성:** 인증 확인 → 옵션 검증 → 재고 차감 → 회원 포인트 차감 → 주문 저장 → 위시리스트 정리 → 카카오 메시지 전송 (실패 허용)
- **카카오 로그인:** 카카오 리다이렉트 → 인가 코드로 콜백 → 액세스 토큰 교환 → 회원 자동 등록 또는 갱신 → JWT 발급

## 환경 변수

`application.properties`에 기본값이 설정되어 있음:
- `JWT_SECRET` / `JWT_EXPIRATION` — JWT 서명 키 및 토큰 만료 시간
- `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` / `KAKAO_REDIRECT_URI` — 카카오 OAuth2 인증 정보

## API 명세서

코드 수정 시 아래 명세와 실제 구현이 일치하는지 확인한다. 불일치가 발견되면 명세를 기준으로 삼는다.

### 회원 API

| URL | 메서드 | 기능 |
|-----|--------|------|
| `/api/members/register` | POST | 회원 가입 — 새 회원을 등록하고 토큰을 받는다 |
| `/api/members/login` | POST | 로그인 — 회원을 인증하고 토큰을 받는다 |

### 카테고리 API

| URL | 메서드 | 기능 |
|-----|--------|------|
| `/api/categories` | POST | 카테고리 생성 |
| `/api/categories/{categoryId}` | PUT | 카테고리 수정 |
| `/api/categories` | GET | 카테고리 목록 조회 |

### 상품 API

| URL | 메서드 | 기능 |
|-----|--------|------|
| `/api/products` | POST | 상품 생성 |
| `/api/products/{productId}` | GET | 상품 조회 |
| `/api/products/{productId}` | PUT | 상품 수정 |
| `/api/products/{productId}` | DELETE | 상품 삭제 |
| `/api/products?page=0&size=10&sort=name,asc&categoryId=1` | GET | 상품 목록 조회 (페이지네이션) |

### 상품 옵션 API

| URL | 메서드 | 기능 |
|-----|--------|------|
| `/api/products/{productId}/options` | POST | 상품 옵션 추가 |
| `/api/products/{productId}/options/{optionId}` | PUT | 상품 옵션 수정 |
| `/api/products/{productId}/options/{optionId}` | DELETE | 상품 옵션 삭제 |
| `/api/products/{productId}/options` | GET | 상품 옵션 목록 조회 |

### 위시 리스트 API

| URL | 메서드 | 기능 |
|-----|--------|------|
| `/api/wishes` | POST | 위시 리스트 상품 추가 |
| `/api/wishes/{wishId}` | DELETE | 위시 리스트 상품 삭제 |
| `/api/wishes?page=0&size=10&sort=createdDate,desc` | GET | 위시 리스트 조회 (페이지네이션) |

### 주문 API

| URL | 메서드 | 기능 |
|-----|--------|------|
| `/api/orders` | POST | 주문하기 |
| `/api/orders?page=0&size=10&sort=orderDateTime,desc` | GET | 주문 목록 조회 (페이지네이션) |

## 프로젝트 컨벤션

- 언어: **Java**
- Bean Validation (`spring-boot-starter-validation`)과 커스텀 `ConstraintValidator` 구현체로 검증 수행
- 페이지네이션 엔드포인트는 Spring의 `Pageable` 사용
