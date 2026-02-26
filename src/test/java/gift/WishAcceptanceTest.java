package gift;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import gift.category.CategoryRepository;
import gift.member.MemberRepository;
import gift.option.OptionRepository;
import gift.order.OrderRepository;
import gift.product.ProductRepository;
import gift.wish.WishRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WishAcceptanceTest {

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

    Long createProduct(Long categoryId) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "테스트상품", "price", 10000, "imageUrl", "https://example.com/p.jpg", "categoryId",
                categoryId))
            .when()
            .post("/api/products")
            .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    Long addWish(String token, Long productId) {
        return given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("productId", productId))
            .when()
            .post("/api/wishes")
            .then()
            .extract().jsonPath().getLong("id");
    }

    // ── 위시 조회 ──

    @Test
    void 위시_목록_조회_성공() {
        // given
        String token = registerAndGetToken("user@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        addWish(token, productId);

        // when
        var response = given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/wishes");

        // then
        response.then()
            .statusCode(200)
            .body("content", hasSize(1))
            .body("content[0].productId", equalTo(productId.intValue()));
    }

    @Test
    void 위시_목록_조회_빈_목록() {
        // given
        String token = registerAndGetToken("user@example.com");

        // when
        var response = given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/wishes");

        // then
        response.then()
            .statusCode(200)
            .body("content", hasSize(0));
    }

    @Test
    void 위시_목록_조회_다른_회원_위시_안보임() {
        // given
        String tokenA = registerAndGetToken("userA@example.com");
        String tokenB = registerAndGetToken("userB@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        addWish(tokenA, productId);

        // when — userB로 조회
        var response = given()
            .header("Authorization", "Bearer " + tokenB)
            .when()
            .get("/api/wishes");

        // then
        response.then()
            .statusCode(200)
            .body("content", hasSize(0));
    }

    @Test
    void 위시_목록_조회_실패_인증_없음() {
        // given — Authorization 헤더 없음

        // when
        var response = given()
            .when()
            .get("/api/wishes");

        // then
        response.then()
            .statusCode(400);
    }

    // ── 위시 추가 ──

    @Test
    void 위시_추가_성공() {
        // given
        String token = registerAndGetToken("user@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("productId", productId))
            .when()
            .post("/api/wishes");

        // then
        response.then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("productId", equalTo(productId.intValue()))
            .body("name", equalTo("테스트상품"))
            .body("price", equalTo(10000));
    }

    @Test
    void 위시_추가_중복_시_기존_반환() {
        // given
        String token = registerAndGetToken("user@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        addWish(token, productId);

        // when — 같은 상품 다시 추가
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("productId", productId))
            .when()
            .post("/api/wishes");

        // then — 200 (기존 반환)
        response.then()
            .statusCode(200)
            .body("productId", equalTo(productId.intValue()));
    }

    @Test
    void 위시_추가_실패_존재하지_않는_상품() {
        // given
        String token = registerAndGetToken("user@example.com");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("productId", 999999))
            .when()
            .post("/api/wishes");

        // then
        response.then()
            .statusCode(500);
    }

    @Test
    void 위시_추가_실패_인증_없음() {
        // given
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(Map.of("productId", productId))
            .when()
            .post("/api/wishes");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 위시_추가_실패_잘못된_토큰() {
        // given
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer invalid-token")
            .body(Map.of("productId", productId))
            .when()
            .post("/api/wishes");

        // then
        response.then()
            .statusCode(401);
    }

    // ── 위시 삭제 ──

    @Test
    void 위시_삭제_성공() {
        // given
        String token = registerAndGetToken("user@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        Long wishId = addWish(token, productId);

        // when
        var response = given()
            .header("Authorization", "Bearer " + token)
            .when()
            .delete("/api/wishes/" + wishId);

        // then
        response.then()
            .statusCode(204);

        // 삭제 확인
        given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/wishes")
            .then()
            .statusCode(200)
            .body("content", hasSize(0));
    }

    @Test
    void 위시_삭제_실패_존재하지_않는_위시() {
        // given
        String token = registerAndGetToken("user@example.com");

        // when
        var response = given()
            .header("Authorization", "Bearer " + token)
            .when()
            .delete("/api/wishes/999999");

        // then
        response.then()
            .statusCode(500);
    }

    @Test
    void 위시_삭제_실패_다른_회원의_위시() {
        // given
        String tokenA = registerAndGetToken("userA@example.com");
        String tokenB = registerAndGetToken("userB@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        Long wishId = addWish(tokenA, productId);

        // when — userB가 userA의 위시 삭제 시도
        var response = given()
            .header("Authorization", "Bearer " + tokenB)
            .when()
            .delete("/api/wishes/" + wishId);

        // then
        response.then()
            .statusCode(500);
    }

    @Test
    void 위시_삭제_실패_인증_없음() {
        // given
        String token = registerAndGetToken("user@example.com");
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        Long wishId = addWish(token, productId);

        // when
        var response = given()
            .when()
            .delete("/api/wishes/" + wishId);

        // then
        response.then()
            .statusCode(400);
    }
}
