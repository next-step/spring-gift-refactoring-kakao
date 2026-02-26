package gift.e2e;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class E2eMemberStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @먼저("{string} 이메일로 가입된 회원이 존재한다")
    public void 이메일로_가입된_회원이_존재한다(String email) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "password"))
                .when()
                .post("/api/members/register")
                .then()
                .extract();
        assertThat(response.statusCode()).isEqualTo(201);
    }

    @만일("{string} 이메일로 회원 가입하면")
    public void 이메일로_회원_가입하면(String email) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "password"))
                .when()
                .post("/api/members/register")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("{string} 이메일로 로그인하면")
    public void 이메일로_로그인하면(String email) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "password"))
                .when()
                .post("/api/members/login")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("{string} 이메일로 잘못된 비밀번호로 로그인하면")
    public void 이메일로_잘못된_비밀번호로_로그인하면(String email) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "wrongpassword"))
                .when()
                .post("/api/members/login")
                .then()
                .extract();
        context.setResponse(response);
    }

    @그러면("회원 가입에 성공한다")
    public void 회원_가입에_성공한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(201);
    }

    @그러면("회원 가입에 실패한다")
    public void 회원_가입에_실패한다() {
        assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(400);
    }

    @그러면("토큰이 발급된다")
    public void 토큰이_발급된다() {
        assertThat(context.getResponse().jsonPath().getString("token")).isNotBlank();
    }

    @그러면("로그인에 성공한다")
    public void 로그인에_성공한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(200);
    }

    @그러면("로그인에 실패한다")
    public void 로그인에_실패한다() {
        assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(400);
    }
}
