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
public class ProductStepDefinitions {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Response lastResponse;
    private final Map<String, Long> categoryIdByName = new HashMap<>();

    @조건("{string} 카테고리에 {string} 상품이 등록되어 있다")
    public void 카테고리에_상품이_등록되어_있다(String categoryName, String productName) {
        Long categoryId = findCategoryIdByName(categoryName);
        jdbcTemplate.update(
            "INSERT INTO product (name, price, image_url, category_id) VALUES (?, ?, ?, ?)",
            productName, 10000, "https://test.com/img.jpg", categoryId
        );
    }

    @만약("{string} 카테고리에 {string} 상품을 {int}원으로 생성한다")
    public void 카테고리에_상품을_n원으로_생성한다(String categoryName, String productName, int price) {
        Long categoryId = findCategoryIdByName(categoryName);
        lastResponse = given()
            .contentType("application/json")
            .body("""
                {
                    "name": "%s",
                    "price": %d,
                    "imageUrl": "https://test.com/img.jpg",
                    "categoryId": %d
                }
                """.formatted(productName, price, categoryId))
            .when()
            .post("/api/products");
    }

    @만약("존재하지 않는 카테고리에 상품을 생성한다")
    public void 존재하지_않는_카테고리에_상품을_생성한다() {
        lastResponse = given()
            .contentType("application/json")
            .body("""
                {
                    "name": "테스트 상품",
                    "price": 10000,
                    "imageUrl": "https://test.com/img.jpg",
                    "categoryId": 99999
                }
                """)
            .when()
            .post("/api/products");
    }

    @만약("상품 목록을 조회한다")
    public void 상품_목록을_조회한다() {
        lastResponse = given()
            .when()
            .get("/api/products");
    }

    @그러면("상품 생성이 성공한다")
    public void 상품_생성이_성공한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(201);
    }

    @그러면("상품 생성이 실패한다")
    public void 상품_생성이_실패한다() {
        assertThat(lastResponse.statusCode()).isGreaterThanOrEqualTo(400);
    }

    @그러면("응답에 상품 id가 포함되어 있다")
    public void 응답에_상품_id가_포함되어_있다() {
        assertThat(lastResponse.jsonPath().getLong("id")).isPositive();
    }

    @그러면("상품 조회가 성공한다")
    public void 상품_조회가_성공한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(200);
    }

    @그러면("상품 목록의 크기가 {int}이다")
    public void 상품_목록의_크기가_n이다(int size) {
        assertThat(lastResponse.jsonPath().getList("content")).hasSize(size);
    }

    @그러면("응답의 상품 이름이 {string}이다")
    public void 응답의_상품_이름이_이다(String expectedName) {
        assertThat(lastResponse.jsonPath().getString("name")).isEqualTo(expectedName);
    }

    @그러면("응답의 상품 가격이 {int}이다")
    public void 응답의_상품_가격이_이다(int expectedPrice) {
        assertThat(lastResponse.jsonPath().getInt("price")).isEqualTo(expectedPrice);
    }

    private Long findCategoryIdByName(String categoryName) {
        if (!categoryIdByName.containsKey(categoryName)) {
            Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM category WHERE name = ?", Long.class, categoryName
            );
            categoryIdByName.put(categoryName, id);
        }
        return categoryIdByName.get(categoryName);
    }
}
