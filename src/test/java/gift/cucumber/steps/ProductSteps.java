package gift.cucumber.steps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import gift.cucumber.SharedState;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.먼저;
import io.cucumber.java.ko.만일;
import org.springframework.beans.factory.annotation.Autowired;

public class ProductSteps {

    @Autowired
    private SharedState state;

    @만일("페이지 {int}, 사이즈 {int}로 상품 목록을 조회한다")
    public void 상품_목록_페이징_조회(int page, int size) {
        state.setResponse(
            given()
                .param("page", page)
                .param("size", size)
            .when()
                .get("/api/products")
        );
    }

    @만일("ID {long} 상품을 조회한다")
    public void 상품_단건_조회(long id) {
        state.setResponse(
            given()
            .when()
                .get("/api/products/" + id)
        );
    }

    @만일("해당 상품을 조회한다")
    public void 해당_상품_조회() {
        state.setResponse(
            given()
            .when()
                .get("/api/products/" + state.getSavedId())
        );
    }

    @만일("이름 {string}, 가격 {int}, 이미지 {string}, 카테고리 {long}로 상품을 생성한다")
    public void 상품_생성(String name, int price, String imageUrl, long categoryId) {
        state.setResponse(
            given()
                .contentType("application/json")
                .body("{\"name\":\"" + name + "\",\"price\":" + price + ",\"imageUrl\":\"" + imageUrl + "\",\"categoryId\":" + categoryId + "}")
            .when()
                .post("/api/products")
        );
    }

    @먼저("이름 {string}, 가격 {int}, 카테고리 {long}로 상품이 존재한다")
    public void 상품_존재(String name, int price, long categoryId) {
        Long id = given()
            .contentType("application/json")
            .body("{\"name\":\"" + name + "\",\"price\":" + price + ",\"imageUrl\":\"https://example.com/img.jpg\",\"categoryId\":" + categoryId + "}")
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
        state.setSavedId(id);
    }

    @먼저("해당 카테고리로 이름 {string}, 가격 {int}인 상품이 존재한다")
    public void 카테고리_상품_존재(String name, int price) {
        Long id = given()
            .contentType("application/json")
            .body("{\"name\":\"" + name + "\",\"price\":" + price + ",\"imageUrl\":\"https://example.com/img.jpg\",\"categoryId\":" + state.getSavedId() + "}")
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
        state.setSavedId(id);
    }

    @만일("해당 상품을 이름 {string}, 가격 {int}, 이미지 {string}, 카테고리 {long}로 수정한다")
    public void 상품_수정(String name, int price, String imageUrl, long categoryId) {
        state.setResponse(
            given()
                .contentType("application/json")
                .body("{\"name\":\"" + name + "\",\"price\":" + price + ",\"imageUrl\":\"" + imageUrl + "\",\"categoryId\":" + categoryId + "}")
            .when()
                .put("/api/products/" + state.getSavedId())
        );
    }

    @만일("해당 상품을 삭제한다")
    public void 상품_삭제() {
        state.setResponse(
            given()
            .when()
                .delete("/api/products/" + state.getSavedId())
        );
    }

    @그리고("응답의 상품 이름은 {string}이다")
    public void 상품_이름_검증(String name) {
        state.getResponse().then().body("name", equalTo(name));
    }

    @그리고("응답의 상품 가격은 {int}이다")
    public void 상품_가격_검증(int price) {
        state.getResponse().then().body("price", equalTo(price));
    }

    @그리고("상품 목록의 사이즈는 {int}이다")
    public void 상품_목록_사이즈_검증(int size) {
        state.getResponse().then().body("content.size()", equalTo(size));
    }

    @그리고("전체 상품 수는 {int} 이상이다")
    public void 전체_상품_수_검증(int count) {
        state.getResponse().then().body("totalElements", greaterThanOrEqualTo(count));
    }
}
