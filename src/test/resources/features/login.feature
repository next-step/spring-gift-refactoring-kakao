Feature: 2. 비회원은 이메일과 비밀번호로 로그인한다

  # user1@example.com은 초기 데이터의 회원 2번(user1)이고, 비밀번호는 password1이다.
  # 초기 데이터에서 회원 2번의 위시는 위시 1, 위시 2다. 받은 로그인 토큰이 user1의 것인지 위시 수로 확인한다.
  Scenario: 2-1. 가입한 이메일과 비밀번호로 로그인하면 로그인 토큰을 받고, 그 토큰으로 회원 기능을 쓸 수 있다
    When 비회원이 이메일 "user1@example.com", 비밀번호 "password1"로 로그인한다
    Then 200이 온다
    And 응답에 로그인 토큰이 있다
    And 응답의 로그인 토큰으로 위시 목록을 조회하면 위시가 2개다

  # 오류 메시지 문구는 open-questions 6번(오류 메시지 언어가 섞여 있다)에 걸려 있다. 6번을 고치면 이 문구도 함께 고친다.
  Scenario: 2-2. 비밀번호가 틀리면 400이 오고 로그인 토큰을 받지 못한다
    When 비회원이 이메일 "user1@example.com", 비밀번호 "wrongpass"로 로그인한다
    Then 400이 온다
    And 응답 본문에 "Invalid email or password."가 들어 있다

  # nobody@example.com은 초기 데이터에 없는 이메일이다.
  # 2-2와 같은 400과 같은 메시지가 온다. 응답으로는 이메일이 없는 것인지 비밀번호가 틀린 것인지 구분되지 않는다.
  Scenario: 2-3. 가입하지 않은 이메일로 로그인하면 400이 오고, 비밀번호가 틀렸을 때와 같은 메시지가 온다
    When 비회원이 이메일 "nobody@example.com", 비밀번호 "password1"로 로그인한다
    Then 400이 온다
    And 응답 본문에 "Invalid email or password."가 들어 있다
