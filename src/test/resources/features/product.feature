Feature: 상품 관리
  상품 CRUD 기능을 검증한다.

  Scenario: 상품 목록을 페이징 조회한다
    When 페이지 0, 사이즈 5로 상품 목록을 조회한다
    Then 응답 코드는 200이다
    And 상품 목록의 사이즈는 5이다
    And 전체 상품 수는 6 이상이다

  Scenario: 상품을 단건 조회한다
    When ID 1 상품을 조회한다
    Then 응답 코드는 200이다
    And 응답의 상품 이름은 "맥북 프로 16인치"이다

  Scenario: 상품을 생성한다
    When 이름 "테스트상품", 가격 10000, 이미지 "https://example.com/img.jpg", 카테고리 1로 상품을 생성한다
    Then 응답 코드는 201이다
    And 응답의 상품 이름은 "테스트상품"이다
    And 응답의 상품 가격은 10000이다

  Scenario: 상품을 수정한다
    Given 이름 "수정용상품", 가격 5000, 카테고리 1로 상품이 존재한다
    When 해당 상품을 이름 "수정된상품", 가격 7000, 이미지 "https://example.com/updated.jpg", 카테고리 1로 수정한다
    Then 응답 코드는 200이다
    And 응답의 상품 이름은 "수정된상품"이다
    And 응답의 상품 가격은 7000이다

  Scenario: 상품을 삭제한다
    Given 이름 "삭제용상품", 가격 5000, 카테고리 1로 상품이 존재한다
    When 해당 상품을 삭제한다
    Then 응답 코드는 204이다
    When 해당 상품을 조회한다
    Then 응답 코드는 404이다

  Scenario: 존재하지 않는 상품 조회 시 404 응답
    When ID 999999 상품을 조회한다
    Then 응답 코드는 404이다

  Scenario: 카카오 포함 이름으로 상품 생성에 실패한다
    When 이름 "카카오톡", 가격 10000, 이미지 "https://example.com/img.jpg", 카테고리 1로 상품을 생성한다
    Then 응답 코드는 400이다

  Scenario: 존재하지 않는 카테고리로 상품 생성에 실패한다
    When 이름 "테스트상품2", 가격 10000, 이미지 "https://example.com/img.jpg", 카테고리 999999로 상품을 생성한다
    Then 응답 코드는 404이다
