# Progress 9: Auth 도메인 — AuthMemberRepository 제거 + AuthenticationPort 계약 확장 + 단위 테스트

**기간**: 2026-03-05

---

## 이 문서의 목적

Phase 2 "도메인 책임 되찾기" — 다섯 번째 Service 교체 작업. `auth/internal/` 패키지의 `KakaoAuthService`, `AuthenticationPortAdaptor`, `JwtPortAdaptor` 3개 클래스가 사용하던 크로스 도메인 `AuthMemberRepository`를 `MemberQueryPort` + `MemberCommandPort`로 교체하고, `AuthenticationPort`에 필수 인증 계약(`getMemberIdFrom`)을 추가했다. 단위 테스트 9개를 구성했다.

---

## 1. 변경 내용

### 1-1. AuthMemberRepository 제거 (구조 변경)

`AuthMemberRepository`를 사용하던 3개 클래스를 Port로 교체했다.

| 클래스                         | Before                                                              | After                                                                 |
|-----------------------------|---------------------------------------------------------------------|-----------------------------------------------------------------------|
| `KakaoAuthService`          | `memberRepo.findByEmail(email)` → `Optional<Member>` find-or-create | `memberQueryPort.getIdByEmail(email)` + try-catch `NotFoundException` |
| `AuthenticationPortAdaptor` | `memberRepo.findByEmail(email).map(Member::getId)`                  | `memberQueryPort.getIdByEmail(email)` + try-catch                     |
| `JwtPortAdaptor`            | `memberRepo.findById(id).orElseThrow().getEmail()`                  | `memberQueryPort.getEmail(id)`                                        |

#### KakaoAuthService 재구성

```java
// Before
Member member = memberRepository.findByEmail(email)
        .orElseGet(() -> Member.builder().email(email).build());
member.updateKakaoAccessToken(kakaoToken.accessToken());
memberRepository.save(member);

// After
try {
    Long memberId = memberQueryPort.getIdByEmail(email);
    memberCommandPort.updateKakaoAccessToken(memberId, accessToken);
} catch (NotFoundException e) {
    memberCommandPort.create(new MemberInfo(email, null, accessToken));
}
```

- `member.updateKakaoAccessToken()` 직접 호출 제거 — Port 컨벤션 준수
- `MemberQueryPort`에 Optional 반환 메서드를 추가하지 않고, 기존 `getIdByEmail()` + try-catch로 처리

#### 논의: Optional 반환 vs try-catch

| 선택지                                   | 방식                                       | 채택    |
|---------------------------------------|------------------------------------------|-------|
| MemberQueryPort에 `findIdByEmail()` 추가 | Optional 반환, Port 인터페이스 확장               | X     |
| 기존 `getIdByEmail()` + try-catch       | NotFoundException을 제어 흐름에 사용, Port 변경 없음 | **O** |

Port 인터페이스를 최소한으로 유지하는 방향을 선택했다.

#### 삭제 파일

| 파일                          | 이유                       |
|-----------------------------|--------------------------|
| `AuthMemberRepository.java` | 사용처 없음 (3개 클래스 모두 교체 완료) |

### 1-2. AuthenticationPort 계약 확장 (작동 변경)

기존 `AuthenticationPort`는 `Optional<Long>` 반환만 제공했다. 모든 Controller에서 `.orElseThrow(UnauthorizedException::new)` 패턴이 반복되므로, 필수 인증 계약을 Port에 추가했다.

| 메서드                                  | 반환               | 실패 시                    | 용도                        |
|--------------------------------------|------------------|-------------------------|---------------------------|
| `requestMemberIdFrom(authorization)` | `Optional<Long>` | `Optional.empty()`      | 선택적 인증 (현재 사용처 없음, 향후 대비) |
| `getMemberIdFrom(authorization)`     | `Long`           | `UnauthorizedException` | 필수 인증 (모든 Controller)     |

#### AuthenticationPortAdaptor 구현 구조

`getMemberIdFrom()`이 핵심 로직을 담당하고, `requestMemberIdFrom()`은 이를 try-catch로 감싸는 구조로 재구성했다.

```java
@Override
public Long getMemberIdFrom(String authorization) {
    String token = removeBearerPrefix(authorization);
    try {
        String memberEmail = jwtProvider.getEmail(token);
        return memberQueryPort.getIdByEmail(memberEmail);
    } catch (JwtException | NotFoundException e) {
        throw new UnauthorizedException("Invalid authorization");
    }
}

@Override
public Optional<Long> requestMemberIdFrom(String authorization) {
    try {
        return Optional.of(this.getMemberIdFrom(authorization));
    } catch (UnauthorizedException ignored) {
        return Optional.empty();
    }
}
```

#### Controller 변경

`WishController`(3곳), `OrderController`(2곳) — `getMemberIdFrom().orElseThrow(UnauthorizedException::new)` → `getMemberIdFrom()`. Controller에서 `UnauthorizedException` 직접 참조 제거.

### 1-3. 클래스 리네이밍

| Before                   | After                       |
|--------------------------|-----------------------------|
| `AuthenticationPortImpl` | `AuthenticationPortAdaptor` |
| `JwtPortImpl`            | `JwtPortAdaptor`            |

### 1-4. 단위 테스트 (9개)

| 테스트 파일                          | 테스트 수 | 검증 내용                                                                                         |
|---------------------------------|-------|-----------------------------------------------------------------------------------------------|
| `KakaoAuthServiceTest`          | 2개    | 기존 회원 → `updateKakaoAccessToken`, 신규 회원 → `create(MemberInfo)`                                |
| `AuthenticationPortAdaptorTest` | 5개    | `requestMemberIdFrom` 3개(성공, 회원없음, 토큰무효) + `getMemberIdFrom` 2개(성공, 실패→UnauthorizedException) |
| `JwtPortAdaptorTest`            | 2개    | JWT 발급 성공 + 회원 없음 → NotFoundException 전파                                                      |

---

## 2. 수정/삭제 파일 목록

| 파일                                   | 변경                                                                     |
|--------------------------------------|------------------------------------------------------------------------|
| `AuthenticationPort.java`            | `getMemberIdFrom()` 필수 인증 계약 추가, 기존 메서드 `requestMemberIdFrom()`으로 리네이밍 |
| `AuthenticationPortAdaptor.java`     | `AuthMemberRepository` → `MemberQueryPort`, 두 계약 구현                    |
| `KakaoAuthService.java`              | `AuthMemberRepository` → `MemberQueryPort` + `MemberCommandPort`       |
| `JwtPortAdaptor.java`                | `AuthMemberRepository` → `MemberQueryPort`                             |
| `WishController.java`                | `getMemberIdFrom()` 사용, `UnauthorizedException` import 제거              |
| `OrderController.java`               | `getMemberIdFrom()` 사용, `UnauthorizedException` import 제거              |
| `AuthMemberRepository.java`          | **삭제**                                                                 |
| `KakaoAuthServiceTest.java`          | **신규** — 2개 테스트                                                        |
| `AuthenticationPortAdaptorTest.java` | **신규** — 5개 테스트                                                        |
| `JwtPortAdaptorTest.java`            | **신규** — 2개 테스트                                                        |

---

## 3. 검증 결과

```bash
./gradlew test      # BUILD SUCCESSFUL (전체 단위 테스트 통과)
./gradlew acceptanceTest  # BUILD SUCCESSFUL (기존 75개 시나리오 통과)
```

---

## 4. Member 크로스 도메인 Repository 제거 현황

| Repository              | 사용자                                                         | Port 교체 | Repository 삭제 |
|-------------------------|-------------------------------------------------------------|---------|---------------|
| `AuthMemberRepository`  | KakaoAuthService, AuthenticationPortAdaptor, JwtPortAdaptor | **완료**  | **완료**        |
| `OrderMemberRepository` | OrderService                                                | 미착수     | 미착수           |

---

## 5. 전체 Port 교체 대상 Service 현황

| Service             | 교체 대상 Repository                 | 교체할 Port                                | 상태              |
|---------------------|----------------------------------|-----------------------------------------|-----------------|
| ProductService      | `ProductCategoryRepository`      | `CategoryQueryPort`                     | 완료 (progress-5) |
| AdminProductService | `AdminProductCategoryRepository` | `CategoryQueryPort`                     | 완료 (progress-6) |
| WishService         | `WishProductRepository`          | `ProductQueryPort`                      | 완료 (progress-7) |
| OptionService       | `OptionProductRepository`        | `ProductQueryPort`                      | 완료 (progress-8) |
| Auth (3개 클래스)       | `AuthMemberRepository`           | `MemberQueryPort` + `MemberCommandPort` | **완료**          |
| OrderService        | `OrderOptionRepository`          | `OptionQueryPort` + `OptionCommandPort` | 미착수             |
| OrderService        | `OrderMemberRepository`          | `MemberQueryPort` + `MemberCommandPort` | 미착수             |
