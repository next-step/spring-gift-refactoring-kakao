@api @wish
Feature: 위시 관리

  # --- Happy Flow ---

  @happy
  Scenario: W-1 위시 추가 성공
    Given 회원이 존재하고 유효한 토큰을 가진다
    And "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    When 인증된 사용자가 위시 추가 요청을 보낸다
      | productId   |
      | {productId} |
    Then 응답 상태 코드는 201
    And 응답 헤더에 Location이 존재한다
    And 응답 body의 "id"가 null이 아니다
    And 응답 body의 "productId"가 해당 상품 ID이다
    And 응답 body의 "name"이 "아메리카노"이다

  @happy
  Scenario: W-2 위시 목록 페이지네이션 조회
    Given 회원이 존재하고 유효한 토큰을 가진다
    And 해당 회원에게 위시 3개가 등록되어 있다
    When 인증된 사용자가 위시 목록을 페이지 0 사이즈 2로 조회 요청을 보낸다
    Then 응답 상태 코드는 200
    And 응답 body의 "content" 배열 크기가 2이다
    And 응답 body의 "page.totalElements"가 3이다

  @happy
  Scenario: W-3 위시 삭제 성공
    Given 회원이 존재하고 유효한 토큰을 가진다
    And 해당 회원에게 위시가 등록되어 있다
    When 인증된 사용자가 해당 위시 삭제 요청을 보낸다
    Then 응답 상태 코드는 204
    And 응답 body가 없다

  @happy
  Scenario: W-4 동일 상품 중복 위시 추가 시 기존 위시 반환
    Given 회원이 존재하고 유효한 토큰을 가진다
    And "교환권" 카테고리에 "아메리카노" 상품이 존재한다
    And 해당 회원에게 "아메리카노" 상품 위시가 이미 등록되어 있다
    When 인증된 사용자가 위시 추가 요청을 보낸다
      | productId   |
      | {productId} |
    Then 응답 상태 코드는 200
    And 응답 body의 "id"가 기존 위시 ID와 동일하다

  # --- Error Flow ---

  @error @auth
  Scenario: W-E1 유효하지 않은 토큰으로 위시 조회
    When 유효하지 않은 토큰으로 위시 목록 조회 요청을 보낸다
    Then 응답 상태 코드는 401

  @error @auth
  Scenario: W-E2 유효하지 않은 토큰으로 위시 추가
    When 유효하지 않은 토큰으로 위시 추가 요청을 보낸다
      | productId |
      | 1         |
    Then 응답 상태 코드는 401

  @error @auth
  Scenario: W-E3 유효하지 않은 토큰으로 위시 삭제
    When 유효하지 않은 토큰으로 위시 삭제 요청을 보낸다
    Then 응답 상태 코드는 401

  @error
  Scenario: W-E4 존재하지 않는 상품 위시 추가
    Given 회원이 존재하고 유효한 토큰을 가진다
    When 인증된 사용자가 위시 추가 요청을 보낸다
      | productId |
      | 99999     |
    Then 응답 상태 코드는 404

  @error
  Scenario: W-E5 다른 회원의 위시 삭제 시도
    Given 회원A와 회원B가 존재한다
    And 회원B의 토큰이 준비된다
    And 회원A에게 위시가 등록되어 있다
    When 회원B가 회원A의 위시 삭제 요청을 보낸다
    Then 응답 상태 코드는 403

  @error
  Scenario: W-E6 존재하지 않는 위시 삭제
    Given 회원이 존재하고 유효한 토큰을 가진다
    When 인증된 사용자가 존재하지 않는 위시 삭제 요청을 보낸다
    Then 응답 상태 코드는 404
