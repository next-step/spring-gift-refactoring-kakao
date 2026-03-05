@api @option
#noinspection NonAsciiCharacters
Feature: 옵션 관리

  # --- Happy Flow ---

  @happy
  Scenario: O-1 옵션 추가 성공
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    When 해당 상품에 옵션 추가 요청을 보낸다
      | name | quantity |
      | 기본옵션 | 100      |
    Then 응답 상태 코드는 201
    And 응답 헤더에 Location이 존재한다
    And 응답 body의 "id"가 null이 아니다
    And 응답 body의 "name"이 "기본옵션"이다
    And 응답 body의 "quantity"가 100이다

  @happy
  Scenario: O-2 옵션 목록 조회
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    And 해당 상품에 수량 100인 "기본옵션"과 수량 50인 "추가옵션" 옵션이 존재한다
    When 해당 상품의 옵션 목록 조회 요청을 보낸다
    Then 응답 상태 코드는 200
    And 응답 body는 크기가 2인 배열이다

  @happy
  Scenario: O-3 옵션 삭제 성공
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    And 해당 상품에 "기본옵션", "추가옵션" 2개의 옵션이 존재한다
    When 해당 상품에서 "추가옵션" 옵션 삭제 요청을 보낸다
    Then 응답 상태 코드는 204
    And 응답 body가 없다

  @happy
  Scenario: O-4 옵션 수정 성공
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    And 해당 상품에 수량 100인 "TALL" 옵션이 존재한다
    When 해당 옵션 수정 요청을 보낸다
      | name   | quantity |
      | GRANDE | 200      |
    Then 응답 상태 코드는 200
    And 응답 body의 "name"이 "GRANDE"이다
    And 응답 body의 "quantity"가 200이다

  # --- State Verification ---

  @happy @state
  Scenario: O-S1 옵션 추가 후 목록에 포함된다
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    And 해당 상품에 "기존옵션" 옵션이 존재한다
    When 해당 상품에 옵션 추가 요청을 보낸다
      | name | quantity |
      | 새옵션  | 50       |
    Then 응답 상태 코드는 201
    When 해당 상품의 옵션 목록 조회 요청을 보낸다
    Then 응답 body는 크기가 2인 배열이다

  @happy @state
  Scenario: O-S3 옵션 수정 후 목록 재조회 시 변경 반영
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    And 해당 상품에 수량 100인 "TALL" 옵션이 존재한다
    When 해당 옵션 수정 요청을 보낸다
      | name   | quantity |
      | GRANDE | 200      |
    Then 응답 상태 코드는 200
    When 해당 상품의 옵션 목록 조회 요청을 보낸다
    Then 응답 상태 코드는 200
    And 응답 body에 "GRANDE" 가 포함된다

  @happy @state
  Scenario: O-S2 옵션 삭제 후 목록에서 제거된다
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    And 해당 상품에 "옵션A", "옵션B" 2개의 옵션이 존재한다
    When 해당 상품에서 "옵션A" 옵션 삭제 요청을 보낸다
    Then 응답 상태 코드는 204
    When 해당 상품의 옵션 목록 조회 요청을 보낸다
    Then 응답 body는 크기가 1인 배열이다

  # --- Error Flow ---

  @error
  Scenario: O-E1 존재하지 않는 상품에 옵션 조회
    When 존재하지 않는 상품의 옵션 목록 조회 요청을 보낸다
    Then 응답 상태 코드는 404

  @error
  Scenario: O-E2 존재하지 않는 상품에 옵션 추가
    When 존재하지 않는 상품에 옵션 추가 요청을 보낸다
      | name | quantity |
      | 기본옵션 | 100      |
    Then 응답 상태 코드는 404

  @error
  Scenario: O-E3 중복 옵션명 추가 시도
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    And 해당 상품에 "기본옵션" 옵션이 존재한다
    When 해당 상품에 옵션 추가 요청을 보낸다
      | name | quantity |
      | 기본옵션 | 200      |
    Then 응답 상태 코드는 400
    And 응답 body에 "이미 존재하는 옵션명입니다." 가 포함된다

  @error
  Scenario: O-E4 마지막 옵션 삭제 시도
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    And 해당 상품에 "기본옵션" 옵션 1개만 존재한다
    When 해당 상품의 현재 옵션 삭제 요청을 보낸다
    Then 응답 상태 코드는 400
    And 응답 body에 "옵션이 1개인 상품은 옵션을 삭제할 수 없습니다." 가 포함된다

  @error
  Scenario: O-E5 존재하지 않는 옵션 삭제
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    And 해당 상품에 "기본옵션", "추가옵션" 2개의 옵션이 존재한다
    When 해당 상품에서 존재하지 않는 옵션 삭제 요청을 보낸다
    Then 응답 상태 코드는 404

  @error @validation
  Scenario: O-E6 옵션 이름 51자 이상
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    When 해당 상품에 옵션 추가 요청을 보낸다
      | name                                                  | quantity |
      | 가나다라마바사아자차카타파하히후가나다라마바사아자차카나다라마바사아자차카타파하히후가나다라마바사아자차카 | 100      |
    Then 응답 상태 코드는 400
    And 응답 body에 "최대 50자" 가 포함된다

  @error @validation
  Scenario: O-E7 옵션 이름에 허용되지 않은 특수문자
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    When 해당 상품에 옵션 추가 요청을 보낸다
      | name  | quantity |
      | 옵션!@# | 100      |
    Then 응답 상태 코드는 400
    And 응답 body에 "허용되지 않는 특수 문자" 가 포함된다

  @error
  Scenario: O-E9 존재하지 않는 상품의 옵션 수정 시도
    When 존재하지 않는 상품의 옵션 수정 요청을 보낸다
      | name   | quantity |
      | GRANDE | 200      |
    Then 응답 상태 코드는 404

  @error
  Scenario: O-E10 존재하지 않는 옵션 수정 시도
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    When 해당 상품의 존재하지 않는 옵션 수정 요청을 보낸다
      | name   | quantity |
      | GRANDE | 200      |
    Then 응답 상태 코드는 404

  @error
  Scenario: O-E11 옵션 수정 시 다른 옵션과 이름 중복
    Given "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    And 해당 상품에 "TALL", "GRANDE" 2개의 옵션이 존재한다
    When "TALL" 옵션을 다음과 같이 수정 요청을 보낸다
      | name   | quantity |
      | GRANDE | 200      |
    Then 응답 상태 코드는 400
    And 응답 body에 "이미 존재하는 옵션명" 가 포함된다

  @error
  Scenario: O-E8 다른 상품의 옵션 삭제 시도
    Given "교환권" 카테고리에 "아메리카노" 상품과 "라떼" 상품이 존재한다
    And "아메리카노"에 "기본A", "추가A" 옵션이 존재한다
    And "라떼"에 "기본B", "추가B" 옵션이 존재한다
    When "아메리카노" 상품에서 "기본B" 옵션 삭제 요청을 보낸다
    Then 응답 상태 코드는 404
