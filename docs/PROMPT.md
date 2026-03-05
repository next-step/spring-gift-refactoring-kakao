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

## 프롬프트 12
DatabaseCleaner 리뷰 반영 — 테이블명 하드코딩 방식을 information_schema 동적 조회 방식으로 변경. 테이블 추가/삭제 시 자동 대응되도록 개선. SKILL 문서도 동기화.

## 프롬프트 13
KakaoAuthService 추출 — KakaoAuthController에 남아있는 비즈니스 로직(OAuth 흐름 오케스트레이션, 인가 URL 조립)을 KakaoAuthService로 추출. 컨트롤러는 HTTP 관심사만 담당하도록 변경.

## 프롬프트 14
구조 변경 - 의존성 정리(외부 인프라 의존, 순환 참조, 크로스 패키지 의존)와 도메인 책임(객체지향 설계, 중복 제거, 호출부 단순화) 분석.

## 프롬프트 15
예외 처리 중앙화 — 5개 REST 컨트롤러에 분산된 @ExceptionHandler 10개를 @RestControllerAdvice(annotations = RestController.class) 1개로 통합. @Controller(Admin 뷰 컨트롤러)는 적용 범위에서 제외.

## 프롬프트 16
인수 테스트 커버리지 검토: GlobalExceptionHandler — NoSuchElementException→404 매핑의 단언문 정밀도 개선. "상품 등록이 실패한다" 스텝을 `≥400`에서 `=404`로 변경. "존재하지 않는 옵션으로 주문" 시나리오에 `=404` 전용 스텝 추가.

## 프롬프트 17
인증 처리 추출 — HandlerMethodArgumentResolver 도입. OrderController(2회)와 WishController(3회)에서 반복되는 인증 보일러플레이트를 @AuthenticatedMember 커스텀 어노테이션 + AuthenticationResolver(HandlerMethodArgumentResolver 구현)로 추출. 컨트롤러가 Member를 파라미터로 직접 받도록 변경.

## 프롬프트 18
5-2 보정: 작동 변경 방지 및 인수 테스트 검증 — AuthenticationResolver에서 Authorization 헤더 누락 시 MissingRequestHeaderException을 throw하여 기존 400 응답 유지. 인수 테스트 "주문이 실패한다" 단언을 ≥400에서 =400으로 정밀화.

## 프롬프트 19
크로스 패키지 Repository 참조 제거 — 4개 서비스(OrderService, WishService, OptionService)에서 타 패키지 Repository 직접 참조를 해당 도메인 Service 위임으로 대체. OptionRepository→OptionService, MemberRepository→MemberService, ProductRepository→ProductService. JPA dirty checking 활용으로 명시적 save() 제거.

## 프롬프트 20
작업 3 검증: 작동 변경 분석 및 인수 테스트 검토 — 크로스 패키지 Repository 참조 제거 리팩터링의 작동 변경 여부 분석(예외 메시지 차이, save() 제거, readOnly 전파). OrderService에 dirty checking 전환 사유 및 예외 메시지 차이 주석 추가. 인수 테스트 17개 시나리오 커버리지 충분성 확인.

## 프롬프트 21
save() 위임 복원 — 프롬프트 19에서 save() 제거(dirty checking 전환)는 요청 범위("Service 위임") 밖의 부수적 작업이므로 복원. OptionService.save(), MemberService.save() 위임 메서드 추가. OrderService에서 optionService.save(option), memberService.save(member) 호출 복원. dirty checking 주석 제거.

## 프롬프트 22
외부 인프라 인터페이스 추출 — OrderMessageClient 인터페이스를 생성하여 OrderService의 KakaoMessageClient 직접 의존을 인터페이스 의존으로 역전. KakaoMessageClient가 OrderMessageClient를 구현하도록 변경. OrderService에서 벤더명(Kakao) 제거.

## 프롬프트 23
auth ↔ member 순환 참조 해소 — member 패키지에 TokenProvider 인터페이스를 생성하고 auth.JwtProvider가 이를 구현하도록 변경. MemberService의 의존을 auth.JwtProvider → member.TokenProvider로 역전. TokenResponse를 auth → member 패키지로 이동하여 MemberController → TokenResponse 순환도 해소. member 패키지에서 auth 패키지 import 0건 달성.

## 프롬프트 24
도메인 책임 이동 — Member 엔티티에 matchesPassword(String) 메서드를 추가하고, MemberService.login()에서 getPassword()로 내부 상태를 꺼내 직접 비교하던 로직을 matchesPassword() 호출로 대체. 비밀번호 검증 책임을 도메인 객체로 이동.
