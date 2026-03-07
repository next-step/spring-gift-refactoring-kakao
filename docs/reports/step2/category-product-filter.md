# 카테고리별 상품 목록 필터링 - 실행 계획

## 목적
`GET /api/products?categoryId=1` 호출 시 해당 카테고리의 상품만 반환하도록 구현한다.
categoryId 파라미터가 없으면 기존처럼 전체 상품을 반환한다.

## 현황 분석

| 파일 | 현재 상태 |
|------|-----------|
| `ProductRepository.java` | `JpaRepository<Product, Long>` 상속만 존재. 카테고리 필터링 쿼리 메서드 없음 |
| `ProductService.java` | `findAll(Pageable)` - 전체 조회만 수행. `findAll()` - AdminProductController용 전체 조회 |
| `ProductController.java` | `getProducts(Pageable)` - categoryId 파라미터 없음 |
| `ProductControllerTest.java` | 기존 `getProducts()` 테스트는 전체 조회만 검증 |

### 영향 범위
- `AdminProductController.java`는 `productService.findAll()` (파라미터 없는 오버로드)을 사용하므로 영향 없음
- `ProductResponse.from(Product)`에 이미 `categoryId` 필드가 포함되어 있어 응답 구조 변경 불필요

## 변경할 파일 목록

### 1. `ProductRepository.java`
- `Page<Product> findByCategoryId(Long categoryId, Pageable pageable)` 쿼리 메서드 추가
- Spring Data JPA 네이밍 규칙으로 자동 생성됨 (Product.category.id 기준)

### 2. `ProductService.java`
- `findAll(Pageable pageable)` 시그니처를 `findAll(Long categoryId, Pageable pageable)`로 변경
- categoryId가 null이면 `productRepository.findAll(pageable)` (기존 동작)
- categoryId가 not null이면 `productRepository.findByCategoryId(categoryId, pageable)`

### 3. `ProductController.java`
- `getProducts()` 메서드에 `@RequestParam(required = false) Long categoryId` 파라미터 추가
- `productService.findAll(categoryId, pageable)` 호출로 변경

### 4. `ProductControllerTest.java` (테스트 추가)
- **테스트 1: 카테고리 필터링 조회**
  - 카테고리 A, B 각각 생성
  - 카테고리 A에 상품 2개, 카테고리 B에 상품 1개 등록
  - `GET /api/products?categoryId={A.id}` 호출
  - 응답 content 크기가 2이고, 모든 상품의 categoryId가 A.id인지 확인

- **테스트 2: categoryId 없이 조회 (기존 동작 유지 확인)**
  - 카테고리 A, B 각각에 상품 등록
  - `GET /api/products` 호출 (categoryId 없음)
  - 응답 content 크기가 전체 상품 수와 일치하는지 확인

## 바꾸지 않는 것
- `findAll()` (파라미터 없는 오버로드) - AdminProductController에서 사용 중
- 기존 상품 CRUD 동작 (POST, PUT, DELETE)
- AdminProductController
- ProductResponse 구조

## 커밋 메시지
```
feat(product): 카테고리별 상품 목록 필터링 구현
```
