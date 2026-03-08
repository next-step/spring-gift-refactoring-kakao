# 프롬프트 기록

## 프롬프트 1
소스코드는 변경하지 않고, 테스트 코드만 현재 API 스펙과 도메인 모델에 맞춰 수정한다.

## 프롬프트 2
분석에 사용할 SKILL을 생성해줘. 분석을 수행할 때에는 반드시 보고서를 docs/ 하위에 작성해야 해.
또한 리팩터링에 사용할 SKILL을 생성해줘. 리팩터링을 수행할 때에는 반드시 보고서를 docs/ 하위에 작성해야 해. (왜 이렇게 변경하는가, 다른 대안들은 없는가, 장단점 비교 분석)

## 프롬프트 3
1-1. 테스트 범위 결정 — REST API Controller 7개의 전체 엔드포인트 목록, 엔드포인트별 정상/실패 시나리오 도출, 기존 테스트 커버리지 분석, 인수 테스트에 반영할 시나리오 선정. 분석 보고서를 docs/step1-1-테스트-범위-결정.md에 작성.

## 프롬프트 4
조회 시나리오 통합
- **결정:** 생성 시나리오에서 조회를 함께 검증. 별도 조회 테스트 미작성.
- **근거:** 생성-조회를 하나의 흐름으로 검증하면 시나리오 과잉 세분화 방지. 정렬/필터/페이징 요구사항이 생기면 분리.

## 프롬프트 5
1-2. 인수 테스트 작성 — docs/step1-1-테스트-범위-결정.md에서 선정한 17개 시나리오를 Cucumber 인수 테스트로 작성. feature 파일 단위로 작업하고, 각 작업 후 cucumberTest로 전체 통과 확인. 프로덕션 코드(src/main) 수정 없음.

## 프롬프트 5 추가 지침
- 각 Task 작업이 끝나면 커밋메시지를 제안하고, 다음 테스크는 사용자가 진행하라고 할 때까지 대기한다.
- 상태 변경(CUD) 시나리오는 응답 코드만이 아니라 반드시 조회 API로 결과를 검증해야 한다. acceptance-test SKILL 검증 전략에 해당 규칙 반영.

## 프롬프트 6
불가피한 상황에서만 DB조회를 사용해야 한다. 실제 조회 API가 있는 경우, 조회 API를 사용하도록 수정한다.

## 프롬프트 7
배경에 명시된 테스트 조건들을 더 명확하게 작성하라.
만약 재활용 가능한 Step이 있다면 그것을 활용하고, 아니라면 새로운 Step으로 리팩터링한다.
가령, 배경으로 회원을 추가한다면 어떤 정보들을 갖는 회원인지 등을 명시해야 한다.

## 프롬프트 8
2단계 스타일 정리를 실행한다.
- Spotless 플러그인(palantir-java-format)을 도입하여 전체 Java 소스에 일관된 포매팅을 적용한다.
- 로직 변경 없이 포매팅만 변경한다.

## 프롬프트 9
3-1. 미사용 코드 분석 — 프로덕션 코드 전체를 정적 분석하여 미사용 항목을 식별하고 주석-동작 불일치를 파악한다.
분석 결과를 docs/step3-1-미사용-코드-분석.md 보고서로 작성한다.

## 프롬프트 10
3-2. 불필요한 코드 제거 — docs/step3-1-미사용-코드-분석.md 보고서에서 "삭제" 또는 "TODO 변경" 판정을 받은 17개 항목을 카테고리별로 나누어 제거/수정한다. 각 작업 후 빌드 검증.

## 프롬프트 11
4단계 서비스 계층 추출 — Controller 9개에 포함된 비즈니스 로직을 도메인 단위 Service 6개(CategoryService, ProductService, MemberService, OptionService, WishService, OrderService)로 추출한다. 단순한 것부터 복잡한 것 순서로 진행하며, 각 도메인별로 작업 후 사용자 검토를 받는다. 신규 기능 추가 없이 구조만 변경하고, 인수 테스트 17개 시나리오가 변경 전후 모두 통과해야 한다.

## 프롬프트 12 (2단계 미션)
2단계 미션 — 작동 변경을 안전하게 수행하고 증거로 증명한다. 3가지 핵심 문제 해결: (1) 트랜잭션 원자성 검증 테스트 추가, (2) 주문 시 위시 자동 삭제 구현, (3) 도메인 로직 정리. 구조 변경으로 Kakao 알림 트랜잭션 분리, 가격 계산/소유권 확인 엔티티 이동, 이메일 검증 추출까지 포함.

## 프롬프트 13 (추가 리팩토링)
추가 리팩토링 7건 — (1) @ExceptionHandler 중복을 GlobalExceptionHandler(@RestControllerAdvice)로 추출, (2) Member 에러 메시지 한국어 통일, (3) hasKakaoIntegration() 캡슐화, (4) Product.getOptions() 불변 컬렉션 반환, (5) subtractQuantity() 에러 메시지에 컨텍스트 추가, (6) Option.belongsToProduct() 추가로 Law of Demeter 해소, (7) Option.getProductId() 편의 메서드 추가.

## 프롬프트 14 (최종 리팩토링 + 테스트 개선)
프로덕션 코드 7건 + 테스트 인프라 3건 — (A-1) KakaoMessageClient JSON 문자열 → ObjectMapper 안전 직렬화 + calculatePrice() 재사용, (A-2) MemberService 에러 메시지 한국어 통일 4곳, (A-3) ProductRequest.toEntity() 미사용 코드 삭제, (A-4) WishService.addWish() Optional 안티패턴 해소, (A-5) OptionService.delete() 중복 쿼리 통합, (A-6) AuthenticationResolver catch(Exception) → catch(JwtException) 축소, (A-7) 불필요한 JPA save() 호출 6곳 제거, (B-1) RestAssured ApiClient 헬퍼 추출, (B-2) .log().all() → .log().ifValidationFails() 일괄 변경, (B-3) DatabaseCleaner 동적 테이블 목록.

## 프롬프트 15 (var 제거 + 인터페이스 추출)
var 제거 + 인터페이스 추출 — (1) 프로덕션 코드 7곳 + 테스트 코드 ~35곳의 var를 명시적 타입으로 교체, (2) KakaoMessageClient → OrderMessageClient 인터페이스 추출 (OrderCompletedEventListener 의존 타입 변경), (3) KakaoLoginClient → OAuthLoginClient 인터페이스 추출 + OAuthTokenResponse/OAuthUserResponse 프로바이더 중립 record 분리 (KakaoAuthController 의존 타입 변경).

## 프롬프트 16 (미충족 사항 점검 + 문서 정비)
미충족 사항 점검 — CLAUDE.md Architecture 섹션을 현재 3-layer 구조와 인터페이스 추출 결과에 맞게 업데이트, LayerDependencyTest에 역방향 의존 금지 규칙 3건 추가 (Service→Controller, Repository→Service, Repository→Controller), ADR 작성 (외부 API 클라이언트 인터페이스 추출 결정 근거 및 대안 비교).
