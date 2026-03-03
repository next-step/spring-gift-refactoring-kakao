package gift.cucumber.steps;

import gift.cucumber.SharedState;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

public class WishSteps {

    private final SharedState state;

    public WishSteps(SharedState state) {
        this.state = state;
    }

    @When("해당 상품을 위시리스트에 추가하면")
    public void 상품을_위시리스트에_추가하면() {
        Long wishId = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header("Authorization", "Bearer " + state.getToken())
            .body(Map.of("productId", state.getSavedId()))
        .when()
            .post("/api/wishes")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
        state.setWishId(wishId);
    }

    @When("마지막 위시를 삭제하면")
    public void 마지막_위시를_삭제하면() {
        given()
            .header("Authorization", "Bearer " + state.getToken())
        .when()
            .delete("/api/wishes/{id}", state.getWishId())
        .then()
            .statusCode(204);
    }

    @When("인증 없이 위시리스트를 조회하면")
    public void 인증_없이_위시리스트를_조회하면() {
        state.setResponse(given()
        .when()
            .get("/api/wishes"));
    }

    @Then("위시리스트의 크기는 {int}이다")
    public void 위시리스트의_크기는(int size) {
        given()
            .header("Authorization", "Bearer " + state.getToken())
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/wishes")
        .then()
            .statusCode(200)
            .body("content", hasSize(size));
    }
}
