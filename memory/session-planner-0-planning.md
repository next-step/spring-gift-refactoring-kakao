# Session 0: 계획 수립 및 도구 구성

**날짜**: 2025-02-27

## 이번 세션에서 한 일

### 1. 프로젝트 현황 파악

- 전체 소스 코드 42개 파일 (약 1,756줄) 분석 완료
- 7개 도메인: auth, category, member, product, option, order, wish
- 서비스 계층 없음 — Controller가 Repository를 직접 호출
- 테스트 0개 — src/test/ 에 .gitkeep만 존재
- 스타일 위반 식별:
    - `var` 키워드: OrderController, WishController, KakaoMessageClient (15+건)
    - `ResponseEntity<?>`: OrderController (2건)
    - `@Autowired` 생성자: MemberController, AdminMemberController, JwtProvider, AuthenticationResolver (4건)
    - Entity Lombok 미사용: 6개 엔티티 전부 수동 getter
    - 불필요한 `final`: MemberController 등 (3+건)
    - `Collectors.toList()`: OptionController (1건)

### 2. Phase 1 작업 계획 수립

작업 순서를 다음과 같이 결정:

1. README.md 작성 (기능 목록 + 구현 전략)
2. 테스트 안전망 구축 (인수 테스트)
3. 스타일 정리 (var 제거, ResponseEntity 수정, @Autowired 제거, final 제거, toList 통일)
4. 불필요한 코드 제거 (Lombok 적용으로 수동 getter/constructor 제거, @RequiredArgsConstructor 적용)
5. 서비스 계층 추출 (Category → Product → Member → Option → Wish → Order 순)

### 3. Claude 도구 구성

- **CLAUDE.md** 생성 (프로젝트 루트): 미션 원칙, 구조/작동 분리, AI 협업 규율, 코드 컨벤션, 커밋 컨벤션
- **memory/** 디렉토리 생성: 세션별 기록 관리 (`session-{N}-{주제}.md` 형식)
- **Skills 구성** (`.claude/skills/`):
    - `/verify` — 코드 변경 후 6단계 검증 (테스트 → diff → 구조/작동 분리 → 30초 규칙 → 컨벤션 → 결과 보고)
    - `/history` — 세션 종료 시 작업 기록 저장
    - `/decision` — 선택지 정리 → 토의 → 결정 기록 (`memory/decisions.md`에 누적)
- 처음 `.claude/commands/`에 생성했으나 Claude가 skill로 인식하지 못해 `.claude/skills/{name}/SKILL.md` 구조로 재구성

### 4. verify 스킬 개선

- 코드 컨벤션 확인 항목을 하드코딩하지 않고 "CLAUDE.md의 코드 컨벤션 섹션 기준" 으로 일반화
- 컨벤션이 변경되어도 verify 스킬을 수정할 필요 없도록 설계

## 결정 사항

- List vs Page 응답 차이는 API 명세상 의도적 → 유지
- Custom Validator는 Phase 1에서 Service로 이동만 (Spring Validation 전환은 작동 변경 가능성)
- 서비스 추출 순서: 난이도 낮은 것(Category)부터 → Order(가장 복잡)까지
- 세션별 memory 파일 분리 관리 (cross-session 추적 용이)
- 결정 사항은 `memory/decisions.md`에 번호(D1, D2...)로 누적 기록

## 생성/변경된 파일

- `CLAUDE.md` (신규)
- `memory/session-0-planning.md` (신규)
- `.claude/skills/verify/SKILL.md` (신규)
- `.claude/skills/history/SKILL.md` (신규)
- `.claude/skills/decision/SKILL.md` (신규)
- `.claude/commands/` (삭제 — skills로 이전)

## 다음 세션 할 일

- README.md에 Phase 1 기능 목록 + 구현 전략 작성
- 인수 테스트 작성 (안전망 확보)
- 스타일 정리 시작 (첫 커밋: var 제거)
