Feature: 회원 관리
  회원가입과 로그인 기능을 검증한다.

  Scenario: 회원가입에 성공한다
    When "newmember@example.com" 이메일과 "pass1234" 비밀번호로 회원가입한다
    Then 응답 코드는 201이다
    And 응답에 토큰이 포함되어 있다

  Scenario: 로그인에 성공한다
    When "user1@example.com" 이메일과 "password1" 비밀번호로 로그인한다
    Then 응답 코드는 200이다
    And 응답에 토큰이 포함되어 있다

  Scenario: 중복 이메일로 가입 시 실패한다
    When "admin@example.com" 이메일과 "anypass" 비밀번호로 회원가입한다
    Then 응답 코드는 400이다

  Scenario: 잘못된 비밀번호로 로그인 시 실패한다
    When "user1@example.com" 이메일과 "wrongpw" 비밀번호로 로그인한다
    Then 응답 코드는 400이다

  Scenario: 존재하지 않는 이메일로 로그인 시 실패한다
    When "nonexistent@example.com" 이메일과 "anypass" 비밀번호로 로그인한다
    Then 응답 코드는 400이다

  Scenario: 잘못된 이메일 형식으로 가입 시 실패한다
    When "not-an-email" 이메일과 "pass1234" 비밀번호로 회원가입한다
    Then 응답 코드는 400이다
