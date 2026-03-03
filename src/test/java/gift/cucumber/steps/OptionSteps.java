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

public class OptionSteps {

    @Autowired
    private SharedState state;

    @Given("{string} 옵션이 {int} 수량으로 등록되어 있다")
    public void 옵션이_등록되어_있다(String name, int quantity) {
        Long id = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of("name", name, "quantity", quantity))
        .when()
            .post("/api/products/{productId}/options", state.getSavedId())
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
        state.setOptionId(id);
    }

    @When("{string} 이름과 {int} 수량으로 옵션을 추가하면")
    public void 옵션을_추가하면(String name, int quantity) {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of("name", name, "quantity", quantity))
        .when()
            .post("/api/products/{productId}/options", state.getSavedId())
        .then()
            .statusCode(201);
    }

    @When("해당 상품의 옵션 목록을 조회하면")
    public void 옵션_목록을_조회하면() {
    }

    @When("해당 옵션을 삭제하면")
    public void 해당_옵션을_삭제하면() {
        state.setResponse(given()
        .when()
            .delete("/api/products/{productId}/options/{optionId}",
                state.getSavedId(), state.getOptionId()));
    }

    @Then("옵션 목록의 크기는 {int}이다")
    public void 옵션_목록의_크기는(int size) {
        given()
        .when()
            .get("/api/products/{productId}/options", state.getSavedId())
        .then()
            .statusCode(200)
            .body(".", hasSize(size));
    }

    @And("옵션 목록에 {string} 이름이 포함되어 있다")
    public void 옵션_목록에_이름이_포함되어_있다(String name) {
        given()
        .when()
            .get("/api/products/{productId}/options", state.getSavedId())
        .then()
            .statusCode(200)
            .body("name", hasItem(name));
    }

    @And("옵션 목록에 {string}, {string}, {string} 이름이 모두 포함되어 있다")
    public void 옵션_목록에_이름들이_모두_포함되어_있다(String name1, String name2, String name3) {
        given()
        .when()
            .get("/api/products/{productId}/options", state.getSavedId())
        .then()
            .statusCode(200)
            .body("name", hasItems(name1, name2, name3));
    }
}
