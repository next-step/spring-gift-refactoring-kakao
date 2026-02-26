package gift.cucumber.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import gift.cucumber.ScenarioContext;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class ProductStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @만일("{string} 상품을 {int}원, 이미지 {string}으로 해당 카테고리에 등록한다")
    public void 상품을_등록한다(String name, int price, String imageUrl) {
        String body = """
                {
                    "name": "%s",
                    "price": %d,
                    "imageUrl": "%s",
                    "categoryId": %d
                }
                """.formatted(name, price, imageUrl, context.getCategoryId());

        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/products")
                .then().log().all()
                .extract();

        context.setResponse(response);
        if (response.statusCode() == 201) {
            context.setProductId(response.jsonPath().getLong("id"));
        }
    }

    @만일("존재하지 않는 카테고리로 상품을 등록한다")
    public void 존재하지_않는_카테고리로_상품을_등록한다() {
        String body = """
                {
                    "name": "아이스 아메리카노",
                    "price": 4500,
                    "imageUrl": "https://example.com/image.png",
                    "categoryId": 999
                }
                """;

        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/products")
                .then().log().all()
                .extract();

        context.setResponse(response);
    }

    @그러면("상품 등록이 성공한다")
    public void 상품_등록이_성공한다() {
        assertThat(context.getStatusCode()).isEqualTo(201);
    }

    @그러면("상품 등록이 실패한다")
    public void 상품_등록이_실패한다() {
        assertThat(context.getStatusCode()).isGreaterThanOrEqualTo(400);
    }

    @그리고("상품 목록에 {string}이 {int}원, 이미지 {string}으로 해당 카테고리에 포함되어 있다")
    public void 상품_목록에_포함되어_있다(String name, int price, String imageUrl) {
        var response = RestAssured.given().log().all()
                .when()
                .get("/api/products")
                .then().log().all()
                .extract();

        List<Long> ids = response.jsonPath().getList("content.id", Long.class);
        assertThat(ids).containsExactly(context.getProductId());

        List<String> names = response.jsonPath().getList("content.name", String.class);
        assertThat(names).containsExactly(name);

        List<Integer> prices = response.jsonPath().getList("content.price", Integer.class);
        assertThat(prices).containsExactly(price);

        List<String> imageUrls = response.jsonPath().getList("content.imageUrl", String.class);
        assertThat(imageUrls).containsExactly(imageUrl);

        List<Long> categoryIds = response.jsonPath().getList("content.categoryId", Long.class);
        assertThat(categoryIds).containsExactly(context.getCategoryId());
    }
}
