@api @member
Feature: 회원 관리

  # --- Happy Flow ---

  @happy
  Scenario: M-1 회원가입 성공
    When 회원가입 요청을 보낸다
      | email            | password  |
      | test@example.com | password1 |
    Then 응답 상태 코드는 201
    And 응답 body의 "token" 필드가 비어있지 않다

  @happy
  Scenario: M-2 로그인 성공
    Given 이메일 "test@example.com" 비밀번호 "password1" 회원이 존재한다
    When 로그인 요청을 보낸다
      | email            | password  |
      | test@example.com | password1 |
    Then 응답 상태 코드는 200
    And 응답 body의 "token" 필드가 비어있지 않다

  # --- Error Flow ---

  @error
  Scenario: M-E1 이메일 중복 가입
    Given 이메일 "test@example.com" 비밀번호 "password1" 회원이 존재한다
    When 회원가입 요청을 보낸다
      | email            | password  |
      | test@example.com | password2 |
    Then 응답 상태 코드는 400
    And 응답 body에 "Email is already registered." 가 포함된다

  @error
  Scenario: M-E2 존재하지 않는 이메일 로그인
    When 로그인 요청을 보낸다
      | email               | password  |
      | unknown@example.com | password1 |
    Then 응답 상태 코드는 400
    And 응답 body에 "Invalid email or password." 가 포함된다

  @error
  Scenario: M-E3 비밀번호 불일치 로그인
    Given 이메일 "test@example.com" 비밀번호 "password1" 회원이 존재한다
    When 로그인 요청을 보낸다
      | email            | password   |
      | test@example.com | wrong-pass |
    Then 응답 상태 코드는 400
    And 응답 body에 "Invalid email or password." 가 포함된다

  @error @validation
  Scenario: M-E4 이메일 형식 위반
    When 회원가입 요청을 보낸다
      | email     | password  |
      | not-email | password1 |
    Then 응답 상태 코드는 400

  @error @validation
  Scenario: M-E5 이메일 빈 값
    When 회원가입 요청을 보낸다
      | email | password  |
      |       | password1 |
    Then 응답 상태 코드는 400

  @error @validation
  Scenario: M-E6 비밀번호 빈 값
    When 회원가입 요청을 보낸다
      | email            | password |
      | test@example.com |          |
    Then 응답 상태 코드는 400
