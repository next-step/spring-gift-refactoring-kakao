# Prompt Log

## 세션: 2026-03-03 — Java 코드 포매터 적용

### 프롬프트 1: 포매터 라이브러리 탐색
> https://bestinu.tistory.com/64 이 자바 컨벤션에 대한 포매터를 이 프로젝트에 적용하고 싶어, 어떤 포매터 라이브러리를 사용하면 되니?

- URL을 제공하여 네이버 핵데이 Java 코딩 컨벤션을 참조하게 함
- Claude가 Spotless와 Checkstyle 두 가지 옵션을 비교 설명

### 프롬프트 2: Spotless 적용 + 글로벌 규칙 설정
> 글로벌 규칙으로 우리의 매번 대화 세션마다, 내가 어떻게 코딩에이전트를 이용했는지 프롬프트를 중심으로 root경로에 prompt.md를 작성합니다. 자동 포매팅이 목적이므로, Spotless를 사용합니다.

- 글로벌 규칙(prompt.md 작성)을 메모리에 저장하도록 지시
- Spotless 선택 의사결정을 전달
- Claude가 `build.gradle.kts`에 Spotless 플러그인 추가, `spotlessApply` 실행하여 전체 소스 포매팅 적용
- ktlint 위반도 함께 수정하여 빌드 성공 확인

### 프롬프트 3: 커밋 + Co-author 등록
> 이 작업 자를 코워커로 등록하고, 커밋을 한 번 합니다. Co-authored-by: koomin1227 <koomin1227@naver.com>

- Spotless 적용 작업 전체를 한 번에 커밋
- Co-author를 지정하여 커밋 메시지에 포함

### 프롬프트 4: 전역 규칙 고정
> 별도의 요청이 있기 전까지, 다음의 두가지를 전역 규칙으로 고정합니다. 1. 매 대화 세션 마다, 프롬프트위주로 나의 코딩 에이전트 사용에 대한 prompt.md파일 추가 작성 2. 커밋에 Co-authored-by: koomin1227 <koomin1227@naver.com> 코워커를 항상 등록하고, 한국어로 커밋메시지작성

- 두 가지 전역 규칙을 메모리에 저장: prompt.md 매 세션 추가 작성, 커밋 시 Co-author + 한국어 메시지

### 프롬프트 5: var → 명시적 타입 교체
> 모든 파일에서 var를 정상적인 타입으로 적용한 뒤, 테스트 해봅니다.

- 3개 파일(`OrderController`, `KakaoMessageClient`, `WishController`)에서 총 16개의 `var`를 명시적 타입으로 교체
- `Member`, `Option`, `Order`, `Product`, `Wish`, `Page<T>`, `int`, `String`, `LinkedMultiValueMap` 등 실제 타입 적용
- `spotlessApply` + `build` 성공 확인

### 프롬프트 6: 미사용 메서드 탐색 및 삭제
> 전체 소스 코드 돌면서, "사용되고 있지 않은" 메서드가 존재합니다. 예를 들면, Wish.java의 생성자 함수가 그 예시입니다. 모든 소스 코드를 대상으로 사용되고 있지 않은 메서드를 제거하고 싶습니다. 다만, 무조건 삭제하지 않고, 이전 작업자가 남긴, 주석이나 향후 변경 영향도 등을 검토하여 작업합니다. 그런 뒤 실행되는지 테스트합니다.

- Explore 에이전트 2개를 병렬로 실행하여 main/test 소스 전체 분석
- 미사용 확인된 항목 삭제: `Product.getOptions()`, `SeedMemberController`(전체), `MemberService`(전체), `CreateMemberRequest`(전체)
- JPA/Thymeleaf 등 프레임워크가 암묵적으로 사용하는 메서드는 보존 (`protected` 기본 생성자, `Member.getPoint()` 등)
- `./gradlew build` 성공 확인

### 프롬프트 7: protected 생성자 사용 여부 확인
> protected로 선언된 생성자는 안쓰이는게 아니니? 검토해줘

- JPA 스펙상 엔티티의 `protected` no-arg 생성자는 필수임을 확인
- 6개 엔티티(`Category`, `Product`, `Option`, `Order`, `Member`, `Wish`) 모두 유지

### 프롬프트 8: 삭제 코드의 출처 확인
> product의 option 메서드 삭제만, 체리픽 머지 이전의 코드야? 확인해 줘

- `git show`로 확인: `getOptions()`는 초기 커밋(`55ca9e4`, author: `wotjd243`)에서 생성된 코드
- 체리픽으로 가져온 코드가 아닌, 이전 작업자의 프로젝트 세팅 코드임을 확인

## 세션: 2026-03-03 — Lombok 적용 리팩토링

### 프롬프트 1: Lombok 적용 계획 수립 및 실행
> Lombok 적용 리팩토링 계획 (Plan 모드에서 수립 후 실행)

- JPA 엔티티 6개(Category, Product, Option, Member, Wish, Order)에 반복되는 getter 메서드(28개)와 protected 기본 생성자(6개)를 Lombok 어노테이션으로 대체
- `build.gradle.kts`에 `compileOnly("org.projectlombok:lombok")` + `annotationProcessor("org.projectlombok:lombok")` 추가
- 각 엔티티에 `@Getter` + `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 적용, 수동 getter/protected 생성자 삭제
- 비즈니스 메서드(`update()`, `subtractQuantity()`, `chargePoint()`, `deductPoint()` 등)와 public 생성자는 그대로 보존
- Record 클래스(Request/Response)는 이미 최적이므로 변경 불필요로 판단하여 제외
- `./gradlew spotlessApply build` — 포매팅 + 컴파일 + 테스트 9개 모두 통과 확인
