# MemberService + AuthService 추출 작업 보고서

> 작업 일시: 2026-02-26

## 작업 목적 및 배경

README.md 3-1항에 따라, MemberController와 AdminMemberController에 직접 구현되어 있던 비즈니스 로직을 Service 계층으로 추출한다. 인증 관련 로직은 `AuthService`로, 회원 관리 로직은 `MemberService`로 분리하여 단일 책임 원칙을 적용한다.

## 변경 사항 요약

### 신규 파일

| 파일 | 설명 |
|------|------|
| `src/main/java/gift/service/AuthService.java` | 인증 서비스. `register()`, `login()` 메서드 제공. 이메일 중복 확인, 비밀번호 검증, JWT 발급 로직 담당. |
| `src/main/java/gift/service/MemberService.java` | 회원 관리 서비스. `findAll()`, `findById()`, `create()`, `update()`, `chargePoint()`, `delete()` 메서드 제공. |

### 수정된 파일

| 파일 | 변경 내용 |
|------|-----------|
| `MemberController.java` | `MemberRepository` + `JwtProvider` 직접 호출 → `AuthService`에 위임. 비즈니스 로직 제거. |
| `AdminMemberController.java` | `MemberRepository` 직접 호출 → `MemberService`에 위임. 비즈니스 로직 제거. |

### 책임 분리 구조

```
MemberController (REST API)
  └─ AuthService: register(), login()
       └─ MemberRepository, JwtProvider

AdminMemberController (Admin Thymeleaf)
  └─ MemberService: findAll(), findById(), create(), update(), chargePoint(), delete()
       └─ MemberRepository
```

## 적용된 규칙

- 구조 변경만 수행 (작동 변경 없음). 기존 API 동작, 에러 메시지, HTTP 상태 코드 모두 동일하게 유지.
- `docs/analysis/member-controller.md` 분석 보고서의 권장 작업 순서를 따름.
- 리팩토링 후 `./gradlew clean build`로 전체 테스트 통과 확인.

## 참고 사항

- `AdminMemberController.create()`에서 이메일 중복 시 `MemberService`가 던지는 `IllegalArgumentException`을 catch하여 기존과 동일하게 에러 폼을 렌더링한다.
- 기존 `MemberControllerTest`는 통합 테스트로 그대로 유지되며, Service 도입 후에도 모든 테스트가 통과한다.
