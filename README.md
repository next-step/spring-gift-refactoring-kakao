# spring-gift-refactoring

### Description

Spring Boot 기반의 선물하기 커머스 플랫폼입니다.
상품 관리, 회원 인증, 위시리스트, 주문 처리 및 카카오톡 연동 기능을 제공합니다.

## 개발 환경 및 실행 방법

### 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 21, Kotlin 1.9 |
| Framework | Spring Boot 3.5.9 |
| Database | MySQL (운영), H2 (테스트) |
| ORM | Spring Data JPA |
| Migration | Flyway 12.0.1 |
| Auth | JWT (JJWT 0.13.0), Kakao OAuth2 |
| Template | Thymeleaf |
| Build | Gradle (Kotlin DSL) |
| Code Style | ktlint |

### 실행 방법

1. `.env` 파일을 프로젝트 루트에 생성하고 환경 변수를 설정합니다.
   ```
   JWT_SECRET=<256비트 이상의 시크릿 키>
   JWT_EXPIRATION=3600000
   KAKAO_CLIENT_ID=<카카오 앱 REST API 키>
   KAKAO_CLIENT_SECRET=<카카오 앱 시크릿 키>
   KAKAO_REDIRECT_URI=http://<로컬 서버 도메인>/api/auth/kakao/callback
   ```
2. MySQL 데이터베이스를 준비합니다.
3. 애플리케이션을 실행합니다.
   ```bash
   ./gradlew bootRun
   ```

## 기능 목록

### 회원 (Member)
- 이메일/비밀번호 기반 회원가입 및 로그인
- 카카오 OAuth2 로그인 (자동 회원가입 포함)
- JWT 기반 인증/인가
- 포인트 충전 및 차감

### 상품 (Product)
- 상품 CRUD (페이지네이션 지원)
- 상품명 유효성 검증 (최대 15자, 허용 특수문자, 한글 지원)
- 카테고리별 상품 분류

### 카테고리 (Category)
- 카테고리 CRUD

### 옵션 (Option)
- 상품별 옵션 관리 (사이즈, 색상 등)
- 옵션명 유효성 검증 (최대 50자)
- 재고 수량 관리

### 주문 (Order)
- 인증된 회원의 주문 생성
- 옵션 재고 차감 및 포인트 차감
- 카카오톡 메시지 알림 발송 (best-effort)
- 주문 내역 조회 (페이지네이션 지원)

### 위시리스트 (Wish)
- 상품 위시리스트 추가/삭제
- 중복 등록 방지
- 본인 위시리스트만 삭제 가능