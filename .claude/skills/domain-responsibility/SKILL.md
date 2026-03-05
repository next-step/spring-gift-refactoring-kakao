---
name: domain-responsibility
description: Service에 누수된 도메인 로직을 Entity로 이동한다. 작동을 유지하면서 책임 위치만 바로잡는다.
disable-model-invocation: true
---

# 도메인 책임 되찾기

## 목표
작동을 유지하면서도 책임과 계산, 판단을 올바른 위치로 이동해 누수와 중복을 줄인다. **변경 전후가 분명한 개선을 최소 2개 이상 수행한다.** 신규 기능을 추가하지 않는다.

## 대상 범위
사용자가 argument로 도메인 패키지명(예: `order`, `option member`)을 지정하면 해당 패키지의 Entity와 Service만 분석한다. argument가 없으면 프로젝트의 Entity와 Service 파일 전체를 대상으로 한다.

- 예: `/domain-responsibility order` → `gift/order/` 패키지의 Entity·Service만 스캔
- 예: `/domain-responsibility order option` → `gift/order/`, `gift/option/` 두 패키지만 스캔
- 예: `/domain-responsibility` → 전체 스캔

## 탐색 절차

코드를 수정하기 전에, 아래 순서대로 누수 지점을 탐색한다.

### 1단계: Service·Entity 파일 스캔
대상 범위에 해당하는 Service 클래스와 Entity 클래스를 열어 메서드 본문을 읽는다.

### 2단계: 누수 패턴 식별
각 Service 메서드와 Entity 메서드에서 아래 패턴에 해당하는 코드를 찾는다.

| # | 패턴 | 설명 | 예시 |
|---|------|------|------|
| 1 | **계산 누수** | Entity 필드를 꺼내 Service에서 산술 연산 | `entity.getPrice() * quantity` |
| 2 | **판단 누수** | Entity 필드를 꺼내 Service에서 조건 분기 | `if (entity.getField() == null)` |
| 3 | **검증 누수** | Entity 상태의 유효성을 Service에서 검사 | `if (!a.getOwnerId().equals(b))` |
| 4 | **상태 질의 누수** | Entity 내부 상태를 getter로 꺼내 boolean 판단 | `entity.getToken() != null` → `entity.hasToken()` |
| 5 | **비교 누수** | Entity 필드를 꺼내 동등성/대소 비교 | `entity.getPassword().equals(input)` |
| 6 | **연쇄 getter 누수** | 연관 Entity의 getter를 꺼내 비교·판단 (Entity 내부 포함) | `this.product.getId().equals(id)` → `this.product.hasId(id)` |

### 3단계: 이동 가능 여부 판단
발견된 각 누수에 대해 아래 기준으로 이동 가능 여부를 판단한다.

**이동 가능 (Entity로 옮긴다)**
- Entity 자신의 필드만으로 판단·계산이 완결되는 경우
- null 체크, 비교, 산술 연산 등 순수한 도메인 규칙인 경우

**이동 불가 (Service에 남긴다)**
- Repository 쿼리가 필수인 검증 (중복 체크, 존재 확인 등)
- 여러 Entity를 조합하는 오케스트레이션
- 이동 시 성능이 저하되는 경우 (Lazy 컬렉션 강제 로딩 등)
- 트랜잭션 경계, 외부 API 호출

### 4단계: 개선 목록 보고
이동 가능한 항목을 정리하여 사용자에게 보고한다. 각 항목에는 다음을 포함한다:
- **현재 코드** (Service의 해당 라인)
- **개선 후 코드** (Entity 메서드 + Service 호출부)
- **개선 증거** (아래 기준 중 해당하는 것)

## 개선 증거 확인 기준
각 항목의 개선이 유효한지 다음 중 하나 이상으로 확인한다:
- **호출부 단순화**: Service 메서드의 라인 수 또는 분기가 감소
- **중복 제거**: 같은 판단이 여러 곳에서 반복되던 것이 한 곳으로 집약
- **테스트 가독성 개선**: 도메인 메서드 단위 테스트가 Service 의존 없이 작성 가능
- **분기·조건 감소**: if/null 체크가 도메인 메서드 안으로 숨겨져 호출부가 깔끔해짐

## 작업 순서
1. 위 탐색 절차(1~4단계)를 수행하여 개선 목록을 사용자에게 보고한다
2. 승인된 항목부터 하나씩 진행한다
3. 새 Entity 메서드의 기대 동작을 검증하는 단위 테스트를 먼저 작성한다 (Red)
4. 해당 Entity에 메서드를 추가하여 테스트를 통과시킨다 (Green)
5. Service에서 기존 로직을 새 메서드 호출로 교체한다
6. `./gradlew test`로 전체 테스트 통과를 확인한다
7. 항목별로 커밋 메시지를 제안한다
8. 다음 항목으로 넘어간다

## 제약
- 기존 API 응답, HTTP 상태 코드, 동작을 변경하지 않는다
- 새로운 엔드포인트나 기능을 추가하지 않는다
- 테스트를 비활성화하거나 삭제하지 않는다
- Value Object 도입(예: Money, Points 등)은 이 단계에서 하지 않는다 — 구조 변경 범위를 넘는다
- 인증 처리 구조 변경(AOP/Interceptor 도입)은 이 단계에서 하지 않는다
