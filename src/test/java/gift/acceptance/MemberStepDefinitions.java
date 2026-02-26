package gift.acceptance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gift.auth.JwtProvider;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import java.util.Map;

public class MemberStepDefinitions {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AcceptanceTestContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    @Given("이메일 {string}, 비밀번호 {string}로 가입한 회원이 있고")
    public void 가입한_회원이_있고(String email, String password) {
        new SimpleJdbcInsert(jdbcTemplate)
            .withTableName("member")
            .usingGeneratedKeyColumns("id")
            .execute(Map.of("email", email, "password", password, "point", 0));
    }

    @Given("이메일 {string}, 비밀번호 {string}로 로그인되어 있고")
    public void 로그인되어_있고(String email, String password) {
        context.setToken(jwtProvider.createToken(email));
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
        context.setToken(extractToken(response.getBody()));
    }

    private String extractToken(String body) {
        try {
            Map<String, Object> map = objectMapper.readValue(body, new TypeReference<>() {});
            return (String) map.get("token");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
