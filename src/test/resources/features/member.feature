Feature: 회원 API

  Scenario: 회원 가입
    When 이메일 "new@test.com", 비밀번호 "password1"로 회원 가입을 요청하면
    Then 응답 코드는 201이다
    And 응답에 토큰이 포함되어 있다
    When 이메일 "new@test.com", 비밀번호 "password1"로 로그인을 요청하면
    Then 응답 코드는 200이다
    And 응답에 토큰이 포함되어 있다

  Scenario: 로그인
    Given 이메일 "login@test.com", 비밀번호 "password1"로 가입한 회원이 있고
    When 이메일 "login@test.com", 비밀번호 "password1"로 로그인을 요청하면
    Then 응답 코드는 200이다
    And 응답에 토큰이 포함되어 있다
