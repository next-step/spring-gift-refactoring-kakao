# 코드 리뷰 보고서 (2026-03-03)

## 총평
- 최근 리팩터링으로 서비스 계층 분리는 전반적으로 잘 진행됐지만, 주문 처리의 트랜잭션 경계와 인증 보안은 실제 운영에서 데이터 불일치/보안 사고로 이어질 수 있는 상태입니다.
- 또한 REQUIREMENT.md의 "Controller는 검증/위임만" 원칙이 일부(`WishController`)에서 완전히 지켜지지 않았고, 중복 방지 규칙이 DB 제약으로 닫혀 있지 않아 동시성 리스크가 남아 있습니다.

## 발견된 문제점

### 🔴 치명적
1. 주문 생성이 원자적으로 처리되지 않아 재고/포인트 불일치가 발생할 수 있음
- 위치: `src/main/java/gift/service/OrderService.java:34`
- 원인: `subtractQuantity`(재고 저장) -> `deductPoint`(포인트 저장) -> `order save`가 트랜잭션 없이 순차 수행됩니다.
- 부작용: 포인트 차감에서 예외가 나면 주문은 생성되지 않는데 재고는 이미 줄어든 상태가 될 수 있습니다.
- 근거 보강: `createOrderInsufficientPoints` 테스트가 400만 검증하고 재고 원복은 검증하지 않음 (`src/test/java/gift/controller/OrderControllerTest.java:153`).

2. 비밀번호를 평문 저장/비교함
- 위치: `src/main/java/gift/service/AuthService.java:24`, `src/main/java/gift/service/AuthService.java:33`
- 원인: 해시 없이 `new Member(email, password)` 저장 후 문자열 equals로 로그인 검증.
- 부작용: DB 유출 시 계정 직접 탈취 위험, 재사용 비밀번호까지 연쇄 노출.

### 🟡 경고
3. 위시 중복 처리 비즈니스 규칙이 컨트롤러에 남아 있어 요구사항과 불일치
- 위치: `src/main/java/gift/controller/WishController.java:46`
- 원인: 중복 조회/분기(200 vs 201) 로직이 컨트롤러에 있음.
- 부작용: 서비스 추출 단계의 목표(컨트롤러는 검증+위임)와 어긋나고, API 규칙 변경 시 컨트롤러 수정 범위가 커짐.

4. 중복 방지 규칙이 애플리케이션 레벨에만 있고 DB 제약이 없음
- 위치: `src/main/resources/db/migration/V1__Initialize_project_tables.sql:29`, `src/main/resources/db/migration/V1__Initialize_project_tables.sql:38`
- 원인: `wish(member_id, product_id)`, `options(product_id, name)` 유니크 제약 부재.
- 부작용: 동시 요청 시 중복 wish/option 생성 가능(사전 exists 체크만으로는 경쟁 상태를 막지 못함).

5. Kakao OAuth 콜백에서 이메일 누락 케이스가 명시적으로 처리되지 않음
- 위치: `src/main/java/gift/service/KakaoAuthService.java:50`
- 원인: `kakaoUser.email()` null 가능성(동의 미제공/응답 스키마 변화)에 대한 방어 로직 없음.
- 부작용: 의도 불명확한 500 또는 잘못된 사용자 처리로 이어질 수 있음.

### 🟢 개선 권장
6. 알림 실패를 완전히 무시해서 운영 관찰성이 낮음
- 위치: `src/main/java/gift/service/KakaoNotificationService.java:24`
- 원인: `catch (Exception ignored) {}`
- 부작용: 실패 원인을 추적할 수 없어 장애 분석이 어려움.

7. `ResponseEntity<?>` 사용으로 반환 타입 의도가 흐림
- 위치: `src/main/java/gift/controller/OrderController.java:29`, `src/main/java/gift/controller/OrderController.java:38`
- 원인: 와일드카드 반환 타입 사용.
- 부작용: API 계약/코드 가독성 저하.

## 개선된 코드

### 1) 주문 생성 트랜잭션 보장 + 알림 실패 로깅
```java
@Service
public class OrderService {
    // ... 생성자/필드 생략

    @Transactional // 재고/포인트/주문을 하나의 트랜잭션으로 묶음
    public Order createOrder(Member member, Long optionId, int quantity, String message) {
        Option option = optionService.subtractQuantity(optionId, quantity);

        int price = Math.multiplyExact(option.getProduct().getPrice(), quantity); // overflow 방어
        memberService.deductPoint(member, price);

        Order saved = orderRepository.save(new Order(option, member.getId(), quantity, message));

        try {
            kakaoNotificationService.sendOrderNotification(member, saved, option); // 외부 연동은 실패해도 주문은 유지
        } catch (Exception e) {
            // TODO: logger.warn("Failed to send Kakao notification", e);
        }
        return saved;
    }
}
```

### 2) 위시 중복 규칙을 서비스로 이동 (컨트롤러는 위임만)
```java
public record WishUpsertResult(Wish wish, boolean created) {}

@Service
public class WishService {
    // ... 생성자/필드 생략

    public WishUpsertResult addOrGetWish(Long memberId, Long productId) {
        return wishRepository.findByMemberIdAndProductId(memberId, productId)
            .map(existing -> new WishUpsertResult(existing, false))
            .orElseGet(() -> {
                Product product = productService.findById(productId);
                Wish saved = wishRepository.save(new Wish(memberId, product));
                return new WishUpsertResult(saved, true);
            });
    }
}

@RestController
@RequestMapping("/api/wishes")
public class WishController {
    // ... 생성자/필드 생략

    @PostMapping
    public ResponseEntity<WishResponse> addWish(@LoginMember Member member, @Valid @RequestBody WishRequest request) {
        WishUpsertResult result = wishService.addOrGetWish(member.getId(), request.productId());
        if (!result.created()) {
            return ResponseEntity.ok(WishResponse.from(result.wish()));
        }
        return ResponseEntity.created(URI.create("/api/wishes/" + result.wish().getId()))
            .body(WishResponse.from(result.wish()));
    }
}
```

### 3) 비밀번호 해시 적용
```java
@Service
public class AuthService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(MemberRepository memberRepository, JwtProvider jwtProvider, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public TokenResponse register(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        Member member = memberRepository.save(new Member(email, passwordEncoder.encode(password))); // 평문 저장 금지
        return new TokenResponse(jwtProvider.createToken(member.getEmail()));
    }

    public TokenResponse login(String email, String password) {
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (member.getPassword() == null || !passwordEncoder.matches(password, member.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return new TokenResponse(jwtProvider.createToken(member.getEmail()));
    }
}
```

### 4) DB 무결성 제약 추가
```sql
alter table wish
    add constraint uq_wish_member_product unique (member_id, product_id);

alter table options
    add constraint uq_options_product_name unique (product_id, name);
```

## 추가 조언
- 주문 플로우는 `재고/포인트/주문` 원자성 테스트를 반드시 추가하세요. 특히 "포인트 부족 시 재고가 유지되는지"를 검증해야 합니다.
- 외부 연동(Kakao)은 Outbox 패턴 또는 비동기 이벤트(`@TransactionalEventListener`)로 분리하면 장애 전파를 줄일 수 있습니다.
- 인증 영역은 최소 `PasswordEncoder(BCrypt)` + 민감정보 마스킹 로그 정책까지 함께 적용하세요.
- 테스트 실행 환경도 정리 필요: 현재 로컬에서 `./gradlew test`가 JDK 버전 파싱 이슈(`25.0.2`)로 실패해 회귀 검증 자동화가 막혀 있습니다.
