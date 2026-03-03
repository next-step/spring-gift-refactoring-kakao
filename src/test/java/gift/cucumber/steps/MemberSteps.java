package gift.cucumber.steps;

import static io.restassured.RestAssured.given;

import gift.cucumber.SharedState;
import io.cucumber.java.ko.만일;
import org.springframework.beans.factory.annotation.Autowired;

public class MemberSteps {

    @Autowired
    private SharedState state;

    @만일("{string} 이메일과 {string} 비밀번호로 회원가입한다")
    public void 회원가입(String email, String password) {
        state.setResponse(
            given()
                .contentType("application/json")
                .body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
            .when()
                .post("/api/members/register")
        );
    }

    @만일("{string} 이메일과 {string} 비밀번호로 로그인한다")
    public void 로그인(String email, String password) {
        state.setResponse(
            given()
                .contentType("application/json")
                .body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
            .when()
                .post("/api/members/login")
        );
    }
}
