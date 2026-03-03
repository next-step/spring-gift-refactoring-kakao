# spring-gift-refactoring

## 1단계 리팩터링 작업 기록

### 테스트 코드 마이그레이션

별도 프로젝트(`spring-gift-test-kakao`)에 있던 Cucumber 인수 테스트를 현재 프로젝트로 옮겼다.
두 프로젝트의 API 구조가 달라서 단순 복사로는 테스트가 실패했고, 아래 항목들을 수정해야 했다.

- **Step Definition 수정**: 엔드포인트 경로, 요청/응답 형식, 인증 방식(Member-Id 헤더 → JWT Bearer 토큰)을 현재 API에 맞게 변경
- **테스트 환경**: 원본 프로젝트가 Docker 기반이었으므로 동일하게 Docker Compose(MySQL + 앱 컨테이너)로 구성. 처음에 H2 인메모리로 간단하게 시작했다가, 원본과 동일한 환경을 유지하기
  위해 Docker로 전환했다
- **민감 정보 분리**: docker-compose.yml에 하드코딩된 비밀번호, JWT 시크릿 등을 `.env` 파일로 분리하고 `.gitignore`에 추가
- **Gradle 설정**: `test` task에서 Cucumber 엔진을 제외하고, `cucumberTest` task를 별도로 분리하여 Docker 의존 테스트와 단위 테스트가 독립 실행되도록 구성

**Claude Code 활용 과정에서의 의사결정:**

- 테스트 코드를 옮긴 뒤 API 구조 차이로 테스트가 실패하는 상황에서, Claude에게 현재 프로젝트의 API 구조를 분석시킨 뒤 Step Definition을 일괄 수정하도록 지시했다
- Docker 빌드 시 Gradle 데몬의 PATH 문제가 발생했는데, Claude가 절대 경로 하드코딩을 시도하길래 `sh -lc` 방식으로 수정하도록 유도했다

---

### 스타일 정리

프로젝트 전반의 스타일 불일치를 찾아 일관되게 정리했다.

- `@Autowired` 생성자 주입 어노테이션 제거 (Spring 4.3+ 단일 생성자 자동 주입)
- 코드만으로 명확한 `@author`, `@since` Javadoc 제거
- 미사용 블록 주석 제거
- `Collectors.toList()` → `.toList()` 통일 (Java 16+)

**Claude Code 활용:**

- Claude에게 프로젝트 전체를 스캔하여 스타일 불일치 항목을 목록으로 뽑도록 지시한 뒤, 한 번에 수정하고 단일 커밋으로 정리했다

---

### 불필요한 코드 제거

- `OrderController`에서 주입만 받고 실제로 사용하지 않는 `WishRepository` 필드, 생성자 파라미터, import를 제거
- 함께 있던 미구현 "cleanup wish" 주석도 제거 (2단계에서 OrderService에 새로 구현 예정)

**Claude Code 활용:**

- Claude가 코드를 읽으면서 미사용 필드를 자동으로 식별했다. 삭제 전에 해당 주석의 의도(위시리스트 정리 기능이 미구현)를 확인하고, 2단계에서 Service에 구현할 것이므로 안전하게 제거할 수 있다고
  판단했다

---

### Service 계층 추출

Controller에 직접 작성되어 있던 비즈니스 로직을 Service로 이동했다. 도메인별로 커밋을 분리하여 7개 Service를 추출했다.

| 커밋 | Service          | 주요 변경                                       |
|----|------------------|---------------------------------------------|
| 3  | CategoryService  | CRUD 위임, update는 Optional 반환으로 기존 404 동작 유지 |
| 4  | ProductService   | API/Admin 컨트롤러가 공유, Admin은 폼 검증을 직접 처리      |
| 5  | MemberService    | 회원가입/로그인/포인트 충전 로직 이동                       |
| 6  | OptionService    | 옵션명 검증, 중복 검사, 최소 1개 보장 로직 이동               |
| 7  | WishService      | 소유자 검증 로직 이동, ForbiddenException 도입         |
| 8  | OrderService     | 재고 차감, 포인트 차감, 주문 저장, 카카오 알림 이동             |
| 9  | KakaoAuthService | OAuth 콜백, 회원 자동등록 로직 이동                     |

마지막으로 각 Controller에 흩어진 `@ExceptionHandler`를 `GlobalExceptionHandler`로 통합했다.

**Claude Code 활용 과정에서의 의사결정:**

- Claude에게 "구조 변경과 동작 변경을 한 커밋에 섞지 말 것"을 명시적으로 지시했다. 이 원칙 덕분에 Service 추출 중 새 기능 추가나 응답 코드 변경이 섞이는 것을 방지할 수 있었다
- Service 메서드에서 엔티티를 못 찾았을 때 예외를 던지면 기존 404 응답이 500으로 바뀌는 문제를 Claude가 스스로 감지하여, `Optional` 반환 또는 `@ExceptionHandler` 추가로
  기존 동작을 유지했다
- `GlobalExceptionHandler` 통합 시 `IllegalArgumentException`을 글로벌로 올리면 OrderController에서 500→400으로 바뀌는 문제도 Claude가 식별하여,
  해당 핸들러는 개별 Controller에 유지하는 판단을 내렸다

---

### 단위 테스트 추가

코드 리뷰에서 "인수 테스트만으로 리팩터링 안정성을 확보하기 어렵다"는 피드백을 받고, 핵심 비즈니스 로직에 대한 단위 테스트 44개를 추가했다.

- **도메인 테스트**: Member(포인트, 비밀번호), Option(재고 차감), 이름 검증기
- **서비스 테스트(Mockito)**: 주문 흐름, 위시 소유자 검증, 옵션 최소 1개 보장, 회원 인증

**Claude Code 활용:**

- Claude에게 전체 Service/도메인 코드를 분석시켜 "깨지면 안 되는 규칙" 목록을 뽑고, 그 목록 기반으로 테스트를 작성하도록 지시했다
