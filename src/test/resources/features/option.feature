Feature: 옵션 API

  Scenario: 옵션 생성
    Given 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    And 상품 "아이폰16", 가격 1350000, 이미지 "https://img.com/ip.jpg", 카테고리 "전자기기"가 등록되어 있고
    When 상품 "아이폰16"에 옵션 "블루 256GB", 수량 30으로 생성을 요청하면
    Then 응답 코드는 201이다
    And 응답의 옵션 이름은 "블루 256GB"이다
    When 상품 "아이폰16"의 옵션 목록을 조회하면
    Then 응답의 옵션 목록에 "블루 256GB"이 포함되어 있다

  Scenario: 옵션 목록 조회
    Given 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    And 상품 "맥북프로", 가격 3360000, 이미지 "https://img.com/mb.jpg", 카테고리 "전자기기"가 등록되어 있고
    And 상품 "맥북프로"에 옵션 "스페이스블랙 M1Pro", 수량 10이 등록되어 있고
    When 상품 "맥북프로"의 옵션 목록을 조회하면
    Then 응답 코드는 200이다
    And 응답의 옵션 목록에 "스페이스블랙 M1Pro"이 포함되어 있다

  Scenario: 옵션 삭제
    Given 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    And 상품 "아이패드", 가격 900000, 이미지 "https://img.com/ipad.jpg", 카테고리 "전자기기"가 등록되어 있고
    And 상품 "아이패드"에 옵션 "WiFi 256GB", 수량 20이 등록되어 있고
    And 상품 "아이패드"에 옵션 "Cellular 512GB", 수량 10이 등록되어 있고
    When 상품 "아이패드"의 옵션 "WiFi 256GB"를 삭제를 요청하면
    Then 응답 코드는 204이다
    When 상품 "아이패드"의 옵션 목록을 조회하면
    Then 응답의 옵션 목록에 "WiFi 256GB"이 포함되어 있지 않다
