package gift.cucumber.steps;

import gift.auth.JwtProvider;
import io.cucumber.java.Before;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

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
    private Long currentOptionId;
    private Response lastResponse;

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 28080;
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE orders");
        jdbcTemplate.execute("TRUNCATE TABLE wish");
        jdbcTemplate.execute("TRUNCATE TABLE options");
        jdbcTemplate.execute("TRUNCATE TABLE product");
        jdbcTemplate.execute("TRUNCATE TABLE category");
        jdbcTemplate.execute("TRUNCATE TABLE member");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    @조건("포인트가 {int}인 회원이 존재한다")
    public void 포인트가_n인_회원이_존재한다(int point) {
        jdbcTemplate.update(
            "INSERT INTO member (email, password, point) VALUES (?, ?, ?)",
            TEST_EMAIL, TEST_PASSWORD, point
        );
        token = jwtProvider.createToken(TEST_EMAIL);
    }

    @조건("가격이 {int}이고 재고가 {int}인 옵션이 존재한다")
    public void 가격이_n이고_재고가_n인_옵션이_존재한다(int price, int stock) {
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
            productId, "테스트 옵션", stock
        );
        currentOptionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @만일("{int}개를 주문한다")
    public void n개를_주문한다(int quantity) {
        lastResponse = given()
            .contentType("application/json")
            .header("Authorization", "Bearer " + token)
            .body("""
                {
                    "optionId": %d,
                    "quantity": %d,
                    "message": "테스트 주문입니다"
                }
                """.formatted(currentOptionId, quantity))
            .when()
            .post("/api/orders");
    }

    @만일("옵션ID {long}로 {int}개를 주문한다")
    public void 옵션ID로_n개를_주문한다(long optionId, int quantity) {
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

    @그러면("응답 상태코드는 {int}이다")
    public void 응답_상태코드는_n이다(int statusCode) {
        assertThat(lastResponse.statusCode()).isEqualTo(statusCode);
    }

    @그러면("재고는 {int}이다")
    public void 재고는_n이다(int expectedStock) {
        Integer actual = jdbcTemplate.queryForObject(
            "SELECT quantity FROM options WHERE id = ?", Integer.class, currentOptionId
        );
        assertThat(actual).isEqualTo(expectedStock);
    }
}
