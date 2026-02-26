package gift.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class E2eProductStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @먼저("{string} 카테고리에 {int}원짜리 {string} 상품이 존재한다")
    public void 카테고리에_상품이_존재한다_가격포함(String categoryName, int price, String productName) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name",
                        productName,
                        "price",
                        price,
                        "imageUrl",
                        "http://example.com/image.jpg",
                        "categoryId",
                        context.getCategoryId(categoryName)))
                .when()
                .post("/api/products")
                .then()
                .extract();
        assertThat(response.statusCode()).isEqualTo(201);
        context.putProductId(productName, response.jsonPath().getLong("id"));
    }

    @먼저("{string} 카테고리에 {string} 상품이 존재한다")
    public void 카테고리에_상품이_존재한다(String categoryName, String productName) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name",
                        productName,
                        "price",
                        4500,
                        "imageUrl",
                        "http://example.com/image.jpg",
                        "categoryId",
                        context.getCategoryId(categoryName)))
                .when()
                .post("/api/products")
                .then()
                .extract();
        assertThat(response.statusCode()).isEqualTo(201);
        context.putProductId(productName, response.jsonPath().getLong("id"));
    }

    @만일("{string} 카테고리에 {int}원짜리 {string} 상품을 생성하면")
    public void 상품을_생성하면(String categoryName, int price, String productName) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name",
                        productName,
                        "price",
                        price,
                        "imageUrl",
                        "http://example.com/image.jpg",
                        "categoryId",
                        context.getCategoryId(categoryName)))
                .when()
                .post("/api/products")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("존재하지 않는 카테고리에 상품을 생성하면")
    public void 존재하지_않는_카테고리에_상품을_생성하면() {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name",
                        "아메리카노",
                        "price",
                        4500,
                        "imageUrl",
                        "http://example.com/image.jpg",
                        "categoryId",
                        Long.MAX_VALUE))
                .when()
                .post("/api/products")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("상품 목록을 조회하면")
    public void 상품_목록을_조회하면() {
        var response = RestAssured.given().when().get("/api/products").then().extract();
        context.setResponse(response);
    }

    @만일("{string} 상품을 조회하면")
    public void 상품을_조회하면(String productName) {
        var productId = context.getProductId(productName);
        var response = RestAssured.given()
                .when()
                .get("/api/products/" + productId)
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("존재하지 않는 상품을 조회하면")
    public void 존재하지_않는_상품을_조회하면() {
        var response = RestAssured.given()
                .when()
                .get("/api/products/" + Long.MAX_VALUE)
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("{string} 상품을 {int}원짜리 {string}로 수정하면")
    public void 상품을_수정하면(String oldName, int price, String newName) {
        var productId = context.getProductId(oldName);
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name",
                        newName,
                        "price",
                        price,
                        "imageUrl",
                        "http://example.com/updated.jpg",
                        "categoryId",
                        context.getResponse() != null
                                ? context.getResponse().jsonPath().getLong("categoryId")
                                : context.getCategoryId("음료")))
                .when()
                .put("/api/products/" + productId)
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("{string} 상품을 삭제하면")
    public void 상품을_삭제하면(String productName) {
        var productId = context.getProductId(productName);
        var response = RestAssured.given()
                .when()
                .delete("/api/products/" + productId)
                .then()
                .extract();
        context.setResponse(response);
    }

    @그러면("상품이 생성된다")
    public void 상품이_생성된다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(201);
        assertThat(context.getResponse().jsonPath().getLong("id")).isNotNull();
    }

    @그러면("상품 이름은 {string}이다")
    public void 상품_이름은_이다(String name) {
        assertThat(context.getResponse().jsonPath().getString("name")).isEqualTo(name);
    }

    @그러면("상품 가격은 {int}원이다")
    public void 상품_가격은_원이다(int price) {
        assertThat(context.getResponse().jsonPath().getInt("price")).isEqualTo(price);
    }

    @그러면("상품 생성에 실패한다")
    public void 상품_생성에_실패한다() {
        assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(400);
    }

    @그러면("{string}, {string} 상품이 조회된다")
    public void 상품이_조회된다(String name1, String name2) {
        var response = context.getResponse();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("content.id")).doesNotContainNull();
        assertThat(response.jsonPath().getList("content.name")).containsExactlyInAnyOrder(name1, name2);
    }

    @그러면("각 상품에 가격, 이미지, 카테고리 정보가 포함되어 있다")
    public void 각_상품에_정보가_포함되어_있다() {
        var response = context.getResponse();
        assertThat(response.jsonPath().getList("content.price")).doesNotContainNull();
        assertThat(response.jsonPath().getList("content.imageUrl")).doesNotContainNull();
        assertThat(response.jsonPath().getList("content.categoryId")).doesNotContainNull();
    }

    @그러면("상품이 조회되지 않는다")
    public void 상품이_조회되지_않는다() {
        var response = context.getResponse();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("content")).isEmpty();
    }

    @그러면("상품 조회에 성공한다")
    public void 상품_조회에_성공한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(200);
    }

    @그러면("상품 조회에 실패한다")
    public void 상품_조회에_실패한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(404);
    }

    @그러면("상품 수정에 성공한다")
    public void 상품_수정에_성공한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(200);
    }

    @그러면("상품 삭제에 성공한다")
    public void 상품_삭제에_성공한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(204);
    }
}
