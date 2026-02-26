package gift.cucumber.steps;

import gift.auth.JwtProvider;
import io.cucumber.java.Before;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만약;
import io.cucumber.java.ko.조건;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class OrderStepDefinitions {

    private static final String TEST_EMAIL = "buyer@test.com";
    private static final String TEST_PASSWORD = "password1234";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    private String token;
    private final Map<String, Long> optionIdByName = new HashMap<>();
    private Response lastResponse;

    @Before
    public void orderSetUp() {
        optionIdByName.clear();
    }

    @조건("포인트가 {int}인 회원이 존재한다")
    public void 포인트가_n인_회원이_존재한다(int point) {
        jdbcTemplate.update(
            "INSERT INTO member (email, password, point) VALUES (?, ?, ?)",
            TEST_EMAIL, TEST_PASSWORD, point
        );
        token = jwtProvider.createToken(TEST_EMAIL);
    }

    @조건("{string} 옵션의 가격이 {int}원이고 재고가 {int}개 있다")
    public void 옵션의_가격이_n원이고_재고가_n개_있다(String optionName, int price, int stock) {
        jdbcTemplate.update(
            "INSERT INTO category (name, color, image_url) VALUES (?, ?, ?)",
            "테스트 카테고리", "#000000", "https://test.com/img.jpg"
        );
        Long categoryId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        jdbcTemplate.update(
            "INSERT INTO product (name, price, image_url, category_id) VALUES (?, ?, ?, ?)",
            "테스트 상품", price, "https://test.com/img.jpg", categoryId
        );
        Long productId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        jdbcTemplate.update(
            "INSERT INTO options (product_id, name, quantity) VALUES (?, ?, ?)",
            productId, optionName, stock
        );
        Long optionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        optionIdByName.put(optionName, optionId);
    }

    @만약("{string} {int}개를 주문한다")
    public void n개를_주문한다(String optionName, int quantity) {
        Long optionId = optionIdByName.get(optionName);
        lastResponse = given()
            .contentType("application/json")
            .header("Authorization", "Bearer " + token)
            .body("""
                {
                    "optionId": %d,
                    "quantity": %d,
                    "message": "테스트 주문입니다"
                }
                """.formatted(optionId, quantity))
            .when()
            .post("/api/orders");
    }

    @만약("존재하지 않는 옵션을 {int}개 주문한다")
    public void 존재하지_않는_옵션을_n개_주문한다(int quantity) {
        lastResponse = given()
            .contentType("application/json")
            .header("Authorization", "Bearer " + token)
            .body("""
                {
                    "optionId": 99999,
                    "quantity": %d,
                    "message": "테스트 주문입니다"
                }
                """.formatted(quantity))
            .when()
            .post("/api/orders");
    }

    @그러면("주문이 성공한다")
    public void 주문이_성공한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(201);
    }

    @그러면("주문이 실패한다")
    public void 주문이_실패한다() {
        assertThat(lastResponse.statusCode()).isGreaterThanOrEqualTo(400);
    }

    @그러면("{string} 옵션의 재고가 {int}개이다")
    public void 옵션의_재고가_n개이다(String optionName, int expectedStock) {
        Long optionId = optionIdByName.get(optionName);
        Integer actual = jdbcTemplate.queryForObject(
            "SELECT quantity FROM options WHERE id = ?", Integer.class, optionId
        );
        assertThat(actual).isEqualTo(expectedStock);
    }
}
