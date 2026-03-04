# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 현재 작업

**Step 2: 작동 변경** 진행 중. 상세 계획은 `docs/step2-plan.md`, 체크리스트는 `README.md`, ADR은 `docs/step2-adr.md` 참조.

## Step 2 목표

작동 변경을 안전하게 수행하고, 그 결과를 증거로 보여준다.

- **트랜잭션 경계 세우기** — 여러 저장 작업이 하나의 논리 작업이라면, 중간 실패에서 부분 반영이 발생하지 않도록 경계를 설정한다.
- **누락된 작동 구현** — 기존 코드에 의도가 남아 있었지만 구현되지 않은 작동을 완료한다. 새로운 작동은 반드시 테스트 또는 검증 가능한 증거로 확인한다.
- **도메인 책임 되찾기** — 작동을 유지하면서도 책임과 계산, 판단을 올바른 위치로 이동해 누수와 중복을 줄인다.

## 작업 원칙

### 계획 우선
- `README.md` 체크리스트에서 다음 단계 1개를 확인한 뒤 코드 수정을 시작한다.
- `docs/step2-plan.md`에 해당 단계의 변경 파일, 코드 스니펫이 정리되어 있다.
- 변경 전에 **"무엇을 바꾸는지, 무엇을 바꾸지 않는지, 무엇이 이를 증명하는지"**를 확인하고 시작한다.

### 작동 변경은 증거와 함께
- 예외가 발생하는지만 확인하는 것으로 충분하지 않다.
- **상태를 재조회하거나 결과를 관찰 가능한 방식으로 검증**해야 한다.

### 구조 변경과 작동 변경 분리
- 한 커밋에는 구조 변경 또는 작동 변경 중 하나만 담는다.
- 구조 변경 커밋과 작동 변경 커밋을 분리한다.

### TDD 루프 유지
- Red → Green → Refactor 순서로 진행한다.
- 최소 요구 사항: **변경 후 전체 테스트 통과** (`./gradlew test`).
- 테스트를 회피하거나 비활성화하지 않는다.

### ADR
- 선택지가 2개 이상이고 트레이드오프가 있었던 경우
- 팀이 반복해서 따라야 할 규칙이나 경계를 정한 경우
- 테스트 전략과 검증 방식이 결정의 핵심이었던 경우
- → 위 조건 중 하나라도 해당하면 `docs/step2-adr.md`에 ADR을 작성한다.

### 과잉 금지
- 요청하지 않은 기능, 불필요한 추상화를 만들지 않는다.
- AI 산출물은 초안일 뿐이다. 의도하지 않은 변경이 있으면 즉시 제거한다.
- `git diff`를 자주 확인해 의도하지 않은 변경이 들어오지 않도록 통제한다.

### 커밋 규칙
- 커밋은 목적 1개, 설명 가능한 단일 논리 단위로 구성한다.
- `git diff`를 보고 30초 안에 커밋 의도를 설명할 수 없으면 더 쪼갠다.

## 빌드 및 개발 명령어

```bash
./gradlew build        # 빌드
./gradlew bootRun      # 애플리케이션 실행
./gradlew test         # 전체 테스트 실행
./gradlew test --tests "fully.qualified.ClassName"           # 특정 클래스
./gradlew test --tests "fully.qualified.ClassName.methodName" # 특정 메서드
```

## 아키텍처

**기술 스택:** Spring Boot 3.5.9, Java 21, Gradle (Kotlin DSL)

**데이터베이스:** H2 (개발/테스트), MySQL (운영). Flyway로 마이그레이션 관리 (`src/main/resources/db/migration/`)

**인증:** JWT 토큰 (jjwt) + 카카오 OAuth2. `AuthenticationResolver`가 `Authorization` 헤더에서 회원 정보를 추출한다.

### 레이어 구조

각 도메인 패키지(`auth`, `member`, `category`, `product`, `option`, `order`, `wish`)는 **Entity → Repository → Service → Controller + DTO** 구조를 따른다.

- **REST API 컨트롤러** (`/api/...`) — JSON 엔드포인트
- **관리자 MVC 컨트롤러** (`/admin/...`) — Thymeleaf 서버 렌더링

### 주요 흐름
- **주문 생성:** 인증 → 옵션 검증 → 재고 차감 → 포인트 차감 → 주문 저장 → 위시 정리 → 카카오 알림 (실패 허용)
- **카카오 로그인:** 인가 코드 → 액세스 토큰 교환 → 회원 자동 등록/갱신 → JWT 발급

## 환경 변수

`application.properties`에 기본값 설정:
- `JWT_SECRET` / `JWT_EXPIRATION`
- `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` / `KAKAO_REDIRECT_URI`

## 프로젝트 컨벤션

- 언어: **Java**
- Bean Validation + 커스텀 `ConstraintValidator`로 검증 수행
- 페이지네이션은 Spring `Pageable` 사용
