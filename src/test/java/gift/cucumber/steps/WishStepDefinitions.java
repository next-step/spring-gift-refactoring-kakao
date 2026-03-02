package gift.cucumber.steps;

import static org.assertj.core.api.Assertions.assertThat;

import gift.cucumber.ScenarioContext;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

public class WishStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @만일("해당 상품을 위시리스트에 추가한다")
    public void 해당_상품을_위시리스트에_추가한다() {
        String body = """
                {
                    "productId": %d
                }
                """
                .formatted(context.getProductId());

        var response = RestAssured.given()
                .log()
                .all()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + context.getToken())
                .body(body)
                .when()
                .post("/api/wishes")
                .then()
                .log()
                .all()
                .extract();

        context.setResponse(response);
        if (response.statusCode() == 201) {
            context.setWishId(response.jsonPath().getLong("id"));
        }
    }

    @그리고("해당 상품을 위시리스트에 다시 추가한다")
    public void 해당_상품을_위시리스트에_다시_추가한다() {
        String body = """
                {
                    "productId": %d
                }
                """
                .formatted(context.getProductId());

        var response = RestAssured.given()
                .log()
                .all()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + context.getToken())
                .body(body)
                .when()
                .post("/api/wishes")
                .then()
                .log()
                .all()
                .extract();

        context.setResponse(response);
    }

    @그리고("해당 위시를 삭제한다")
    public void 해당_위시를_삭제한다() {
        var response = RestAssured.given()
                .log()
                .all()
                .header("Authorization", "Bearer " + context.getToken())
                .when()
                .delete("/api/wishes/" + context.getWishId())
                .then()
                .log()
                .all()
                .extract();

        context.setResponse(response);
    }

    @그리고("다른 회원이 해당 위시를 삭제한다")
    public void 다른_회원이_해당_위시를_삭제한다() {
        String body =
                """
                {
                    "email": "other@test.com",
                    "password": "password"
                }
                """;

        var registerResponse = RestAssured.given()
                .log()
                .all()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/members/register")
                .then()
                .log()
                .all()
                .extract();

        String otherToken = registerResponse.jsonPath().getString("token");

        var response = RestAssured.given()
                .log()
                .all()
                .header("Authorization", "Bearer " + otherToken)
                .when()
                .delete("/api/wishes/" + context.getWishId())
                .then()
                .log()
                .all()
                .extract();

        context.setResponse(response);
    }

    @그러면("위시 추가가 성공한다")
    public void 위시_추가가_성공한다() {
        assertThat(context.getStatusCode()).isEqualTo(201);
    }

    @그러면("위시 중복 추가가 성공한다")
    public void 위시_중복_추가가_성공한다() {
        assertThat(context.getStatusCode()).isEqualTo(200);
    }

    @그러면("위시 삭제가 성공한다")
    public void 위시_삭제가_성공한다() {
        assertThat(context.getStatusCode()).isEqualTo(204);
    }

    @그러면("위시 삭제가 거부된다")
    public void 위시_삭제가_거부된다() {
        assertThat(context.getStatusCode()).isEqualTo(403);
    }

    @그리고("위시리스트에 해당 상품이 포함되어 있다")
    public void 위시리스트에_해당_상품이_포함되어_있다() {
        var response = RestAssured.given()
                .log()
                .all()
                .header("Authorization", "Bearer " + context.getToken())
                .when()
                .get("/api/wishes")
                .then()
                .log()
                .all()
                .extract();

        List<Long> productIds = response.jsonPath().getList("content.productId", Long.class);
        assertThat(productIds).contains(context.getProductId());
    }

    @그리고("위시리스트가 비어있다")
    public void 위시리스트가_비어있다() {
        var response = RestAssured.given()
                .log()
                .all()
                .header("Authorization", "Bearer " + context.getToken())
                .when()
                .get("/api/wishes")
                .then()
                .log()
                .all()
                .extract();

        List<Long> productIds = response.jsonPath().getList("content.productId", Long.class);
        assertThat(productIds).isEmpty();
    }
}
