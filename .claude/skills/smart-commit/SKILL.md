---
name: smart-commit
description: 커밋되지 않은 모든 git 변경사항을 분석하여 적절한 단위로 나누고, AngularJS Commit Message Conventions에 따라 여러 개의 커밋을 생성한다
argument-hint: "[추가 지시사항 (선택)]"
disable-model-invocation: true
---

# Smart Commit
커밋되지 않은 모든 git 변경사항을 확인하고, 적절한 단위로 나눠 여러 개의 커밋을 생성한다.

## 실행 절차
### 1단계: 변경사항 전체 파악
아래 명령어를 병렬로 실행하여 현재 상태를 파악한다:
- `git status` — untracked, modified, staged 파일 전체 목록
- `git diff` — unstaged 변경 내용
- `git diff --cached` — staged 변경 내용
- `git log --oneline -10` — 최근 커밋 히스토리 (컨텍스트 파악용)

### 2단계: 변경사항 분석 및 그룹핑
변경된 파일들의 내용을 읽고, 논리적 단위로 그룹을 나눈다.

그룹핑 기준 (우선순위순):
1. **feat 커밋은 기능 단위로 세부 분리** — 하나의 feat 커밋에 여러 기능이 섞이지 않도록 한다
   - 같은 도메인이라도 독립적인 기능이면 별도 커밋으로 분리
   - 예: "상품 CRUD"와 "상품 검증 로직"은 별도 커밋
2. **type별 분리** — feat, fix, docs, style, refactor, test, chore는 반드시 별도 커밋
3. **도메인/모듈별 분리** — 서로 다른 도메인의 변경은 가능한 별도 커밋
4. **의존성 순서 고려** — 기반 코드(엔티티, 설정)를 먼저 커밋하고, 이를 사용하는 코드를 나중에 커밋

분리하지 않는 경우:
- 하나의 기능을 위해 함께 수정된 파일들 (예: Entity + Repository + Controller + DTO)
- 설정 변경과 그 설정을 사용하는 코드

### 3단계: 커밋 계획 수립 및 사용자 확인
그룹핑 결과를 아래 형식으로 **사용자에게 보여주고 승인을 받는다:**
**반드시 사용자 승인을 받은 후에만 커밋을 실행한다.**
```
📋 커밋 계획 (총 N개)
1. feat(product): 상품 CRUD 엔드포인트 추가
   - src/main/java/gift/product/Product.java
   - src/main/java/gift/product/ProductController.java
   - src/main/java/gift/product/ProductRepository.java
2. feat(product): 상품명 검증 로직 추가
   - src/main/java/gift/product/ProductNameValidator.java
3. chore(build): Gradle 빌드 설정
   - build.gradle.kts
   ...
```

### 4단계: 순차 커밋 실행
승인받은 계획에 따라 순서대로 커밋한다:
1. 해당 그룹의 파일만 `git add <파일1> <파일2> ...` 로 스테이징 
   (절대 `git add .` 또는 `git add -A` 사용 금지)
2. 커밋 메시지 작성 후 커밋
3. 다음 그룹으로 이동
4. 모든 커밋 완료 후 `git log --oneline` 으로 결과 확인


## 커밋 메시지 형식
- AngularJS Git Commit Message Conventions을 따른다.
- `<type>(<scope>): <subject>`
- body는 작성하지 않는다. subject만으로 변경 내용을 충분히 전달한다.

### type
| type | 용도 |
|------|------|
| feat | 새로운 기능 |
| fix | 버그 수정 |
| docs | 문서 변경 |
| style | 포맷팅, 세미콜론 누락 등 (로직 변경 없음) |
| refactor | 리팩토링 (기능 변경 없음) |
| test | 테스트 추가/수정 |
| chore | 빌드, 설정 등 유지보수 |

### scope
변경이 발생한 위치를 명시한다. 이 프로젝트에서는 주로:
- 도메인명: `product`, `order`, `member`, `wish`, `category`, `option`, `auth`
- 인프라: `config`, `db`, `build`

### subject
- 한국어로 작성
- 명령형 현재 시제 사용: "추가" (O) / "추가함" (X) / "추가했음" (X)
- 첫 글자 소문자 (type과 scope는 영어, subject는 한국어)
- 끝에 마침표 없음

### 메시지 예시
- feat(product): 상품 엔티티 및 레포지토리 추가
- feat(auth): JWT 토큰 생성 및 검증 기능 추가
- chore(build): Gradle 의존성 설정
- docs: 프로젝트 문서 작성


## 추가 지시사항
$ARGUMENTS

## 주의사항
- .env, credentials, secret key 등 민감한 파일은 절대 커밋하지 않는다. 발견 시 사용자에게 경고한다.
- 커밋 메시지의 한 줄은 100자를 넘지 않는다.
- 각 커밋 후 `git status`로 상태를 확인하고, 의도하지 않은 파일이 포함되지 않았는지 검증한다.
