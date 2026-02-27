package gift.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class E2eOptionStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @먼저("{string} 상품에 수량 {int}개인 {string} 옵션이 존재한다")
    public void 상품에_옵션이_존재한다(String productName, int quantity, String optionName) {
        var productId = context.getProductId(productName);
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", optionName, "quantity", quantity))
                .when()
                .post("/api/products/" + productId + "/options")
                .then()
                .extract();
        assertThat(response.statusCode()).isEqualTo(201);
        context.putOptionId(optionName, response.jsonPath().getLong("id"));
    }

    @만일("{string} 상품에 수량 {int}개인 {string} 옵션을 생성하면")
    public void 상품에_옵션을_생성하면(String productName, int quantity, String optionName) {
        var productId = context.getProductId(productName);
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", optionName, "quantity", quantity))
                .when()
                .post("/api/products/" + productId + "/options")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("존재하지 않는 상품에 옵션을 생성하면")
    public void 존재하지_않는_상품에_옵션을_생성하면() {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "ICE", "quantity", 10))
                .when()
                .post("/api/products/" + Long.MAX_VALUE + "/options")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("{string} 상품의 옵션 목록을 조회하면")
    public void 상품의_옵션_목록을_조회하면(String productName) {
        var productId = context.getProductId(productName);
        var response = RestAssured.given()
                .when()
                .get("/api/products/" + productId + "/options")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("{string} 상품의 {string} 옵션을 삭제하면")
    public void 상품의_옵션을_삭제하면(String productName, String optionName) {
        var productId = context.getProductId(productName);
        var optionId = context.getOptionId(optionName);
        var response = RestAssured.given()
                .when()
                .delete("/api/products/" + productId + "/options/" + optionId)
                .then()
                .extract();
        context.setResponse(response);
    }

    @그러면("옵션이 생성된다")
    public void 옵션이_생성된다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(201);
        assertThat(context.getResponse().jsonPath().getLong("id")).isNotNull();
    }

    @그러면("옵션 이름은 {string}이다")
    public void 옵션_이름은_이다(String name) {
        assertThat(context.getResponse().jsonPath().getString("name")).isEqualTo(name);
    }

    @그러면("옵션 수량은 {int}개이다")
    public void 옵션_수량은_개이다(int quantity) {
        assertThat(context.getResponse().jsonPath().getInt("quantity")).isEqualTo(quantity);
    }

    @그러면("{string}, {string} 옵션이 조회된다")
    public void 옵션이_조회된다(String name1, String name2) {
        var response = context.getResponse();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("name")).containsExactlyInAnyOrder(name1, name2);
    }

    @그러면("옵션 삭제에 성공한다")
    public void 옵션_삭제에_성공한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(204);
    }

    @그러면("옵션 삭제에 실패한다")
    public void 옵션_삭제에_실패한다() {
        assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(400);
    }

    @그러면("옵션 생성에 실패한다")
    public void 옵션_생성에_실패한다() {
        assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(400);
    }
}
