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
class OptionAcceptanceTest {

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

    // ── 옵션 조회 ──

    @Test
    void 옵션_목록_조회_성공() {
        // given
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        createOption(productId, "기본", 100);
        createOption(productId, "대용량", 50);

        // when
        var response = given()
            .when()
            .get("/api/products/" + productId + "/options");

        // then
        response.then()
            .statusCode(200)
            .body("$", hasSize(2))
            .body("[0].id", notNullValue())
            .body("[0].name", notNullValue())
            .body("[0].quantity", notNullValue());
    }

    @Test
    void 옵션_목록_조회_빈_목록() {
        // given
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);

        // when
        var response = given()
            .when()
            .get("/api/products/" + productId + "/options");

        // then
        response.then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    @Test
    void 옵션_목록_조회_실패_존재하지_않는_상품() {
        // given — 없는 상품 ID

        // when
        var response = given()
            .when()
            .get("/api/products/999999/options");

        // then
        response.then()
            .statusCode(500);
    }

    // ── 옵션 생성 ──

    @Test
    void 옵션_생성_성공() {
        // given
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        var request = Map.of("name", "기본 옵션", "quantity", 100);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/products/" + productId + "/options");

        // then
        response.then()
            .statusCode(201)
            .header("Location", containsString("/api/products/" + productId + "/options/"))
            .body("id", notNullValue())
            .body("name", equalTo("기본 옵션"))
            .body("quantity", equalTo(100));
    }

    @Test
    void 옵션_생성_실패_존재하지_않는_상품() {
        // given
        var request = Map.of("name", "기본 옵션", "quantity", 100);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/products/999999/options");

        // then
        response.then()
            .statusCode(500);
    }

    @Test
    void 옵션_생성_실패_이름_중복() {
        // given
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        createOption(productId, "기본", 100);
        var request = Map.of("name", "기본", "quantity", 50);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/products/" + productId + "/options");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 옵션_생성_실패_이름_누락() {
        // given
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        var request = Map.of("quantity", 100);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/products/" + productId + "/options");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 옵션_생성_실패_이름_50자_초과() {
        // given
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        var request = Map.of("name", "a".repeat(51), "quantity", 100);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/products/" + productId + "/options");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 옵션_생성_실패_이름_허용되지_않는_특수문자() {
        // given
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        var request = Map.of("name", "옵션!@#", "quantity", 100);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/products/" + productId + "/options");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 옵션_생성_실패_수량_0이하() {
        // given
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        var request = Map.of("name", "기본", "quantity", 0);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/products/" + productId + "/options");

        // then
        response.then()
            .statusCode(400);
    }

    // ── 옵션 삭제 ──

    @Test
    void 옵션_삭제_성공() {
        // given
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        Long optionA = createOption(productId, "옵션A", 100);
        Long optionB = createOption(productId, "옵션B", 50);

        // when
        var response = given()
            .when()
            .delete("/api/products/" + productId + "/options/" + optionA);

        // then
        response.then()
            .statusCode(204);

        // 삭제 확인 — 1개만 남음
        given()
            .when()
            .get("/api/products/" + productId + "/options")
            .then()
            .statusCode(200)
            .body("$", hasSize(1));
    }

    @Test
    void 옵션_삭제_실패_마지막_옵션() {
        // given — 옵션 1개만 존재
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        Long optionId = createOption(productId, "유일한 옵션", 100);

        // when
        var response = given()
            .when()
            .delete("/api/products/" + productId + "/options/" + optionId);

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 옵션_삭제_실패_존재하지_않는_상품() {
        // given — 없는 상품 ID

        // when
        var response = given()
            .when()
            .delete("/api/products/999999/options/1");

        // then
        response.then()
            .statusCode(500);
    }

    @Test
    void 옵션_삭제_실패_존재하지_않는_옵션() {
        // given
        Long categoryId = createCategory();
        Long productId = createProduct(categoryId);
        createOption(productId, "옵션A", 100);
        createOption(productId, "옵션B", 50);

        // when
        var response = given()
            .when()
            .delete("/api/products/" + productId + "/options/999999");

        // then
        response.then()
            .statusCode(500);
    }

    @Test
    void 옵션_삭제_실패_다른_상품의_옵션() {
        // given
        Long categoryId = createCategory();
        Long productA = createProduct(categoryId);
        Long productB = createProduct(categoryId);
        createOption(productA, "옵션A1", 100);
        createOption(productA, "옵션A2", 50);
        Long optionB = createOption(productB, "옵션B", 100);

        // when — productA의 옵션 삭제 URL에 productB의 옵션 ID
        var response = given()
            .when()
            .delete("/api/products/" + productA + "/options/" + optionB);

        // then
        response.then()
            .statusCode(500);
    }
}
