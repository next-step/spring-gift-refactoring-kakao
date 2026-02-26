package gift.cucumber.steps;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만약;
import io.cucumber.java.ko.조건;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class MemberStepDefinitions {

    private static final String TEST_PASSWORD = "password1234";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Response lastResponse;

    @조건("{string} 회원이 존재한다")
    public void 회원이_존재한다(String email) {
        jdbcTemplate.update(
            "INSERT INTO member (email, password, point) VALUES (?, ?, ?)",
            email, TEST_PASSWORD, 0
        );
    }

    @만약("{string} 이메일과 {string} 비밀번호로 회원가입한다")
    public void 이메일과_비밀번호로_회원가입한다(String email, String password) {
        lastResponse = given()
            .contentType("application/json")
            .body("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password))
            .when()
            .post("/api/members/register");
    }

    @만약("{string} 이메일과 {string} 비밀번호로 로그인한다")
    public void 이메일과_비밀번호로_로그인한다(String email, String password) {
        lastResponse = given()
            .contentType("application/json")
            .body("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password))
            .when()
            .post("/api/members/login");
    }

    @그러면("회원가입이 성공한다")
    public void 회원가입이_성공한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(201);
    }

    @그러면("회원가입이 실패한다")
    public void 회원가입이_실패한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(400);
    }

    @그러면("로그인이 성공한다")
    public void 로그인이_성공한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(200);
    }

    @그러면("로그인이 실패한다")
    public void 로그인이_실패한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(400);
    }

    @그러면("응답에 토큰이 포함되어 있다")
    public void 응답에_토큰이_포함되어_있다() {
        String token = lastResponse.jsonPath().getString("token");
        assertThat(token).isNotNull().isNotBlank();
    }
}
