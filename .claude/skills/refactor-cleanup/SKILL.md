---
name: refactor-cleanup
description: 리팩터링 Phase 2 미사용 코드 제거. 사용자가 "미사용 코드 제거", "불필요한 코드 정리", "dead code", "unused code" 등을 언급하면 이 스킬을 사용한다.
---

# Phase 2: 미사용 코드 제거

불필요한 코드를 제거하여 유지보수성을 높인다.
**동작 변경 없음** — 커밋 타입은 `refactor`만 사용.

---

## 절차

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

---

## 제거 대상

미사용 import, 변수, 필드, private 메서드는 Checkstyle/컴파일러가 잡아주므로
`./gradlew checkstyleMain`과 컴파일 경고를 활용한다.

**도구로 잡히지 않아 수동 확인이 필요한 항목:**
| 대상 | 확인 방법 |
|---|---|
| 주석 처리된 코드 | 수동 확인 + git blame |
| 빈 메서드/클래스 | 수동 확인 |
| 미사용 public 메서드 | Grep으로 호출처 확인 |

---

## 제거 판단 기준

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

---

## 커밋 예시
```
refactor(member): 미사용 import 및 private 메서드 제거
refactor(product): 주석 처리된 레거시 코드 제거
```
