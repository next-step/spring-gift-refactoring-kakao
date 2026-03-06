# 상품 옵션 수정 API 구현 보고서

## 작업 목적 및 배경

API 명세에 정의된 `PUT /api/products/{productId}/options/{optionId}` 엔드포인트가 구현되어 있지 않았다. 기존 CRUD 중 Create(POST), Read(GET), Delete(DELETE)만 존재했고 Update(PUT)가 누락되어 있어 이를 구현했다.

## 변경 사항 요약

### 수정된 파일

| 파일 | 변경 내용 |
|------|-----------|
| `src/main/java/gift/model/Option.java` | `update(String name, int quantity)` 메서드 추가 |
| `src/main/java/gift/repository/OptionRepository.java` | `existsByProductIdAndNameAndIdNot()` 쿼리 메서드 추가 |
| `src/main/java/gift/service/OptionService.java` | `update(Long productId, Long optionId, String name, int quantity)` 메서드 추가 |
| `src/main/java/gift/controller/OptionController.java` | `@PutMapping("/{optionId}")` 엔드포인트 추가 |

### 신규 파일

| 파일 | 내용 |
|------|------|
| `src/test/java/gift/service/OptionServiceTest.java` | update 성공/실패 시나리오 테스트 3건 |

### 주요 변경 내용

1. **Option.update()**: `Product.update()` 패턴과 동일하게 필드 직접 갱신
2. **OptionService.update()**: productId 존재 확인, optionId 존재 및 product 소속 확인, 이름 유효성(NameValidator), 이름 중복 검사(자기 자신 제외)
3. **OptionController PUT**: `@Valid @RequestBody OptionRequest` 수신, `OptionResponse` 반환 (200 OK)
4. **OptionRepository**: `existsByProductIdAndNameAndIdNot()` — 같은 상품 내 자기 자신을 제외한 이름 중복 체크

## 적용된 규칙

- 기존 `ProductController.updateProduct()` 패턴 준수
- 기존 `OptionService.delete()` 의 product 소속 검증 패턴 재사용
- 기존 DTO(`OptionRequest`, `OptionResponse`) 변경 없이 재사용
- 테스트는 Mockito 기반 단위 테스트 (`OrderServiceTest` 패턴 준수)

## 검증

- `./gradlew test` — 전체 134개 테스트 통과 (기존 131 + 신규 3)
