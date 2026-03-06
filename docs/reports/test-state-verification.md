# 5-2. 테스트 검증 강화 — 예외 테스트에 상태 불변 검증 추가

## 작업 목적 및 배경

예외 발생 테스트에서 예외 발생 여부만 확인하고, 예외 발생 후 내부 상태가 변하지 않았는지 검증하지 않는 테스트들이 있었다. 예외가 발생했더라도 상태가 변해버렸다면 버그이므로, 상태 불변 검증을 추가하여 테스트의 신뢰성을 높였다.

## 변경 사항 요약

### 수정된 파일

| 파일 | 추가된 검증 수 |
|------|-------------|
| `src/test/java/gift/model/OptionTest.java` | 2건 |
| `src/test/java/gift/model/MemberTest.java` | 6건 |

### 상세 변경 내역

**OptionTest.java**

| 테스트명 | 추가된 검증 |
|---------|-----------|
| `subtractExceedingQuantityThrows` | `assertThat(option.getQuantity()).isEqualTo(5)` |
| `subtractFromZeroStockThrows` | `assertThat(option.getQuantity()).isEqualTo(0)` |

**MemberTest.java**

| 테스트명 | 추가된 검증 |
|---------|-----------|
| `chargeZeroThrows` | `assertThat(member.getPoint()).isEqualTo(0)` |
| `chargeNegativeThrows` | `assertThat(member.getPoint()).isEqualTo(0)` |
| `deductExceedingBalanceThrows` | `assertThat(member.getPoint()).isEqualTo(1000)` |
| `deductZeroThrows` | `assertThat(member.getPoint()).isEqualTo(1000)` |
| `deductNegativeThrows` | `assertThat(member.getPoint()).isEqualTo(1000)` |
| `deductFromZeroBalanceThrows` | `assertThat(member.getPoint()).isEqualTo(0)` |

## 적용된 규칙

- 각 테스트의 `assertThatThrownBy` 블록 뒤에 상태 불변 assertion 1줄 추가
- 기존 예외 검증 코드는 수정하지 않음
- 새 파일 생성 없음

## 검증

- `./gradlew test` 전체 테스트 통과 확인 (BUILD SUCCESSFUL)
