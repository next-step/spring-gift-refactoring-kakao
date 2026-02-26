package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductAcceptanceTest {

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
        // FK 역순으로 삭제 — 테스트 격리 보장
        orderRepository.deleteAll();
        wishRepository.deleteAll();
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    Long createCategory(String name) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name, "color", "#000000", "imageUrl", "https://example.com/cat.jpg", "description", ""))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    Long createProduct(String name, int price, String imageUrl, Long categoryId) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name, "price", price, "imageUrl", imageUrl, "categoryId", categoryId))
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    // ── 상품 생성 ──

    @Test
    void 상품_생성_성공() {
        // given
        Long categoryId = createCategory("전자기기");
        var request = Map.of("name", "노트북", "price", 1000000, "imageUrl", "https://example.com/laptop.jpg", "categoryId", categoryId);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(201)
            .header("Location", containsString("/api/products/"))
            .body("id", notNullValue())
            .body("name", equalTo("노트북"))
            .body("price", equalTo(1000000))
            .body("imageUrl", equalTo("https://example.com/laptop.jpg"))
            .body("categoryId", equalTo(categoryId.intValue()));
    }

    @Test
    void 상품_생성_실패_존재하지_않는_카테고리() {
        // given
        var request = Map.of("name", "노트북", "price", 1000000, "imageUrl", "https://example.com/laptop.jpg", "categoryId", 999999);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(500);
    }

    @Test
    void 상품_생성_실패_이름_누락() {
        // given
        Long categoryId = createCategory("전자기기");
        var request = Map.of("price", 1000, "imageUrl", "https://example.com/img.jpg", "categoryId", categoryId);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 상품_생성_실패_이름_15자_초과() {
        // given
        Long categoryId = createCategory("전자기기");
        var request = Map.of("name", "a".repeat(16), "price", 1000, "imageUrl", "https://example.com/img.jpg", "categoryId", categoryId);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 상품_생성_실패_이름_허용되지_않는_특수문자() {
        // given
        Long categoryId = createCategory("전자기기");
        var request = Map.of("name", "상품!@#", "price", 1000, "imageUrl", "https://example.com/img.jpg", "categoryId", categoryId);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 상품_생성_실패_이름_카카오_포함() {
        // given
        Long categoryId = createCategory("전자기기");
        var request = Map.of("name", "카카오선물", "price", 1000, "imageUrl", "https://example.com/img.jpg", "categoryId", categoryId);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 상품_생성_성공_이름_허용_특수문자() {
        // given
        Long categoryId = createCategory("전자기기");
        var request = Map.of("name", "(A+B) [C-D]/E", "price", 1000, "imageUrl", "https://example.com/img.jpg", "categoryId", categoryId);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(201)
            .body("name", equalTo("(A+B) [C-D]/E"));
    }

    // ── 상품 조회 ──

    @Test
    void 상품_목록_조회_성공() {
        // given
        Long categoryId = createCategory("전자기기");
        createProduct("노트북", 1000000, "https://example.com/laptop.jpg", categoryId);
        createProduct("키보드", 50000, "https://example.com/keyboard.jpg", categoryId);

        // when
        var response = given()
        .when()
            .get("/api/products");

        // then
        response.then()
            .statusCode(200)
            .body("content", hasSize(2))
            .body("content[0].id", notNullValue());
    }

    @Test
    void 상품_목록_조회_빈_목록() {
        // given — 상품 없음

        // when
        var response = given()
        .when()
            .get("/api/products");

        // then
        response.then()
            .statusCode(200)
            .body("content", hasSize(0));
    }

    @Test
    void 상품_목록_조회_페이지네이션() {
        // given
        Long categoryId = createCategory("전자기기");
        for (int i = 1; i <= 3; i++) {
            createProduct("상품" + i, 1000 * i, "https://example.com/" + i + ".jpg", categoryId);
        }

        // when
        var response = given()
            .queryParam("page", 0)
            .queryParam("size", 2)
        .when()
            .get("/api/products");

        // then
        response.then()
            .statusCode(200)
            .body("content", hasSize(2))
            .body("totalElements", equalTo(3))
            .body("totalPages", equalTo(2));
    }

    @Test
    void 상품_단건_조회_성공() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000000, "https://example.com/laptop.jpg", categoryId);

        // when
        var response = given()
        .when()
            .get("/api/products/" + productId);

        // then
        response.then()
            .statusCode(200)
            .body("id", equalTo(productId.intValue()))
            .body("name", equalTo("노트북"))
            .body("price", equalTo(1000000))
            .body("categoryId", equalTo(categoryId.intValue()));
    }

    @Test
    void 상품_단건_조회_실패_존재하지_않는_ID() {
        // given — 없는 ID

        // when
        var response = given()
        .when()
            .get("/api/products/999999");

        // then
        response.then()
            .statusCode(500);
    }

    // ── 상품 수정 ──

    @Test
    void 상품_수정_성공() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000000, "https://example.com/laptop.jpg", categoryId);
        var request = Map.of("name", "게이밍노트북", "price", 2000000, "imageUrl", "https://example.com/gaming.jpg", "categoryId", categoryId);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/products/" + productId);

        // then
        response.then()
            .statusCode(200)
            .body("name", equalTo("게이밍노트북"))
            .body("price", equalTo(2000000));
    }

    @Test
    void 상품_수정_성공_카테고리_변경() {
        // given
        Long catA = createCategory("전자기기");
        Long catB = createCategory("가구");
        Long productId = createProduct("책상", 500000, "https://example.com/desk.jpg", catA);
        var request = Map.of("name", "책상", "price", 500000, "imageUrl", "https://example.com/desk.jpg", "categoryId", catB);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/products/" + productId);

        // then
        response.then()
            .statusCode(200)
            .body("categoryId", equalTo(catB.intValue()));
    }

    @Test
    void 상품_수정_실패_존재하지_않는_상품() {
        // given
        Long categoryId = createCategory("전자기기");
        var request = Map.of("name", "노트북", "price", 1000000, "imageUrl", "https://example.com/laptop.jpg", "categoryId", categoryId);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/products/999999");

        // then
        response.then()
            .statusCode(500);
    }

    @Test
    void 상품_수정_실패_존재하지_않는_카테고리() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000000, "https://example.com/laptop.jpg", categoryId);
        var request = Map.of("name", "노트북", "price", 1000000, "imageUrl", "https://example.com/laptop.jpg", "categoryId", 999999);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/products/" + productId);

        // then
        response.then()
            .statusCode(500);
    }

    @Test
    void 상품_수정_실패_이름_카카오_포함() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000000, "https://example.com/laptop.jpg", categoryId);
        var request = Map.of("name", "카카오노트북", "price", 1000000, "imageUrl", "https://example.com/laptop.jpg", "categoryId", categoryId);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/products/" + productId);

        // then
        response.then()
            .statusCode(400);
    }

    // ── 상품 삭제 ──

    @Test
    void 상품_삭제_성공() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000000, "https://example.com/laptop.jpg", categoryId);

        // when
        var response = given()
        .when()
            .delete("/api/products/" + productId);

        // then
        response.then()
            .statusCode(204);

        // 삭제 확인
        given()
        .when()
            .get("/api/products/" + productId)
        .then()
            .statusCode(500);
    }

    @Test
    void 상품_삭제_존재하지_않는_ID_무시() {
        // given — 없는 ID

        // when
        var response = given()
        .when()
            .delete("/api/products/999999");

        // then — deleteById는 존재하지 않아도 예외 없이 204
        response.then()
            .statusCode(204);
    }
}
