Feature: 위시리스트 관리
  위시리스트 CRUD 기능을 검증한다.

  Scenario: 위시리스트를 조회한다
    Given "user1@example.com" 이메일과 "password1" 비밀번호로 로그인되어 있다
    When 위시리스트를 조회한다
    Then 응답 코드는 200이다

  Scenario: 위시리스트에 상품을 추가한다
    Given "user1@example.com" 이메일과 "password1" 비밀번호로 로그인되어 있다
    When 상품 2를 위시리스트에 추가한다
    Then 응답 코드는 201이다

  Scenario: 이미 있는 상품 추가 시 200 응답
    Given "user1@example.com" 이메일과 "password1" 비밀번호로 로그인되어 있다
    When 상품 1을 위시리스트에 추가한다
    Then 응답 코드는 200이다

  Scenario: 위시를 삭제한다
    Given "wish-remove@example.com" 이메일과 "pass1234" 비밀번호로 회원가입되어 있다
    And 상품 1을 위시리스트에 추가하고 ID를 저장한다
    When 저장된 위시를 삭제한다
    Then 응답 코드는 204이다

  Scenario: 다른 회원의 위시를 삭제하면 403 응답
    Given "wish-owner@example.com" 이메일과 "pass1234" 비밀번호로 회원가입되어 있다
    And 상품 4를 위시리스트에 추가하고 ID를 저장한다
    And "wish-other@example.com" 이메일과 "pass1234" 비밀번호로 회원가입되어 있다
    When 저장된 위시를 삭제한다
    Then 응답 코드는 403이다

  Scenario: 인증 없이 위시리스트 조회 시 401 응답
    Given 유효하지 않은 토큰으로 인증한다
    When 위시리스트를 조회한다
    Then 응답 코드는 401이다

  Scenario: 없는 상품을 위시리스트에 추가 시 404 응답
    Given "user1@example.com" 이메일과 "password1" 비밀번호로 로그인되어 있다
    When 상품 999999를 위시리스트에 추가한다
    Then 응답 코드는 404이다
