# spring-gift-test

## 테스트 실행

### 1단계 인수 테스트 (H2)

```bash
./gradlew test
```

Docker 불필요. H2 인메모리 DB를 사용한다.

### Cucumber 인수 테스트 (Docker PostgreSQL)

Docker 이미지를 빌드하고 Docker Compose로 애플리케이션과 PostgreSQL을 띄운 뒤 테스트를 실행한다.

```bash
./gradlew dockerBuild   # Docker 이미지 빌드
./gradlew dockerUp      # Docker Compose 시작 (postgres + app)
./gradlew cucumberTest  # Cucumber 인수 테스트 실행
./gradlew dockerDown    # Docker Compose 종료
```

**Docker 런타임이 실행 중이어야 한다.**

## Docker 런타임 설정

Docker 관련 Gradle task(`dockerBuild`, `dockerUp`, `dockerDown`)는 아래 순서로 Docker 소켓을 자동 탐색한다:

| 우선순위 | 소켓 경로 | 런타임 |
|---------|----------|--------|
| 0 | `DOCKER_HOST` 환경변수 | 사용자 지정 (최우선) |
| 1 | `~/.docker/run/docker.sock` | Docker Desktop (macOS) |
| 2 | `~/.docker/desktop/docker.sock` | Docker Desktop (Linux) |
| 3 | `~/.colima/default/docker.sock` | Colima |
| 4 | `~/.orbstack/run/docker.sock` | OrbStack |
| 5 | `~/.rd/docker.sock` | Rancher Desktop |
| 6 | `/var/run/docker.sock` | Linux native / symlink |

대부분의 환경에서 Docker 런타임만 실행하면 추가 설정 없이 동작한다.

### 소켓 탐색에 실패하는 경우

자동 탐색 목록에 없는 런타임이거나, 소켓 경로가 다른 경우 `DOCKER_HOST`를 직접 지정한다:

```bash
DOCKER_HOST=unix:///path/to/docker.sock ./gradlew dockerBuild
```

## 리팩토링 작업 이력

Claude Code를 활용하여 수행한 리팩토링 작업 목록이다.

### 1. 코드 스타일 통일

- **Spotless + Google Java Format 적용** — `build.gradle.kts`에 Spotless 플러그인 추가, 전체 소스 자동 포매팅
- **var → 명시적 타입 교체** — 3개 파일에서 16개의 `var`를 실제 타입으로 교체하여 가독성 향상

### 2. 데드코드 정리

- **미사용 메서드 및 클래스 삭제** — `Product.getOptions()`, `SeedMemberController`, `MemberService`, `CreateMemberRequest` 등 미사용 코드 제거
- JPA/Thymeleaf가 암묵적으로 사용하는 `protected` 기본 생성자, getter 등은 보존

### 3. Lombok 적용

- 6개 JPA 엔티티에 `@Getter` + `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 적용
- 수동 getter 28개, protected 기본 생성자 6개 제거

### 4. 매직넘버/리터럴 상수화

- HTTP 상태 코드 → `HttpStatus` 상수, 카카오 API URL → `private static final` 상수
- `"Bearer "` 접두사, Content-Type 등 → Spring 프레임워크 상수 활용

### 5. 서비스 레이어 추출

- 7개 서비스 클래스 신규 생성: `CategoryService`, `MemberService`, `ProductService`, `OptionService`, `WishService`, `OrderService`, `KakaoAuthService`
- 10개 컨트롤러에서 리포지토리 직접 호출을 서비스 위임으로 교체
- `OrderController` 의존성 6개 → 2개로 축소, `@Transactional` 단일 트랜잭션으로 원자성 보장

### 6. 공통 예외 처리 통합

- `GlobalExceptionHandler`에 `IllegalArgumentException` 핸들러 추가
- 3개 컨트롤러의 중복 로컬 `@ExceptionHandler` 제거
- 재고 부족/포인트 부족 등 비즈니스 예외가 500 → 400으로 정상 반환

### 7. 요청 DTO 분리 (우발적 중복 제거)

- `CategoryRequest` → `CreateCategoryRequest` + `UpdateCategoryRequest`
- `ProductRequest` → `CreateProductRequest` + `UpdateProductRequest`
- `MemberRequest` → `RegisterMemberRequest` + `LoginMemberRequest`
- 서비스 추출 이후 미사용 상태이던 `toEntity()` 데드코드 함께 제거

### 8. 외부 API 클라이언트 패키지 분리

- `KakaoLoginClient`, `KakaoLoginProperties`, `KakaoMessageClient` → `gift.infrastructure.kakao` 패키지로 이동
- 도메인 패키지(auth, order)에서 외부 의존성을 분리하여 경계 명확화
