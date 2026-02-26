# spring-gift-refactoring

레거시 코드를 안전하게 리팩토링하는 프로젝트입니다.

## 목표

**작동을 바꾸기 쉬운 상태를 만드는 것** - 구조 변경을 통해 변경 난이도를 낮추되 작동은 유지합니다.

## 리팩토링 전략

1. 인수 테스트로 안전망 확보
2. 서비스 계층 추출 (구조 변경, 작동 변경 없음)
3. 코드 스멜 수정 (품질 개선)

---

## 완료된 작업

### 1단계: 코드 분석

- [x] 프로젝트 구조 분석 (`PROJECT_STRUCTURE.md`)
- [x] Anti-pattern 분석 (`anti-pattern.md`)
- [x] 스타일 불일치 분석 (`style.md`)
- [x] 미참조 코드 분석 (`unreferenced.md`)
- [x] 기타 코드 스멜 분석 (`etc.md`)

### 2단계: 인수 테스트 작성

- [x] 테스트 전략 문서 작성 (`TEST_STRATEGY.md`)
- [x] MemberAcceptanceTest (8개 테스트)
- [x] CategoryAcceptanceTest (17개 테스트)
- [x] ProductAcceptanceTest (18개 테스트)
- [x] OptionAcceptanceTest (17개 테스트)
- [x] WishAcceptanceTest (12개 테스트)
- [x] OrderAcceptanceTest (13개 테스트)

**총 75개 인수 테스트 통과**

---

## 진행 예정 작업

### 3단계: 서비스 계층 추출

Controller에 있는 비즈니스 로직을 Service 계층으로 이동합니다. 작동 변경 없이 순수하게 코드를 이동하는 구조 변경입니다.

- [ ] MemberService 추출
- [ ] CategoryService 추출
- [ ] ProductService 추출
- [ ] OptionService 추출
- [ ] WishService 추출
- [ ] OrderService 추출

### 4단계: 코드 스멜 수정

서비스 계층이 정리된 후, 분석 문서에 정리된 코드 스멜을 수정합니다.

#### Anti-pattern 수정 (`anti-pattern.md`)
- [ ] `orElse(null)` 후 null 체크 → `orElseThrow()` 패턴으로 변경
- [ ] Exception Swallowing → 최소 로깅 추가
- [ ] `ResponseEntity<?>` 와일드카드 → 구체 타입으로 변경
- [ ] 매직 넘버 HTTP 상태 코드 → `HttpStatus` 상수 사용

#### 스타일 통일 (`style.md`)
- [ ] 에러 메시지 한국어 통일
- [ ] `@Autowired` 생략
- [ ] `var` → 명시적 타입 선언
- [ ] `.collect(Collectors.toList())` → `.toList()`
- [ ] `path=` 속성 생략

#### 기타 코드 스멜 (`etc.md`)
- [ ] 가변 컬렉션 노출 → 불변 뷰 반환
- [ ] 과도한 접근 범위 → package-private 검토

#### 미참조 코드 삭제 (`unreferenced.md`)
- [ ] `Order.getMemberId()` 삭제
- [ ] `Product.getOptions()` 삭제

---

## AI 활용 기록

이 프로젝트는 Claude Code를 활용하여 리팩토링을 진행합니다.

### 1단계: 코드 분석
- **활용 방식**: 도메인별 코드 순회, Grep으로 패턴 탐색
- **결과물**: 5개의 분석 문서 (anti-pattern, style, unreferenced, etc, PROJECT_STRUCTURE)

### 2단계: 인수 테스트 작성
- **활용 방식**: TEST_STRATEGY.md 기반으로 RestAssured 인수 테스트 생성
- **수정 내용**:
  - FK 제약조건 순서에 맞게 테스트 데이터 삭제 순서 조정
  - 인증 헤더 누락 시 400 반환 (Spring MissingRequestHeaderException)
  - 옵션 삭제 테스트에서 옵션 개수 체크 로직 고려
- **학습 내용**:
  - Spring의 `@RequestHeader(required=true)`는 헤더 누락 시 400을 반환함 (401이 아님)
  - 테스트 데이터 정리 시 FK 순서 중요 (orders → wishes → options → products → categories → members)

### 3단계: 서비스 계층 추출
- (진행 예정)

### 4단계: 코드 스멜 수정
- (진행 예정)

---

## 기술 스택

- Java 21
- Spring Boot 3.5.9
- Gradle 8.14
- H2 Database
- JPA
- RestAssured (테스트)

---

## 실행 방법

```bash
# 테스트 실행
./gradlew test

# 애플리케이션 실행
./gradlew bootRun
```
