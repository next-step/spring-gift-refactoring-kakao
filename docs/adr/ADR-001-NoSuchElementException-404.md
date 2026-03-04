# ADR-001: NoSuchElementException 응답 코드를 404로 변경

## 상태
채택됨

## 맥락
기존에 `NoSuchElementException`은 `GlobalExceptionHandler`에서 **400 Bad Request**로 처리되고 있었다.
그러나 "요청한 리소스가 존재하지 않는다"는 의미는 HTTP 404 Not Found가 의미론적으로 정확하다.

400은 "클라이언트가 잘못된 요청을 보냈다"는 의미이며, 존재하지 않는 리소스 조회는 요청 형식의 문제가 아니라
서버 측에서 해당 리소스를 찾을 수 없는 상황이다.

## 결정
`NoSuchElementException` 핸들러의 응답 코드를 **400 → 404**로 변경한다.

## 근거
- RFC 7231: 404는 "서버가 대상 리소스의 현재 표현을 찾지 못했음"을 의미
- 400은 "서버가 클라이언트 오류로 인해 요청을 처리할 수 없음"을 의미
- API 소비자 입장에서 404를 받으면 리소스 부재를, 400을 받으면 요청 형식 오류를 의심한다
- `IllegalArgumentException`(유효성 검증 실패)과 구분이 명확해진다

## 영향
- 기존에 `NoSuchElementException`으로 400을 기대하던 클라이언트는 404를 받게 됨
- Cucumber "선물하기가 실패한다" step은 `>= 400` 검증이므로 변경 불필요
- H2 인수 테스트 G4의 기대값을 400 → 404로 수정
