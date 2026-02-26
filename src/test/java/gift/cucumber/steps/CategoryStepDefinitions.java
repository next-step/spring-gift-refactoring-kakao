package gift.cucumber.steps;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만약;
import io.cucumber.java.ko.조건;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class CategoryStepDefinitions {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Response lastResponse;
    private Long lastCategoryId;

    @조건("{string} 카테고리가 등록되어 있다")
    public void 카테고리가_등록되어_있다(String name) {
        jdbcTemplate.update(
            "INSERT INTO category (name, color, image_url) VALUES (?, ?, ?)",
            name, "#000000", "https://test.com/img.jpg"
        );
        lastCategoryId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @만약("{string} 카테고리를 생성한다")
    public void 카테고리를_생성한다(String name) {
        lastResponse = given()
            .contentType("application/json")
            .body("""
                {
                    "name": "%s",
                    "color": "#000000",
                    "imageUrl": "https://test.com/img.jpg"
                }
                """.formatted(name))
            .when()
            .post("/api/categories");

        if (lastResponse.statusCode() == 201) {
            lastCategoryId = lastResponse.jsonPath().getLong("id");
        }
    }

    @만약("카테고리 목록을 조회한다")
    public void 카테고리_목록을_조회한다() {
        lastResponse = given()
            .when()
            .get("/api/categories");
    }

    @만약("등록된 카테고리의 이름을 {string}으로 수정한다")
    public void 등록된_카테고리의_이름을_수정한다(String newName) {
        lastResponse = given()
            .contentType("application/json")
            .body("""
                {
                    "name": "%s",
                    "color": "#000000",
                    "imageUrl": "https://test.com/img.jpg"
                }
                """.formatted(newName))
            .when()
            .put("/api/categories/" + lastCategoryId);
    }

    @만약("존재하지 않는 카테고리를 수정한다")
    public void 존재하지_않는_카테고리를_수정한다() {
        lastResponse = given()
            .contentType("application/json")
            .body("""
                {
                    "name": "수정",
                    "color": "#000000",
                    "imageUrl": "https://test.com/img.jpg"
                }
                """)
            .when()
            .put("/api/categories/99999");
    }

    @만약("등록된 카테고리를 삭제한다")
    public void 등록된_카테고리를_삭제한다() {
        lastResponse = given()
            .when()
            .delete("/api/categories/" + lastCategoryId);
    }

    @그러면("카테고리 생성이 성공한다")
    public void 카테고리_생성이_성공한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(201);
    }

    @그러면("카테고리 조회가 성공한다")
    public void 카테고리_조회가_성공한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(200);
    }

    @그러면("카테고리 수정이 성공한다")
    public void 카테고리_수정이_성공한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(200);
    }

    @그러면("카테고리 수정이 실패한다")
    public void 카테고리_수정이_실패한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(404);
    }

    @그러면("응답에 id가 포함되어 있다")
    public void 응답에_id가_포함되어_있다() {
        assertThat(lastResponse.jsonPath().getLong("id")).isPositive();
    }

    @그러면("카테고리 목록의 크기가 {int}이다")
    public void 카테고리_목록의_크기가_n이다(int size) {
        assertThat(lastResponse.jsonPath().getList("$")).hasSize(size);
    }

    @그러면("응답의 카테고리 이름이 {string}이다")
    public void 응답의_카테고리_이름이_이다(String expectedName) {
        assertThat(lastResponse.jsonPath().getString("name")).isEqualTo(expectedName);
    }
}
