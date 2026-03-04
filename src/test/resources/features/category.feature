Feature: 카테고리 API

  Scenario: 카테고리 생성
    When 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"로 생성을 요청하면
    Then 응답 코드는 201이다
    And 응답의 카테고리 이름은 "전자기기"이다
    When 카테고리 목록을 조회하면
    Then 응답의 카테고리 목록에 "전자기기"이 포함되어 있다

  Scenario: 카테고리 목록 조회
    Given 카테고리 "음식", 색상 "#32CD32", 이미지 "https://img.com/f.jpg"가 등록되어 있고
    When 카테고리 목록을 조회하면
    Then 응답 코드는 200이다
    And 응답의 카테고리 목록에 "음식"이 포함되어 있다

  Scenario: 카테고리 수정
    Given 카테고리 "패션", 색상 "#FF6347", 이미지 "https://img.com/fa.jpg"가 등록되어 있고
    When 카테고리 "패션"의 이름을 "의류"로 수정을 요청하면
    Then 응답 코드는 200이다
    And 응답의 카테고리 이름은 "의류"이다
    When 카테고리 목록을 조회하면
    Then 응답의 카테고리 목록에 "의류"이 포함되어 있다

  Scenario: 카테고리 삭제
    Given 카테고리 "임시", 색상 "#000000", 이미지 "https://img.com/t.jpg"가 등록되어 있고
    When 카테고리 "임시"의 삭제를 요청하면
    Then 응답 코드는 204이다
    When 카테고리 목록을 조회하면
    Then 응답의 카테고리 목록에 "임시"이 포함되어 있지 않다
