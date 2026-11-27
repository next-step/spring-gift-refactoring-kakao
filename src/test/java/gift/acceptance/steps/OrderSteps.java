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

public class OrderSteps {

    private final ScenarioContext context;

    public OrderSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("^상품 (\\d+)의 옵션 (\\d+) 재고는 (?:그대로 )?(\\d+)(?:이다|다)$")
    public void stockIs(long productId, int optionId, int expected) {
        JsonPath options = given().when()
                .get("/api/products/{productId}/options", productId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        Integer stock = options.param("optionId", optionId).get("find { it.id == optionId }.quantity");
        assertThat(stock).as("상품 %d의 옵션 %d 재고", productId, optionId).isEqualTo(expected);
    }

    @Given("^(user1|user2)의 주문 내역은 (?:그대로 )?(\\d+)건이다$")
    public void orderCountIs(String member, int expected) {
        int total = given().header("Authorization", context.authorizationOf(member))
                .when()
                .get("/api/orders")
                .then()
                .statusCode(200)
                .extract()
                .path("totalElements");

        assertThat(total).isEqualTo(expected);
    }

    @When("^(user1|user2)(?:이|가) 옵션 (\\d+)(?:을|를) 주문 수량 (\\d+)(?:으로|로) 주문한다$")
    public void order(String member, long optionId, int quantity) {
        placeOrder(member, Map.of("optionId", optionId, "quantity", quantity));
    }

    @When("^(user1|user2)(?:이|가) 옵션 (\\d+)(?:을|를) 주문 수량 (\\d+), 주문 메시지 \"([^\"]*)\"로 주문한다$")
    public void orderWithMessage(String member, long optionId, int quantity, String message) {
        placeOrder(member, Map.of("optionId", optionId, "quantity", quantity, "message", message));
    }

    @When("^틀린 로그인 토큰으로 옵션 (\\d+)(?:을|를) 주문 수량 (\\d+)(?:으로|로) 주문한다$")
    public void orderWithWrongToken(long optionId, int quantity) {
        context.setLastResponse(given().header("Authorization", "Bearer wrong-token")
                .contentType(ContentType.JSON)
                .body(Map.of("optionId", optionId, "quantity", quantity))
                .when()
                .post("/api/orders"));
    }

    @When("^로그인 토큰 헤더 없이 옵션 (\\d+)(?:을|를) 주문 수량 (\\d+)(?:으로|로) 주문한다$")
    public void orderWithoutHeader(long optionId, int quantity) {
        context.setLastResponse(given().contentType(ContentType.JSON)
                .body(Map.of("optionId", optionId, "quantity", quantity))
                .when()
                .post("/api/orders"));
    }

    @Then("^응답의 주문은 옵션 (\\d+), 주문 수량 (\\d+), 주문 메시지 \"([^\"]*)\"(?:이다|다)$")
    public void responseOrderIs(long optionId, int quantity, String message) {
        JsonPath body = context.lastResponse().jsonPath();
        assertThat(body.getLong("optionId")).isEqualTo(optionId);
        assertThat(body.getInt("quantity")).isEqualTo(quantity);
        assertThat(body.getString("message")).isEqualTo(message);
    }

    @When("^(user1|user2)(?:이|가) 자기 로그인 토큰으로 주문 내역을 조회한다$")
    public void fetchOrders(String member) {
        context.setLastResponse(given().header("Authorization", context.authorizationOf(member))
                .when()
                .get("/api/orders"));
    }

    @When("^틀린 로그인 토큰으로 주문 내역을 조회한다$")
    public void fetchOrdersWithWrongToken() {
        context.setLastResponse(
                given().header("Authorization", "Bearer wrong-token").when().get("/api/orders"));
    }

    @When("^로그인 토큰 헤더 없이 주문 내역을 조회한다$")
    public void fetchOrdersWithoutHeader() {
        context.setLastResponse(given().when().get("/api/orders"));
    }

    @Then("^응답의 주문 번호는 (\\d+(?:, \\d+)*)(?:이다|다)$")
    public void responseOrderIdsAre(String ids) {
        List<Integer> expected =
                Arrays.stream(ids.split(", ")).map(Integer::valueOf).toList();
        List<Integer> actual = context.lastResponse().jsonPath().getList("content.id", Integer.class);

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Then("^응답의 주문 (\\d+)(?:은|는) 옵션 (\\d+), 주문 수량 (\\d+), 주문 메시지 \"([^\"]*)\"(?:이다|다)$")
    public void responseOrderInListIs(int orderId, long optionId, int quantity, String message) {
        assertOrderInList(context.lastResponse().jsonPath(), orderId, optionId, quantity, message);
    }

    @Then("^(user1|user2)의 주문 내역에 응답의 주문이 옵션 (\\d+), 주문 수량 (\\d+), 주문 메시지 \"([^\"]*)\"로 있다$")
    public void orderFromResponseIsInHistory(String member, long optionId, int quantity, String message) {
        int orderId = context.lastResponse().jsonPath().getInt("id");

        JsonPath orders = given().header("Authorization", context.authorizationOf(member))
                .when()
                .get("/api/orders")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        assertOrderInList(orders, orderId, optionId, quantity, message);
    }

    private void assertOrderInList(JsonPath orders, int orderId, long optionId, int quantity, String message) {
        Map<String, Object> order = orders.param("orderId", orderId).get("content.find { it.id == orderId }");
        assertThat(order).as("주문 %d", orderId).isNotNull();
        assertThat(((Number) order.get("optionId")).longValue()).isEqualTo(optionId);
        assertThat(((Number) order.get("quantity")).intValue()).isEqualTo(quantity);
        assertThat(order.get("message")).isEqualTo(message);
    }

    private void placeOrder(String member, Map<String, Object> body) {
        context.setLastResponse(given().header("Authorization", context.authorizationOf(member))
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/orders"));
    }
}
