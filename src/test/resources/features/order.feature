Feature: 주문 API

  Scenario: 주문 생성
    Given 이메일 "order@test.com", 비밀번호 "password1"로 가입한 회원이 있고
    And "order@test.com" 회원의 포인트가 10000000원이고
    And 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    And 상품 "아이폰16", 가격 1350000, 이미지 "https://img.com/ip.jpg", 카테고리 "전자기기"가 등록되어 있고
    And 상품 "아이폰16"에 옵션 "블루 256GB", 수량 30이 등록되어 있고
    And 이메일 "order@test.com", 비밀번호 "password1"로 로그인되어 있고
    When 옵션 "블루 256GB"를 1개, 메시지 "생일 축하해!"로 주문을 요청하면
    Then 응답 코드는 201이다
    And 응답의 주문 수량은 1이다
    And 응답의 주문 메시지는 "생일 축하해!"이다
    When 주문 목록을 조회하면
    Then 응답의 주문 목록 크기는 1이다

  Scenario: 주문 목록 조회
    Given 이메일 "order2@test.com", 비밀번호 "password1"로 가입한 회원이 있고
    And "order2@test.com" 회원의 포인트가 10000000원이고
    And 카테고리 "패션", 색상 "#FF6347", 이미지 "https://img.com/fa.jpg"가 등록되어 있고
    And 상품 "나이키에어맥스", 가격 179000, 이미지 "https://img.com/air.jpg", 카테고리 "패션"가 등록되어 있고
    And 상품 "나이키에어맥스"에 옵션 "270mm", 수량 15이 등록되어 있고
    And 이메일 "order2@test.com", 비밀번호 "password1"로 로그인되어 있고
    And 옵션 "270mm"를 1개 주문이 등록되어 있고
    When 주문 목록을 조회하면
    Then 응답 코드는 200이다
    And 응답의 주문 목록 크기는 1이다

  Scenario: 포인트 부족으로 주문 실패
    Given 이메일 "poor@test.com", 비밀번호 "password1"로 가입한 회원이 있고
    And "poor@test.com" 회원의 포인트가 0원이고
    And 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    And 상품 "아이폰16", 가격 1350000, 이미지 "https://img.com/ip.jpg", 카테고리 "전자기기"가 등록되어 있고
    And 상품 "아이폰16"에 옵션 "블루 256GB", 수량 30이 등록되어 있고
    And 이메일 "poor@test.com", 비밀번호 "password1"로 로그인되어 있고
    When 옵션 "블루 256GB"를 1개, 메시지 "주문합니다"로 주문을 요청하면
    Then 응답 코드는 500이다
