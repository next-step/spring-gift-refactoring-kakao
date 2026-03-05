# Progress 13: Option PUT — 옵션 수정 엔드포인트 추가

**기간**: 2026-03-05

---

## 이 문서의 목적

Phase 2 "누락된 작동 구현" — 2-2번 작업. 현재 Option CRUD에서 PUT(수정)이 누락되어 있었다. `PUT /api/products/{productId}/options/{optionId}` 엔드포인트를 추가하여 옵션의 이름과 수량을 수정할 수 있게 한다.

**변경 유형**: 작동 변경 (feat) — 새 엔드포인트 추가.

---

## 1. 설계 결정

| 항목                | 결정                                     | 이유                                     |
|-------------------|----------------------------------------|----------------------------------------|
| OptionRequest 공유  | 생성/수정에 동일한 `OptionRequest` 사용          | CLAUDE.md 계획대로. 필드가 동일(name, quantity) |
| 이름 중복 체크          | 자기 자신 제외 (`IdNot`)                     | 이름 변경 없이 수량만 변경할 때 false positive 방지   |
| 교차 상품 접근          | `findByIdAndProductId` 사용              | 다른 상품의 옵션을 수정할 수 없도록 방지                |
| Entity update 메서드 | `update(String name, int quantity)` 추가 | 기존에 `subtractQuantity(int)`만 존재했음      |

---

## 2. 변경 내용

### 2-1. Option Entity — `update()` 메서드 추가

```java
public void update(String name, int quantity) {
    this.name = name;
    this.quantity = quantity;
}
```

- 기존에는 `subtractQuantity(int)` — 주문 시 수량 차감만 존재
- 이름+수량 전체 변경 메서드가 필요하여 추가

### 2-2. OptionRepository — 자기 자신 제외 중복 체크 쿼리

```java
boolean existsByProductIdAndNameAndIdNot(Long productId, String name, Long optionId);
```

- 기존 `existsByProductIdAndName(Long, String)` 은 생성 시 사용
- 수정 시에는 자기 자신의 이름을 제외해야 하므로 `IdNot` 파생 쿼리 추가

### 2-3. OptionService — `updateOption()` 추가

```java

@Transactional
public OptionResponse updateOption(Long productId, Long optionId, OptionRequest updateRequest) {
    productQueryPort.validateExists(productId);

    String name = updateRequest.name();
    int quantity = updateRequest.quantity();

    if (optionRepo.existsByProductIdAndNameAndIdNot(productId, name, optionId)) {
        throw new DuplicateOptionNameException();
    }

    Option option = optionRepo.findByIdAndProductId(optionId, productId)
            .orElseThrow(NotFoundException::optionNotFound);

    option.update(name, quantity);

    return OptionResponse.from(option);
}
```

처리 순서:

1. 상품 존재 검증 (`validateExists`)
2. 이름 중복 검증 — 자기 자신 제외 (`existsByProductIdAndNameAndIdNot`)
3. 옵션 조회 — 교차 상품 접근 방지 (`findByIdAndProductId`)
4. Entity 상태 변경 (`update`)
5. Response 매핑

### 2-4. OptionController — `PUT /{optionId}` 엔드포인트 추가

```java

@PutMapping("/{optionId}")
public ResponseEntity<OptionResponse> updateOption(
        @PathVariable Long productId,
        @PathVariable Long optionId,
        @Valid @RequestBody OptionRequest request
) {
    OptionResponse response = optionService.updateOption(productId, optionId, request);

    return ResponseEntity
            .ok(response);
}
```

- `@Valid` 적용 — 기존 `OptionRequest`의 Bean Validation 재사용 (name 50자, pattern, quantity 1~99,999,999)

---

## 3. 단위 테스트

### OptionServiceTest 추가 (4개)

파일: `src/test/java/gift/option/internal/OptionServiceTest.java`

| 테스트                               | 검증 내용                                                                                                                                     |
|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `testUpdateOption`                | happy — `validateExists` + 중복 체크(false) + `findByIdAndProductId` + Entity 상태 변경(`option.getName()`, `option.getQuantity()`) + Response 매핑 |
| `testUpdateOptionProductNotFound` | 상품 없음 → NotFoundException 전파                                                                                                              |
| `testUpdateOptionNotFound`        | 옵션 없음 → NotFoundException                                                                                                                 |
| `testUpdateOptionDuplicateName`   | 다른 옵션과 이름 중복 → DuplicateOptionNameException                                                                                               |

---

## 4. 인수 테스트

### 추가 시나리오 (5개)

파일: `src/test/resources/features/option.feature`

| ID    | 시나리오                   | 유형    | 검증 내용                     |
|-------|------------------------|-------|---------------------------|
| O-4   | 옵션 수정 성공               | happy | 이름+수량 변경 → 200 + 변경된 값 응답 |
| O-S3  | 옵션 수정 후 목록 재조회 시 변경 반영 | state | 수정 → 목록 조회 → 변경된 이름 포함 확인 |
| O-E9  | 존재하지 않는 상품의 옵션 수정 시도   | error | 404                       |
| O-E10 | 존재하지 않는 옵션 수정 시도       | error | 404                       |
| O-E11 | 옵션 수정 시 다른 옵션과 이름 중복   | error | 400 + "이미 존재하는 옵션명"       |

### Step Definition 추가 (4개)

파일: `src/test/java/gift/acceptance/steps/OptionApiSteps.java`

| Step                                      | 동작                                                              |
|-------------------------------------------|-----------------------------------------------------------------|
| `@When("해당 옵션 수정 요청을 보낸다")`               | `PUT /api/products/{productId}/options/{currentOptionId}`       |
| `@When("{string} 옵션을 다음과 같이 수정 요청을 보낸다")` | `PUT /api/products/{productId}/options/{이름으로 조회한 ID}`           |
| `@When("존재하지 않는 상품의 옵션 수정 요청을 보낸다")`      | `PUT /api/products/{NON_EXISTENT_ID}/options/{NON_EXISTENT_ID}` |
| `@When("해당 상품의 존재하지 않는 옵션 수정 요청을 보낸다")`   | `PUT /api/products/{productId}/options/{NON_EXISTENT_ID}`       |

기존 `toOptionRequest(DataTable)` 헬퍼를 그대로 재사용.

---

## 5. 검증 결과

```bash
./gradlew test --tests "gift.option.internal.OptionServiceTest"  # 13개 통과 (기존 9 + 추가 4)
./gradlew test                # BUILD SUCCESSFUL (전체 단위 테스트 통과)
./gradlew acceptanceTest      # BUILD SUCCESSFUL (기존 82개 + 신규 5개 = 87개 시나리오)
```

---

## 6. 수정/생성 파일 목록

| 파일                                                          | 변경                                               |
|-------------------------------------------------------------|--------------------------------------------------|
| `src/main/java/gift/option/Option.java`                     | `update(String, int)` 메서드 추가                     |
| `src/main/java/gift/option/internal/OptionRepository.java`  | `existsByProductIdAndNameAndIdNot` 파생 쿼리 추가      |
| `src/main/java/gift/option/internal/OptionService.java`     | `updateOption()` 메서드 추가                          |
| `src/main/java/gift/option/internal/OptionController.java`  | `PUT /{optionId}` 엔드포인트 + `PutMapping` import 추가 |
| `src/test/java/gift/option/internal/OptionServiceTest.java` | 4개 테스트 추가                                        |
| `src/test/resources/features/option.feature`                | 5개 시나리오 추가 (O-4, O-S3, O-E9, O-E10, O-E11)       |
| `src/test/java/gift/acceptance/steps/OptionApiSteps.java`   | 4개 When step 추가                                  |

---

## 7. Phase 2 누락 작동 구현 진행 현황

| 작업  | 설명                                   | 상태     |
|-----|--------------------------------------|--------|
| 2-1 | 주문 부수 효과 (위시 정리 + 카카오 알림 이벤트 분리)     | 미착수    |
| 2-2 | Option PUT (옵션 수정 엔드포인트)             | **완료** |
| 2-3 | Category GET /{id} (카테고리 단건 조회)      | **완료** |
| 2-4 | Product 카테고리 필터 (`?categoryId={id}`) | **완료** |
