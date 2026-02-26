package gift.acceptance;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.Map;

public class MemberStepDefinitions {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AcceptanceTestContext context;

    @Given("이메일 {string}, 비밀번호 {string}로 가입한 회원이 있고")
    public void 가입한_회원이_있고(String email, String password) {
        restTemplate.postForEntity(
            "/api/members/register",
            Map.of("email", email, "password", password),
            String.class
        );
    }

    @When("이메일 {string}, 비밀번호 {string}로 회원 가입을 요청하면")
    public void 회원_가입을_요청하면(String email, String password) {
        var response = restTemplate.postForEntity(
            "/api/members/register",
            Map.of("email", email, "password", password),
            String.class
        );
        context.setResponse(response);
    }

    @When("이메일 {string}, 비밀번호 {string}로 로그인을 요청하면")
    public void 로그인을_요청하면(String email, String password) {
        var response = restTemplate.postForEntity(
            "/api/members/login",
            Map.of("email", email, "password", password),
            String.class
        );
        context.setResponse(response);
    }
}
