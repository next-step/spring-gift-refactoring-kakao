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
