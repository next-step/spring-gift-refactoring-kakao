package gift.cucumber.steps;

import gift.auth.JwtProvider;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만약;
import io.cucumber.java.ko.조건;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Component
public class WishStepDefinitions {

    private static final String TEST_EMAIL = "wish-user@test.com";
    private static final String TEST_PASSWORD = "password1234";
    private static final String OTHER_EMAIL = "other-user@test.com";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    private String token;
    private Long testProductId;
    private Long lastWishId;
    private Long otherWishId;
    private Response lastResponse;

    @조건("위시 테스트용 회원이 존재한다")
    public void 위시_테스트용_회원이_존재한다() {
        jdbcTemplate.update(
            "INSERT INTO member (email, password, point) VALUES (?, ?, ?)",
            TEST_EMAIL, TEST_PASSWORD, 0
        );
        token = jwtProvider.createToken(TEST_EMAIL);
    }

    @조건("위시 테스트용 상품이 등록되어 있다")
    public void 위시_테스트용_상품이_등록되어_있다() {
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

    @조건("위시리스트에 상품이 등록되어 있다")
    public void 위시리스트에_상품이_등록되어_있다() {
        Long memberId = jdbcTemplate.queryForObject(
            "SELECT id FROM member WHERE email = ?", Long.class, TEST_EMAIL
        );
        jdbcTemplate.update(
            "INSERT INTO wish (member_id, product_id) VALUES (?, ?)",
            memberId, testProductId
        );
        lastWishId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @조건("다른 회원의 위시리스트에 상품이 등록되어 있다")
    public void 다른_회원의_위시리스트에_상품이_등록되어_있다() {
        jdbcTemplate.update(
            "INSERT INTO member (email, password, point) VALUES (?, ?, ?)",
            OTHER_EMAIL, TEST_PASSWORD, 0
        );
        Long otherMemberId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        jdbcTemplate.update(
            "INSERT INTO wish (member_id, product_id) VALUES (?, ?)",
            otherMemberId, testProductId
        );
        otherWishId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @만약("위시리스트에 상품을 추가한다")
    public void 위시리스트에_상품을_추가한다() {
        lastResponse = given()
            .contentType("application/json")
            .header("Authorization", "Bearer " + token)
            .body("""
                {
                    "productId": %d
                }
                """.formatted(testProductId))
            .when()
            .post("/api/wishes");

        if (lastResponse.statusCode() == 201) {
            lastWishId = lastResponse.jsonPath().getLong("id");
        }
    }

    @만약("위시리스트를 조회한다")
    public void 위시리스트를_조회한다() {
        lastResponse = given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/wishes");
    }

    @만약("위시리스트에서 상품을 삭제한다")
    public void 위시리스트에서_상품을_삭제한다() {
        lastResponse = given()
            .header("Authorization", "Bearer " + token)
            .when()
            .delete("/api/wishes/" + lastWishId);
    }

    @만약("인증 없이 위시리스트를 조회한다")
    public void 인증_없이_위시리스트를_조회한다() {
        lastResponse = given()
            .header("Authorization", "Bearer invalid-token")
            .when()
            .get("/api/wishes");
    }

    @만약("다른 회원의 위시 항목을 삭제한다")
    public void 다른_회원의_위시_항목을_삭제한다() {
        lastResponse = given()
            .header("Authorization", "Bearer " + token)
            .when()
            .delete("/api/wishes/" + otherWishId);
    }

    @그러면("위시 추가가 성공한다")
    public void 위시_추가가_성공한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(201);
    }

    @그러면("위시 조회가 성공한다")
    public void 위시_조회가_성공한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(200);
    }

    @그러면("인증이 실패한다")
    public void 인증이_실패한다() {
        assertThat(lastResponse.statusCode()).isEqualTo(401);
    }

    @그러면("위시 삭제가 거부된다")
    public void 위시_삭제가_거부된다() {
        assertThat(lastResponse.statusCode()).isEqualTo(403);
    }

    @그러면("위시 목록의 크기가 {int}이다")
    public void 위시_목록의_크기가_n이다(int size) {
        assertThat(lastResponse.jsonPath().getList("content")).hasSize(size);
    }
}
