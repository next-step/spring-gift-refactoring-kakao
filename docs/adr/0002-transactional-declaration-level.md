# 0002. @Transactional 선언 레벨 전략

## 상태

accepted

## 컨텍스트

현재 6개 Service 클래스의 27개 메서드 모두에 `@Transactional` 또는
`@Transactional(readOnly = true)`가 메서드 레벨로 개별 선언되어 있다.

새 메서드를 추가할 때 `@Transactional`을 빠뜨리면 트랜잭션 없이 실행되는 위험이 있고,
읽기 메서드마다 `readOnly = true`를 반복 선언하는 중복도 존재한다.

클래스 레벨 선언으로 전환할지 결정이 필요하다.

## 선택지

### 선택지 1: 메서드 레벨 유지 (현재 상태)

모든 메서드에 개별적으로 `@Transactional`을 선언한다.

- 장점: 각 메서드의 트랜잭션 의도가 명시적으로 보인다
- 단점: 반복 선언, 새 메서드 추가 시 누락 위험

### 선택지 2: 클래스 레벨 `@Transactional(readOnly = true)` + 쓰기 메서드 오버라이드

클래스에 `@Transactional(readOnly = true)`를 선언하고,
쓰기 메서드에만 `@Transactional`을 오버라이드한다.

- 장점: 읽기 메서드의 반복 제거, 새 메서드가 기본적으로 readOnly 보호를 받음, 실수로 쓰기 트랜잭션이 열리는 것을 방지
- 단점: 쓰기 메서드에는 여전히 개별 선언 필요, 클래스 레벨 선언을 모르면 혼란 가능

## 결정

**선택지 2: 클래스 레벨 `@Transactional(readOnly = true)` + 쓰기 메서드 오버라이드**를 채택한다.

핵심 근거: 새 메서드 추가 시 기본적으로 읽기 전용 트랜잭션이 적용되어
실수로 트랜잭션 없이 실행되거나 불필요한 쓰기 트랜잭션이 열리는 것을 방지한다.

## 결과

- 6개 Service 클래스에 `@Transactional(readOnly = true)`를 클래스 레벨로 선언한다.
- 기존 읽기 메서드의 `@Transactional(readOnly = true)` 어노테이션을 제거한다.
- 쓰기 메서드의 `@Transactional`은 그대로 유지한다 (클래스 레벨 readOnly를 오버라이드).
- 이 변경은 선언 위치 이동이므로 구조 변경(`refactor`) 커밋으로 처리한다 (ADR-0001 기준).
