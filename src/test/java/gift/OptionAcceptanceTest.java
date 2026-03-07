package gift;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

class OptionAcceptanceTest extends AcceptanceTestFixture {

    // --- GET /api/products/{productId}/options ---

    @Test
    void 옵션_목록_조회_성공() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        Long optionId1 = createOption(productId, "8GB", 10);
        Long optionId2 = createOption(productId, "16GB", 5);

        // when
        var response = given()
        .when()
            .get("/api/products/" + productId + "/options");

        // then
        response.then()
            .statusCode(200)
            .body("size()", is(2))
            .body("id", hasItems(optionId1.intValue(), optionId2.intValue()))
            .body("name", hasItems("8GB", "16GB"))
            .body("quantity", hasItems(10, 5));
    }

    @Test
    void 옵션_목록_조회_실패_존재하지_않는_상품() {
        // given
        // 존재하지 않는 productId

        // when
        var response = given()
        .when()
            .get("/api/products/" + nonExistingId() + "/options");

        // then
        response.then()
            .statusCode(404);
    }

    // --- POST /api/products/{productId}/options ---

    @Test
    void 옵션_추가_성공() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        var request = Map.of("name", "8GB", "quantity", 10);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products/" + productId + "/options");

        // then
        response.then()
            .statusCode(201)
            .body("id", greaterThan(0))
            .body("name", equalTo("8GB"))
            .body("quantity", equalTo(10));

        assertThat(optionRepository.findByProductId(productId)).hasSize(1);
    }

    @Test
    void 옵션_추가_실패_이름_누락() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        var request = Map.of("quantity", 10);

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
    void 옵션_추가_실패_수량_0이하() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        var request = Map.of("name", "8GB", "quantity", 0);

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
    void 옵션_추가_실패_존재하지_않는_상품() {
        // given
        var request = Map.of("name", "8GB", "quantity", 10);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products/" + nonExistingId() + "/options");

        // then
        response.then()
            .statusCode(404);
    }

    @Test
    void 옵션_추가_실패_중복_옵션명() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        createOption(productId, "8GB", 10);

        var request = Map.of("name", "8GB", "quantity", 5);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products/" + productId + "/options");

        // then
        response.then()
            .statusCode(409)
            .body(containsString("이미 존재하는 옵션명"));
    }

    @Test
    void 옵션_추가_실패_이름_50자_초과() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        String longName = "가".repeat(51);
        var request = Map.of("name", longName, "quantity", 10);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products/" + productId + "/options");

        // then
        response.then()
            .statusCode(400)
            .body(containsString("최대 50자"));
    }

    // --- DELETE /api/products/{productId}/options/{optionId} ---

    @Test
    void 옵션_삭제_성공() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        createOption(productId, "8GB", 10);
        Long optionId = createOption(productId, "16GB", 5);

        // when
        var response = given()
        .when()
            .delete("/api/products/" + productId + "/options/" + optionId);

        // then
        response.then()
            .statusCode(204);

        assertThat(optionRepository.findById(optionId)).isEmpty();
        assertThat(optionRepository.findByProductId(productId)).hasSize(1);
    }

    @Test
    void 옵션_삭제_실패_마지막_옵션() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        Long optionId = createOption(productId, "8GB", 10);

        // when
        var response = given()
        .when()
            .delete("/api/products/" + productId + "/options/" + optionId);

        // then
        response.then()
            .statusCode(400)
            .body(containsString("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다"));

        assertThat(optionRepository.findById(optionId)).isPresent();
    }

    @Test
    void 옵션_삭제_실패_존재하지_않는_옵션() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        createOption(productId, "8GB", 10);
        createOption(productId, "16GB", 5);

        // when
        var response = given()
        .when()
            .delete("/api/products/" + productId + "/options/" + nonExistingId());

        // then
        response.then()
            .statusCode(404);
    }
}
