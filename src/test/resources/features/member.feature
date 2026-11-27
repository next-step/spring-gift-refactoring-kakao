Feature: 1. 비회원은 이메일과 비밀번호로 회원 가입을 한다

  # newbie@example.com은 초기 데이터(V2)에 없는 이메일이다.
  # 응답의 로그인 토큰으로 회원 기능(위시 목록)을 쓸 수 있는지, 같은 이메일과 비밀번호로 로그인되는지로 가입된 것을 확인한다.
  Scenario: 1-1. 새 이메일로 회원 가입하면 로그인 토큰을 받고, 가입한 계정으로 로그인할 수 있다
    Given 이메일 "newbie@example.com"으로 가입한 회원은 없다
    When 비회원이 이메일 "newbie@example.com", 비밀번호 "newpass1"로 회원 가입한다
    Then 201이 온다
    And 응답에 로그인 토큰이 있다
    And 응답의 로그인 토큰으로 위시 목록을 조회하면 위시가 0개다
    And 이메일 "newbie@example.com", 비밀번호 "newpass1"로 로그인하면 200이 온다

  # user1@example.com은 초기 데이터의 회원 2번(user1)이고, 비밀번호는 password1이다.
  # 오류 메시지 문구는 open-questions 6번(오류 메시지 언어가 섞여 있다)에 걸려 있다. 6번을 고치면 이 문구도 함께 고친다.
  # 기존 비밀번호로는 로그인되고 새로 보낸 비밀번호로는 안 되는지로, 기존 계정을 덮어쓰지 않았는지 확인한다.
  Scenario: 1-2. 이미 가입한 이메일로 회원 가입하면 400이 오고 기존 계정은 그대로다
    Given 이메일 "user1@example.com", 비밀번호 "password1"로 로그인하면 200이 온다
    When 비회원이 이메일 "user1@example.com", 비밀번호 "otherpass"로 회원 가입한다
    Then 400이 온다
    And 응답 본문에 "Email is already registered."가 들어 있다
    And 이메일 "user1@example.com", 비밀번호 "password1"로 로그인하면 200이 온다
    And 이메일 "user1@example.com", 비밀번호 "otherpass"로 로그인하면 400이 온다

  # (확인 안 함) 요청으로 확인하지 않았다. 이메일 형식 검사(@Email)를 코드로만 보고 쓴 시나리오다.
  # 본문 문구는 보지 않는다.
  Scenario: 1-3. 이메일 형식이 틀리면 회원 가입이 되지 않는다
    When 비회원이 이메일 "abc", 비밀번호 "newpass1"로 회원 가입한다
    Then 400이 온다
