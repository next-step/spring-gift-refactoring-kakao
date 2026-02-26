# spring-gift-refactoring

## 1단계: 리팩터링 준비하기

### 구현(리팩터링) 기능 목록

#### 1. 스타일 정리
- [x] Javadoc `@author/@since` 어노테이션 일괄 제거 (일부 파일에만 존재하는 불일치)
- [x] 불필요한 `@Autowired` 제거 (생성자 1개일 때 불필요)
- [x] 블록 주석(`/* */`) 클래스 설명 제거 (다수파 기준 — 클래스 설명 주석 없음으로 통일)
- [x] Entity 필드 사이 빈 줄 패턴 통일
- [x] `Collectors.toList()` → `.toList()` 통일 (Java 16+)

#### 2. 불필요한 코드 제거
- [ ] `OrderController.wishRepository` — 주입만 되고 사용되지 않는 필드 제거
- [ ] `OptionController`의 `import java.util.stream.Collectors` — `.toList()` 전환 후 제거

#### 3. 서비스 계층 추출 (컨트롤러별, 작동 변경 없음)
- [ ] `OrderController` → `OrderService` 추출
- [ ] `MemberController` → `MemberService` 추출
- [ ] `KakaoAuthController` → `KakaoAuthService` 추출
- [ ] `OptionController` → `OptionService` 추출
- [ ] `WishController` → `WishService` 추출
- [ ] `ProductController` → `ProductService` 추출
- [ ] `AdminProductController` → `ProductService` 재사용 또는 별도 추출
- [ ] `AdminMemberController` → `MemberService` 재사용 또는 별도 추출
- [ ] `CategoryController` → `CategoryService` 추출 (단순 CRUD지만 일관성을 위해)

### 구현 전략
1. **스타일 → 불필요 코드 → 서비스 추출** 순서로 진행한다.
2. 스타일/불필요 코드를 먼저 정리해야 서비스 추출 시 diff가 깨끗해진다.
3. 서비스 추출은 **비즈니스 로직이 복잡한 컨트롤러부터** 진행한다.
   - OrderController(다단계 트랜잭션) → MemberController(인증) → KakaoAuthController(OAuth) → OptionController → WishController → ProductController → Admin 컨트롤러 → CategoryController
4. 한 번에 하나의 컨트롤러만 변경하고, 매 변경마다 전체 테스트 통과를 확인한다.
5. 구조 변경만 수행하며, 작동 변경/신규 기능 추가는 금지한다.

### AI 도구 활용 기록
- **Claude Code**를 사용하여 프로젝트 전체 분석 및 리팩터링 계획 수립
- 코드 분석: 전체 소스 파일의 스타일 불일치, 미사용 코드, 비즈니스 로직 위치를 자동 탐색
- 계획 문서(README, 단계, plan, 판단근거) 초안 작성에 활용
- (이후 단계에서 수정 내역 추가 예정)
