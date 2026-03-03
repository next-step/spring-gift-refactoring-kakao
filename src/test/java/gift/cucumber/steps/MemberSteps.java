package gift.cucumber.steps;

import gift.cucumber.SharedState;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.http.MediaType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MemberSteps {

    private final SharedState state;
    private Response lastResponse;

    public MemberSteps(SharedState state) {
        this.state = state;
    }

    @Given("{string} 이메일과 {string} 비밀번호로 회원이 등록되어 있다")
    public void 회원이_등록되어_있다(String email, String password) {
        String token =
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "email", email,
                "password", password
            ))
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("token");
        state.setToken(token);
    }

    @When("{string} 이메일과 {string} 비밀번호로 회원가입하면")
    public void 회원가입하면(String email, String password) {
        lastResponse = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "email", email,
                "password", password
            ))
        .when()
            .post("/api/members/register");
        state.setStatusCode(lastResponse.statusCode());
    }

    @When("{string} 이메일과 {string} 비밀번호로 로그인하면")
    public void 로그인하면(String email, String password) {
        lastResponse = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "email", email,
                "password", password
            ))
        .when()
            .post("/api/members/login");
        state.setStatusCode(lastResponse.statusCode());
    }

    @Then("응답 코드는 {int}이다")
    public void 응답_코드는(int expectedStatusCode) {
        assertEquals(expectedStatusCode, state.getStatusCode());
    }

    @And("응답에 토큰이 포함되어 있다")
    public void 응답에_토큰이_포함되어_있다() {
        lastResponse.then().body("token", notNullValue());
    }
}
