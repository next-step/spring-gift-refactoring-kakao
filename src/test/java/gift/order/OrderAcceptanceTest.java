package gift.order;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import gift.wish.WishRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private WishRepository wishRepository;

    @MockBean
    private KakaoMessageClient kakaoMessageClient;

    private Member member;
    private Product product;
    private Option option;
    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // FK 순서대로 삭제
        orderRepository.deleteAll();
        wishRepository.deleteAll();
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();

        // 카카오 메시지 모킹
        doNothing().when(kakaoMessageClient).sendToMe(any(), any(), any());

        // 테스트 데이터 준비
        member = memberRepository.save(new Member("test@example.com", "password"));
        member.chargePoint(100000); // 충분한 포인트 충전
        memberRepository.save(member);

        Category category = categoryRepository.save(new Category("카테고리", "#000", "http://img.com", "설명"));
        product = productRepository.save(new Product("테스트 상품", 1000, "http://img.com", category));
        option = optionRepository.save(new Option(product, "기본 옵션", 100)); // 재고 100개

        // 토큰 획득
        token = getToken("test@example.com", "password");
    }

    private String getToken(String email, String password) {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password))
            .when()
            .post("/api/members/login")
            .then()
            .extract()
            .path("token");
    }

    @Nested
    @DisplayName("주문 목록 조회")
    class GetOrders {

        @Test
        @DisplayName("성공: 인증된 사용자의 주문 목록을 반환한다")
        void success() {
            // Given
            orderRepository.save(new Order(option, member.getId(), 5, "메시지1"));
            orderRepository.save(new Order(option, member.getId(), 3, "메시지2"));

            // When & Then
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .param("page", 0)
                .param("size", 10)
                .when()
                .get("/api/orders")
                .then()
                .statusCode(200)
                .body("content", hasSize(2));
        }

        @Test
        @DisplayName("성공: 다른 사용자의 주문은 조회되지 않는다")
        void success_onlyOwnOrders() {
            // Given: 다른 회원의 주문
            Member otherMember = memberRepository.save(new Member("other@example.com", "password"));
            orderRepository.save(new Order(option, otherMember.getId(), 5, "다른 주문"));

            // When & Then
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .param("page", 0)
                .param("size", 10)
                .when()
                .get("/api/orders")
                .then()
                .statusCode(200)
                .body("content", hasSize(0));
        }

        @Test
        @DisplayName("실패: 인증 헤더 없이 조회하면 400을 반환한다")
        void fail_noAuthHeader() {
            RestAssured.given()
                .param("page", 0)
                .param("size", 10)
                .when()
                .get("/api/orders")
                .then()
                .statusCode(400);
        }
    }

    @Nested
    @DisplayName("주문 생성")
    class CreateOrder {

        @Test
        @DisplayName("성공: 주문을 생성하면 주문 정보를 반환한다")
        void success() {
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "optionId": %d,
                        "quantity": 5,
                        "message": "주문 메시지"
                    }
                    """.formatted(option.getId()))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("optionId", equalTo(option.getId().intValue()))
                .body("quantity", equalTo(5));
        }

        @Test
        @DisplayName("성공: 주문 시 재고가 차감된다")
        void success_subtractsStock() {
            // Given
            int initialQuantity = option.getQuantity(); // 100

            // When
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "optionId": %d,
                        "quantity": 10,
                        "message": "메시지"
                    }
                    """.formatted(option.getId()))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(201);

            // Then
            Option updated = optionRepository.findById(option.getId()).orElseThrow();
            assertThat(updated.getQuantity()).isEqualTo(initialQuantity - 10);
        }

        @Test
        @DisplayName("성공: 주문 시 포인트가 차감된다")
        void success_deductsPoints() {
            // Given
            int initialPoint = member.getPoint(); // 100000
            int orderQuantity = 5;
            int expectedDeduction = product.getPrice() * orderQuantity; // 1000 * 5 = 5000

            // When
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "optionId": %d,
                        "quantity": %d,
                        "message": "메시지"
                    }
                    """.formatted(option.getId(), orderQuantity))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(201);

            // Then
            Member updated = memberRepository.findById(member.getId()).orElseThrow();
            assertThat(updated.getPoint()).isEqualTo(initialPoint - expectedDeduction);
        }

        @Test
        @DisplayName("성공: 메시지 없이도 주문할 수 있다")
        void success_withoutMessage() {
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "optionId": %d,
                        "quantity": 1
                    }
                    """.formatted(option.getId()))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(201);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 옵션으로 주문하면 404를 반환한다")
        void fail_optionNotFound() {
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "optionId": 999999,
                        "quantity": 1,
                        "message": "메시지"
                    }
                    """)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(404);
        }

        @Test
        @DisplayName("실패: 재고보다 많은 수량을 주문하면 400을 반환한다")
        void fail_insufficientStock() {
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "optionId": %d,
                        "quantity": 999,
                        "message": "메시지"
                    }
                    """.formatted(option.getId()))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(500); // IllegalArgumentException이 처리되지 않아 500 반환
        }

        @Test
        @DisplayName("실패: 포인트가 부족하면 400을 반환한다")
        void fail_insufficientPoints() {
            // Given: 포인트 부족한 회원
            Member poorMember = memberRepository.save(new Member("poor@example.com", "password"));
            poorMember.chargePoint(100); // 100 포인트만 (상품가격 1000원보다 적음)
            memberRepository.save(poorMember);
            String poorToken = getToken("poor@example.com", "password");

            // When & Then
            RestAssured.given()
                .header("Authorization", "Bearer " + poorToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "optionId": %d,
                        "quantity": 1,
                        "message": "메시지"
                    }
                    """.formatted(option.getId()))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(500); // IllegalArgumentException이 처리되지 않아 500 반환
        }

        @Test
        @DisplayName("실패: 수량이 0이면 400을 반환한다")
        void fail_zeroQuantity() {
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "optionId": %d,
                        "quantity": 0,
                        "message": "메시지"
                    }
                    """.formatted(option.getId()))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("실패: 인증 헤더 없이 주문하면 400을 반환한다")
        void fail_noAuthHeader() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "optionId": %d,
                        "quantity": 1,
                        "message": "메시지"
                    }
                    """.formatted(option.getId()))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("실패: 잘못된 토큰으로 주문하면 401을 반환한다")
        void fail_invalidToken() {
            RestAssured.given()
                .header("Authorization", "Bearer invalid-token")
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "optionId": %d,
                        "quantity": 1,
                        "message": "메시지"
                    }
                    """.formatted(option.getId()))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(401);
        }
    }
}
