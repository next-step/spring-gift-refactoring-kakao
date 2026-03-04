---
name: refactoring
description: 리팩터링 전체 절차 스킬. 사용자가 "리팩터링", "스타일 정리", "미사용 코드 제거", "Service 추출", "Controller 분리", "계층 분리", "코드 정리" 등을 언급하면 이 스킬을 사용한다. Phase 1(스타일 정리), Phase 2(미사용 코드 제거), Phase 3(Service Layer 추출)의 구체적 절차와 검증 방법을 정의한다.
---

# 리팩터링 절차 가이드

리팩터링은 **외부 동작을 변경하지 않으면서** 코드 내부 구조를 개선하는 작업이다.
모든 리팩터링은 인수테스트가 통과하는 상태에서 시작하고, 통과하는 상태로 끝난다.

---

## 핵심 원칙

1. **구조 변경과 동작 변경을 절대 섞지 않는다** — 한 커밋에 하나의 관심사만
2. **한 번에 한 파일, 한 가지 변경** — 대량 변경 금지
3. **매 커밋마다 테스트 통과** — `./gradlew test` 확인
4. **README.md 먼저 업데이트** — 작업 전 체크리스트 정의, 작업 후 체크

---

## Phase 1: 스타일 정리

### 목적
코드 포맷과 네이밍을 통일하여 가독성을 확보한다.
**동작 변경 없음** — 커밋 타입은 `style`만 사용.

### 절차

```
1. ./gradlew checkstyleMain → 현재 위반 사항 확인
2. 위반 사항 수정 (import 순서, 네이밍, 공백 등)
3. ./gradlew checkstyleMain → 위반 0건 확인
4. ./gradlew test → 동작 유지 확인
5. 커밋
```

### 정리 대상
| 항목 | 기준 |
|---|---|
| 들여쓰기 | 4 spaces (프로젝트 표준) |
| import 정렬 | 알파벳 순, 와일드카드 import 금지 |
| 공백 | 연산자 앞뒤, 중괄호 앞 공백 |
| 네이밍 | 클래스: PascalCase, 메서드/변수: camelCase, 상수: UPPER_SNAKE |
| 줄바꿈 | 메서드 사이 1줄 공백, 파일 끝 개행 |

### 커밋 예시
```
style(product): 코드 포맷팅 및 import 정리
style(order): 네이밍 컨벤션 camelCase 통일
```

### 주의사항
- 포맷팅 중 로직 변경 절대 금지
- rename만으로 동작이 바뀔 수 있으니 테스트로 확인
- 한 도메인씩 커밋 (여러 도메인 한꺼번에 정리 금지)

---

## Phase 2: 미사용 코드 제거

### 목적
불필요한 코드를 제거하여 유지보수성을 높인다.
**동작 변경 없음** — 커밋 타입은 `refactor`만 사용.

### 절차

```
1. 제거 후보 식별 (IDE 인스펙션, 컴파일러 경고)
2. 제거 전 검증:
   a. git blame → 왜 작성되었는지 맥락 파악
   b. 주변 주석/TODO → 향후 사용 계획 확인
   c. Grep → 다른 파일에서 참조 여부 확인
3. 제거 실행
4. ./gradlew test → 동작 유지 확인
5. 커밋
```

### 제거 대상
| 대상 | 확인 방법 |
|---|---|
| 미사용 import | IDE 경고 또는 Checkstyle |
| 미사용 private 메서드 | IDE "unused" 경고 |
| 미사용 변수/필드 | 컴파일러 경고 |
| 주석 처리된 코드 | 수동 확인 + git blame |
| 빈 메서드/클래스 | 수동 확인 |

### 제거 판단 기준

```
제거 가능:
  - git blame 확인 결과 의도가 불분명하고 참조 없음
  - 주석 처리된 코드이며 git 히스토리에 원본 존재
  - 컴파일러가 미사용으로 경고하는 코드

제거 보류 (TODO로 남김):
  - 주변에 "TODO", "FIXME", "향후 사용" 주석 존재
  - git blame 결과 최근 작성되어 작업 중일 가능성
  - 다른 파일에서 리플렉션/문자열로 참조할 가능성
```

### 커밋 예시
```
refactor(member): 미사용 import 및 private 메서드 제거
refactor(product): 주석 처리된 레거시 코드 제거
```

---

## Phase 3: Service Layer 추출

### 목적
Controller에 있는 비즈니스 로직을 Service 계층으로 분리한다.
Controller는 **요청 수신 + 응답 반환**만 담당하도록 얇게(Thin) 유지한다.

### 추출 순서
의존성이 적은 도메인부터 진행한다:

```
1. CategoryService   (의존성 없음, 가장 단순)
2. MemberService     (인증 로직 포함)
3. ProductService    (Category 의존)
4. OptionService     (Product 의존)
5. WishService       (Member + Product 의존)
6. OrderService      (Member + Option + 외부 API 의존, 가장 복잡)
```

### 추출 절차 (도메인 하나당)

```
1. README.md에 작업 항목 확인
2. ./gradlew test → 현재 상태 확인
3. Service 클래스 생성 (빈 클래스 + @Service)
4. Controller의 비즈니스 로직을 Service로 이동
   - Repository 호출 → Service로
   - 데이터 가공/검증 로직 → Service로
   - DTO 변환 → Service로
5. Controller에서 Service 주입 및 호출로 변경
6. ./gradlew test → 동작 유지 확인
7. 커밋
```

### Service 클래스 구조 템플릿

```java
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
            .map(CategoryResponse::from)
            .toList();
    }

    public void create(CategoryRequest request) {
        categoryRepository.save(request.toEntity());
    }

    public void update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("카테고리를 찾을 수 없습니다."));
        category.update(request.name(), request.color(), request.imageUrl(), request.description());
    }

    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }
}
```

### 추출 후 Controller 모습

```java
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody CategoryRequest request) {
        categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
```

### Controller에 남겨야 할 것
- `@RequestMapping`, `@GetMapping` 등 라우팅 어노테이션
- `@Valid`, `@RequestBody` 등 요청 바인딩
- `ResponseEntity` 반환 (HTTP 상태 코드 결정)
- `@ExceptionHandler` (컨트롤러 레벨 예외 처리)

### Service로 이동해야 할 것
- Repository 호출 (`findAll`, `findById`, `save`, `delete`)
- 비즈니스 검증 로직 (중복 체크, 권한 확인, 재고 확인 등)
- 데이터 가공 (Entity → DTO 변환)
- 트랜잭션이 필요한 복합 연산 (주문 처리: 재고 차감 + 포인트 차감)
- 외부 API 호출 (Kakao 메시지 전송)

### 커밋 예시
```
refactor(category): CategoryService 생성 및 비즈니스 로직 이동
refactor(product): ProductService 생성 및 Controller에서 로직 분리
refactor(order): OrderService 생성 및 주문 처리 로직 이동
```

### 주의사항
- 한 커밋에 하나의 도메인 Service만 추출
- 로직 이동 과정에서 기능을 추가하거나 변경하지 않음
- `@Transactional`은 Service 메서드에 필요한 경우만 추가
- Controller의 `@ExceptionHandler`는 이 단계에서 옮기지 않음 (별도 커밋)

---

## Phase 4: 에러 처리 구조화

### 목적
산재된 `@ExceptionHandler`와 `IllegalArgumentException`/`NoSuchElementException`을
도메인별 에러 코드 체계로 통합한다.

### 구조

```
gift/error/                         ← 공통 에러 인프라
├── ErrorCode.java                  ← 인터페이스
├── ErrorResponse.java              ← 응답 DTO
├── BusinessException.java          ← 추상 베이스 예외
├── GlobalExceptionHandler.java     ← @RestControllerAdvice
├── CommonErrorCode.java            ← 공통 에러 코드 enum
└── CommonException.java            ← 공통 예외

gift/{domain}/                      ← 각 도메인 패키지 내부
├── {Domain}ErrorCode.java
└── {Domain}Exception.java
```

### 진행 순서

```
1. 공통 에러 인프라 생성 (ErrorCode, ErrorResponse, BusinessException, GlobalExceptionHandler)
2. CommonErrorCode + CommonException 생성
3. 도메인별 ErrorCode + Exception 생성 (기존 예외 메시지에서 도출)
4. Service/Controller에서 기존 예외를 도메인 예외로 교체 (도메인별 1커밋)
5. Controller별 @ExceptionHandler 제거 (GlobalExceptionHandler로 통합)
6. ./gradlew test 통과 확인
```

### 주의사항
- `@RestControllerAdvice`는 `@RestController`에만 적용 — Admin Controller(`@Controller`)는 기존 방식 유지
- 교체 작업은 도메인별로 분리하여 매 커밋마다 테스트 확인
- 에러 응답 형식이 변경되므로, 기존 테스트의 응답 검증 부분 업데이트 필요

---

## 전체 흐름 요약

```
Phase 0: 인수테스트 작성 → 안전망 확보
         ↓
Phase 1: 스타일 정리 → 가독성 확보
         ↓ (./gradlew test 통과)
Phase 2: 미사용 코드 제거 → 불필요한 코드 정리
         ↓ (./gradlew test 통과)
Phase 3: Service Layer 추출 → 계층 분리 완료
         ↓ (./gradlew test 통과)
Phase 4: 에러 처리 구조화 → 예외 체계 통합
         ↓ (./gradlew test 통과)
       완료
```

각 Phase 사이에 반드시 **테스트 전체 통과**를 확인한다.
Phase를 건너뛰거나 순서를 바꾸지 않는다.
