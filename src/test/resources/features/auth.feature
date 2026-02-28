@api @auth
Feature: 인증

  @happy
  Scenario: A-1 발급된 토큰으로 인증 필요 API 접근 가능
    Given 회원이 존재하고 유효한 토큰을 가진다
    When 인증이 필요한 API에 해당 토큰으로 요청한다
    Then 응답 상태 코드는 200
