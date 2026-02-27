package gift;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import java.util.HashMap;
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
class CategoryAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    WishRepository wishRepository;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    ProductRepository productRepository;

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
        return createCategory(name, "#FF0000", "https://example.com/test.jpg", null);
    }

    Long createCategory(String name, String color, String imageUrl, String description) {
        var request = new HashMap<String, String>();
        request.put("name", name);
        request.put("color", color);
        request.put("imageUrl", imageUrl);
        request.put("description", description);

        return given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/categories")
            .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    // === POST /api/categories ===

    @Test
    void 카테고리_생성_성공() {
        // given
        var request = Map.of(
            "name", "테스트카테고리",
            "color", "#FF0000",
            "imageUrl", "https://example.com/test.jpg",
            "description", "테스트 설명"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/categories");

        // then
        response.then()
            .statusCode(201)
            .header("Location", matchesPattern("/api/categories/\\d+"))
            .body("id", notNullValue())
            .body("name", equalTo("테스트카테고리"))
            .body("color", equalTo("#FF0000"))
            .body("imageUrl", equalTo("https://example.com/test.jpg"))
            .body("description", equalTo("테스트 설명"));
    }

    @Test
    void 카테고리_생성_성공_설명_없이() {
        // given
        var request = Map.of(
            "name", "테스트카테고리",
            "color", "#FF0000",
            "imageUrl", "https://example.com/test.jpg"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/categories");

        // then
        response.then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("테스트카테고리"))
            .body("description", nullValue());
    }

    @Test
    void 카테고리_생성_실패_이름_누락() {
        // given
        var request = Map.of(
            "color", "#FF0000",
            "imageUrl", "https://example.com/test.jpg"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/categories");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 카테고리_생성_실패_이름_빈문자열() {
        // given
        var request = new HashMap<String, String>();
        request.put("name", "");
        request.put("color", "#FF0000");
        request.put("imageUrl", "https://example.com/test.jpg");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/categories");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 카테고리_생성_실패_색상_누락() {
        // given
        var request = Map.of(
            "name", "테스트카테고리",
            "imageUrl", "https://example.com/test.jpg"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/categories");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 카테고리_생성_실패_이미지URL_누락() {
        // given
        var request = Map.of(
            "name", "테스트카테고리",
            "color", "#FF0000"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/categories");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 카테고리_생성_실패_중복_이름() {
        // given — 먼저 카테고리 생성
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "중복카테고리",
                "color", "#FF0000",
                "imageUrl", "https://example.com/test.jpg"
            ))
            .when()
            .post("/api/categories")
            .then()
            .statusCode(201);

        // when — 같은 이름으로 재생성
        var response = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "중복카테고리",
                "color", "#00FF00",
                "imageUrl", "https://example.com/test2.jpg"
            ))
            .when()
            .post("/api/categories");

        // then — DB unique 제약 위반
        response.then()
            .statusCode(500);
    }

    // === GET /api/categories ===

    @Test
    void 카테고리_목록_조회_빈목록() {
        // given — setUp에서 전체 삭제됨

        // when
        var response = given()
            .when()
            .get("/api/categories");

        // then
        response.then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    @Test
    void 카테고리_목록_조회_성공() {
        // given
        createCategory("전자기기", "#1E90FF", "https://example.com/electronics.jpg", "전자기기 설명");
        createCategory("패션", "#FF6347", "https://example.com/fashion.jpg", "패션 설명");

        // when
        var response = given()
            .when()
            .get("/api/categories");

        // then
        response.then()
            .statusCode(200)
            .body("$", hasSize(2))
            .body("name", hasItems("전자기기", "패션"));
    }

    @Test
    void 카테고리_목록_조회_응답_필드_전체_검증() {
        // given
        createCategory("전자기기", "#1E90FF", "https://example.com/electronics.jpg", "전자기기 설명");

        // when
        var response = given()
            .when()
            .get("/api/categories");

        // then
        response.then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].id", notNullValue())
            .body("[0].name", equalTo("전자기기"))
            .body("[0].color", equalTo("#1E90FF"))
            .body("[0].imageUrl", equalTo("https://example.com/electronics.jpg"))
            .body("[0].description", equalTo("전자기기 설명"));
    }

    // === PUT /api/categories/{id} ===

    @Test
    void 카테고리_수정_성공() {
        // given
        Long id = createCategory("수정전", "#FF0000", "https://example.com/before.jpg", "수정전 설명");
        var request = Map.of(
            "name", "수정후",
            "color", "#00FF00",
            "imageUrl", "https://example.com/after.jpg",
            "description", "수정후 설명"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/api/categories/" + id);

        // then
        response.then()
            .statusCode(200)
            .body("id", equalTo(id.intValue()))
            .body("name", equalTo("수정후"))
            .body("color", equalTo("#00FF00"))
            .body("imageUrl", equalTo("https://example.com/after.jpg"))
            .body("description", equalTo("수정후 설명"));
    }

    @Test
    void 카테고리_수정_실패_존재하지_않는_ID() {
        // given
        var request = Map.of(
            "name", "수정후",
            "color", "#00FF00",
            "imageUrl", "https://example.com/after.jpg"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/api/categories/999999");

        // then
        response.then()
            .statusCode(500);
    }

    @Test
    void 카테고리_수정_실패_이름_빈문자열() {
        // given
        Long id = createCategory("수정전", "#FF0000", "https://example.com/before.jpg", null);
        var request = new HashMap<String, String>();
        request.put("name", "");
        request.put("color", "#00FF00");
        request.put("imageUrl", "https://example.com/after.jpg");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/api/categories/" + id);

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 카테고리_수정_실패_중복_이름() {
        // given
        createCategory("기존카테고리");
        Long id = createCategory("수정대상");
        var request = Map.of(
            "name", "기존카테고리",
            "color", "#00FF00",
            "imageUrl", "https://example.com/after.jpg"
        );

        // when — 다른 카테고리와 같은 이름으로 수정
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/api/categories/" + id);

        // then — DB unique 제약 위반
        response.then()
            .statusCode(500);
    }

    @Test
    void 카테고리_수정_성공_설명_null로_변경() {
        // given
        Long id = createCategory("카테고리", "#FF0000", "https://example.com/test.jpg", "원래 설명");
        var request = new HashMap<String, Object>();
        request.put("name", "카테고리");
        request.put("color", "#FF0000");
        request.put("imageUrl", "https://example.com/test.jpg");
        request.put("description", null);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/api/categories/" + id);

        // then
        response.then()
            .statusCode(200)
            .body("description", nullValue());
    }

    // === DELETE /api/categories/{id} ===

    @Test
    void 카테고리_삭제_성공() {
        // given
        Long id = createCategory("삭제대상", "#FF0000", "https://example.com/delete.jpg", null);

        // when
        var response = given()
            .when()
            .delete("/api/categories/" + id);

        // then — Layer 1: 상태 코드
        response.then()
            .statusCode(204);

        // then — Layer 3: DB 상태 변화 확인
        given()
            .when()
            .get("/api/categories")
            .then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    @Test
    void 카테고리_삭제_실패_연관_상품_존재() {
        // given — 카테고리 생성 후 해당 카테고리에 상품 등록
        Long categoryId = createCategory("전자기기");
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "맥북",
                "price", 3000000,
                "imageUrl", "https://example.com/macbook.jpg",
                "categoryId", categoryId
            ))
            .when()
            .post("/api/products")
            .then()
            .statusCode(201);

        // when — 상품이 참조 중인 카테고리 삭제 시도
        var response = given()
            .when()
            .delete("/api/categories/" + categoryId);

        // then — FK 제약 위반으로 삭제 실패
        response.then()
            .statusCode(500);
    }

    @Test
    void 카테고리_삭제_존재하지_않는_ID_무시() {
        // given — 존재하지 않는 ID

        // when
        var response = given()
            .when()
            .delete("/api/categories/999999");

        // then — Spring Data JPA 3.x: deleteById는 존재하지 않아도 예외 없이 204
        response.then()
            .statusCode(204);
    }
}
