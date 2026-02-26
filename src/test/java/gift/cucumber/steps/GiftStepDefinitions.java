package gift.cucumber.steps;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import gift.cucumber.ScenarioContext;
import gift.fixture.MemberFixture;
import gift.fixture.OptionFixture;
import gift.fixture.ProductFixture;
import gift.support.TestDataInitializer;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class GiftStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private TestDataInitializer initializer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @조건("회원이 등록되어 있다")
    public void 회원이_등록되어_있다() {
        var member = MemberFixture.주문회원();
        Long memberId = initializer.saveMember(member);
        context.setMemberId(memberId);

        String body = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(member.getEmail(), member.getPassword());

        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/members/login")
                .then().log().all()
                .extract();

        String token = response.jsonPath().getString("token");
        context.setToken(token);
    }

    @조건("재고 {int}개인 옵션을 가진 상품이 등록되어 있다")
    public void 재고_n개인_옵션을_가진_상품이_등록되어_있다(int quantity) {
        Long productId = initializer.saveProduct(ProductFixture.기본상품(), context.getCategoryId());
        context.setProductId(productId);

        var option = OptionFixture.기본옵션(quantity);
        Long optionId = initializer.saveOption(option, productId);
        context.setOptionId(optionId);
    }

    @만일("회원이 {int}개를 주문한다")
    public void 회원이_n개를_주문한다(int quantity) {
        String body = """
                {
                    "optionId": %d,
                    "quantity": %d,
                    "message": "주문합니다"
                }
                """.formatted(context.getOptionId(), quantity);

        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + context.getToken())
                .body(body)
                .when()
                .post("/api/orders")
                .then().log().all()
                .extract();

        context.setResponse(response);
    }

    @만일("존재하지 않는 옵션으로 주문한다")
    public void 존재하지_않는_옵션으로_주문한다() {
        String body = """
                {
                    "optionId": 999,
                    "quantity": 1,
                    "message": "주문합니다"
                }
                """;

        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + context.getToken())
                .body(body)
                .when()
                .post("/api/orders")
                .then().log().all()
                .extract();

        context.setResponse(response);
    }

    @만일("인증되지 않은 사용자가 주문한다")
    public void 인증되지_않은_사용자가_주문한다() {
        String body = """
                {
                    "optionId": %d,
                    "quantity": 1,
                    "message": "주문합니다"
                }
                """.formatted(context.getOptionId());

        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/orders")
                .then().log().all()
                .extract();

        context.setResponse(response);
    }

    @그러면("주문이 성공한다")
    public void 주문이_성공한다() {
        assertThat(context.getStatusCode()).isEqualTo(201);
    }

    @그러면("주문이 실패한다")
    public void 주문이_실패한다() {
        assertThat(context.getStatusCode()).isGreaterThanOrEqualTo(400);
    }

    @조건("회원의 포인트가 {int}이다")
    public void 회원의_포인트가_n이다(int point) {
        jdbcTemplate.update("UPDATE member SET point = ? WHERE id = ?", point, context.getMemberId());
    }

    @그리고("주문 목록에 해당 주문이 포함되어 있다")
    public void 주문_목록에_해당_주문이_포함되어_있다() {
        var response = RestAssured.given().log().all()
                .header("Authorization", "Bearer " + context.getToken())
                .when()
                .get("/api/orders")
                .then().log().all()
                .extract();

        List<Long> optionIds = response.jsonPath().getList("content.optionId", Long.class);
        assertThat(optionIds).contains(context.getOptionId());
    }

    @그리고("옵션 재고가 {int}개로 차감되어 있다")
    public void 옵션_재고가_n개로_차감되어_있다(int expectedQuantity) {
        Integer actualQuantity = jdbcTemplate.queryForObject(
                "SELECT quantity FROM options WHERE id = ?", Integer.class, context.getOptionId());
        assertThat(actualQuantity).isEqualTo(expectedQuantity);
    }

    @그리고("옵션 재고가 {int}개로 유지되어 있다")
    public void 옵션_재고가_n개로_유지되어_있다(int expectedQuantity) {
        Integer actualQuantity = jdbcTemplate.queryForObject(
                "SELECT quantity FROM options WHERE id = ?", Integer.class, context.getOptionId());
        assertThat(actualQuantity).isEqualTo(expectedQuantity);
    }
}
