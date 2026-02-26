package gift.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import gift.auth.JwtProvider;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.ProductRepository;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class E2eOrderStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @먼저("주문 회원이 존재한다")
    public void 주문_회원이_존재한다() {
        var member = new Member("sender@test.com", "password");
        member.chargePoint(10_000_000);
        memberRepository.save(member);
        var token = jwtProvider.createToken(member.getEmail());
        context.setToken(token);
    }

    @먼저("포인트가 부족한 회원이 존재한다")
    public void 포인트가_부족한_회원이_존재한다() {
        var member = new Member("poor@test.com", "password");
        memberRepository.save(member);
        var token = jwtProvider.createToken(member.getEmail());
        context.setOtherToken(token);
    }

    @먼저("재고가 {int}개인 옵션이 존재한다")
    public void 재고가_n개인_옵션이_존재한다(int quantity) {
        var categoryResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "음료",
                        "color", "#000000",
                        "imageUrl", "http://example.com/category.jpg"))
                .when()
                .post("/api/categories")
                .then()
                .extract();
        assertThat(categoryResponse.statusCode()).isEqualTo(201);

        var productResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name",
                        "아메리카노",
                        "price",
                        4500,
                        "imageUrl",
                        "http://example.com/image.jpg",
                        "categoryId",
                        categoryResponse.jsonPath().getLong("id")))
                .when()
                .post("/api/products")
                .then()
                .extract();
        assertThat(productResponse.statusCode()).isEqualTo(201);

        var product = productRepository
                .findById(productResponse.jsonPath().getLong("id"))
                .get();
        var option = optionRepository.save(new Option(product, "ICE", quantity));
        context.setOptionId(option.getId());
    }

    @먼저("{string} 상품에 재고가 {int}개인 {string} 옵션이 존재한다")
    public void 상품에_옵션이_존재한다(String productName, int quantity, String optionName) {
        var product =
                productRepository.findById(context.getProductId(productName)).get();
        var option = optionRepository.save(new Option(product, optionName, quantity));
        context.setOptionId(option.getId());
    }

    @먼저("해당 옵션을 {int}개 주문에 성공한다")
    public void 해당_옵션을_주문에_성공한다(int quantity) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + context.getToken())
                .body(Map.of("optionId", context.getOptionId(), "quantity", quantity, "message", "주문합니다"))
                .when()
                .post("/api/orders")
                .then()
                .extract();
        assertThat(response.statusCode()).isEqualTo(201);
    }

    @만일("해당 옵션을 {int}개 주문하면")
    public void 해당_옵션을_주문하면(int quantity) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + context.getToken())
                .body(Map.of("optionId", context.getOptionId(), "quantity", quantity, "message", "주문합니다"))
                .when()
                .post("/api/orders")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("존재하지 않는 옵션을 주문하면")
    public void 존재하지_않는_옵션을_주문하면() {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + context.getToken())
                .body(Map.of("optionId", Long.MAX_VALUE, "quantity", 1, "message", "주문합니다"))
                .when()
                .post("/api/orders")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("인증 없이 해당 옵션을 주문하면")
    public void 인증_없이_주문하면() {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("optionId", context.getOptionId(), "quantity", 1, "message", "주문합니다"))
                .when()
                .post("/api/orders")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("주문 목록을 조회하면")
    public void 주문_목록을_조회하면() {
        var response = RestAssured.given()
                .header("Authorization", "Bearer " + context.getToken())
                .when()
                .get("/api/orders")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("포인트 부족 회원이 해당 옵션을 {int}개 주문하면")
    public void 포인트_부족_회원이_주문하면(int quantity) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + context.getOtherToken())
                .body(Map.of("optionId", context.getOptionId(), "quantity", quantity, "message", "주문합니다"))
                .when()
                .post("/api/orders")
                .then()
                .extract();
        context.setResponse(response);
    }

    @그러면("주문에 성공한다")
    public void 주문에_성공한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(201);
    }

    @그러면("주문에 실패한다")
    public void 주문에_실패한다() {
        assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(400);
    }

    @그러면("해당 옵션의 재고는 {int}개이다")
    public void 해당_옵션의_재고는_n개이다(int expectedQuantity) {
        var option = optionRepository.findById(context.getOptionId()).get();
        assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
    }

    @그러면("주문 목록 조회에 성공한다")
    public void 주문_목록_조회에_성공한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(200);
    }

    @그러면("주문이 {int}건 조회된다")
    public void 주문이_n건_조회된다(int count) {
        assertThat(context.getResponse().jsonPath().getList("content")).hasSize(count);
    }
}
