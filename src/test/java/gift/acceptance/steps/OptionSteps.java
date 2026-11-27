package gift.acceptance.steps;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import gift.acceptance.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OptionSteps {

    private final ScenarioContext context;

    public OptionSteps(ScenarioContext context) {
        this.context = context;
    }

    @When("^비회원이 상품 (\\d+)의 옵션 목록을 조회한다$")
    public void fetchOptions(long productId) {
        context.setLastResponse(given().when().get("/api/products/{productId}/options", productId));
    }

    @Then("^응답의 옵션 번호는 ([\\d, ]+?)(?:이다|다)$")
    public void responseOptionIdsAre(String ids) {
        List<Integer> actual = context.lastResponse().jsonPath().getList("id", Integer.class);

        assertThat(actual).containsExactlyInAnyOrderElementsOf(parseIds(ids));
    }

    @Then("^응답의 옵션 (\\d+)(?:은|는) 옵션명 \"([^\"]*)\", 재고 (\\d+)(?:이다|다)$")
    public void responseOptionIs(int optionId, String name, int stock) {
        Map<String, Object> option =
                context.lastResponse().jsonPath().param("optionId", optionId).get("find { it.id == optionId }");
        assertThat(option).as("옵션 %d", optionId).isNotNull();
        assertThat(option.get("name")).isEqualTo(name);
        assertThat(((Number) option.get("quantity")).intValue()).isEqualTo(stock);
    }

    @When("^로그인 토큰 없이 상품 (\\d+)에 옵션명 \"([^\"]*)\", 재고 (\\d+)(?:으로|로) 옵션을 등록한다$")
    public void createOptionWithoutLoginToken(long productId, String name, int stock) {
        context.setLastResponse(given().contentType(ContentType.JSON)
                .body(Map.of("name", name, "quantity", stock))
                .when()
                .post("/api/products/{productId}/options", productId));
    }

    @When("^로그인 토큰 없이 상품 (\\d+)의 옵션 (\\d+)(?:을|를) 삭제한다$")
    public void deleteOptionWithoutLoginToken(long productId, long optionId) {
        context.setLastResponse(
                given().when().delete("/api/products/{productId}/options/{optionId}", productId, optionId));
    }

    @Then("^응답의 옵션은 옵션명 \"([^\"]*)\", 재고 (\\d+)(?:이다|다)$")
    public void responseOptionIs(String name, int stock) {
        JsonPath body = context.lastResponse().jsonPath();
        assertThat(body.getString("name")).isEqualTo(name);
        assertThat(body.getInt("quantity")).isEqualTo(stock);
    }

    @Then("^Location은 상품 (\\d+)의 응답 옵션 주소다$")
    public void locationIsResponseOption(long productId) {
        long optionId = context.lastResponse().jsonPath().getLong("id");

        assertThat(context.lastResponse().header("Location"))
                .isEqualTo("/api/products/" + productId + "/options/" + optionId);
    }

    @Then("^상품 (\\d+)의 옵션 목록에 응답의 옵션이 옵션명 \"([^\"]*)\"(?:으로|로) 있다$")
    public void optionFromResponseIsInList(long productId, String name) {
        int optionId = context.lastResponse().jsonPath().getInt("id");

        Map<String, Object> option =
                optionsOf(productId).param("optionId", optionId).get("find { it.id == optionId }");
        assertThat(option).as("옵션 %d", optionId).isNotNull();
        assertThat(option.get("name")).isEqualTo(name);
    }

    @Given("^상품 (\\d+)의 옵션 번호는 (?:그대로 )?([\\d, ]+?)(?:이다|다)$")
    public void optionIdsOfProductAre(long productId, String ids) {
        List<Integer> actual = optionsOf(productId).getList("id", Integer.class);

        assertThat(actual).containsExactlyInAnyOrderElementsOf(parseIds(ids));
    }

    @Then("^상품 (\\d+)의 옵션은 (\\d+)개다$")
    public void optionCountOfProductIs(long productId, int expected) {
        assertThat(optionsOf(productId).getList("id")).hasSize(expected);
    }

    private JsonPath optionsOf(long productId) {
        return given().when()
                .get("/api/products/{productId}/options", productId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
    }

    private List<Integer> parseIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Integer::valueOf)
                .toList();
    }
}
