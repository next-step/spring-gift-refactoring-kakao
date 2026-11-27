Feature: 5. 비회원은 카테고리 목록을 본다
  카테고리 목록은 로그인 토큰 없이 본다.

  # 초기 데이터의 카테고리는 카테고리 1 전자기기, 카테고리 2 패션, 카테고리 3 식품이다.
  # 이름, 색, 이미지 URL, 설명은 모두 초기 데이터(V2) 그대로다. 요청으로 확인했다.
  Scenario: 5-1. 카테고리 목록을 보면 모든 카테고리가 이름, 색, 이미지 URL, 설명과 함께 나온다
    When 비회원이 카테고리 목록을 조회한다
    Then 200이 온다
    And 응답의 카테고리 번호는 1, 2, 3이다
    And 응답의 카테고리 1은 이름 "전자기기", 색 "#1E90FF", 이미지 URL "https://example.com/images/electronics.jpg", 설명 "스마트폰, 노트북 등 전자기기"다
    And 응답의 카테고리 2는 이름 "패션", 색 "#FF6347", 이미지 URL "https://example.com/images/fashion.jpg", 설명 "의류, 신발, 액세서리"다
    And 응답의 카테고리 3은 이름 "식품", 색 "#32CD32", 이미지 URL "https://example.com/images/food.jpg", 설명 "신선식품, 가공식품, 음료"다
