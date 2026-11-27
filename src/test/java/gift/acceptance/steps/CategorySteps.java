package gift.acceptance.steps;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import gift.acceptance.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CategorySteps {

    private final ScenarioContext context;

    public CategorySteps(ScenarioContext context) {
        this.context = context;
    }

    @When("^비회원이 카테고리 목록을 조회한다$")
    public void fetchCategories() {
        context.setLastResponse(given().when().get("/api/categories"));
    }

    @Then("^응답의 카테고리 번호는 ([\\d, ]+?)(?:이다|다)$")
    public void responseCategoryIdsAre(String ids) {
        List<Integer> actual = context.lastResponse().jsonPath().getList("id", Integer.class);

        assertThat(actual).containsExactlyInAnyOrderElementsOf(parseIds(ids));
    }

    @Then("^응답의 카테고리 (\\d+)(?:은|는) 이름 \"([^\"]*)\", 색 \"([^\"]*)\", 이미지 URL \"([^\"]*)\", 설명 \"([^\"]*)\"(?:이다|다)$")
    public void responseCategoryInListIs(
            int categoryId, String name, String color, String imageUrl, String description) {
        Map<String, Object> category = context.lastResponse()
                .jsonPath()
                .param("categoryId", categoryId)
                .get("find { it.id == categoryId }");
        assertThat(category).as("카테고리 %d", categoryId).isNotNull();
        assertThat(category.get("name")).isEqualTo(name);
        assertThat(category.get("color")).isEqualTo(color);
        assertThat(category.get("imageUrl")).isEqualTo(imageUrl);
        assertThat(category.get("description")).isEqualTo(description);
    }

    @When("^로그인 토큰 없이 이름 \"([^\"]*)\", 색 \"([^\"]*)\", 이미지 URL \"([^\"]*)\", 설명 \"([^\"]*)\"(?:으로|로) 카테고리를 등록한다$")
    public void createCategoryWithoutLoginToken(String name, String color, String imageUrl, String description) {
        context.setLastResponse(given().contentType(ContentType.JSON)
                .body(categoryBody(name, color, imageUrl, description))
                .when()
                .post("/api/categories"));
    }

    @When(
            "^로그인 토큰 없이 카테고리 (\\d+)(?:을|를) 이름 \"([^\"]*)\", 색 \"([^\"]*)\", 이미지 URL \"([^\"]*)\", 설명 \"([^\"]*)\"(?:으로|로) 수정한다$")
    public void updateCategoryWithoutLoginToken(
            long id, String name, String color, String imageUrl, String description) {
        context.setLastResponse(given().contentType(ContentType.JSON)
                .body(categoryBody(name, color, imageUrl, description))
                .when()
                .put("/api/categories/{id}", id));
    }

    @When("^로그인 토큰 없이 카테고리 (\\d+)(?:을|를) 삭제한다$")
    public void deleteCategoryWithoutLoginToken(long id) {
        context.setLastResponse(given().when().delete("/api/categories/{id}", id));
    }

    @When("^로그인 토큰 없이 방금 등록한 카테고리를 삭제한다$")
    public void deleteCreatedCategoryWithoutLoginToken() {
        long createdCategoryId = context.lastResponse().jsonPath().getLong("id");
        context.setLastResponse(given().when().delete("/api/categories/{id}", createdCategoryId));
    }

    @Then("^응답의 카테고리 이름은 \"([^\"]*)\"(?:이다|다)$")
    public void responseCategoryNameIs(String name) {
        assertThat(context.lastResponse().jsonPath().getString("name")).isEqualTo(name);
    }

    @Then("^Location은 응답 카테고리 주소다$")
    public void locationIsResponseCategory() {
        long categoryId = context.lastResponse().jsonPath().getLong("id");

        assertThat(context.lastResponse().header("Location")).isEqualTo("/api/categories/" + categoryId);
    }

    @Then("^카테고리 목록에 응답의 카테고리가 이름 \"([^\"]*)\"(?:으로|로) 있다$")
    public void categoryFromResponseIsInList(String name) {
        int categoryId = context.lastResponse().jsonPath().getInt("id");

        Map<String, Object> category =
                categories().param("categoryId", categoryId).get("find { it.id == categoryId }");
        assertThat(category).as("카테고리 %d", categoryId).isNotNull();
        assertThat(category.get("name")).isEqualTo(name);
    }

    @Then("^카테고리 목록에서 카테고리 (\\d+)의 이름은 \"([^\"]*)\"(?:이다|다)$")
    public void categoryNameInListIs(int categoryId, String name) {
        Map<String, Object> category =
                categories().param("categoryId", categoryId).get("find { it.id == categoryId }");
        assertThat(category).as("카테고리 %d", categoryId).isNotNull();
        assertThat(category.get("name")).isEqualTo(name);
    }

    @Then("^카테고리 목록의 카테고리 번호는 (?:그대로 )?([\\d, ]+?)(?:이다|다)$")
    public void categoryIdsInListAre(String ids) {
        assertThat(categories().getList("id", Integer.class)).containsExactlyInAnyOrderElementsOf(parseIds(ids));
    }

    @Then("^카테고리 목록의 카테고리는 (\\d+)개다$")
    public void categoryCountIs(int expected) {
        assertThat(categories().getList("id")).hasSize(expected);
    }

    private JsonPath categories() {
        return given().when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
    }

    private Map<String, String> categoryBody(String name, String color, String imageUrl, String description) {
        return Map.of("name", name, "color", color, "imageUrl", imageUrl, "description", description);
    }

    private List<Integer> parseIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Integer::valueOf)
                .toList();
    }
}
