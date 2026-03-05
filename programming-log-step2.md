# step2 리팩토링 로그

## 1. 예외처리

### 1-1. 배경

Service 계층 도입 이후 예외 처리가 Controller별 `@ExceptionHandler`와 `IllegalArgumentException` 중심으로 분산되어 있었다.  
이로 인해 에러 응답 정책과 메시지 관리 지점이 흩어져, 변경 시 일관성을 유지하기 어려웠다.

### 1-2. 수정사항

| 항목 | 내용 |
|---|---|
| 공통 구조 추가 | `BaseErrorCode`, `BaseException`, `ErrorResponse`, `GlobalExceptionHandler` |
| auth 도메인 | `AuthErrorCode` 추가, `AuthenticationException`/`ForbiddenException`을 `BaseException` 기반으로 전환 |
| member 도메인 | `MemberErrorCode`, `MemberException` 추가, `MemberService`/`AdminMemberService`/`Member` 예외 전환 |
| 컨트롤러 정리 | `MemberController` 로컬 `@ExceptionHandler` 제거(전역 처리로 이관) |

### 1-3. 기대효과

1. 예외 응답 정책을 전역에서 통합 관리할 수 있다.
2. 도메인별 에러코드로 상태코드/메시지 의도가 명확해진다.
3. Controller는 요청/응답 흐름에 집중하고, 예외 처리 중복을 줄일 수 있다.

---

