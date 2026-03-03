# spring-gift-refactoring

본 작업은 전 도메인 인수 테스트를 구축하고, 해당 테스트를 기반으로 서비스 레이어를 도입하여 트랜잭션 문제를 해결한 리팩토링 과정입니다.

---

## 1. 인수 테스트 기반 실행 구조

### 테스트 실행 방식

현재 인수 테스트는 Docker 기반 통합 환경에서 실행됩니다.

```
./gradlew cucumberTest
```

해당 태스크는 다음 순서로 동작하도록 구성하였습니다.

1. `dockerUp` 실행
   - MySQL과 애플리케이션 컨테이너를 기동합니다.
2. Cucumber 인수 테스트 실행
   - 별도의 테스트 JVM에서 HTTP 요청을 통해 API를 검증합니다.
3. `dockerDown` 실행
   - 컨테이너 종료 및 볼륨을 정리합니다.

테스트는 항상 독립된 환경에서 시작되도록 설계하였습니다.

---

### 테스트 아키텍처

테스트 구조는 다음과 같습니다.

- 테스트 JVM과 애플리케이션 JVM은 별도 프로세스로 실행됩니다.
- 동일한 MySQL 인스턴스를 공유합니다.
- 기능 검증은 HTTP 요청 기반으로 수행합니다.
- 데이터 셋업 및 검증은 테스트 JVM에서 DB 직접 접근 방식으로 수행합니다.

이 구조를 통해 실제 운영 환경과 유사한 통합 테스트가 가능하도록 구성하였습니다.

---

### 데이터 초기화 전략

각 시나리오 실행 전 전체 테이블을 TRUNCATE 합니다.

- `@Before`에서 공통 초기화를 수행합니다.
- `FOREIGN_KEY_CHECKS`를 비활성화한 뒤 TRUNCATE를 수행합니다.
- 모든 Step Definition 클래스에 중복 정의하지 않고 `CommonStepDefinitions`로 통합하였습니다.

이를 통해 테스트 간 데이터 간섭을 제거하였습니다.

---

### 인수 테스트 범위

총 34개의 시나리오를 작성하였습니다.

| 도메인 | 시나리오 수 |
| --- | --- |
| 회원 | 5 |
| 카테고리 | 7 |
| 상품 | 7 |
| 옵션 | 4 |
| 위시리스트 | 4 |
| 주문 | 7 |

모든 리팩토링 작업은 해당 시나리오가 통과하는 것을 기준으로 진행하였습니다.

---

## 2. 테스트를 통해 발견한 문제

주문 도메인 테스트 중 다음과 같은 문제가 확인되었습니다.

- 포인트가 부족한 경우 주문은 실패합니다.
- 그러나 재고는 이미 차감된 상태로 남아 있었습니다.

원인은 다음과 같습니다.

- 컨트롤러에서 Repository를 직접 호출하고 있었습니다.
- 재고 차감과 포인트 차감이 각각 별도의 트랜잭션으로 커밋되고 있었습니다.

인수 테스트를 통해 데이터 불일치가 재현되었습니다.

---

## 3. 서비스 레이어 도입 및 트랜잭션 적용

문제 해결을 위해 서비스 레이어를 도입하였습니다.

- 모든 비즈니스 로직을 서비스 계층으로 이동하였습니다.
- 컨트롤러는 요청/응답 처리만 담당하도록 축소하였습니다.
- `OrderService`에 `@Transactional`을 적용하였습니다.

### 변경 후 주문 처리 구조

```
@Transactional
publicOrderResponsecreateOrder(LongmemberId,OrderRequestrequest) {
Membermember=memberRepository.findById(memberId).orElseThrow(...);
Optionoption=optionRepository.findById(request.optionId()).orElseThrow(...);

option.subtractQuantity(request.quantity());
intprice=option.getProduct().getPrice()*request.quantity();
member.deductPoint(price);

Ordersaved=orderRepository.save(request.toEntity(option,member.getId()));
returnOrderResponse.from(saved);
}
```

- 재고 차감과 포인트 차감이 하나의 트랜잭션으로 묶입니다.
- 포인트 부족 예외 발생 시 전체 롤백됩니다.
- JPA Dirty Checking을 활용하여 명시적 `save()` 호출을 제거하였습니다.

---

### 테스트 결과 변화

기존 기대값:

- 포인트 부족 시 재고 9개

변경 후 기대값:

- 포인트 부족 시 재고 10개

트랜잭션 적용 이후 재고가 정상적으로 롤백되는 것을 인수 테스트로 검증하였습니다.

---

## 4. AI 활용 방식

본 작업에서는 AI를 다음과 같은 방식으로 활용하였습니다.

### 1. 테스트 설계 검증

- 도메인 코드 분석 후 feature 시나리오 초안을 생성하였습니다.
- 시나리오가 비즈니스 의도를 정확히 반영하는지 반복 검토하였습니다.
- Step 충돌 및 NPE 원인을 분석하는 과정에서 구조적 원인을 함께 도출하였습니다.

---

### 2. 리팩토링 전략 수립

- 컨트롤러 9개의 의존성을 분석하여 서비스 분리 순서를 설계하였습니다.
- 단순 CRUD → 복합 로직 순으로 단계별 리팩토링 계획을 수립하였습니다.
- 트랜잭션 경계 설정 위치에 대한 대안을 비교 검토하였습니다.

---

### 3. 문제 원인 분석

- 트랜잭션 미적용으로 인한 부분 커밋 문제를 재현하고 원인을 정리하였습니다.
- JPA Dirty Checking과 detached 엔티티 개념을 코드 흐름 기준으로 설명받고 검증하였습니다.
- 테스트 기대값 수정이 설계 변경의 결과인지 확인하였습니다.
