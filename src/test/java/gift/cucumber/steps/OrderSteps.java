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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class OrderSteps {

    @Autowired
    private SharedState state;

    @Given("해당 회원에게 {int} 포인트가 충전되어 있다")
    public void 포인트가_충전되어_있다(int amount) {
        given()
            .param("amount", amount)
        .when()
            .post("/admin/members/{id}/charge-point", 1)
        .then()
            .statusCode(302);
    }

    @When("{int} 수량과 {string} 메시지로 주문하면")
    public void 주문하면(int quantity, String message) {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header("Authorization", "Bearer " + state.getToken())
            .body(Map.of(
                "optionId", state.getOptionId(),
                "quantity", quantity,
                "message", message
            ))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201);
    }

    @When("인증 없이 주문 목록을 조회하면")
    public void 인증_없이_주문_목록을_조회하면() {
        state.setResponse(given()
        .when()
            .get("/api/orders"));
    }

    @Then("주문 목록의 크기는 {int}이다")
    public void 주문_목록의_크기는(int size) {
        given()
            .header("Authorization", "Bearer " + state.getToken())
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/orders")
        .then()
            .statusCode(200)
            .body("content", hasSize(size));
    }

    @And("주문 목록의 첫 번째 주문 수량은 {int}이다")
    public void 주문_수량은(int quantity) {
        given()
            .header("Authorization", "Bearer " + state.getToken())
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/orders")
        .then()
            .statusCode(200)
            .body("content[0].quantity", equalTo(quantity));
    }

    @And("주문 목록의 첫 번째 주문 메시지는 {string}이다")
    public void 주문_메시지는(String message) {
        given()
            .header("Authorization", "Bearer " + state.getToken())
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/orders")
        .then()
            .statusCode(200)
            .body("content[0].message", equalTo(message));
    }
}
