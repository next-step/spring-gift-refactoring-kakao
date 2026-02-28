package gift.acceptance.steps;

import static gift.acceptance.context.ScenarioContext.NON_EXISTENT_ID;

import gift.acceptance.client.ApiClient;
import gift.acceptance.context.ContextKeys;
import gift.acceptance.context.ScenarioContext;
import gift.product.ProductRequest;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProductApiSteps {

    private static final String
            BASE_URI = "/api/products";

    private final ScenarioContext context;
    private final ApiClient apiClient;

    @When("상품 생성 요청을 보낸다")
    public void createProduct(DataTable dataTable) {
        ProductRequest createRequest = toProductRequest(dataTable);

        apiClient.post(BASE_URI, createRequest);
    }

    private ProductRequest toProductRequest(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().getFirst();

        String name = row.getOrDefault("name", "");
        String imageUrl = row.getOrDefault("imageUrl", "");
        String price = row.getOrDefault("price", "0");
        String categoryId = row.getOrDefault("categoryId", "0");

        return new ProductRequest(
                name,
                Integer.parseInt(price),
                imageUrl,
                context.resolvePlaceHolderId(categoryId, ContextKeys.CURRENT_CATEGORY)
        );
    }

    @When("해당 상품 조회 요청을 보낸다")
    public void getProduct() {
        Long productId = context.currentProductId();

        String uri = BASE_URI + "/" + productId;

        apiClient.get(uri);
    }

    @When("존재하지 않는 상품 조회 요청을 보낸다")
    public void getProductNotFound() {
        String uri = BASE_URI + "/" + NON_EXISTENT_ID;

        apiClient.get(uri);
    }

    @When("상품 목록을 페이지 {int} 사이즈 {int}로 조회 요청을 보낸다")
    public void getProductsPaginated(int page, int size) {
        apiClient.get(BASE_URI, page, size);
    }

    @When("해당 상품 수정 요청을 보낸다")
    public void updateProduct(DataTable dataTable) {
        Long productId = context.currentProductId();

        ProductRequest updateRequest = toProductRequest(dataTable);

        String uri = BASE_URI + "/" + productId;

        apiClient.put(uri, updateRequest);
    }

    @When("존재하지 않는 상품 수정 요청을 보낸다")
    public void updateProductNotFound(DataTable dataTable) {
        ProductRequest updateRequest = toProductRequest(dataTable);

        String uri = BASE_URI + "/" + NON_EXISTENT_ID;

        apiClient.put(uri, updateRequest);
    }

    @When("해당 상품 삭제 요청을 보낸다")
    public void deleteProduct() {
        Long productId = context.currentProductId();

        String uri = BASE_URI + "/" + productId;

        apiClient.delete(uri);
    }
}
