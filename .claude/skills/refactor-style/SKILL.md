---
name: refactor-style
description: 리팩터링 Phase 1 스타일 정리. 사용자가 "스타일 정리", "코드 포맷팅", "네이밍 컨벤션", "import 정리", "checkstyle" 등을 언급하면 이 스킬을 사용한다.
---

# Phase 1: 스타일 정리

코드 포맷과 네이밍을 통일하여 가독성을 확보한다.
**동작 변경 없음** — 커밋 타입은 `style`만 사용.

---

## 절차

```
1. ./gradlew checkstyleMain → 현재 위반 사항 확인
2. 위반 사항 수정
3. ./gradlew checkstyleMain → 위반 0건 확인
4. ./gradlew test → 동작 유지 확인
5. 커밋
```

구체적인 스타일 규칙은 `config/checkstyle/checkstyle.xml`에 정의되어 있으므로
스킬에서 별도로 나열하지 않는다. `./gradlew checkstyleMain`이 유일한 기준이다.

---

## 커밋 예시
```
style(product): 코드 포맷팅 및 import 정리
style(order): 네이밍 컨벤션 camelCase 통일
```

## 주의사항
- 포맷팅 중 로직 변경 절대 금지
- rename만으로 동작이 바뀔 수 있으니 테스트로 확인
- 한 도메인씩 커밋 (여러 도메인 한꺼번에 정리 금지)
