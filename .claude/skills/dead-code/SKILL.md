---
name: dead-code
description: 미사용 코드를 근거와 함께 제거한다. 작동 변경 없이 불필요한 코드만 정리한다.
disable-model-invocation: true
---

# 2단계: 불필요한 코드 제거 (작동 변경 없음)

## 목표
IDE 또는 정적 분석 관점에서 "미사용"으로 판단되는 항목을 제거한다. **작동이 바뀌지 않아야 한다.**

## 대상 범위
`src/main/java/gift/` 아래의 모든 `.java` 파일

## 발견된 미사용 코드

### 1. 미사용 메서드
- `Product.getOptions()` — 정의만 있고 프로젝트 어디에서도 호출되지 않음

### 2. 불필요한 주석
- `Order.java` — `// primitive FK` (필드 타입으로 자명)
- `Wish.java` — `// primitive FK - no entity reference` (필드 타입으로 자명)
- `Member.java` — `// point deduction for order payment` (메서드명 `deductPoints`로 자명)

### 3. 간소화 가능한 코드
- `OptionController.java` — `Collectors.toList()` → `.toList()` (Java 16+ 내장 메서드로 대체 가능, import 제거 가능)

## 제외 항목 (삭제하지 않음)
- JPA `protected` 기본 생성자 — 프레임워크가 요구
- 중복 코드(인증 체크, null 체크 패턴) — 3단계 서비스 계층 추출에서 해소
- `@Autowired` 제거 — 1단계 스타일 정리에서 처리

## 작업 방식
1. 각 항목의 삭제 근거를 사용자에게 보고한다
2. 승인 후 한 항목씩 제거한다
3. 각 항목 제거 후 `./gradlew build`로 빌드가 깨지지 않는지 확인한다
4. 항목별로 커밋 메시지를 제안한다

## 제약
- 사용 중인 코드를 삭제하지 않는다
- 메서드 시그니처, 로직, 반환값을 변경하지 않는다
- 새로운 클래스나 메서드를 추가하지 않는다
- 테스트를 비활성화하거나 삭제하지 않는다
