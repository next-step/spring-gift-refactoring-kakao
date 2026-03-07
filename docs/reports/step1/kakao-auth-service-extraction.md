# KakaoAuthService 추출 작업 보고서

## 작업 목적 및 배경

README.md 3-7에 해당하는 작업. KakaoAuthController에 직접 구현된 Kakao OAuth2 콜백 흐름(토큰 교환 → 사용자 정보 조회 → 회원 자동 등록 → kakaoAccessToken 저장 → JWT 발급)과 인가 URL 생성 로직을 KakaoAuthService로 추출.

기존 AuthService(이메일/비밀번호 인증)와 통합하지 않고 별도 Service로 생성. 카카오 OAuth는 외부 API 연동이 핵심이므로 단일 책임 원칙에 따라 분리.

## 변경 사항 요약

### 신규 파일 (1개)

| 파일 | 역할 |
|------|------|
| `src/main/java/gift/service/KakaoAuthService.java` | 카카오 OAuth2 인가 URL 생성 + 콜백 처리 (토큰 교환 → 회원 등록/조회 → JWT 발급) |

### 수정 파일 (1개)

| 파일 | 변경 내용 |
|------|-----------|
| `src/main/java/gift/controller/KakaoAuthController.java` | `KakaoLoginProperties` + `KakaoLoginClient` + `MemberRepository` + `JwtProvider` → `KakaoAuthService` 단일 의존. 블록 주석은 Service로 이동. |

### 의존 체인

```
KakaoAuthController → KakaoAuthService → KakaoLoginClient, KakaoLoginProperties, MemberRepository, JwtProvider
```

## 적용된 규칙

- 기존 서비스 추출 패턴과 동일하게 `@Service` 클래스로 추출
- Controller는 HTTP 관심사(리다이렉트, 응답 반환)만 담당
- Service는 비즈니스 로직(OAuth 흐름, 회원 등록/조회, JWT 발급) 담당
- `@Transactional` 미추가 (기존 동작 유지)
- KakaoLoginClient, KakaoLoginProperties 변경 없음

## 검증

- `./gradlew clean build` — BUILD SUCCESSFUL, 모든 테스트 통과
- HTTP 응답 동작 변경 없음 (동일한 302 리다이렉트, 동일한 TokenResponse)
