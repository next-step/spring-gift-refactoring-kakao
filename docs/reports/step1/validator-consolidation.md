# 2-2. Validator 중복 코드 정리 보고서

## 작업 목적 및 배경

`ProductNameValidator`와 `OptionNameValidator`가 거의 동일한 코드를 가지고 있었다.
동일한 정규식, 동일한 null/blank 체크, 동일한 특수문자 검증 로직이 두 클래스에 중복되어 있어,
정규식 수정 시 양쪽을 모두 변경해야 하는 유지보수 문제가 있었다.

### 기존 중복 현황

| 항목 | ProductNameValidator | OptionNameValidator | 상태 |
|------|----------------------|---------------------|------|
| 정규식 패턴 | `^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ ()\\[\\]+\\-&/_]*$` | 동일 | **중복** |
| null/blank 체크 | `name == null \|\| name.isBlank()` | 동일 | **중복** |
| 길이 검증 | `name.length() > MAX_LENGTH` | 동일 | **중복** |
| 특수문자 검증 | `!ALLOWED_PATTERN.matcher(name).matches()` | 동일 | **중복** |
| MAX_LENGTH | 15 | 50 | 다름 |
| 카카오 검증 | 있음 (`allowKakao` 파라미터) | 없음 | 다름 |

## 변경 사항 요약

### 생성된 파일

| 파일 | 설명 |
|------|------|
| `src/main/java/gift/common/NameValidator.java` | 통합 Validator 클래스 |
| `src/test/java/gift/common/NameValidatorTest.java` | 통합 테스트 클래스 |

### 삭제된 파일

| 파일 | 설명 |
|------|------|
| `src/main/java/gift/product/ProductNameValidator.java` | 기존 상품명 Validator |
| `src/main/java/gift/option/OptionNameValidator.java` | 기존 옵션명 Validator |
| `src/test/java/gift/product/ProductNameValidatorTest.java` | 기존 상품명 테스트 |
| `src/test/java/gift/option/OptionNameValidatorTest.java` | 기존 옵션명 테스트 |

### 수정된 파일

| 파일 | 변경 내용 |
|------|----------|
| `ProductController.java` | `ProductNameValidator.validate(name)` → `NameValidator.validate(name, "Product name", 15, true)` |
| `AdminProductController.java` | `ProductNameValidator.validate(name, true)` → `NameValidator.validate(name, "Product name", 15)` |
| `OptionController.java` | `OptionNameValidator.validate(name)` → `NameValidator.validate(name, "Option name", 50)` |

## 통합 API 설계

```java
// 기본 호출 (checkKakao = false)
NameValidator.validate(String name, String label, int maxLength)

// 카카오 검증 포함
NameValidator.validate(String name, String label, int maxLength, boolean checkKakao)
```

### 파라미터 의미 변경

기존 `ProductNameValidator`의 `allowKakao`(허용 여부)에서 `checkKakao`(검사 여부)로 의미를 변경했다.

| 기존 호출 | 새 호출 | 설명 |
|-----------|---------|------|
| `ProductNameValidator.validate(name)` | `NameValidator.validate(name, "Product name", 15, true)` | 카카오 검사 O |
| `ProductNameValidator.validate(name, true)` | `NameValidator.validate(name, "Product name", 15)` | 카카오 검사 X (기본값) |
| `OptionNameValidator.validate(name)` | `NameValidator.validate(name, "Option name", 50)` | 카카오 검사 X |

## 적용된 규칙

- **구조 변경만, 작동 변경 없음**: 에러 메시지 내용, 검증 로직이 완전히 동일하게 유지됨
- **정규식 한 곳 관리**: 향후 허용 문자 변경 시 `NameValidator` 한 곳만 수정하면 됨
- **기존 테스트 케이스 100% 보존**: 모든 테스트 시나리오를 `NameValidatorTest`에 이관

## 참고 사항

- `NameValidator`는 기존 공통 패키지인 `gift.common`에 배치함
- `label` 파라미터를 통해 에러 메시지에 "Product name" / "Option name" 등 도메인별 라벨이 포함됨
- `checkKakao`의 기본값은 `false`로 설정하여, 대부분의 경우(옵션, Admin) 간결한 3-파라미터 호출이 가능
