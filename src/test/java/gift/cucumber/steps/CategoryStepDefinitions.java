package gift.cucumber.steps;

import static org.assertj.core.api.Assertions.assertThat;

import gift.cucumber.ScenarioContext;
import gift.fixture.CategoryFixture;
import gift.support.TestDataInitializer;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

public class CategoryStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private TestDataInitializer initializer;

    @조건("{string} 카테고리가 등록되어 있다")
    public void 카테고리가_등록되어_있다(String name) {
        Long categoryId = initializer.saveCategory(CategoryFixture.카테고리(name));
        context.setCategoryId(categoryId);
    }

    @만일("{string} 카테고리를 등록한다")
    public void 카테고리를_등록한다(String name) {
        String body =
                """
                {
                    "name": "%s",
                    "color": "#000000",
                    "imageUrl": "https://example.com/default.png"
                }
                """
                        .formatted(name);

        var response = RestAssured.given()
                .log()
                .all()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/categories")
                .then()
                .log()
                .all()
                .extract();

        context.setResponse(response);
        if (response.statusCode() == 201) {
            context.setCategoryId(response.jsonPath().getLong("id"));
        }
    }

    @그러면("카테고리 등록이 성공한다")
    public void 카테고리_등록이_성공한다() {
        assertThat(context.getStatusCode()).isEqualTo(201);
    }

    @만일("해당 카테고리의 이름을 {string}으로 수정한다")
    public void 해당_카테고리의_이름을_수정한다(String name) {
        String body =
                """
                {
                    "name": "%s",
                    "color": "#000000",
                    "imageUrl": "https://example.com/default.png"
                }
                """
                        .formatted(name);

        var response = RestAssured.given()
                .log()
                .all()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/api/categories/" + context.getCategoryId())
                .then()
                .log()
                .all()
                .extract();

        context.setResponse(response);
    }

    @그러면("카테고리 수정이 성공한다")
    public void 카테고리_수정이_성공한다() {
        assertThat(context.getStatusCode()).isEqualTo(200);
    }

    @만일("해당 카테고리를 삭제한다")
    public void 해당_카테고리를_삭제한다() {
        var response = RestAssured.given()
                .log()
                .all()
                .when()
                .delete("/api/categories/" + context.getCategoryId())
                .then()
                .log()
                .all()
                .extract();

        context.setResponse(response);
    }

    @그러면("카테고리 삭제가 성공한다")
    public void 카테고리_삭제가_성공한다() {
        assertThat(context.getStatusCode()).isEqualTo(204);
    }

    @그리고("카테고리 목록이 비어있다")
    public void 카테고리_목록이_비어있다() {
        var response = RestAssured.given()
                .log()
                .all()
                .when()
                .get("/api/categories")
                .then()
                .log()
                .all()
                .extract();

        List<Long> ids = response.jsonPath().getList("id", Long.class);
        assertThat(ids).isEmpty();
    }

    @그리고("카테고리 목록에 {string}이 포함되어 있다")
    public void 카테고리_목록에_이름이_포함되어_있다(String name) {
        var response = RestAssured.given()
                .log()
                .all()
                .when()
                .get("/api/categories")
                .then()
                .log()
                .all()
                .extract();

        List<Long> ids = response.jsonPath().getList("id", Long.class);
        assertThat(ids).containsExactly(context.getCategoryId());

        List<String> names = response.jsonPath().getList("name", String.class);
        assertThat(names).containsExactly(name);
    }
}
