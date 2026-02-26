Feature: 상품 API

  Scenario: 상품 생성
    Given 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    When 상품 "아이폰16", 가격 1350000, 이미지 "https://img.com/ip.jpg", 카테고리 "전자기기"로 생성을 요청하면
    Then 응답 코드는 201이다
    And 응답의 상품 이름은 "아이폰16"이다
    When 상품 목록을 조회하면
    Then 응답의 상품 목록에 "아이폰16"이 포함되어 있다

  Scenario: 상품 단건 조회
    Given 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    And 상품 "맥북프로", 가격 3360000, 이미지 "https://img.com/mb.jpg", 카테고리 "전자기기"가 등록되어 있고
    When 상품 "맥북프로"를 조회하면
    Then 응답 코드는 200이다
    And 응답의 상품 이름은 "맥북프로"이다

  Scenario: 상품 목록 조회
    Given 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    And 상품 "아이폰16", 가격 1350000, 이미지 "https://img.com/ip.jpg", 카테고리 "전자기기"가 등록되어 있고
    When 상품 목록을 조회하면
    Then 응답 코드는 200이다
    And 응답의 상품 목록에 "아이폰16"이 포함되어 있다

  Scenario: 상품 수정
    Given 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    And 상품 "아이폰15", 가격 1200000, 이미지 "https://img.com/ip15.jpg", 카테고리 "전자기기"가 등록되어 있고
    When 상품 "아이폰15"의 이름을 "아이폰16"으로 수정을 요청하면
    Then 응답 코드는 200이다
    And 응답의 상품 이름은 "아이폰16"이다
    When 상품 목록을 조회하면
    Then 응답의 상품 목록에 "아이폰16"이 포함되어 있다

  Scenario: 상품 삭제
    Given 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    And 상품 "삭제용상품", 가격 10000, 이미지 "https://img.com/del.jpg", 카테고리 "전자기기"가 등록되어 있고
    When 상품 "삭제용상품"의 삭제를 요청하면
    Then 응답 코드는 204이다
    When 상품 목록을 조회하면
    Then 응답의 상품 목록에 "삭제용상품"이 포함되어 있지 않다
