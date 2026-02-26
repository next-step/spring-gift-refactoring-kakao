package gift.cucumber.steps;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만약;
import io.cucumber.java.ko.조건;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Component
public class OptionStepDefinitions {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long testProductId;
    private final Map<String, Long> optionIdByName = new HashMap<>();
    private Response lastResponse;

    @조건("옵션 테스트용 상품이 등록되어 있다")
    public void 옵션_테스트용_상품이_등록되어_있다() {
        jdbcTemplate.update(
            "INSERT INTO category (name, color, image_url) VALUES (?, ?, ?)",
            "테스트 카테고리", "#000000", "https://test.com/img.jpg"
        );
        Long categoryId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        jdbcTemplate.update(
            "INSERT INTO product (name, price, image_url, category_id) VALUES (?, ?, ?, ?)",
            "테스트 상품", 10000, "https://test.com/img.jpg", categoryId
        );
        testProductId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @조건("{string} 옵션이 등록되어 있다")
    public void 옵션이_등록되어_있다(String optionName) {
        jdbcTemplate.update(
            "INSERT INTO options (product_id, name, quantity) VALUES (?, ?, ?)",
            testProductId, optionName, 10
        );
        Long optionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        optionIdByName.put(optionName, optionId);
    }

    @만약("{string} 옵션을 {int}개 재고로 추가한다")
    public void 옵션을_n개_재고로_추가한다(String optionName, int quantity) {
        lastResponse = given()
            .contentType("application/json")
            .body("""
                {
                    "name": "%s",
                    "quantity": %d
                }
                """.formatted(optionName, quantity))
            .when()
            .post("/api/products/" + testProductId + "/options");
    }

    @만약("옵션 목록을 조회한다")
    public void 옵션_목록을_조회한다() {
        lastResponse = given()
            .when()
            .get("/api/products/" + testProductId + "/options");
    }

    @만약("{string} 옵션을 삭제한다")
    public void 옵션을_삭제한다(String optionName) {
        Long optionId = optionIdByName.get(optionName);
        lastResponse = given()
            .when()
            .delete("/api/products/" + testProductId + "/options/" + optionId);
    }

    @그러면("옵션 추가가 성공한다")
    public void 옵션_추가가_성공한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(201);
    }

    @그러면("옵션 추가가 실패한다")
    public void 옵션_추가가_실패한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(400);
    }

    @그러면("옵션 조회가 성공한다")
    public void 옵션_조회가_성공한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(200);
    }

    @그러면("옵션 삭제가 실패한다")
    public void 옵션_삭제가_실패한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(400);
    }

    @그러면("옵션 목록의 크기가 {int}이다")
    public void 옵션_목록의_크기가_n이다(int size) {
        assertThat(lastResponse.jsonPath().getList("$")).hasSize(size);
    }
}
