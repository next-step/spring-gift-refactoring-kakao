# CategoryService + ProductService 추출 작업 보고서

> 작업 일시: 2026-02-26

## 작업 목적 및 배경

README.md 3-2(CategoryService 추출)과 3-3(ProductService 추출)을 함께 진행한다. 두 도메인이 밀접하게 엮여 있어(ProductController/AdminProductController가 CategoryRepository를 직접 사용) 동시 추출이 효율적이다.

분석 보고서:
- `docs/analysis/category-service.md`
- `docs/analysis/product-service.md`

## 변경 사항 요약

### 신규 파일

| 파일 | 설명 |
|------|------|
| `gift/service/CategoryService.java` | 카테고리 CRUD 서비스. `findAll()`, `findById()`, `create()`, `update()`, `delete()` |
| `gift/service/ProductService.java` | 상품 CRUD 서비스. NameValidator 검증(`checkKakao=true`) 포함. CategoryService 의존. |

### 수정된 파일

| 파일 | 변경 내용 |
|------|-----------|
| `CategoryController.java` | `CategoryRepository` 직접 호출 → `CategoryService` 위임 |
| `ProductController.java` | `ProductRepository` + `CategoryRepository` 직접 호출 + `validateName()` → `ProductService` 위임. private 메서드 제거. |
| `AdminProductController.java` | `ProductRepository` + `CategoryRepository` + `NameValidator` 직접 호출 → `ProductService` + `CategoryService` 위임 |

### 의존 구조

```
CategoryController → CategoryService → CategoryRepository

ProductController → ProductService → ProductRepository
                                    → CategoryService → CategoryRepository

AdminProductController → ProductService (상품 CRUD)
                       → CategoryService (카테고리 목록 조회)
```

## 적용된 규칙

- 분석 보고서의 권장 작업 순서를 따름 (3-2 CategoryService 선행 → 3-3 ProductService)
- ProductService가 CategoryRepository 대신 CategoryService를 의존하여 cross-package repository 직접 접근을 제거
- NameValidator `checkKakao` 불일치 해소: ProductService 내부에서 `checkKakao=true`로 통일 (분석 보고서 권장사항)
- AdminProductController는 try-catch로 IllegalArgumentException을 잡아 기존 폼 에러 렌더링 방식을 유지
- 리팩토링 후 `./gradlew clean build`로 전체 테스트 통과 확인

## 참고 사항

- AdminProductController에서 기존에 `NameValidator.validate(name, "Product name", 15)` (checkKakao=false)로 호출하던 것이 ProductService를 통해 `checkKakao=true`로 통일됨. 관리자 페이지에서도 "카카오" 포함 상품명에 대한 검증이 적용됨.
- WishController, OptionController의 `productRepository.findById()` 사용은 각각 3-5(WishService), 3-4(OptionService) 추출에서 처리 예정.
