Feature: 주문하기

  Background:
    Given 이름이 "식품"인 카테고리가 등록되어 있고
    And "아메리카노" 상품이 가격 4500, 이미지 "http://image.url"로 등록되어 있고
    And 재고 10개인 "ICE" 옵션이 등록되어 있고
    And 포인트 100000을 가진 회원 "test@test.com"이 등록되어 있고

  Scenario: 유효한 주문을 하면 201 Created와 재고가 차감된다
    When "ICE" 옵션 3개를 주문하면
    Then 응답 상태 코드는 201이다
    And 응답에 주문 정보가 포함되어 있다
    And "ICE" 옵션의 재고는 7이다
    And 회원의 포인트는 86500이다

  Scenario: 존재하지 않는 옵션으로 주문하면 404 에러가 발생한다
    When 존재하지 않는 옵션으로 주문하면
    Then 응답 상태 코드는 404이다

  Scenario: 재고보다 많은 수량을 주문하면 에러가 발생한다
    When "ICE" 옵션 11개를 주문하면
    Then 응답 상태 코드는 400이다

  Scenario: 포인트가 부족하면 에러가 발생한다
    Given 포인트 100을 가진 회원 "poor@test.com"이 등록되어 있고
    When "poor@test.com" 회원이 "ICE" 옵션 1개를 주문하면
    Then 응답 상태 코드는 400이다

  Scenario: 인증 없이 주문하면 401 에러가 발생한다
    When 인증 없이 주문하면
    Then 응답 상태 코드는 401이다

  Scenario: 재고와 동일한 수량을 주문하면 재고가 0이 된다
    When "ICE" 옵션 10개를 주문하면
    Then 응답 상태 코드는 201이다
    And "ICE" 옵션의 재고는 0이다

  Scenario: 주문 후 주문 목록을 조회할 수 있다
    When "ICE" 옵션 2개를 주문하면
    Then 응답 상태 코드는 201이다
    When 주문 목록을 조회하면
    Then 응답 상태 코드는 200이다
    And 주문 목록에 주문 내역이 포함되어 있다
