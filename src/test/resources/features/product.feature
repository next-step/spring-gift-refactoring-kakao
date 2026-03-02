#noinspection NonAsciiCharacters
Feature: 상품 관리

  # --- Happy Flow ---

  Scenario: P-1 상품 생성 성공
    Given "교환권" 카테고리가 존재한다
    When 상품 생성 요청을 보낸다
      | name  | price | imageUrl                   | categoryId   |
      | 아메리카노 | 5000  | http://example.com/img.png | {categoryId} |
    Then 응답 상태 코드는 201
    And 응답 헤더에 Location이 존재한다
    And 응답 body의 "id"가 null이 아니다
    And 응답 body의 "name"이 "아메리카노"이다
    And 응답 body의 "price"가 5000이다
    And 응답 body의 "imageUrl"이 "http://example.com/img.png"이다
    And 응답 body의 "categoryId"가 해당 카테고리 ID이다

  Scenario: P-2 상품 단건 조회 성공
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    When 해당 상품 조회 요청을 보낸다
    Then 응답 상태 코드는 200
    And 응답 body의 "name"이 "아메리카노"이다
    And 응답 body의 "id"가 null이 아니다

  Scenario: P-3 상품 목록 페이지네이션 조회
    Given "교환권" 카테고리에 상품 3개가 존재한다
    When 상품 목록을 페이지 0 사이즈 2로 조회 요청을 보낸다
    Then 응답 상태 코드는 200
    And 응답 body의 "content" 배열 크기가 2이다
    And 응답 body의 "page.totalElements"가 3이다

  Scenario: P-4 상품 수정 성공
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    When 해당 상품 수정 요청을 보낸다
      | name | price | imageUrl                   | categoryId   |
      | 라떼   | 6000  | http://example.com/new.png | {categoryId} |
    Then 응답 상태 코드는 200
    And 응답 body의 "name"이 "라떼"이다
    And 응답 body의 "price"가 6000이다
    And 응답 body의 "imageUrl"이 "http://example.com/new.png"이다

  Scenario: P-5 상품 삭제 성공
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    When 해당 상품 삭제 요청을 보낸다
    Then 응답 상태 코드는 204
    And 응답 body가 없다

  # --- Error Flow ---

  @error
  Scenario: P-E1 존재하지 않는 상품 조회
    When 존재하지 않는 상품 조회 요청을 보낸다
    Then 응답 상태 코드는 404

  @error
  Scenario: P-E2 존재하지 않는 카테고리로 상품 생성 시도
    When 상품 생성 요청을 보낸다
      | name  | price | imageUrl                   | categoryId |
      | 아메리카노 | 5000  | http://example.com/img.png | 99999      |
    Then 응답 상태 코드는 404

  @error
  Scenario: P-E3 존재하지 않는 카테고리로 상품 수정 시도
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    When 해당 상품 수정 요청을 보낸다
      | name  | price | imageUrl                   | categoryId |
      | 아메리카노 | 5000  | http://example.com/img.png | 99999      |
    Then 응답 상태 코드는 404

  @error
  Scenario: P-E4 존재하지 않는 상품 수정 시도
    Given "교환권" 카테고리가 존재한다
    When 존재하지 않는 상품 수정 요청을 보낸다
      | name  | price | imageUrl                   | categoryId   |
      | 아메리카노 | 5000  | http://example.com/img.png | {categoryId} |
    Then 응답 상태 코드는 404

  @error @validation
  Scenario: P-E5 상품 이름 16자 이상
    Given "교환권" 카테고리가 존재한다
    When 상품 생성 요청을 보낸다
      | name             | price | imageUrl                   | categoryId   |
      | 가나다라마바사아자차카타파하히후 | 5000  | http://example.com/img.png | {categoryId} |
    Then 응답 상태 코드는 400
    And 응답 body에 "최대 15자" 가 포함된다

  @error @validation
  Scenario: P-E6 상품 이름에 허용되지 않은 특수문자
    Given "교환권" 카테고리가 존재한다
    When 상품 생성 요청을 보낸다
      | name  | price | imageUrl                   | categoryId   |
      | 상품!@# | 5000  | http://example.com/img.png | {categoryId} |
    Then 응답 상태 코드는 400
    And 응답 body에 "허용되지 않는 특수 문자" 가 포함된다

  @error @validation
  Scenario: P-E7 상품 이름에 카카오 포함
    Given "교환권" 카테고리가 존재한다
    When 상품 생성 요청을 보낸다
      | name  | price | imageUrl                   | categoryId   |
      | 카카오선물 | 5000  | http://example.com/img.png | {categoryId} |
    Then 응답 상태 코드는 400
    And 응답 body에 "카카오" 가 포함된다

  @error @validation
  Scenario: P-E8 상품 이름 빈 값
    Given "교환권" 카테고리가 존재한다
    When 상품 생성 요청을 보낸다
      | name | price | imageUrl                   | categoryId   |
      |      | 5000  | http://example.com/img.png | {categoryId} |
    Then 응답 상태 코드는 400

  @error @validation
  Scenario: P-E9 상품 가격 0 이하
    Given "교환권" 카테고리가 존재한다
    When 상품 생성 요청을 보낸다
      | name  | price | imageUrl                   | categoryId   |
      | 아메리카노 | 0     | http://example.com/img.png | {categoryId} |
    Then 응답 상태 코드는 400
