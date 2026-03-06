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
내가 작성한 커밋들에 대해서 코드리뷰를 받았는데, 한번 피드백 읽어보고 반영해줘. 추가로 내가 몇개의 댓글을 작성했는데, 해당 어투 및 문장 길이에 맞게 각 코드 리뷰에 대해서 답변을 작성해줘.
```
```
내가 제공해준거 문서로 만들어줘. 🚀 2단계 - 리팩터링 완성하기 (기능 요구 사항, 프로그래밍 요구 사항, 힌트)
```
```
api 명세서 제공해줄테니까 문서 써줘 📖 API 명세
```
```
자 이제 리팩토링을 해야할 부분들을 한번 너가 코드베이스를 읽어보고 작성해줄래 ??
```
```
Implement the following plan: # 4-1. OrderService.createOrder() 트랜잭션 적용 계획
```
```
4-2 ㄱㄱㄱ
```
```
Implement the following plan:

# 5-1. 상품 옵션 수정 API 구현 계획

## Context

API 명세에 `PUT /api/products/{productId}/options/{optionId}` (상품 옵션 수정)이 정의되어 있지만, OptionController에 PUT 엔드포인트가 없고 OptionService에 update() 메서드도 없다. 누락된 작동을 구현한다.
```
```
Implement the following plan:

# 5-2. 테스트 검증 강화 — 예외만 확인하는 테스트를 상태 검증으로 개선

## Context

예외 발생 테스트에서 예외 발생 여부만 확인하고, 예외 발생 후 상태가 변하지 않았는지 검증하지 않는 테스트들이 있다. 예외가 발생했더라도 내부 상태가 변해버렸다면 버그이므로, 상태 불변 검증을 추가한다.
```
```
Implement the following plan:

# 6-1. 디미터 법칙 위반 해소 — DTO 변환에서 엔티티 체인 접근 제거
```
```
Implement the following plan:

# 6-2. 디미터 법칙 위반 해소 — 서비스/클라이언트 레이어 체인 접근 제거
```
```
@.claude/persona/REVIEWER.md 를 참조해서 내 코드 리뷰를 해줘
```
```
치명적인 3개에 대해서 수정하려고 하는데 각각의 문제들에 대해서 대안을 소개해줄래 ?
```
```
내가 pr에 코드리뷰를 받았는데, 총 4개를 받았어. 받은 것들에 대해서 각각으로 대안을 한번 작성해줄래 ?? 판단을 하기 위한 근거가 필요하니 근거도 작성해줘.
```