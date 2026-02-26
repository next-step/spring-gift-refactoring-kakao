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

public class OptionStepDefinitions {

    @Autowired
    private ScenarioContext context;

    private Long addedOptionId;

    @만일("해당 상품에 {string} 옵션을 수량 {int}개로 추가한다")
    public void 해당_상품에_옵션을_수량_개로_추가한다(String name, int quantity) {
        String body =
                """
                {
                    "name": "%s",
                    "quantity": %d
                }
                """
                        .formatted(name, quantity);

        var response = RestAssured.given()
                .log()
                .all()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/products/" + context.getProductId() + "/options")
                .then()
                .log()
                .all()
                .extract();

        context.setResponse(response);
        if (response.statusCode() == 201) {
            addedOptionId = response.jsonPath().getLong("id");
        }
    }

    @만일("해당 옵션을 삭제한다")
    public void 해당_옵션을_삭제한다() {
        var response = RestAssured.given()
                .log()
                .all()
                .when()
                .delete("/api/products/" + context.getProductId() + "/options/" + context.getOptionId())
                .then()
                .log()
                .all()
                .extract();

        context.setResponse(response);
    }

    @그리고("추가한 옵션을 삭제한다")
    public void 추가한_옵션을_삭제한다() {
        var response = RestAssured.given()
                .log()
                .all()
                .when()
                .delete("/api/products/" + context.getProductId() + "/options/" + addedOptionId)
                .then()
                .log()
                .all()
                .extract();

        context.setResponse(response);
    }

    @그러면("옵션 등록이 성공한다")
    public void 옵션_등록이_성공한다() {
        assertThat(context.getStatusCode()).isEqualTo(201);
    }

    @그러면("옵션 등록이 실패한다")
    public void 옵션_등록이_실패한다() {
        assertThat(context.getStatusCode()).isEqualTo(400);
    }

    @그러면("옵션 삭제가 성공한다")
    public void 옵션_삭제가_성공한다() {
        assertThat(context.getStatusCode()).isEqualTo(204);
    }

    @그러면("옵션 삭제가 실패한다")
    public void 옵션_삭제가_실패한다() {
        assertThat(context.getStatusCode()).isEqualTo(400);
    }

    @그리고("옵션 목록에 {string} 옵션이 수량 {int}개로 포함되어 있다")
    public void 옵션_목록에_옵션이_수량_개로_포함되어_있다(String name, int quantity) {
        var response = RestAssured.given()
                .log()
                .all()
                .when()
                .get("/api/products/" + context.getProductId() + "/options")
                .then()
                .log()
                .all()
                .extract();

        List<String> names = response.jsonPath().getList("name", String.class);
        assertThat(names).contains(name);

        int index = names.indexOf(name);
        List<Integer> quantities = response.jsonPath().getList("quantity", Integer.class);
        assertThat(quantities.get(index)).isEqualTo(quantity);
    }

    @그리고("옵션 목록에 {string} 옵션이 포함되어 있지 않다")
    public void 옵션_목록에_옵션이_포함되어_있지_않다(String name) {
        var response = RestAssured.given()
                .log()
                .all()
                .when()
                .get("/api/products/" + context.getProductId() + "/options")
                .then()
                .log()
                .all()
                .extract();

        List<String> names = response.jsonPath().getList("name", String.class);
        assertThat(names).doesNotContain(name);
    }

    @그리고("옵션 목록에 {string} 옵션이 존재한다")
    public void 옵션_목록에_옵션이_존재한다(String name) {
        var response = RestAssured.given()
                .log()
                .all()
                .when()
                .get("/api/products/" + context.getProductId() + "/options")
                .then()
                .log()
                .all()
                .extract();

        List<String> names = response.jsonPath().getList("name", String.class);
        assertThat(names).contains(name);
    }
}
