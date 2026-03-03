package gift.cucumber.steps;

import gift.cucumber.SharedState;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class MemberSteps {

    private final SharedState state;

    public MemberSteps(SharedState state) {
        this.state = state;
    }

    @When("{string} 이메일과 {string} 비밀번호로 회원가입하면")
    public void 회원가입하면(String email, String password) {
        state.setResponse(given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of("email", email, "password", password))
        .when()
            .post("/api/members/register"));
    }

    @When("{string} 이메일과 {string} 비밀번호로 로그인하면")
    public void 로그인하면(String email, String password) {
        state.setResponse(given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of("email", email, "password", password))
        .when()
            .post("/api/members/login"));
    }
}
