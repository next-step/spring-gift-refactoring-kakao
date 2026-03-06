# ADR-002: 비밀번호 인코딩 책임을 MemberService에 집중

## 상황 (Context)

- `passwordEncoder.encode(password)` 호출이 `AuthService.register()`, `MemberService.create()`, `MemberService.update()` 3곳에 분산되어 있었음
- 새로운 회원 생성 경로가 추가될 때 인코딩을 빠뜨릴 위험이 있음
- 인코딩 정책(BCrypt → Argon2 등) 변경 시 여러 곳을 수정해야 함

## 결정 (Decision)

비밀번호 인코딩은 `MemberService`에서만 수행한다.

- `MemberService.create()`: 인코딩 + 저장
- `MemberService.update()`: 인코딩 + 수정
- `AuthService.register()`: `MemberService.create()`에 위임

## 이유 (Rationale)

- 인코딩 로직이 한 곳에만 존재하므로 누락/불일치 위험 제거
- JPA 엔티티(`Member`)에 스프링 인프라(`PasswordEncoder`) 의존성을 넣지 않아 관심사 분리 유지
- 변경 범위가 최소(AuthService만 수정)

## 대안

1. **Member 엔티티 생성자에서 인코딩** — 인코딩 누락 방지력은 강하지만, 엔티티가 PasswordEncoder에 의존하여 테스트 복잡도 증가
2. **정적 팩토리 메서드** — 팩토리를 우회할 수 있어 강제성이 약함
