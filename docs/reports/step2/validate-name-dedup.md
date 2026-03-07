# validateName 중복 제거 - 실행 계획 보고서

## 목적
ProductService와 OptionService에 동일하게 존재하는 `validateName` private 메서드를 `NameValidator`에 통합하여 중복을 제거한다.

## 현황 분석

### 중복 코드
두 서비스의 `validateName` 메서드가 완전히 동일한 패턴을 사용한다:

```java
private void validateName(String name) {
    List<String> errors = NAME_VALIDATOR.validate(name);
    if (!errors.isEmpty()) {
        throw new IllegalArgumentException(String.join(", ", errors));
    }
}
```

- `ProductService.java` (lines 61-66)
- `OptionService.java` (lines 69-74)

### 차이점
- 검증 규칙(rules)은 각 서비스의 `NAME_VALIDATOR` 인스턴스에 이미 설정되어 있으므로 `validateOrThrow`는 규칙에 의존하지 않는다.
- 에러 수집 → 예외 변환 로직만 공통화하면 된다.

## 변경 계획

### 1. `src/main/java/gift/common/NameValidator.java`
- `validateOrThrow(String name)` 메서드 추가
- 기존 `validate(String name)` 호출 후, errors가 비어있지 않으면 `IllegalArgumentException`을 던지는 로직

```java
public void validateOrThrow(String name) {
    List<String> errors = validate(name);
    if (!errors.isEmpty()) {
        throw new IllegalArgumentException(String.join(", ", errors));
    }
}
```

### 2. `src/main/java/gift/service/ProductService.java`
- `private void validateName(String name)` 메서드 삭제 (lines 61-66)
- `validateName(name)` 호출부(lines 44, 50)를 `NAME_VALIDATOR.validateOrThrow(name)`으로 변경

### 3. `src/main/java/gift/service/OptionService.java`
- `private void validateName(String name)` 메서드 삭제 (lines 69-74)
- `validateName(name)` 호출부(line 38)를 `NAME_VALIDATOR.validateOrThrow(name)`으로 변경

## 바꾸지 않는 것
- 검증 규칙(maxLength, allowedCharacters, noKakao) 변경 없음
- 예외 타입(`IllegalArgumentException`) 및 메시지 형식(`, `로 join) 동일 유지
- `NameValidator.validate()` 메서드 그대로 유지
- 기존 테스트 코드 변경 없음 (동작이 동일하므로)

## 영향 범위
- 동작 변경 없음 (순수 리팩토링)
- 기존 테스트가 모두 통과해야 함
