# Spring Gift Server

선물 쇼핑 플랫폼 REST API 서버입니다.
상품 관리, 주문, 위시리스트, 포인트 시스템을 제공하며, 카카오 OAuth2 로그인과 카카오톡 메시지 알림을 지원합니다.

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

## 프로젝트 구조

```
src/main/java/gift/
├── auth/           # 인증 (JWT, 카카오 OAuth2)
├── category/       # 상품 카테고리
├── product/        # 상품
├── option/         # 상품 옵션 (재고 관리)
├── order/          # 주문
├── wish/           # 위시리스트
├── member/         # 회원
└── Application.java

src/test/
├── java/gift/
│   ├── restassured/   # RestAssured 통합 테스트
│   └── cucumber/      # Cucumber BDD 테스트
└── resources/features/ # Gherkin 시나리오 (한국어)
```

## ERD

```
category ──1:N──> product ──1:N──> options ──1:N──> orders
                     ^                                  |
                     └──── wish <──── member <───────────┘
```

## API 명세

자세한 API 명세는 [docs/API.md](docs/API.md)를 참고한다.

## 주요 기능

### 주문 프로세스
1. JWT 인증 검증
2. 옵션 재고 차감
3. 포인트 차감 (상품 가격 × 수량)
4. 주문 저장
5. 카카오톡 메시지 전송 (best-effort, 실패 시 무시)

### 상품명 검증 규칙
- 최대 15자
- 허용 문자: 한글, 영문, 숫자, `( ) [ ] + - & / _`
- "카카오" 포함 시 별도 허가 필요

### 포인트 시스템
- 관리자가 회원에게 포인트 충전
- 주문 시 자동 차감 (잔액 부족 시 주문 실패)

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

## BDD 테스트 예시

```gherkin
# language: ko
기능: 상품 관리

  배경:
    먼저 "전자기기" 카테고리가 등록되어 있다

  시나리오: 상품을 생성하면 목록에서 조회된다
    만일 "아이폰 16" 이름과 1350000 가격으로 상품을 생성하면
    그러면 상품 목록의 크기는 1이다
    그리고 상품 목록에 "아이폰 16" 이름이 포함되어 있다
```
