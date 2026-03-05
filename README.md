# Spring Gift Server

선물 쇼핑 플랫폼 REST API 서버입니다.
상품 관리, 주문, 위시리스트, 포인트 시스템을 제공하며, 카카오 OAuth2 로그인과 카카오톡 메시지 알림을 지원합니다.

## 주요 기능

| 기능 | 설명 |
|------|------|
| **상품 관리** | 카테고리별 상품 분류, 상품 정보(이름·가격·이미지) CRUD, 옵션 단위 재고 관리 |
| **주문 시스템** | 인증 검증 → 재고 차감 → 포인트 차감 → 주문 저장 → 카카오톡 메시지 전송(best-effort) |
| **위시리스트** | 관심 상품 추가/삭제, 페이징 조회 지원 |
| **포인트 시스템** | 관리자 포인트 충전, 주문 시 자동 차감 (잔액 부족 시 주문 실패) |
| **인증** | JWT 토큰 발급/검증, 카카오 OAuth2 소셜 로그인 및 카카오톡 메시지 전송 |
| **관리자 UI** | Thymeleaf 기반 SSR 관리자 페이지에서 상품·카테고리 데이터 관리 |
| **상품명 검증** | 최대 15자, 허용 문자: 한글·영문·숫자·`( ) [ ] + - & / _`, "카카오" 포함 시 별도 허가 필요 |

## 기술 스택

| 구분 | 기술 |
|------|------|
| Framework | Spring Boot 3.5.9 |
| Language | Java 25 |
| Build | Gradle (Kotlin DSL) |
| Database | MySQL (운영) / H2 (테스트) |
| ORM | Spring Data JPA |
| Migration | Flyway |
| Auth | JWT (JJWT 0.13.0), 카카오 OAuth2 |
| Template | Thymeleaf (관리자 UI) |
| Test | JUnit 5, RestAssured, Cucumber BDD |

## 문서

- [API 명세](docs/API.md)
- [Architecture Decision Records](docs/ADR.md)

## 시작하기

### 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `JWT_SECRET` | JWT 서명 키 (256비트 이상) | 개발용 기본값 제공 |
| `JWT_EXPIRATION` | JWT 만료 시간 (ms) | `3600000` (1시간) |
| `KAKAO_CLIENT_ID` | 카카오 앱 REST API 키 | - |
| `KAKAO_CLIENT_SECRET` | 카카오 앱 시크릿 | - |
| `KAKAO_REDIRECT_URI` | OAuth2 콜백 URI | `http://localhost:8080/api/auth/kakao/callback` |

### 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

### 테스트

```bash
# 전체 테스트
./gradlew test

# RestAssured 통합 테스트
./gradlew test --tests "*.restassured.*"

# Cucumber BDD 테스트
./gradlew test --tests "gift.cucumber.CucumberTest"
```

## 라이선스

MIT License
