package gift.acceptance.steps;

import static gift.acceptance.ScenarioContext.amount;
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

public class WishSteps {

    private final ScenarioContext context;

    public WishSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("^(user1|user2)의 위시 번호는 (?:그대로 )?(\\d+(?:, \\d+)*)(?:이다|다)$")
    public void wishIdsAre(String member, String ids) {
        List<Integer> expected =
                Arrays.stream(ids.split(", ")).map(Integer::valueOf).toList();

        List<Integer> actual = given().header("Authorization", context.authorizationOf(member))
                .when()
                .get("/api/wishes")
                .then()
                .statusCode(200)
                .extract()
                .path("content.id");

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @When("^(user1|user2)(?:이|가) 자기 로그인 토큰으로 위시 (\\d+)(?:을|를) 삭제한다$")
    public void deleteWish(String member, long wishId) {
        context.setLastResponse(given().header("Authorization", context.authorizationOf(member))
                .when()
                .delete("/api/wishes/{id}", wishId));
    }

    @When("^(user1|user2)(?:이|가) 자기 로그인 토큰으로 위시 목록을 조회한다$")
    public void fetchWishes(String member) {
        context.setLastResponse(given().header("Authorization", context.authorizationOf(member))
                .when()
                .get("/api/wishes"));
    }

    @When("^틀린 로그인 토큰으로 위시 목록을 조회한다$")
    public void fetchWishesWithWrongToken() {
        context.setLastResponse(
                given().header("Authorization", "Bearer wrong-token").when().get("/api/wishes"));
    }

    @When("^로그인 토큰 헤더 없이 위시 목록을 조회한다$")
    public void fetchWishesWithoutHeader() {
        context.setLastResponse(given().when().get("/api/wishes"));
    }

    @When("^(user1|user2)(?:이|가) 상품 (\\d+)(?:을|를) 위시에 담는다$")
    public void addWish(String member, long productId) {
        context.setLastResponse(given().header("Authorization", context.authorizationOf(member))
                .contentType(ContentType.JSON)
                .body(Map.of("productId", productId))
                .when()
                .post("/api/wishes"));
    }

    @Then("^응답의 위시는 상품 (\\d+), 상품명 \"([^\"]*)\", 가격 ([\\d,]+)(?:이다|다)$")
    public void responseWishIs(long productId, String name, String price) {
        JsonPath body = context.lastResponse().jsonPath();
        assertThat(body.getLong("productId")).isEqualTo(productId);
        assertThat(body.getString("name")).isEqualTo(name);
        assertThat(body.getInt("price")).isEqualTo(amount(price));
    }

    @Then("^응답은 위시 (\\d+)(?:이다|다)$")
    public void responseIsWish(int wishId) {
        assertThat(context.lastResponse().jsonPath().getInt("id")).isEqualTo(wishId);
    }

    @Then("^(user1|user2)의 위시 목록에 응답의 위시가 상품 (\\d+)(?:으로|로) 있다$")
    public void wishFromResponseIsInList(String member, long productId) {
        int wishId = context.lastResponse().jsonPath().getInt("id");

        JsonPath wishes = given().header("Authorization", context.authorizationOf(member))
                .when()
                .get("/api/wishes")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        Map<String, Object> wish = wishes.param("wishId", wishId).get("content.find { it.id == wishId }");
        assertThat(wish).as("위시 %d", wishId).isNotNull();
        assertThat(((Number) wish.get("productId")).longValue()).isEqualTo(productId);
    }

    @Given("^(user1|user2)의 위시는 (?:그대로 )?(\\d+)개다$")
    public void wishCountIs(String member, int expected) {
        int total = given().header("Authorization", context.authorizationOf(member))
                .when()
                .get("/api/wishes")
                .then()
                .statusCode(200)
                .extract()
                .path("totalElements");

        assertThat(total).isEqualTo(expected);
    }

    @Then("^응답의 위시 번호는 (\\d+(?:, \\d+)*)(?:이다|다)$")
    public void responseWishIdsAre(String ids) {
        List<Integer> expected =
                Arrays.stream(ids.split(", ")).map(Integer::valueOf).toList();
        List<Integer> actual = context.lastResponse().jsonPath().getList("content.id", Integer.class);

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Then("^응답의 위시 (\\d+)(?:은|는) 상품 (\\d+), 상품명 \"([^\"]*)\", 가격 ([\\d,]+)(?:이다|다)$")
    public void responseWishInListIs(int wishId, long productId, String name, String price) {
        Map<String, Object> wish =
                context.lastResponse().jsonPath().param("wishId", wishId).get("content.find { it.id == wishId }");
        assertThat(wish).as("위시 %d", wishId).isNotNull();
        assertThat(((Number) wish.get("productId")).longValue()).isEqualTo(productId);
        assertThat(wish.get("name")).isEqualTo(name);
        assertThat(((Number) wish.get("price")).intValue()).isEqualTo(amount(price));
    }
}
