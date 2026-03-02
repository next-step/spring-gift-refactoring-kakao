@api @order
#noinspection NonAsciiCharacters
Feature: 주문 관리

  # --- Happy Flow ---

  @happy
  Scenario: OR-1 주문 생성 성공
    Given 포인트가 충분한 회원이 존재하고 유효한 토큰을 가진다
    And "교환권" 카테고리에 가격 5000인 "아메리카노" 상품이 존재한다
    And 해당 상품에 수량 100인 "기본옵션" 옵션이 존재한다
    When 인증된 사용자가 주문 생성 요청을 보낸다
      | optionId   | quantity | message |
      | {optionId} | 2        | 선물입니다   |
    Then 응답 상태 코드는 201
    And 응답 헤더에 Location이 존재한다
    And 응답 body의 "id"가 null이 아니다
    And 응답 body의 "optionId"가 해당 옵션 ID이다
    And 응답 body의 "quantity"가 2이다
    And 응답 body의 "orderDateTime"이 null이 아니다
    And 응답 body의 "message"가 "선물입니다"이다

  @happy
  Scenario: OR-2 주문 후 옵션 재고가 차감된다
    Given 포인트가 충분한 회원이 존재하고 유효한 토큰을 가진다
    And "교환권" 카테고리에 가격 5000인 "아메리카노" 상품이 존재한다
    And 해당 상품에 수량 100인 "기본옵션" 옵션이 존재한다
    When 인증된 사용자가 수량 3으로 주문 생성 요청을 보낸다
    And 해당 상품의 옵션 목록 조회 요청을 보낸다
    Then 응답 상태 코드는 200
    And 응답 body는 크기가 1인 배열이다

  @happy
  Scenario: OR-3 주문 목록 페이지네이션 조회
    Given 회원이 존재하고 유효한 토큰을 가진다
    And 해당 회원에게 주문 3개가 존재한다
    When 인증된 사용자가 주문 목록을 페이지 0 사이즈 2로 조회 요청을 보낸다
    Then 응답 상태 코드는 200
    And 응답 body의 "content" 배열 크기가 2이다
    And 응답 body의 "page.totalElements"가 3이다

  @happy
  Scenario: OR-4 message 없이 주문 생성 성공
    Given 포인트가 충분한 회원이 존재하고 유효한 토큰을 가진다
    And "교환권" 카테고리에 가격 5000인 "아메리카노" 상품이 존재한다
    And 해당 상품에 수량 100인 "기본옵션" 옵션이 존재한다
    When 인증된 사용자가 주문 생성 요청을 보낸다
      | optionId   | quantity |
      | {optionId} | 1        |
    Then 응답 상태 코드는 201
    And 응답 body의 "message"가 null이다

  # --- Error Flow ---

  @error @auth
  Scenario: OR-E1 유효하지 않은 토큰으로 주문 생성
    When 유효하지 않은 토큰으로 주문 생성 요청을 보낸다
      | optionId | quantity |
      | 1        | 1        |
    Then 응답 상태 코드는 401

  @error @auth
  Scenario: OR-E2 유효하지 않은 토큰으로 주문 조회
    When 유효하지 않은 토큰으로 주문 목록 조회 요청을 보낸다
    Then 응답 상태 코드는 401

  @error
  Scenario: OR-E3 존재하지 않는 옵션으로 주문
    Given 회원이 존재하고 유효한 토큰을 가진다
    When 인증된 사용자가 주문 생성 요청을 보낸다
      | optionId | quantity |
      | 99999    | 1        |
    Then 응답 상태 코드는 404

  @error
  Scenario: OR-E4 재고 부족 시 주문 실패
    Given 포인트가 충분한 회원이 존재하고 유효한 토큰을 가진다
    And "교환권" 카테고리에 가격 5000인 "아메리카노" 상품이 존재한다
    And 해당 상품에 수량 1인 "기본옵션" 옵션이 존재한다
    When 인증된 사용자가 주문 생성 요청을 보낸다
      | optionId   | quantity |
      | {optionId} | 5        |
    Then 응답 상태 코드는 500

  @error
  Scenario: OR-E5 포인트 부족 시 주문 실패
    Given 포인트가 100인 회원이 존재하고 유효한 토큰을 가진다
    And "교환권" 카테고리에 가격 5000인 "아메리카노" 상품이 존재한다
    And 해당 상품에 수량 100인 "기본옵션" 옵션이 존재한다
    When 인증된 사용자가 주문 생성 요청을 보낸다
      | optionId   | quantity |
      | {optionId} | 1        |
    Then 응답 상태 코드는 500

  @error @validation
  Scenario: OR-E6 수량 0 이하로 주문 시도
    Given 회원이 존재하고 유효한 토큰을 가진다
    When 인증된 사용자가 주문 생성 요청을 보낸다
      | optionId | quantity |
      | 1        | 0        |
    Then 응답 상태 코드는 400
