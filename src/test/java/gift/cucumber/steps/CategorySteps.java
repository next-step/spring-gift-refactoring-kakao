package gift.cucumber.steps;

import gift.cucumber.SharedState;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CategorySteps {

    private final SharedState state;

    public CategorySteps(SharedState state) {
        this.state = state;
    }

    @Given("{string} 카테고리가 등록되어 있다")
    public void 카테고리가_등록되어_있다(String name) {
        Long id =
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "name", name,
                "color", "#000000",
                "imageUrl", "https://example.com/image.jpg",
                "description", "test"
            ))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
        state.setCategoryId(id);
        state.putCategoryId(name, id);
    }

    @When("{string} 이름으로 카테고리를 생성하면")
    public void 카테고리를_생성하면(String name) {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "name", name,
                "color", "#1E90FF",
                "imageUrl", "https://example.com/drink.jpg",
                "description", "음료 카테고리"
            ))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(201);
    }

    @When("카테고리 목록을 조회하면")
    public void 카테고리_목록을_조회하면() {
        // 조회는 그러면 단계에서 수행
    }

    @When("해당 카테고리의 이름을 {string}로 변경하면")
    public void 카테고리_이름을_변경하면(String newName) {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "name", newName,
                "color", "#FFFFFF",
                "imageUrl", "https://example.com/updated.jpg",
                "description", "수정된 설명"
            ))
        .when()
            .put("/api/categories/{id}", state.getCategoryId())
        .then()
            .statusCode(200);
    }

    @When("{string} 카테고리를 삭제하면")
    public void 카테고리를_삭제하면(String name) {
        given()
        .when()
            .delete("/api/categories/{id}", state.getCategoryIdByName(name))
        .then()
            .statusCode(204);
    }

    @Then("카테고리 목록의 크기는 {int}이다")
    public void 카테고리_목록의_크기는(int size) {
        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body(".", hasSize(size));
    }

    @And("카테고리 목록에 {string} 이름이 포함되어 있다")
    public void 카테고리_목록에_이름이_포함되어_있다(String name) {
        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body("name", hasItem(name));
    }

    @And("카테고리 목록에 {string}, {string}, {string} 이름이 모두 포함되어 있다")
    public void 카테고리_목록에_이름들이_모두_포함되어_있다(String name1, String name2, String name3) {
        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body("name", hasItems(name1, name2, name3));
    }
}
