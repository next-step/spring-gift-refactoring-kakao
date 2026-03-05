@api @category
#noinspection NonAsciiCharacters
Feature: 카테고리 관리

  # --- Happy Flow ---

  @happy
  Scenario: C-1 카테고리 생성 성공
    When 카테고리 생성 요청을 보낸다
      | name | color   | imageUrl                   | description |
      | 교환권  | #FF0000 | http://example.com/img.png | 교환권 설명      |
    Then 응답 상태 코드는 201
    And 응답 헤더에 Location이 존재한다
    And 응답 body의 "id"가 null이 아니다
    And 응답 body의 "name"이 "교환권"이다
    And 응답 body의 "color"가 "#FF0000"이다
    And 응답 body의 "imageUrl"이 "http://example.com/img.png"이다
    And 응답 body의 "description"이 "교환권 설명"이다

  @happy
  Scenario: C-2 카테고리 목록 조회
    Given 다음 카테고리가 존재한다
      | name | color   | imageUrl                 | description |
      | 교환권  | #FF0000 | http://example.com/a.png | 설명1         |
      | 상품권  | #00FF00 | http://example.com/b.png | 설명2         |
    When 카테고리 목록 조회 요청을 보낸다
    Then 응답 상태 코드는 200
    And 응답 body는 크기가 2인 배열이다

  @happy
  Scenario: C-3 카테고리 수정 성공
    Given "교환권" 카테고리가 존재한다
    When 해당 카테고리 수정 요청을 보낸다
      | name | color   | imageUrl                   | description |
      | 수정카테 | #0000FF | http://example.com/new.png | 수정 설명       |
    Then 응답 상태 코드는 200
    And 응답 body의 "name"이 "수정카테"이다
    And 응답 body의 "color"가 "#0000FF"이다
    And 응답 body의 "imageUrl"이 "http://example.com/new.png"이다
    And 응답 body의 "description"이 "수정 설명"이다

  @happy
  Scenario: C-4 카테고리 삭제 성공
    Given "교환권" 카테고리가 존재한다
    When 해당 카테고리 삭제 요청을 보낸다
    Then 응답 상태 코드는 204

  @happy
  Scenario: C-5 데이터 없을 때 빈 목록 조회
    When 카테고리 목록 조회 요청을 보낸다
    Then 응답 상태 코드는 200
    And 응답 body는 빈 배열이다

  @happy
  Scenario: C-6 카테고리 단건 조회 성공
    Given "교환권" 카테고리가 존재한다
    When 해당 카테고리 조회 요청을 보낸다
    Then 응답 상태 코드는 200
    And 응답 body의 "id"가 null이 아니다
    And 응답 body의 "name"이 "교환권"이다

  # --- State Verification ---

  @happy @state
  Scenario: C-S1 카테고리 삭제 후 목록 조회 시 빈 배열
    Given "교환권" 카테고리가 존재한다
    When 해당 카테고리 삭제 요청을 보낸다
    Then 응답 상태 코드는 204
    When 카테고리 목록 조회 요청을 보낸다
    Then 응답 body는 빈 배열이다

  @happy @state
  Scenario: C-S2 카테고리 수정 후 목록에서 변경 내용이 반영된다
    Given "교환권" 카테고리가 존재한다
    When 해당 카테고리 수정 요청을 보낸다
      | name | color   | imageUrl                   | description |
      | 음료   | #FF0000 | http://example.com/new.png | 음료 카테고리     |
    Then 응답 상태 코드는 200
    When 카테고리 목록 조회 요청을 보낸다
    Then 응답 body는 크기가 1인 배열이다
    And 응답 body의 "[0].name"이 "음료"이다
    And 응답 body의 "[0].color"가 "#FF0000"이다

  @happy @state
  Scenario: C-S3 카테고리 수정 후 단건 재조회 시 변경 내용이 반영된다
    Given "교환권" 카테고리가 존재한다
    When 해당 카테고리 수정 요청을 보낸다
      | name | color   | imageUrl                   | description |
      | 음료   | #0000FF | http://example.com/new.png | 음료 카테고리     |
    Then 응답 상태 코드는 200
    When 해당 카테고리 조회 요청을 보낸다
    Then 응답 상태 코드는 200
    And 응답 body의 "name"이 "음료"이다
    And 응답 body의 "color"가 "#0000FF"이다

  @happy @state
  Scenario: C-S4 카테고리 삭제 후 단건 재조회 시 404
    Given "교환권" 카테고리가 존재한다
    When 해당 카테고리 삭제 요청을 보낸다
    Then 응답 상태 코드는 204
    When 해당 카테고리 조회 요청을 보낸다
    Then 응답 상태 코드는 404

  # --- Error Flow ---

  @error @validation
  Scenario: C-E1 name 빈 값으로 카테고리 생성 시도
    When 카테고리 생성 요청을 보낸다
      | name | color   | imageUrl                   |
      |      | #FF0000 | http://example.com/img.png |
    Then 응답 상태 코드는 400

  @error @validation
  Scenario: C-E2 color 빈 값으로 카테고리 생성 시도
    When 카테고리 생성 요청을 보낸다
      | name | color | imageUrl                   |
      | 교환권  |       | http://example.com/img.png |
    Then 응답 상태 코드는 400

  @error @validation
  Scenario: C-E3 imageUrl 빈 값으로 카테고리 생성 시도
    When 카테고리 생성 요청을 보낸다
      | name | color   | imageUrl |
      | 교환권  | #FF0000 |          |
    Then 응답 상태 코드는 400

  @error
  Scenario: C-E4 존재하지 않는 카테고리 수정 시도
    When 존재하지 않는 카테고리 수정 요청을 보낸다
      | name | color   | imageUrl                   |
      | 수정카테 | #FF0000 | http://example.com/img.png |
    Then 응답 상태 코드는 404

  @happy
  Scenario: C-E5 description이 null이어도 카테고리 생성 성공
    When 카테고리 생성 요청을 보낸다
      | name | color   | imageUrl                   |
      | 교환권  | #FF0000 | http://example.com/img.png |
    Then 응답 상태 코드는 201
    And 응답 body의 "description"이 null이다

  @error
  Scenario: C-E6 존재하지 않는 카테고리 단건 조회
    When 존재하지 않는 카테고리 조회 요청을 보낸다
    Then 응답 상태 코드는 404
