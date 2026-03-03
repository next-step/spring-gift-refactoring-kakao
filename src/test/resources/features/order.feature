Feature: 주문 관리
  주문 CRUD 기능을 검증한다.

  Scenario: 주문 목록을 조회한다
    Given "user1@example.com" 이메일과 "password1" 비밀번호로 로그인되어 있다
    When 주문 목록을 조회한다
    Then 응답 코드는 200이다

  Scenario: 주문을 생성한다
    Given "user1@example.com" 이메일과 "password1" 비밀번호로 로그인되어 있다
    When 옵션 5, 수량 1, 메시지 "테스트 주문"으로 주문한다
    Then 응답 코드는 201이다
    And 응답에 주문 ID가 포함되어 있다

  Scenario: 포인트 부족 시 주문에 실패한다
    Given "broke@example.com" 이메일과 "pass1234" 비밀번호로 회원가입되어 있다
    When 옵션 1, 수량 1, 메시지 ""으로 주문한다
    Then 응답 코드는 400이다

  Scenario: 재고 초과 주문 시 실패한다
    Given "user1@example.com" 이메일과 "password1" 비밀번호로 로그인되어 있다
    When 옵션 1, 수량 99999999, 메시지 ""으로 주문한다
    Then 응답 코드는 400이다

  Scenario: 없는 옵션으로 주문 시 404 응답
    Given "user1@example.com" 이메일과 "password1" 비밀번호로 로그인되어 있다
    When 옵션 999999, 수량 1, 메시지 ""으로 주문한다
    Then 응답 코드는 404이다

  Scenario: 인증 없이 주문 조회 시 401 응답
    Given 유효하지 않은 토큰으로 인증한다
    When 주문 목록을 조회한다
    Then 응답 코드는 401이다
