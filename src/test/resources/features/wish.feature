Feature: 위시리스트 API

  Scenario: 위시리스트에 상품 추가
    Given 이메일 "wish@test.com", 비밀번호 "password1"로 가입한 회원이 있고
    And 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    And 상품 "아이폰16", 가격 1350000, 이미지 "https://img.com/ip.jpg", 카테고리 "전자기기"가 등록되어 있고
    When 이메일 "wish@test.com", 비밀번호 "password1"로 로그인을 요청하면
    And 상품 "아이폰16"을 위시리스트에 추가를 요청하면
    Then 응답 코드는 201이다
    And 응답의 위시 상품 이름은 "아이폰16"이다
    When 위시리스트를 조회하면
    Then 응답의 위시리스트에 "아이폰16"이 포함되어 있다

  Scenario: 위시리스트 조회
    Given 이메일 "wish2@test.com", 비밀번호 "password1"로 가입한 회원이 있고
    And 카테고리 "패션", 색상 "#FF6347", 이미지 "https://img.com/fa.jpg"가 등록되어 있고
    And 상품 "나이키에어맥스", 가격 179000, 이미지 "https://img.com/air.jpg", 카테고리 "패션"가 등록되어 있고
    And 이메일 "wish2@test.com", 비밀번호 "password1"로 로그인되어 있고
    And 상품 "나이키에어맥스"이 위시리스트에 등록되어 있고
    When 위시리스트를 조회하면
    Then 응답 코드는 200이다
    And 응답의 위시리스트에 "나이키에어맥스"이 포함되어 있다

  Scenario: 중복 위시 추가
    Given 이메일 "wishdup@test.com", 비밀번호 "password1"로 가입한 회원이 있고
    And 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    And 상품 "아이폰16", 가격 1350000, 이미지 "https://img.com/ip.jpg", 카테고리 "전자기기"가 등록되어 있고
    And 이메일 "wishdup@test.com", 비밀번호 "password1"로 로그인되어 있고
    And 상품 "아이폰16"이 위시리스트에 등록되어 있고
    When 상품 "아이폰16"을 위시리스트에 추가를 요청하면
    Then 응답 코드는 200이다

  Scenario: 다른 사용자의 위시 삭제 시도
    Given 이메일 "wishA@test.com", 비밀번호 "password1"로 가입한 회원이 있고
    And 이메일 "wishB@test.com", 비밀번호 "password1"로 가입한 회원이 있고
    And 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    And 상품 "아이폰16", 가격 1350000, 이미지 "https://img.com/ip.jpg", 카테고리 "전자기기"가 등록되어 있고
    And 이메일 "wishA@test.com", 비밀번호 "password1"로 로그인되어 있고
    And 상품 "아이폰16"이 위시리스트에 등록되어 있고
    And 이메일 "wishB@test.com", 비밀번호 "password1"로 로그인되어 있고
    When "wishA@test.com" 회원의 위시 "아이폰16"의 삭제를 요청하면
    Then 응답 코드는 403이다

  Scenario: 위시리스트에서 삭제
    Given 이메일 "wish3@test.com", 비밀번호 "password1"로 가입한 회원이 있고
    And 카테고리 "식품", 색상 "#32CD32", 이미지 "https://img.com/food.jpg"가 등록되어 있고
    And 상품 "제주감귤", 가격 25000, 이미지 "https://img.com/tangerine.jpg", 카테고리 "식품"가 등록되어 있고
    And 이메일 "wish3@test.com", 비밀번호 "password1"로 로그인되어 있고
    And 상품 "제주감귤"이 위시리스트에 등록되어 있고
    When 위시리스트에서 "제주감귤"의 삭제를 요청하면
    Then 응답 코드는 204이다
    When 위시리스트를 조회하면
    Then 응답의 위시리스트에 "제주감귤"이 포함되어 있지 않다
