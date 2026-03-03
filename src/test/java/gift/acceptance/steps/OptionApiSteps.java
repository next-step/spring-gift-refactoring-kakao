package gift.acceptance.steps;

import static gift.acceptance.context.ScenarioContext.NON_EXISTENT_ID;

import gift.acceptance.client.ApiClient;
import gift.acceptance.context.ScenarioContext;
import gift.option.internal.OptionRequest;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OptionApiSteps {

    private static final String
            BASE_PRODUCT_URI = "/api/products";

    private final ScenarioContext context;
    private final ApiClient apiClient;

    @When("해당 상품에 옵션 추가 요청을 보낸다")
    public void createOption(DataTable dataTable) {
        Long productId = context.currentProductId();
        OptionRequest createRequest = toOptionRequest(dataTable);

        String uri = baseOptionUri(productId);

        apiClient.post(uri, createRequest);
    }

    private OptionRequest toOptionRequest(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().getFirst();

        String name = row.getOrDefault("name", "");
        String quantity = row.getOrDefault("quantity", "1");

        return new OptionRequest(
                name,
                Integer.parseInt(quantity)
        );
    }

    private static String baseOptionUri(long productId) {
        return BASE_PRODUCT_URI + "/" + productId + "/options";
    }

    @When("존재하지 않는 상품에 옵션 추가 요청을 보낸다")
    public void createOptionForNonExistentProduct(DataTable dataTable) {
        OptionRequest createRequest = toOptionRequest(dataTable);

        String uri = baseOptionUri(NON_EXISTENT_ID);

        apiClient.post(uri, createRequest);
    }

    @When("해당 상품의 옵션 목록 조회 요청을 보낸다")
    public void getOptions() {
        Long productId = context.currentProductId();

        apiClient.get(baseOptionUri(productId));
    }

    @When("존재하지 않는 상품의 옵션 목록 조회 요청을 보낸다")
    public void getOptionsForNonExistentProduct() {
        String uri = baseOptionUri(NON_EXISTENT_ID);

        apiClient.get(uri);
    }

    @When("해당 상품에서 {string} 옵션 삭제 요청을 보낸다")
    public void deleteOptionByName(String optionName) {
        Long productId = context.currentProductId();
        Long optionId = context.getId(optionName);

        String uri = buildUri(productId, optionId);

        apiClient.delete(uri);
    }

    private static String buildUri(long productId, long optionId) {
        String optionUri = baseOptionUri(productId);
        return optionUri + "/" + optionId;
    }

    @When("해당 상품의 현재 옵션 삭제 요청을 보낸다")
    public void deleteOption() {
        Long productId = context.currentProductId();
        Long optionId = context.currentOptionId();

        String uri = buildUri(productId, optionId);

        apiClient.delete(uri);
    }

    @When("해당 상품에서 존재하지 않는 옵션 삭제 요청을 보낸다")
    public void deleteOptionNotFound() {
        Long productId = context.currentProductId();

        String uri = buildUri(productId, NON_EXISTENT_ID);

        apiClient.delete(uri);
    }

    @When("{string} 상품에서 {string} 옵션 삭제 요청을 보낸다")
    public void deleteCrossProductOption(String productName, String optionName) {
        Long productId = context.getId(productName);
        Long optionId = context.getId(optionName);

        String uri = buildUri(productId, optionId);

        apiClient.delete(uri);
    }
}
