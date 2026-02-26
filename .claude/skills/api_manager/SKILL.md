# API 관리자 스킬

## 설명

프로젝트의 컨트롤러, DTO, 엔티티 파일들을 분석하여 `./api_receipt/` 디렉토리에 멋진 HTML API 명세서를 자동 생성하고 업데이트하는 스킬.
SWAGGER나 Spring REST Docs 같은 외부 라이브러리 없이, 프로젝트 구조와 어노테이션을 직접 분석하여 명세서를 생성합니다. 생성된 명세서는 한국어로 작성되고, 다크 모드 기반의 모던한 디자인으로 제공됩니다.

## 활성화 조건

사용자가 다음과 같은 요청을 할 때 이 스킬을 실행:
- "API 명세서 생성해줘" / "API 문서 만들어줘"
- "API 명세서 업데이트해줘" / "API 문서 갱신해줘"
- "api_receipt 업데이트" / "API spec 생성"
- 컨트롤러나 DTO를 수정한 후 "명세서 반영해줘"

## 수행 방법

### 분석 대상 파일

1. **컨트롤러 파일**: `src/main/java/**/\*Controller.java` - HTTP 메서드, 경로, 요청/응답 타입 추출
2. **DTO/Record 파일**: `*Request.java`, `*Response.java` - 필드명, 타입, 유효성 검증 어노테이션 추출
3. **엔티티 파일**: `src/main/java/**/\*.java` (JPA @Entity) - 데이터 모델 구조 파악
4. **설정 파일**: `application.properties` - 기본 설정 정보

### 생성 절차

1. **탐색**: 프로젝트의 모든 컨트롤러, DTO, 엔티티 파일을 Glob/Grep으로 탐색
2. **분석**: 각 파일을 Read하여 다음을 추출:
   - `@RestController` / `@Controller` 클래스의 `@RequestMapping` 기본 경로
   - 각 메서드의 `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` 경로
   - `@RequestBody`, `@PathVariable`, `@RequestParam` 파라미터
   - `@ResponseStatus` 응답 코드
   - `@Valid` 유효성 검증이 걸린 DTO의 제약조건 (`@NotNull`, `@Min`, `@Max`, `@Email` 등)
   - 인증 필요 여부 (`@Authenticated` 또는 Authorization 헤더 사용)
3. **분류**: API를 도메인별로 그룹핑 (Auth, Member, Category, Product, Option, Order, Wish)
4. **생성**: `./api_receipt/index.html` 파일을 생성 또는 덮어쓰기

### HTML 명세서 요구사항

- **단일 파일**: 외부 CDN 없이 CSS/JS 모두 인라인으로 포함 (오프라인 열람 가능)
- **한국어**: 모든 UI 텍스트는 한국어로 작성
- **디자인**: 다크 모드 기반, 모던하고 가독성 높은 디자인
- **구성 요소**:
  - 사이드바: 도메인별 API 그룹 네비게이션
  - 메인 영역: 각 API 엔드포인트 카드
  - 각 카드에 포함할 정보:
    - HTTP 메서드 뱃지 (GET=초록, POST=파랑, PUT=주황, DELETE=빨강)
    - 엔드포인트 경로
    - 설명 (컨트롤러 메서드명 기반 추론)
    - 인증 필요 여부 표시
    - Request Body 스키마 (필드명, 타입, 필수 여부, 유효성 규칙)
    - Response Body 스키마
    - HTTP 상태 코드
  - 상단: 프로젝트명, 생성 일시, 총 API 개수 표시
  - 검색 기능: 엔드포인트 경로/설명 필터링
- **반응형**: 모바일에서도 볼 수 있도록 반응형 레이아웃
- **접이식**: 각 API 카드는 클릭으로 펼치기/접기 가능
- **API 테스트 (Try It Out)**: Swagger UI처럼 직접 API를 호출하고 응답을 확인할 수 있는 인터랙티브 기능
  - 상단에 Base URL 입력 필드 (기본값: `http://localhost:8080`)
  - 각 API 카드 내에 "Try It Out" 버튼
  - Path 파라미터, Query 파라미터 입력 필드 자동 생성
  - Request Body JSON 에디터 (DTO 스키마 기반 샘플 JSON 자동 생성)
  - Authorization 헤더 입력 (인증 필요 API용, 글로벌 토큰 설정 지원)
  - "실행" 버튼으로 `fetch()` API 호출
  - 응답 뷰어: HTTP 상태 코드, 응답 헤더, 응답 본문 (JSON 포맷팅) 표시
  - 로딩 스피너, 에러 표시
  - Admin HTML 엔드포인트는 Try It Out 비활성화 (REST API만 지원)

### 업데이트 정책

- 기존 `./api_receipt/index.html`이 있으면 전체를 새로 생성 (덮어쓰기)
- 생성 일시를 HTML 내에 기록하여 마지막 업데이트 시점 확인 가능
- REST API (`/api/**`)와 Admin HTML (`/admin/**`)을 별도 섹션으로 분리

### 출력 경로

```
./api_receipt/
└── index.html
```
