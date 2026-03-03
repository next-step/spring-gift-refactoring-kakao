package gift.cucumber.steps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import gift.cucumber.SharedState;
import io.cucumber.java.Before;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.먼저;
import io.cucumber.java.ko.그러면;
import org.springframework.beans.factory.annotation.Autowired;

public class CommonSteps {

    @Autowired
    private SharedState state;

    @Before
    public void resetState() {
        state.reset();
    }

    @먼저("{string} 이메일과 {string} 비밀번호로 로그인되어 있다")
    public void 로그인되어_있다(String email, String password) {
        String token = given()
            .contentType("application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
        .when()
            .post("/api/members/login")
        .then()
            .statusCode(200)
            .extract().jsonPath().getString("token");
        state.setToken(token);
    }

    @먼저("{string} 이메일과 {string} 비밀번호로 회원가입되어 있다")
    public void 회원가입되어_있다(String email, String password) {
        String token = given()
            .contentType("application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("token");
        state.setToken(token);
    }

    @먼저("유효하지 않은 토큰으로 인증한다")
    public void 유효하지_않은_토큰() {
        state.setToken("invalid-token");
    }

    @그러면("응답 코드는 {int}이다")
    public void 응답_코드_검증(int statusCode) {
        state.getResponse().then().statusCode(statusCode);
    }

    @그리고("응답에 토큰이 포함되어 있다")
    public void 응답에_토큰_포함() {
        state.getResponse().then().body("token", notNullValue());
    }
}
