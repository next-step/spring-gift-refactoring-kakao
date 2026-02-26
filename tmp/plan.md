# Plan: 스타일 정리

스타일 불일치를 하나의 기준으로 통일한다. 작동은 안 바꿈.

## 할 일

### @author/@since Javadoc 제거
auth, member 패키지 일부 파일에만 있고 나머지엔 없으니까 없는 쪽으로 통일.
대상: AuthenticationResolver, JwtProvider, TokenResponse, AdminMemberController, Member, MemberController, MemberRequest, MemberRepository

### @Autowired 제거
생성자 1개면 Spring이 알아서 주입해주니까 필요 없음. 대부분의 클래스가 이미 안 쓰고 있음.
대상: AuthenticationResolver, JwtProvider, AdminMemberController, MemberController

### Entity 필드 빈 줄 통일
Member.java만 필드 사이에 빈 줄이 있고 나머지 엔티티는 없음. 빈 줄 없는 쪽으로 통일.

### Collectors.toList() -> .toList()
OptionController만 `Collectors.toList()` 쓰고 있고 CategoryController는 `.toList()`. Java 21 프로젝트니까 `.toList()`로 통일.

## 제약
- 작동 변경 금지
- 로직 변경 없음 (포맷/어노테이션/import만)

## 검증
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test
```
