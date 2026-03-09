Feature: 위시리스트 관리

  Background:
    Given 이름이 "식품"인 카테고리가 등록되어 있고
    And "아메리카노" 상품이 가격 4500, 이미지 "http://img.url"로 등록되어 있고
    And 포인트 100000을 가진 회원 "wish@test.com"이 등록되어 있고

  Scenario: 위시를 추가하면 200 OK와 위시 정보가 반환된다
    When "wish@test.com" 회원이 해당 상품을 위시에 추가하면
    Then 응답 상태 코드는 200이다
    And 응답에 위시 정보가 포함되어 있다

  Scenario: 이미 추가된 상품을 다시 위시에 추가하면 기존 위시가 반환된다
    Given "wish@test.com" 회원이 해당 상품을 위시에 추가하면
    When "wish@test.com" 회원이 해당 상품을 위시에 추가하면
    Then 응답 상태 코드는 200이다
    And 응답의 위시 ID는 기존과 동일하다

  Scenario: 위시 목록을 조회할 수 있다
    Given "wish@test.com" 회원이 해당 상품을 위시에 추가하면
    When "wish@test.com" 회원이 위시 목록을 조회하면
    Then 응답 상태 코드는 200이다
    And 위시 목록에 1개의 위시가 포함되어 있다

  Scenario: 위시를 삭제하면 204 No Content가 반환된다
    Given "wish@test.com" 회원이 해당 상품을 위시에 추가하면
    When "wish@test.com" 회원이 해당 위시를 삭제하면
    Then 응답 상태 코드는 204이다

  Scenario: 타인의 위시를 삭제하면 403 에러가 발생한다
    Given "wish@test.com" 회원이 해당 상품을 위시에 추가하면
    And 포인트 100000을 가진 회원 "other@test.com"이 등록되어 있고
    When "other@test.com" 회원이 해당 위시를 삭제하면
    Then 응답 상태 코드는 403이다

  Scenario: 인증 없이 위시 목록을 조회하면 401 에러가 발생한다
    When 인증 없이 위시 목록을 조회하면
    Then 응답 상태 코드는 401이다

  Scenario: 존재하지 않는 상품을 위시에 추가하면 404 에러가 발생한다
    When "wish@test.com" 회원이 존재하지 않는 상품을 위시에 추가하면
    Then 응답 상태 코드는 404이다
