# ADR: 외부 API 클라이언트 인터페이스 추출

## 상태

승인됨 (2026-03-04)

## 맥락

리팩토링 완료 후 코드를 점검한 결과, 외부 API를 호출하는 구체 클래스(`KakaoMessageClient`, `KakaoLoginClient`)를 서비스/리스너가 직접 의존하고 있었다.

문제점:
- `OrderCompletedEventListener`가 `KakaoMessageClient`를 직접 의존하여 카카오 외 다른 메시지 프로바이더로 교체할 수 없음
- `KakaoAuthController`가 `KakaoLoginClient`를 직접 의존하여 OAuth 프로바이더 변경 시 컨트롤러 수정 필요
- 단위 테스트 시 외부 HTTP 호출을 가짜 구현으로 대체하기 어려움

## 선택지

| 선택지 | 장점 | 단점 |
|--------|------|------|
| A. 인터페이스 추출 | 프로바이더 교체 용이, 테스트 시 가짜 구현 주입 가능, 의존 역전 원칙 준수 | 인터페이스 + record 파일 추가 (5개) |
| B. 현행 유지 | 파일 수 적음, 단순 | 외부 API 결합도 높음, 테스트 시 HTTP 모킹 필요 |
| C. @MockBean으로 테스트만 해결 | 프로덕션 코드 변경 없음 | 설계 개선 없이 테스트만 우회, 결합도 그대로 |

## 결정

**선택지 A**: 인터페이스 추출

### 메시지 전송: `OrderMessageClient`

```
OrderMessageClient (interface)
└── KakaoMessageClient (구현체, @Component)

의존 방향:
OrderCompletedEventListener → OrderMessageClient ← KakaoMessageClient
```

- `OrderCompletedEventListener`는 `OrderMessageClient` 인터페이스만 의존
- 이벤트 필드명/로그 메시지도 프로바이더 중립적으로 통일 (`accessToken`, "주문 알림 메시지 전송 실패")

### OAuth 로그인: `OAuthLoginClient`

```
OAuthLoginClient (interface)
├── OAuthTokenResponse (record) — 프로바이더 중립 반환 타입
├── OAuthUserResponse (record) — 프로바이더 중립 반환 타입
└── KakaoLoginClient (구현체, @Component)
    ├── KakaoTokenResponse (package-private record) — 카카오 JSON 역직렬화용
    └── KakaoUserResponse (package-private record) — 카카오 JSON 역직렬화용

의존 방향:
KakaoAuthController → OAuthLoginClient ← KakaoLoginClient
```

- `KakaoLoginClient` 내부의 카카오 전용 record는 package-private으로 유지 (JSON 매핑용)
- 반환 시 프로바이더 중립 record(`OAuthTokenResponse`, `OAuthUserResponse`)로 변환

### 보류: `JwtProvider`

`JwtProvider`는 외부 API를 호출하지 않는 내부 인프라(HMAC-SHA 서명)이므로 인터페이스 추출을 보류했다.
POJO로 이미 단위 테스트가 가능하며, JWT 라이브러리 교체 가능성이 낮다.

## 결과

- 외부 API 의존이 인터페이스 뒤로 숨겨져 프로바이더 교체가 구현체 변경만으로 가능
- 단위 테스트 시 가짜 구현 주입으로 외부 HTTP 호출 없이 검증 가능
- 신규 파일 5개: `OrderMessageClient`, `OAuthLoginClient`, `OAuthTokenResponse`, `OAuthUserResponse` (인터페이스/record)
- 변경 파일 4개: `KakaoMessageClient`, `KakaoLoginClient`, `OrderCompletedEventListener`, `KakaoAuthController`