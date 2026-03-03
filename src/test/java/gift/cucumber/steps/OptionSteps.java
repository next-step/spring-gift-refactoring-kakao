package gift.cucumber.steps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import gift.cucumber.SharedState;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.먼저;
import io.cucumber.java.ko.만일;
import org.springframework.beans.factory.annotation.Autowired;

public class OptionSteps {

    @Autowired
    private SharedState state;

    @만일("상품 {long}의 옵션 목록을 조회한다")
    public void 옵션_목록_조회(long productId) {
        state.setResponse(
            given()
            .when()
                .get("/api/products/" + productId + "/options")
        );
    }

    @만일("상품 {long}에 이름 {string}, 수량 {int}으로 옵션을 생성한다")
    public void 옵션_생성(long productId, String name, int quantity) {
        state.setResponse(
            given()
                .contentType("application/json")
                .body("{\"name\":\"" + name + "\",\"quantity\":" + quantity + "}")
            .when()
                .post("/api/products/" + productId + "/options")
        );
    }

    @먼저("상품 {long}에 이름 {string}, 수량 {int}으로 옵션이 존재한다")
    public void 옵션_존재(long productId, String name, int quantity) {
        Number id = given()
            .contentType("application/json")
            .body("{\"name\":\"" + name + "\",\"quantity\":" + quantity + "}")
        .when()
            .post("/api/products/" + productId + "/options")
        .then()
            .statusCode(201)
            .extract().jsonPath().get("id");
        state.setOptionProductId(productId);
        state.setOptionId(id.longValue());
    }

    @먼저("해당 상품에 이름 {string}, 수량 {int}으로 옵션이 존재한다")
    public void 해당_상품_옵션_존재(String name, int quantity) {
        Long productId = state.getSavedId();
        Number id = given()
            .contentType("application/json")
            .body("{\"name\":\"" + name + "\",\"quantity\":" + quantity + "}")
        .when()
            .post("/api/products/" + productId + "/options")
        .then()
            .statusCode(201)
            .extract().jsonPath().get("id");
        state.setOptionProductId(productId);
        state.setOptionId(id.longValue());
    }

    @만일("상품 {long}의 해당 옵션을 삭제한다")
    public void 옵션_삭제(long productId) {
        state.setResponse(
            given()
            .when()
                .delete("/api/products/" + productId + "/options/" + state.getOptionId())
        );
    }

    @만일("해당 상품의 해당 옵션을 삭제한다")
    public void 해당_상품_옵션_삭제() {
        state.setResponse(
            given()
            .when()
                .delete("/api/products/" + state.getOptionProductId() + "/options/" + state.getOptionId())
        );
    }

    @그리고("옵션 목록의 사이즈는 {int} 이상이다")
    public void 옵션_목록_사이즈_검증(int size) {
        state.getResponse().then().body("size()", greaterThanOrEqualTo(size));
    }

    @그리고("응답의 옵션 이름은 {string}이다")
    public void 옵션_이름_검증(String name) {
        state.getResponse().then().body("name", equalTo(name));
    }

    @그리고("응답의 옵션 수량은 {int}이다")
    public void 옵션_수량_검증(int quantity) {
        state.getResponse().then().body("quantity", equalTo(quantity));
    }
}
