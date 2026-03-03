package gift.cucumber.steps;

import gift.cucumber.SharedState;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class CommonSteps {

    @Autowired
    private SharedState state;

    @Before
    public void resetState() {
        state.reset();
    }

    @Given("{string} 이메일과 {string} 비밀번호로 회원이 등록되어 있다")
    public void 회원이_등록되어_있다(String email, String password) {
        String token = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of("email", email, "password", password))
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("token");
        state.setToken(token);
    }

    @Then("응답 코드는 {int}이다")
    public void 응답_코드는(int expectedStatusCode) {
        state.getResponse().then().statusCode(expectedStatusCode);
    }

    @And("응답에 토큰이 포함되어 있다")
    public void 응답에_토큰이_포함되어_있다() {
        state.getResponse().then().body("token", notNullValue());
    }
}
