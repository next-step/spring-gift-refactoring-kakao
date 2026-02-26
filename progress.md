
# 진행 과정

아래 내용은 각 진행 방법에 대해 어떻게 진행한지 기록한 내용임.

---

## 일단 테스트를 만든다.

semi 가 claude 한테 인수 테스트코드를 만들어달라 요청함.


- 어떤 방식으로 테스트 만들지 논의하고 정리한다.

먼저 간단한 happy flow 에 해당하는 테스트코드를 구성한 다음 test coverage 로 보완 안된 부분을 추가함.

예를들어 CUD 요청에서 404, 401 같은 flow 들은 happy flow 에 안되있으니까 test coverage 로 보면 cover 안된 부분 보임. 그래서 claude 한테 이 비어있는 부분도 cover 하도록 인수 테스트 만들어달라 함.

---

## 규칙화된 스타일을 세운다. 여타 컨벤션? 도 세운다.

코딩 컨벤션 세워야 한다.
- 아래 링크 제공해서 간단한 convention 만들도록 했다.
- https://naver.github.io/hackday-conventions-java/

### 요청 검증 부분

요청 객체에 대한 validation 이 종종 `...Validator` 로 구성되어있는데, 이를 일괄 spring validation 을 이용한다.

> ### 문제상황 : ProductNameValidator
> 
> 현재 코드를 보니 ProductNameValidator 가 2 군데서 사용된다. 하나는 ProductController, 하나는 AdminProductController 이다.
> 
> 문제는 AdminProductController 에서 예외 처리이다. 여기선 validator 가 model 에 에러 메세지를 추가해 view 에서 보여주는 용도로 사용되고 있다. 즉, 여기선 exception 이 throw 되면 안된다.
> 
> 반면 ProductController 에서는 exception (IllegalArgumentException --> 400 CODE) 던지는데 사용된다.
> 
> product name 은 아래 규칙을 따라야 한다.
> - 길이 규칙, 특수문자 규칙, not blank 규칙
> - admin 에서는 이름 '카카오' 가 허용되는데 일반 product 에서는 안된다.
>
> 이것을 어떻게 처리해야할지 아직 논의중

### 요청 응답 `ResponseEntity<...>`

코드를 보니 (개발자의 실수인 것 같지만) `ResponseEntity<?>` 처럼 되어 있는 부분이 있다.

wildcard 는 사용하지 않는다. `ResponseEntity<Void>`, `ResponseEntity<SomeResponse>` 처럼 명시한다.

### Bean 주입

모두 생성자 주입으로 되어있긴 한데, (이것도 아마 개발자의 실수) 생성자에 불필요한 `@Autowired` 가 있다.

여튼 생성자 주입을 사용하고 lombok 의 required argument constructor 를 이용한다. (+주입된 bean 은 모두 final 로 선언한다.)

### 변수, entity 선언

변수 선언에는 `var` 키워드는 사용하지 않는다. 불필요한 `final` 은 사용하지 않는다.

Entity 에 lombok getter 와 protected constructor 를 사용한다.

### Controller 메서드 이름

**논의해야 함**

- 지금 그대로 유지 VS create 는 `create...` 처럼 통일

### `목록 (GET method)` 응답 타입

코드를 보니 몇며 `목록 GET` endpoint 는 `List<...>` 로, 어떤건 `Page<...>` 로 되어 있다.

그리고 몇몇거는 `Pageable` param 을 받는다. **이걸 통일할지 말지 논의해야 함.**

**--> API 명세를 보니 의도적으로 나뉜 응답임. 그대로 납두도록**

---

## 목록화된 스타일을 차례대로 진행한다.


---

## 불필요한 코드를 제거한다.

lombok getter

### 불필요한 목록과 그 근거를 정리한다.

### 차례로 없앤다.

---

## 서비스 게층을 추출한다.

### 각 도메인별 어떻게 구성할건지 논의하고 기록한다.


### 차례대로 진행한다.

---

- 목록화된 스타일을 차례대로 진행한다.
- 불필요한 코드를 제거한다.
    - 불필요한 목록들을 정리하고 근거와 함께 차례대로 없앤다.
- 서비스 계층을 추출한다.
    - 각 도메인? 별 어떻게 구성할건지 논의한다. 그리고 기록한다.
    - 차례대로 진행한다.
