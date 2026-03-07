# 프롬프트
```
클로드.md 파일에 우리가 치는 프롬프트들 PROMPT.md에 정리해 주는 요구사항 넣어서 만들어줘
```
```
그러면 우리가 readme md 파일에 1. 스타일정리와 관련된 부분들은 전부 선정했어. todo.md 파일도 보고 스타일정리를해줄 수 있을까 ??
```
```
/analyze-before-remove [미사용 @Autowired 제거]
```
```
Implement the following plan: # 1단계: 스타일 정리 구현 계획 (에러 메시지 영어 통일, 예외 처리 패턴 통일, Null 처리 패턴 통일, import 정리 및 코드 포맷팅)
```
```
큰 단위의 작업을 진행했을때 md 파일로 문서화 시켜주는 내용을 Claude.md 에 추가해줘
```
```
너가 방금 스타일 정리 작업을 진행했잖아 ?? 뭔 작업을 진행했는지 한번 정리해서 문서로 만들어줄 수 있을까 ??
```
```
/smart-commit
```
```
너가 그룹화를 진행한게 @REQUIREMENT.md 에 있는 프로그래밍 요구사항의 커밋과 관련된 부분을 따른건지 한번 더 확인해줄 수 있을까 ?
```
```
해당 개선안을 커밋 메세지를 한글로 바꿔서 진행해줘
```
```
테스트 돌려봐
```
```
md 문서들이 지금 너무 root에 많아서 docs 폴더 안에 정리해줄 수 있을까? 폴더 만들어도 상관없어. 스킬로도 해당 내용 만들어놔 ~ 나중에 쓰게!
```
```
/analyze-before-remove Validator 중복 코드 정리
```
```
Implement the following plan: # 2-1. 미사용 @Autowired 제거 실행 계획
```
```
claude.md에 있는 작업 문서화 규칙에 따르면 너가 방금 한 일을 문서화 했어야 했는데 왜 안했어 ?
```
```
@README.md 와 @docs/plan/TODO.md , @docs/plan/REQUIREMENT.md 이 3개의 파일을 보고 2-2의 validator 중복코드 정리 문제를 우리가 선정한 방식으로 리팩터링을 진행해줄 수 있을까 ?? 다 진행하면 잊지말고 문서화 부탁해 ~
```
```
/analyze-before-remove 미사용 kotlin 의존성 정리해줘.
```
```
/analyze-before-remove MemberController 구현 의도
```
```
@docs/removal-analysis-unused-kotlin-dependencies.md 랑 @README.md 보고 2-3에 우리가 어떤 방식으로 리팩토링할지 선정했어. 이걸 기반으로 코드를 리팩토링해줄 수 있을까 ?
```
```
@README.md 랑 @docs/removal-analysis-member-controller.md 파일 보고 3-1에서 우리가 선정한 방식으로 membercontroller 리팩토링 진행해줘
```
```
skills 중에 analyze-before-remove가 있는데 지금 너무 삭제에 취중되어있는거 같애. 삭제가 명칭을 리팩토링으로 바꿔줬으면 좋겠어.
```
```
/analyze-before-refactoring CategoryService 추출
```
```
/analyze-before-refactoring productservice 추출
```
```
@README.md, @docs/refactoring-analysis-product-service-extraction.md , @docs/refactoring-analysis-category-service-extraction.md 이 3개의 md 파일을 보고, readme.md 파일의 3-2, 3-3 부분을 같이 진행해줄 수 있을까 ? 이렇게 부탁하는 이유는 2개의 도메인이 꽤 많이 엮여있는 것 같아서 그래. 3개의 파일을 참고해서 진행해줘. 추가로 @docs/plan/TODO.md , @docs/plan/REQUIREMENT.md 를 참고해도 좋아
```
```
/analyze-before-refactoring OptionService
```
```
/analyze-before-refactoring OrderService 추출
```
```
/analyze-before-refactoring WishService 추출
```
```
@README.md 와 @docs/refactoring-analysis-wish-service-extraction.md 파일을 보고 WISH_SERVICE 먼저 리팩토링해줄 수 있을까 ?
```
```
Implement the following plan:

# OptionService (3-4) + OrderService (3-6) 추출 계획

## Context

README.md 3-4(OptionService)와 3-6(OrderService + KakaoNotificationService)를 함께 진행한다.
OrderService가 재고 차감을 위해 OptionService에 의존하므로, OptionService를 먼저 만들고 OrderService가 이를 사용하는 구조로 설계한다.

의존 체인:
OptionController → OptionService → OptionRepository, ProductService
OrderController  → OrderService  → OptionService, MemberRepository, OrderRepository, KakaoNotificationService → KakaoMessageClient
```
```
/analyze-before-refactoring KakaoAuthService 추출
```
```
@.claude/persona/REVIEWER.md git commit 로그 및 전체 코드를 분석해서 수정된 작업이 목적에 맞게 잘 수정했는지 리뷰해줘
```
```
지금 보고서들이 docs 폴더 안에도 너무 복잡하게 존재하는데, 분석 보고서와 작업 보고서를 나눠서 정리해줄 수 있을까 ?? 보고서들이 너무 많아서 복잡해. 추가적으로 이름도 마음에 안들어서, 통일된 규칙을 가지도록 이름도 바꿔줘
```
```
@README.md @docs/analysis/kakao-auth-service.md 이 2개의 파일을 보고 readme의 3-7 부분을 리팩토링 해줄 수 있을까 ? 보고서 작성도 부탁할게.
```
```
ok 이제 리팩토링이 다 끝났는데, 기존의 테스트들이 전부 통과해야될거 아냐 ?? 그러면 테스트를 한번 실행시켜볼래 ? 너가 일을 잘 했는지 검증할 수 있는 순간이야.
```
```
우리 프로젝트의 코드가 지금 도메인별로 패키지가 구성되어 있는데, 우리가 생각했을 때에는 도메인별로 구분한다기 보다, serice별로, controller별로 등 역할별로 패키지를 만드는 것이 더 낫다고 생각해. 그래서 해당 구조로 디렉토리 구조를 바꿔줄 수 있을까 ?? 변경하고 변경 내역을 report에 md로 남겨줘
```
```
ok 고생했어. 추가적으로 @docs/reports/code-review-service-extraction.md 이거를 보니까 2번은 지금 수정할만 것 같은데, 2번을 수정해줄 수 있을까? 추가적으로 해당 md 파일도 바뀐 프로젝트 구조에 맞게 파일의 이름, 구조 등을 변경해줘
```
```
지금 docs들을 보면 예전에 만든 것들이라서 경로가 예전방식으로 되어있을 것으로 예상되고, 실제로 그래. 그러면 프로젝트의 모든 md 파일들을 보고 바뀐 프로젝트 구조에 맞게 경로를 수정해줄 수 있을까 ?
```
```
/smart-commit - 모든 보고서 파일에서 경로를 맞게 확인하고 있는지 한번 봐줘
```
```
현재 해당 코드에 비밀번호 평문 저장/비교 중인거 같애. 수정안 제시해줘
```
```
지금은 service 단에서 encode하고 넘겨주잖아. 근데 Member 클래스 안에서 받아서 password를 인코딩 하는 방법도 있을거 같아. 이런 방법들을 결정하는 기준이 무엇일까?
```
```
도메인 모델은 언제나 다른것을 의존하고 있으면 안되는거야?
```
```
그리고 현재 member라는 객체에서 password를 꺼내서 직접 비교하고 있는데, Password같은 일급 객체를 만들어서 책임을 위임해보는것도 좋을 것 같다는 생각이 드네요. -> 이런 피드백을 봤는데, 일급 객체로 썼을 경우 좋은점이 뭐야 ?
```
```
그러면 객체가 의존하는 형태가 Member -> Password -> PasswordEncoder 아니야?
```
```
Password 일급 객체 도입 트레이드오프 문서 작성 — BCrypt 해싱 적용 이후 피드백에 대해 3가지 방식(String 현재, Password+PasswordEncoder, Password 값 래퍼) 비교 분석 문서를 작성해줘
```
```
@docs/plan/REQUIREMENT_STEP2.md 해당 설명을 보고 이번 미션에서 해야할 작업 및 실행계획 문서를 만들어줘. 이번미션은 작동 변경이 목표야.
```
```
TODO_STEP2에 있는 작업들을 각각의 에이전트가 담당하여 실행시키는 에이전트 팀을 생성하려고 해. 프롬프트 생성해줘. 각 팀원들은 실행 계획 및 계획 보고서 생성 -> 나의 검토 -> 동작시작 -> 작업 보고서 생성 이런식으로 진행될거야.
```
```
너가 방금 작성한거 md 파일로 만들어줘
```
```
@docs/plan/AGENT_TEAM_PROMPTS.md 에이전트 팀 만들어서 해당 문서대로 작업 진행해줘
```
```
[Agent Task] 트랜잭션 경계 세우기 - OrderService.createOrder()에 @Transactional 추가하여 주문 원자성 보장
```
```
[Agent Task 3] 카테고리별 상품 필터링 - Phase 1 계획 보고서 작성
```
```
[작업 4: 주문 금액 계산 도메인 이동] 가격 계산(price * quantity)을 서비스에서 Option 도메인 객체로 이동한다.
```
```
[Agent Task 5] validateName 중복 제거 - ProductService와 OptionService에 중복된 validateName 메서드를 NameValidator로 통합
```
```
[Agent Task] 트랜잭션 경계 세우기 - OrderService.createOrder()에 @Transactional 추가하여 주문 원자성 보장
```
```
[Agent Task 5 - Phase 1] validateName 중복 제거 실행 계획 보고서 작성
```
```
@docs/plan/TODO_STEP2.md 해당 문서를 보고 안한거나 부족한 부분 보완해줘
```
```
/organize-docs 이름도 명확하지 않으면 파일명 수정해줘도돼
```
```
step2에서 마무리한 작업들을 정리해서 README에 작성해줘
```
```
이번에는 작업하면서 claude agents team 이라는 기능도 써봤잖아. 이에 대한 내용도 간단히 추가하는거 어때?
```