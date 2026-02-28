package gift.acceptance.steps;

import gift.acceptance.client.ApiClient;
import gift.acceptance.context.ContextKeys;
import gift.acceptance.context.ScenarioContext;
import gift.order.OrderRequest;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderApiSteps {

    private static final String
            BASE_URI = "/api/orders";

    private final ScenarioContext context;
    private final ApiClient apiClient;

    @When("인증된 사용자가 주문 생성 요청을 보낸다")
    public void createOrder(DataTable dataTable) {
        OrderRequest createRequest = toOrderRequest(dataTable);

        apiClient.authPost(BASE_URI, createRequest);
    }

    private OrderRequest toOrderRequest(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().getFirst();

        String optionId = row.getOrDefault("optionId", "0");
        String quantity = row.getOrDefault("quantity", "1");
        String message = row.getOrDefault("message", null);

        return new OrderRequest(
                context.resolvePlaceHolderId(optionId, ContextKeys.CURRENT_OPTION),
                Integer.parseInt(quantity),
                message
        );
    }

    @When("인증된 사용자가 수량 {int}으로 주문 생성 요청을 보낸다")
    public void createOrderWithQuantity(int quantity) {
        Long optionId = context.currentOptionId();

        OrderRequest body = new OrderRequest(optionId, quantity, null);

        apiClient.authPost(BASE_URI, body);
    }

    // --- Invalid token scenarios ---

    @When("인증된 사용자가 주문 목록을 페이지 {int} 사이즈 {int}로 조회 요청을 보낸다")
    public void getOrdersPaginated(int page, int size) {
        apiClient.authGet(BASE_URI, page, size);
    }

    @When("유효하지 않은 토큰으로 주문 생성 요청을 보낸다")
    public void createOrderInvalidToken(DataTable dataTable) {
        OrderRequest body = toOrderRequest(dataTable);

        apiClient.tokenPost("invalid-token", BASE_URI, body);
    }

    @When("유효하지 않은 토큰으로 주문 목록 조회 요청을 보낸다")
    public void getOrdersInvalidToken() {
        apiClient.tokenGet("invalid-token", BASE_URI);
    }
}
