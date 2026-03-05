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

  Scenario: 카카오가 포함된 상품명으로 생성
    Given 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    When 상품 "카카오프렌즈", 가격 15000, 이미지 "https://img.com/k.jpg", 카테고리 "전자기기"로 생성을 요청하면
    Then 응답 코드는 400이다

  Scenario: 15자 초과 상품명으로 생성
    Given 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    When 상품 "일이삼사오육칠팔구십일이삼사오육", 가격 1000, 이미지 "https://img.com/l.jpg", 카테고리 "전자기기"로 생성을 요청하면
    Then 응답 코드는 400이다

  Scenario: 허용되지 않는 특수문자가 포함된 상품명으로 생성
    Given 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    When 상품 "아이폰!@#", 가격 1000, 이미지 "https://img.com/s.jpg", 카테고리 "전자기기"로 생성을 요청하면
    Then 응답 코드는 400이다

  Scenario: 가격 0원으로 상품 생성
    Given 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    When 상품 "무료상품", 가격 0, 이미지 "https://img.com/free.jpg", 카테고리 "전자기기"로 생성을 요청하면
    Then 응답 코드는 400이다

  Scenario: 삭제한 상품 조회
    Given 카테고리 "전자기기", 색상 "#1E90FF", 이미지 "https://img.com/e.jpg"가 등록되어 있고
    And 상품 "삭제용상품2", 가격 10000, 이미지 "https://img.com/del2.jpg", 카테고리 "전자기기"가 등록되어 있고
    When 상품 "삭제용상품2"의 삭제를 요청하면
    And 상품 "삭제용상품2"를 조회하면
    Then 응답 코드는 404이다
