package gift.acceptance.steps;

import static gift.acceptance.ScenarioContext.amount;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import gift.acceptance.Members;
import gift.acceptance.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

public class MemberSteps {

    private final ScenarioContext context;
    private final JdbcTemplate jdbcTemplate;

    public MemberSteps(ScenarioContext context, JdbcTemplate jdbcTemplate) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 인수 테스트에서 DB를 읽는 곳은 여기(포인트)와 OrderNotificationSteps(주문 알림 상태)다.
     * 회원이 자기 포인트를 조회하는 API가 없어서(open-questions 4번) 포인트만 DB에서 확인한다.
     * 관리자 화면을 쓰지 않는 이유는 docs/work/02-acceptance-test/test-strategy.md에 있다.
     */
    @Given("^(user1|user2)의 포인트는 (?:그대로 )?([\\d,]+)이다$")
    public void pointIs(String member, String expected) {
        Integer point = jdbcTemplate.queryForObject(
                "select point from member where email = ?", Integer.class, Members.email(member));
        assertThat(point).isEqualTo(amount(expected));
    }

    /**
     * 회원 목록을 보는 회원용 API가 없어서, 그 이메일로 로그인이 안 되는지(400)로 확인한다.
     */
    @Given("^이메일 \"([^\"]*)\"(?:으로|로) 가입한 회원은 없다$")
    public void noMemberWithEmail(String email) {
        assertThat(login(email, "not-a-registered-password").statusCode()).isEqualTo(400);
    }

    @When("^비회원이 이메일 \"([^\"]*)\", 비밀번호 \"([^\"]*)\"로 회원 가입한다$")
    public void register(String email, String password) {
        context.setLastResponse(given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/api/members/register"));
    }

    @When("^비회원이 이메일 \"([^\"]*)\", 비밀번호 \"([^\"]*)\"로 로그인한다$")
    public void loginAsGuest(String email, String password) {
        context.setLastResponse(login(email, password));
    }

    @Then("^응답에 로그인 토큰이 있다$")
    public void responseHasLoginToken() {
        assertThat(context.lastResponse().jsonPath().getString("token")).isNotBlank();
    }

    @Then("^응답의 로그인 토큰으로 위시 목록을 조회하면 위시가 (\\d+)개다$")
    public void wishCountWithResponseToken(int expected) {
        String token = context.lastResponse().jsonPath().getString("token");

        int total = given().header("Authorization", "Bearer " + token)
                .when()
                .get("/api/wishes")
                .then()
                .statusCode(200)
                .extract()
                .path("totalElements");

        assertThat(total).isEqualTo(expected);
    }

    @Given("^이메일 \"([^\"]*)\", 비밀번호 \"([^\"]*)\"로 로그인하면 (\\d+)이 온다$")
    public void loginStatusIs(String email, String password, int expected) {
        assertThat(login(email, password).statusCode()).isEqualTo(expected);
    }

    private Response login(String email, String password) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/api/members/login");
    }
}
