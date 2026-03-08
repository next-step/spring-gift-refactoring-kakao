package gift.cucumber.steps;

import static gift.cucumber.support.ApiClient.*;
import static org.assertj.core.api.Assertions.assertThat;

import gift.cucumber.ScenarioContext;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

public class MemberStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @조건("{string} 이메일로 가입된 회원이 있다")
    public void 이메일로_가입된_회원이_있다(String email) {
        String body =
                """
                {
                    "email": "%s",
                    "password": "password"
                }
                """
                        .formatted(email);

        post("/api/members/register", body);
    }

    @조건("{string} 이메일과 {string} 비밀번호로 가입된 회원이 있다")
    public void 이메일과_비밀번호로_가입된_회원이_있다(String email, String password) {
        String body =
                """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """
                        .formatted(email, password);

        post("/api/members/register", body);
    }

    @만일("{string} 이메일과 {string} 비밀번호로 회원가입한다")
    public void 이메일과_비밀번호로_회원가입한다(String email, String password) {
        String body =
                """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """
                        .formatted(email, password);

        ExtractableResponse<Response> response = post("/api/members/register", body);

        context.setResponse(response);
    }

    @만일("{string} 이메일과 {string} 비밀번호로 로그인한다")
    public void 이메일과_비밀번호로_로그인한다(String email, String password) {
        String body =
                """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """
                        .formatted(email, password);

        ExtractableResponse<Response> response = post("/api/members/login", body);

        context.setResponse(response);
    }

    @그러면("회원가입이 성공한다")
    public void 회원가입이_성공한다() {
        assertThat(context.getStatusCode()).isEqualTo(201);
    }

    @그러면("회원가입이 실패한다")
    public void 회원가입이_실패한다() {
        assertThat(context.getStatusCode()).isEqualTo(400);
    }

    @그러면("로그인이 성공한다")
    public void 로그인이_성공한다() {
        assertThat(context.getStatusCode()).isEqualTo(200);
    }

    @그러면("로그인이 실패한다")
    public void 로그인이_실패한다() {
        assertThat(context.getStatusCode()).isEqualTo(400);
    }

    @그리고("응답에 토큰이 포함되어 있다")
    public void 응답에_토큰이_포함되어_있다() {
        String token = context.getResponse().jsonPath().getString("token");
        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
    }
}
