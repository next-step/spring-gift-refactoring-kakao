package gift.cucumber.steps;

import static org.assertj.core.api.Assertions.assertThat;

import gift.cucumber.ScenarioContext;
import gift.fixture.MemberFixture;
import gift.fixture.OptionFixture;
import gift.support.TestDataInitializer;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class GiftStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private TestDataInitializer initializer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @조건("포인트 {int}인 회원이 등록되어 있다")
    public void 포인트_n인_회원이_등록되어_있다(int point) {
        var member = MemberFixture.회원(point);
        Long memberId = initializer.saveMember(member);
        context.setMemberId(memberId);

        String body =
                """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """
                        .formatted(member.getEmail(), member.getPassword());

        var response = RestAssured.given()
                .log()
                .all()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/members/login")
                .then()
                .log()
                .all()
                .extract();

        String token = response.jsonPath().getString("token");
        context.setToken(token);
    }

    @조건("해당 상품에 {string} 옵션이 재고 {int}개로 등록되어 있다")
    public void 해당_상품에_옵션이_재고_개로_등록되어_있다(String name, int quantity) {
        Long optionId = initializer.saveOption(OptionFixture.옵션(name, quantity), context.getProductId());
        context.setOptionId(optionId);
    }

    @만일("회원이 {int}개를 주문한다")
    public void 회원이_n개를_주문한다(int quantity) {
        String body =
                """
                {
                    "optionId": %d,
                    "quantity": %d,
                    "message": "주문합니다"
                }
                """
                        .formatted(context.getOptionId(), quantity);

        var response = RestAssured.given()
                .log()
                .all()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + context.getToken())
                .body(body)
                .when()
                .post("/api/orders")
                .then()
                .log()
                .all()
                .extract();

        context.setResponse(response);
    }

    @만일("존재하지 않는 옵션으로 주문한다")
    public void 존재하지_않는_옵션으로_주문한다() {
        String body =
                """
                {
                    "optionId": 999,
                    "quantity": 1,
                    "message": "주문합니다"
                }
                """;

        var response = RestAssured.given()
                .log()
                .all()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + context.getToken())
                .body(body)
                .when()
                .post("/api/orders")
                .then()
                .log()
                .all()
                .extract();

        context.setResponse(response);
    }

    @만일("인증되지 않은 사용자가 주문한다")
    public void 인증되지_않은_사용자가_주문한다() {
        String body =
                """
                {
                    "optionId": %d,
                    "quantity": 1,
                    "message": "주문합니다"
                }
                """
                        .formatted(context.getOptionId());

        var response = RestAssured.given()
                .log()
                .all()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/orders")
                .then()
                .log()
                .all()
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
        var response = RestAssured.given()
                .log()
                .all()
                .header("Authorization", "Bearer " + context.getToken())
                .when()
                .get("/api/orders")
                .then()
                .log()
                .all()
                .extract();

        List<Long> optionIds = response.jsonPath().getList("content.optionId", Long.class);
        assertThat(optionIds).contains(context.getOptionId());
    }

    @그리고("옵션 재고가 {int}개로 차감되어 있다")
    public void 옵션_재고가_n개로_차감되어_있다(int expectedQuantity) {
        int actualQuantity = getOptionQuantityViaApi();
        assertThat(actualQuantity).isEqualTo(expectedQuantity);
    }

    @그리고("옵션 재고가 {int}개로 유지되어 있다")
    public void 옵션_재고가_n개로_유지되어_있다(int expectedQuantity) {
        int actualQuantity = getOptionQuantityViaApi();
        assertThat(actualQuantity).isEqualTo(expectedQuantity);
    }

    private int getOptionQuantityViaApi() {
        var response = RestAssured.given()
                .log()
                .all()
                .when()
                .get("/api/products/" + context.getProductId() + "/options")
                .then()
                .log()
                .all()
                .extract();

        List<Long> ids = response.jsonPath().getList("id", Long.class);
        List<Integer> quantities = response.jsonPath().getList("quantity", Integer.class);
        int index = ids.indexOf(context.getOptionId());
        return quantities.get(index);
    }
}
