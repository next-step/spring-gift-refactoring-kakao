package gift.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import gift.auth.JwtProvider;
import gift.member.Member;
import gift.member.MemberRepository;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class E2eWishStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @먼저("위시 테스트 회원이 존재한다")
    public void 위시_테스트_회원이_존재한다() {
        var member = new Member("wish-user@test.com", "password");
        memberRepository.save(member);
        var token = jwtProvider.createToken(member.getEmail());
        context.setToken(token);
    }

    @먼저("다른 회원이 존재한다")
    public void 다른_회원이_존재한다() {
        var other = new Member("other@test.com", "password");
        memberRepository.save(other);
        var otherToken = jwtProvider.createToken(other.getEmail());
        context.setOtherToken(otherToken);
    }

    @먼저("{string} 상품이 위시리스트에 존재한다")
    public void 상품이_위시리스트에_존재한다(String productName) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + context.getToken())
                .body(Map.of("productId", context.getProductId(productName)))
                .when()
                .post("/api/wishes")
                .then()
                .extract();
        assertThat(response.statusCode()).isIn(200, 201);
        context.setWishId(response.jsonPath().getLong("id"));
    }

    @만일("{string} 상품을 위시리스트에 추가하면")
    public void 상품을_위시리스트에_추가하면(String productName) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + context.getToken())
                .body(Map.of("productId", context.getProductId(productName)))
                .when()
                .post("/api/wishes")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("존재하지 않는 상품을 위시리스트에 추가하면")
    public void 존재하지_않는_상품을_위시리스트에_추가하면() {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + context.getToken())
                .body(Map.of("productId", Long.MAX_VALUE))
                .when()
                .post("/api/wishes")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("위시리스트를 조회하면")
    public void 위시리스트를_조회하면() {
        var response = RestAssured.given()
                .header("Authorization", "Bearer " + context.getToken())
                .when()
                .get("/api/wishes")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("인증 없이 위시리스트를 조회하면")
    public void 인증_없이_위시리스트를_조회하면() {
        var response = RestAssured.given().when().get("/api/wishes").then().extract();
        context.setResponse(response);
    }

    @만일("해당 위시를 삭제하면")
    public void 해당_위시를_삭제하면() {
        var response = RestAssured.given()
                .header("Authorization", "Bearer " + context.getToken())
                .when()
                .delete("/api/wishes/" + context.getWishId())
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("다른 회원이 해당 위시를 삭제하면")
    public void 다른_회원이_해당_위시를_삭제하면() {
        var response = RestAssured.given()
                .header("Authorization", "Bearer " + context.getOtherToken())
                .when()
                .delete("/api/wishes/" + context.getWishId())
                .then()
                .extract();
        context.setResponse(response);
    }

    @그러면("위시리스트 추가에 성공한다")
    public void 위시리스트_추가에_성공한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(201);
        assertThat(context.getResponse().jsonPath().getLong("id")).isNotNull();
    }

    @그러면("위시리스트 중복 추가시 기존 위시를 반환한다")
    public void 위시리스트_중복_추가시_기존_위시를_반환한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(200);
    }

    @그러면("위시리스트에 {string} 상품이 포함되어 있다")
    public void 위시리스트에_상품이_포함되어_있다(String productName) {
        var response = context.getResponse();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("content.name")).contains(productName);
    }

    @그러면("위시 삭제에 성공한다")
    public void 위시_삭제에_성공한다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(204);
    }

    @그러면("위시 삭제가 거부된다")
    public void 위시_삭제가_거부된다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(403);
    }

    @그러면("위시리스트 조회에 실패한다")
    public void 위시리스트_조회에_실패한다() {
        assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(400);
    }

    @그러면("위시리스트 추가에 실패한다")
    public void 위시리스트_추가에_실패한다() {
        assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(400);
    }
}
