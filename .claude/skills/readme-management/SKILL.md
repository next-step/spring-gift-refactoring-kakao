---
name: readme-management
description: README.md 작성, 업데이트, 기능 목록 관리를 위한 스킬. 사용자가 "README 작성", "기능 목록 정리", "체크리스트 업데이트", "진행 기록 작성", "AI 활용 기록", "구현 전략 정리" 등을 언급하면 이 스킬을 사용한다. 새 작업을 시작하거나, 커밋 전 체크리스트를 갱신하거나, 작업 회고를 기록할 때도 반드시 참고한다.
---

# README.md 작성 및 관리 가이드

README.md는 이 프로젝트의 **단일 진실 공급원(Single Source of Truth)**이다.
코드를 한 줄이라도 수정하기 전에 README.md에 작업이 정의되어 있어야 한다.

---

## 문서 구조 템플릿

README.md는 아래 섹션 순서를 따른다:

```markdown
# 프로젝트명

## 구현할 기능 목록

### Phase 0: 인수테스트 작성
- [ ] 항목 설명 (현재 동작을 검증하는 테스트)

### Phase 1: 스타일 정리
- [ ] 항목 설명
- [ ] 항목 설명

### Phase 2: 미사용 코드 제거
- [ ] 항목 설명

### Phase 3: Service Layer 추출
- [ ] 항목 설명

## 구현 전략
<!-- 각 Phase별 접근 방식, 우선순위, 주의사항 -->

## 진행 기록
<!-- 날짜별 의사결정 로그 -->

## AI 활용 기록
<!-- AI 도구 활용 방식, 수정 내용, 학습 내용 -->
```

---

## 기능 목록 작성 규칙

### 체크리스트 항목 하나 = 커밋 하나
- 각 항목은 하나의 커밋으로 완료할 수 있는 크기여야 한다.
- `git diff`를 30초 내에 설명할 수 있는 범위로 쪼갠다.
- 너무 크면 하위 항목으로 분리한다.

### 작성 포맷
```markdown
- [ ] <type>(<scope>): 작업 설명
```

**예시:**
```markdown
### Phase 0: 인수테스트 작성
- [ ] test(product): 상품 CRUD API 인수테스트 작성
- [ ] test(category): 카테고리 CRUD API 인수테스트 작성
- [ ] test(order): 주문 API 인수테스트 작성
- [ ] test(wish): 위시리스트 API 인수테스트 작성
- [ ] test(member): 회원 API 인수테스트 작성
- [ ] test(auth): 인증/인가 API 인수테스트 작성

### Phase 1: 스타일 정리
- [ ] style(product): 들여쓰기 및 공백 통일
- [ ] style(order): 네이밍 컨벤션 정리 (camelCase 통일)

### Phase 2: 미사용 코드 제거
- [ ] refactor(member): 미사용 import 제거
- [ ] refactor(wish): 사용되지 않는 private 메서드 제거

### Phase 3: Service Layer 추출
- [ ] refactor(product): ProductService 생성 및 로직 이동
- [ ] refactor(order): OrderService 생성 및 로직 이동
```

### 완료 시
```markdown
- [x] style(product): 들여쓰기 및 공백 통일
```

---

## 구현 전략 작성 가이드

각 Phase별로 아래 항목을 정리한다:

```markdown
## 구현 전략

### Phase 0: 인수테스트 작성
- **목적**: 리팩터링 전 현재 동작을 보호하는 안전망 확보
- **접근**: 각 도메인의 Controller API를 대상으로 인수테스트(Acceptance Test) 작성
- **범위**: 정상 요청/응답 흐름 위주, 엣지 케이스는 Phase 3 이후 보강
- **도구**: SpringBootTest + MockMvc 또는 RestAssured
- **원칙**: 테스트가 통과하는 상태에서만 다음 Phase로 진행
- **검증**: `./gradlew test` 전체 통과

### Phase 1: 스타일 정리
- **도구**: ktlint, IDE 포맷터
- **접근**: `./gradlew ktlintFormat` 우선 적용 후 수동 정리
- **검증**: `./gradlew test` 통과 확인

### Phase 2: 미사용 코드 제거
- **접근**: IDE 인스펙션 → git blame 확인 → 제거
- **주의**: TODO/주석 의도 확인 필수
- **검증**: 빌드 + 테스트 통과

### Phase 3: Service Layer 추출
- **순서**: 의존성 적은 도메인부터 (category → product → ...)
- **원칙**: Controller는 요청 수신 + 응답 반환만
- **검증**: 각 도메인 추출 후 테스트 통과
```

---

## 진행 기록 작성 포맷

작업 중 내린 결정, 발견한 문제, 학습한 내용을 기록한다.

```markdown
## 진행 기록

### 2026-XX-XX
#### 작업 내용
- Phase 1 스타일 정리 완료

#### 의사결정
- ktlint 자동 포맷 후 수동으로 정리한 부분: ...
- 특정 파일의 스타일을 변경하지 않은 이유: ...

#### 이슈
- (발견한 문제와 해결 방법)
```

---

## AI 활용 기록 작성 포맷

**필수 규칙: Phase 단위로 AI를 활용할 때마다 반드시 README.md의 `## AI 활용 기록`에 내용을 추가한다.**

- Phase가 완료될 때 해당 Phase에서의 AI 활용 내역을 기록한다.
- AI 활용 기록은 코드 커밋과 분리하여 `docs` 타입으로 별도 커밋한다.

```markdown
## AI 활용 기록

### 2026-XX-XX — Phase N: <Phase 이름>
- **활용 방식**: (예: Service 추출 초안 생성 요청)
- **AI 산출물 수정 내용**: (예: 불필요한 null 체크 제거, 예외 처리 방식 변경)
- **학습한 내용**: (예: @Transactional 전파 속성에 대해 학습)
```

---

## 작업 흐름 요약

```
1. README.md에 다음 작업 항목 정의 (또는 확인)
2. 테스트 실행 → 현재 상태 확인
3. 코드 수정
4. 테스트 통과 확인
5. README.md 체크리스트 업데이트 (- [ ] → - [x])
6. 코드 커밋 (체크리스트 항목과 1:1 대응)
7. Phase 완료 시 AI 활용 기록 작성
8. 문서 변경은 별도 docs 커밋으로 분리
```
