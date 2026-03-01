package gift.acceptance.steps;

import static gift.acceptance.context.ScenarioContext.NON_EXISTENT_ID;

import gift.acceptance.client.ApiClient;
import gift.acceptance.context.ScenarioContext;
import gift.category.internal.CategoryRequest;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CategoryApiSteps {

    private static final String BASE_URI = "/api/categories";

    private final ScenarioContext context;
    private final ApiClient apiClient;

    @When("카테고리 생성 요청을 보낸다")
    public void createCategory(DataTable dataTable) {
        apiClient.post(
                BASE_URI,
                toCategoryRequest(dataTable)
        );
    }

    private CategoryRequest toCategoryRequest(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().getFirst();

        return new CategoryRequest(
                row.getOrDefault("name", ""),
                row.getOrDefault("color", ""),
                row.getOrDefault("imageUrl", ""),
                row.getOrDefault("description", null)
        );
    }

    @When("카테고리 목록 조회 요청을 보낸다")
    public void getCategories() {
        apiClient.get(BASE_URI);
    }

    @When("해당 카테고리 수정 요청을 보낸다")
    public void updateCategory(DataTable dataTable) {
        String uri = BASE_URI + "/" + context.currentCategoryId();
        CategoryRequest updateRequest = toCategoryRequest(dataTable);

        apiClient.put(uri, updateRequest);
    }

    @When("해당 카테고리 삭제 요청을 보낸다")
    public void deleteCategory() {
        String uri = BASE_URI + "/" + context.currentCategoryId();

        apiClient.delete(uri);
    }

    @When("존재하지 않는 카테고리 수정 요청을 보낸다")
    public void updateCategoryNotFound(DataTable dataTable) {
        String uri = BASE_URI + "/" + NON_EXISTENT_ID;
        CategoryRequest updateRequest = toCategoryRequest(dataTable);

        apiClient.put(uri, updateRequest);
    }
}
