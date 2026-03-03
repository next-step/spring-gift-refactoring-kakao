package gift.cucumber.steps;

import gift.cucumber.SharedState;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class ProductSteps {

    @Autowired
    private SharedState state;

    @Given("{string} 이름과 {int} 가격의 상품이 등록되어 있다")
    public void 상품이_등록되어_있다(String name, int price) {
        Long id = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "name", name,
                "price", price,
                "imageUrl", "https://example.com/image.jpg",
                "categoryId", state.getCategoryId()
            ))
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
        state.setSavedId(id);
    }

    @When("{string} 이름과 {int} 가격으로 상품을 생성하면")
    public void 상품을_생성하면(String name, int price) {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "name", name,
                "price", price,
                "imageUrl", "https://example.com/image.jpg",
                "categoryId", state.getCategoryId()
            ))
        .when()
            .post("/api/products")
        .then()
            .statusCode(201);
    }

    @When("상품 목록을 조회하면")
    public void 상품_목록을_조회하면() {
    }

    @When("해당 상품의 이름을 {string}로 가격을 {int}으로 변경하면")
    public void 상품을_변경하면(String newName, int newPrice) {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "name", newName,
                "price", newPrice,
                "imageUrl", "https://example.com/updated.jpg",
                "categoryId", state.getCategoryId()
            ))
        .when()
            .put("/api/products/{id}", state.getSavedId())
        .then()
            .statusCode(200);
    }

    @When("해당 상품을 삭제하면")
    public void 상품을_삭제하면() {
        given()
        .when()
            .delete("/api/products/{id}", state.getSavedId())
        .then()
            .statusCode(204);
    }

    @Then("상품 목록의 크기는 {int}이다")
    public void 상품_목록의_크기는(int size) {
        given()
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("content", hasSize(size));
    }

    @And("상품 목록에 {string} 이름이 포함되어 있다")
    public void 상품_목록에_이름이_포함되어_있다(String name) {
        given()
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("content.name", hasItem(name));
    }

    @And("상품 목록에 {string}, {string}, {string} 이름이 모두 포함되어 있다")
    public void 상품_목록에_이름들이_모두_포함되어_있다(String name1, String name2, String name3) {
        given()
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("content.name", hasItems(name1, name2, name3));
    }

    @Then("해당 상품의 이름은 {string}이다")
    public void 상품의_이름은(String expectedName) {
        given()
        .when()
            .get("/api/products/{id}", state.getSavedId())
        .then()
            .statusCode(200)
            .body("name", equalTo(expectedName));
    }

    @And("해당 상품의 가격은 {int}이다")
    public void 상품의_가격은(int expectedPrice) {
        given()
        .when()
            .get("/api/products/{id}", state.getSavedId())
        .then()
            .statusCode(200)
            .body("price", equalTo(expectedPrice));
    }
}
