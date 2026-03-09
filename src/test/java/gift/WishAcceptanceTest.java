package gift;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

class WishAcceptanceTest extends AcceptanceTestFixture {

    // --- 헬퍼 ---

    Long addWish(String token, Long productId) {
        return given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("productId", productId))
            .when()
            .post("/api/wishes")
            .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    // --- GET /api/wishes ---

    @Test
    void 위시리스트_조회_성공() {
        // given
        String token = registerAndGetToken("wish@test.com", "pass");
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        Long wishId = addWish(token, productId);

        // when
        var response = given()
            .header("Authorization", "Bearer " + token)
            .param("page", 0)
            .param("size", 10)
            .when()
            .get("/api/wishes");

        // then
        response.then()
            .statusCode(200)
            .body("content.size()", is(1))
            .body("content[0].id", equalTo(wishId.intValue()))
            .body("content[0].productId", equalTo(productId.intValue()))
            .body("content[0].name", equalTo("노트북"));
    }

    @Test
    void 위시리스트_조회_실패_인증헤더_누락() {
        // given
        // Authorization 헤더 없음

        // when
        var response = given()
            .param("page", 0)
            .param("size", 10)
            .when()
            .get("/api/wishes");

        // then
        response.then()
            .statusCode(401);
    }

    @Test
    void 위시리스트_빈_목록_조회() {
        // given
        String token = registerAndGetToken("empty@test.com", "pass");

        // when
        var response = given()
            .header("Authorization", "Bearer " + token)
            .param("page", 0)
            .param("size", 10)
            .when()
            .get("/api/wishes");

        // then
        response.then()
            .statusCode(200)
            .body("content.size()", is(0))
            .body("totalElements", is(0));
    }

    // --- POST /api/wishes ---

    @Test
    void 위시_추가_성공() {
        // given
        String token = registerAndGetToken("add@test.com", "pass");
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);

        var request = Map.of("productId", productId);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(request)
            .when()
            .post("/api/wishes");

        // then
        response.then()
            .statusCode(201)
            .body("id", greaterThan(0))
            .body("productId", equalTo(productId.intValue()))
            .body("name", equalTo("노트북"))
            .body("price", equalTo(1000));

        assertThat(wishRepository.count()).isEqualTo(1);
    }

    @Test
    void 위시_추가_실패_인증헤더_누락() {
        // given
        var request = Map.of("productId", 1);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/wishes");

        // then
        response.then()
            .statusCode(401);
    }

    @Test
    void 위시_추가_실패_존재하지_않는_상품() {
        // given
        String token = registerAndGetToken("notfound@test.com", "pass");
        var request = Map.of("productId", nonExistingId());

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(request)
            .when()
            .post("/api/wishes");

        // then
        response.then()
            .statusCode(404);
    }

    @Test
    void 위시_추가_이미_존재하는_상품_기존_항목_반환() {
        // given
        String token = registerAndGetToken("dup@test.com", "pass");
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        Long existingWishId = addWish(token, productId);

        var request = Map.of("productId", productId);

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(request)
            .when()
            .post("/api/wishes");

        // then
        response.then()
            .statusCode(200)
            .body("id", equalTo(existingWishId.intValue()))
            .body("productId", equalTo(productId.intValue()));

        assertThat(wishRepository.count()).isEqualTo(1);
    }

    @Test
    void 위시리스트_조회_createdDate_내림차순_정렬() {
        // given
        String token = registerAndGetToken("sort-desc@test.com", "pass");
        Long categoryId = createCategory("전자기기");
        Long productAId = createProduct("상품A", 1000, "http://img.test/a.png", categoryId);
        Long productBId = createProduct("상품B", 2000, "http://img.test/b.png", categoryId);
        addWish(token, productAId);
        addWish(token, productBId);

        // when
        var response = given()
            .header("Authorization", "Bearer " + token)
            .param("page", 0)
            .param("size", 10)
            .param("sort", "createdDate,desc")
            .when()
            .get("/api/wishes");

        // then
        response.then()
            .statusCode(200)
            .body("content.size()", is(2))
            .body("content[0].productId", equalTo(productBId.intValue()))
            .body("content[1].productId", equalTo(productAId.intValue()));
    }

    @Test
    void 위시리스트_조회_createdDate_오름차순_정렬() {
        // given
        String token = registerAndGetToken("sort-asc@test.com", "pass");
        Long categoryId = createCategory("전자기기");
        Long productAId = createProduct("상품A", 1000, "http://img.test/a.png", categoryId);
        Long productBId = createProduct("상품B", 2000, "http://img.test/b.png", categoryId);
        addWish(token, productAId);
        addWish(token, productBId);

        // when
        var response = given()
            .header("Authorization", "Bearer " + token)
            .param("page", 0)
            .param("size", 10)
            .param("sort", "createdDate,asc")
            .when()
            .get("/api/wishes");

        // then
        response.then()
            .statusCode(200)
            .body("content.size()", is(2))
            .body("content[0].productId", equalTo(productAId.intValue()))
            .body("content[1].productId", equalTo(productBId.intValue()));
    }

    // --- DELETE /api/wishes/{id} ---

    @Test
    void 위시_삭제_성공() {
        // given
        String token = registerAndGetToken("del@test.com", "pass");
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        Long wishId = addWish(token, productId);

        // when
        var response = given()
            .header("Authorization", "Bearer " + token)
            .when()
            .delete("/api/wishes/" + wishId);

        // then
        response.then()
            .statusCode(204);

        assertThat(wishRepository.findById(wishId)).isEmpty();
    }

    @Test
    void 위시_삭제_실패_인증헤더_누락() {
        // given
        // Authorization 헤더 없음

        // when
        var response = given()
            .when()
            .delete("/api/wishes/1");

        // then
        response.then()
            .statusCode(401);
    }

    @Test
    void 위시_삭제_실패_다른_사용자의_위시() {
        // given
        String ownerToken = registerAndGetToken("owner@test.com", "pass");
        String otherToken = registerAndGetToken("other@test.com", "pass");
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        Long wishId = addWish(ownerToken, productId);

        // when
        var response = given()
            .header("Authorization", "Bearer " + otherToken)
            .when()
            .delete("/api/wishes/" + wishId);

        // then
        response.then()
            .statusCode(403);

        assertThat(wishRepository.findById(wishId)).isPresent();
    }
}
