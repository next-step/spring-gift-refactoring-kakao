package gift;

import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.order.OrderRepository;
import gift.product.ProductRepository;
import gift.wish.WishRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    WishRepository wishRepository;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        orderRepository.deleteAll();
        wishRepository.deleteAll();
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    // ── 헬퍼 ──

    String registerAndGetToken(String email) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", "password123"))
            .when()
            .post("/api/members/register")
            .then()
            .statusCode(201)
            .extract().jsonPath().getString("token");
    }

    /** 회원에게 포인트 충전 (API 없으므로 Repository 직접 사용) */
    void chargePoint(String email, int amount) {
        Member member = memberRepository.findByEmail(email).orElseThrow();
        member.chargePoint(amount);
        memberRepository.save(member);
    }

    Long createCategory() {
        return given()
            .contentType(ContentType.JSON)
            .body(
                Map.of("name", "테스트", "color", "#000000", "imageUrl", "https://example.com/cat.jpg", "description", ""))
            .when()
            .post("/api/categories")
            .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    Long createProduct(Long categoryId, int price) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "테스트상품", "price", price, "imageUrl", "https://example.com/p.jpg", "categoryId",
                categoryId))
            .when()
            .post("/api/products")
            .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    /** 옵션 생성 (API 사용) */
    Long createOption(Long productId, String name, int quantity) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name, "quantity", quantity))
            .when()
            .post("/api/products/" + productId + "/options")
            .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    /** 위시 추가 (API 사용) */
    void addWish(String token, Long productId) {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("productId", productId))
            .when()
            .post("/api/wishes");
    }

    // ── 주문 조회 ──

    @Test
    void 주문_목록_조회_성공() {
        // given
        String token = registerAndGetToken("user@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId, 1000);
        Long optionId = createOption(productId, "기본", 100);
        chargePoint("user@example.com", 5000);

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("optionId", optionId, "quantity", 2, "message", "테스트"))
            .when()
            .post("/api/orders")
            .then()
            .statusCode(201);

        // when
        var response = given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/orders");

        // then
        response.then()
            .statusCode(200)
            .body("content", hasSize(1))
            .body("content[0].optionId", equalTo(optionId.intValue()));
    }

    @Test
    void 주문_목록_조회_빈_목록() {
        // given
        String token = registerAndGetToken("user@example.com");

        // when
        var response = given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/orders");

        // then
        response.then()
            .statusCode(200)
            .body("content", hasSize(0));
    }

    @Test
    void 주문_목록_조회_다른_회원_주문_안보임() {
        // given
        String tokenA = registerAndGetToken("userA@example.com");
        String tokenB = registerAndGetToken("userB@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId, 1000);
        Long optionId = createOption(productId, "기본", 100);
        chargePoint("userA@example.com", 5000);

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + tokenA)
            .body(Map.of("optionId", optionId, "quantity", 1, "message", ""))
            .when()
            .post("/api/orders")
            .then()
            .statusCode(201);

        // when — userB로 조회
        var response = given()
            .header("Authorization", "Bearer " + tokenB)
            .when()
            .get("/api/orders");

        // then
        response.then()
            .statusCode(200)
            .body("content", hasSize(0));
    }

    @Test
    void 주문_목록_조회_실패_인증_없음() {
        // when
        var response = given()
            .when()
            .get("/api/orders");

        // then
        response.then()
            .statusCode(400);
    }

    // ── 주문 생성 ──

    @Test
    void 주문_생성_성공() {
        // given
        String token = registerAndGetToken("user@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId, 1000);
        Long optionId = createOption(productId, "기본", 100);
        chargePoint("user@example.com", 10000);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("optionId", optionId, "quantity", 3, "message", "선물입니다"))
            .when()
            .post("/api/orders");

        // then
        response.then()
            .statusCode(201)
            .header("Location", containsString("/api/orders/"))
            .body("id", notNullValue())
            .body("optionId", equalTo(optionId.intValue()))
            .body("quantity", equalTo(3))
            .body("message", equalTo("선물입니다"))
            .body("orderDateTime", notNullValue());
    }

    @Test
    void 주문_생성_성공_재고_차감_확인() {
        // given
        String token = registerAndGetToken("user@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId, 1000);
        Long optionId = createOption(productId, "기본", 100);
        chargePoint("user@example.com", 50000);

        // when
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("optionId", optionId, "quantity", 5, "message", ""))
            .when()
            .post("/api/orders")
            .then()
            .statusCode(201);

        // then — 재고 확인 (Layer 3)
        Option option = optionRepository.findById(optionId).orElseThrow();
        assertThat(option.getQuantity()).isEqualTo(95);
    }

    @Test
    void 주문_생성_성공_포인트_차감_확인() {
        // given
        String token = registerAndGetToken("user@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId, 1000);
        Long optionId = createOption(productId, "기본", 100);
        chargePoint("user@example.com", 10000);

        // when
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("optionId", optionId, "quantity", 3, "message", ""))
            .when()
            .post("/api/orders")
            .then()
            .statusCode(201);

        // then — 포인트 확인 (Layer 3): 10000 - (1000 * 3) = 7000
        Member member = memberRepository.findByEmail("user@example.com").orElseThrow();
        assertThat(member.getPoint()).isEqualTo(7000);
    }

    @Test
    void 주문_생성_실패_존재하지_않는_옵션() {
        // given
        String token = registerAndGetToken("user@example.com");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("optionId", 999999, "quantity", 1, "message", ""))
            .when()
            .post("/api/orders");

        // then
        response.then()
            .statusCode(500);
    }

    @Test
    void 주문_생성_실패_재고_부족() {
        // given
        String token = registerAndGetToken("user@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId, 100);
        Long optionId = createOption(productId, "기본", 5);
        chargePoint("user@example.com", 100000);

        // when — 재고 5개인데 10개 주문
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("optionId", optionId, "quantity", 10, "message", ""))
            .when()
            .post("/api/orders");

        // then
        response.then()
            .statusCode(500);
    }

    @Test
    void 주문_생성_실패_포인트_부족() {
        // given
        String token = registerAndGetToken("user@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId, 10000);
        Long optionId = createOption(productId, "기본", 100);
        chargePoint("user@example.com", 5000);

        // when — 포인트 5000인데 10000짜리 1개 주문
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("optionId", optionId, "quantity", 1, "message", ""))
            .when()
            .post("/api/orders");

        // then
        response.then()
            .statusCode(500);
    }

    @Test
    void 주문_생성_실패_인증_없음() {
        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(Map.of("optionId", 1, "quantity", 1, "message", ""))
            .when()
            .post("/api/orders");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 주문_생성_실패_잘못된_토큰() {
        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer invalid-token")
            .body(Map.of("optionId", 1, "quantity", 1, "message", ""))
            .when()
            .post("/api/orders");

        // then
        response.then()
            .statusCode(401);
    }

    @Test
    void 주문_생성_실패_수량_0이하() {
        // given
        String token = registerAndGetToken("user@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId, 1000);
        Long optionId = createOption(productId, "기본", 100);
        chargePoint("user@example.com", 10000);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("optionId", optionId, "quantity", 0, "message", ""))
            .when()
            .post("/api/orders");

        // then
        response.then()
            .statusCode(400);
    }
}
