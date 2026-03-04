package gift.cucumber.steps;

import static org.assertj.core.api.Assertions.assertThat;

import gift.cucumber.ScenarioState;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class GiftSteps {

  @Autowired private ScenarioState state;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Given("회원 {string}과 {string}이 등록되어 있다")
  public void 회원이_등록되어_있다(String sender, String receiver) {
    registerMember(sender);
    registerMember(receiver);
  }

  private void registerMember(String name) {
    if (state.getMemberId(name) != null) {
      return;
    }
    String email = name + "@test.com";
    // Register via API to get JWT token
    ExtractableResponse<Response> response =
        RestAssured.given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", "password"))
            .when()
            .post("/api/members/register")
            .then()
            .log()
            .all()
            .extract();
    String token = response.jsonPath().getString("token");
    state.putToken(name, token);

    // Get member ID and charge points via JDBC
    Long memberId =
        jdbcTemplate.queryForObject("SELECT id FROM member WHERE email = ?", Long.class, email);
    state.putMemberId(name, memberId);
    jdbcTemplate.update("UPDATE member SET point = 1000000 WHERE id = ?", memberId);
  }

  @Given("{string} 카테고리에 {string} 상품이 등록되어 있다")
  public void 카테고리에_상품이_등록되어_있다(String categoryName, String productName) {
    if (state.getCategoryId(categoryName) == null) {
      ExtractableResponse<Response> catResponse =
          RestAssured.given()
              .log()
              .all()
              .contentType(ContentType.JSON)
              .body(
                  Map.of(
                      "name",
                      categoryName,
                      "color",
                      "#000000",
                      "imageUrl",
                      "http://img.com/default.png"))
              .when()
              .post("/api/categories")
              .then()
              .log()
              .all()
              .extract();
      state.putCategoryId(categoryName, catResponse.jsonPath().getLong("id"));
    }

    if (state.getProductId(productName) == null) {
      Long categoryId = state.getCategoryId(categoryName);
      ExtractableResponse<Response> prodResponse =
          RestAssured.given()
              .log()
              .all()
              .contentType(ContentType.JSON)
              .body(
                  Map.of(
                      "name",
                      productName,
                      "price",
                      10000,
                      "imageUrl",
                      "http://img.com/" + productName + ".png",
                      "categoryId",
                      categoryId))
              .when()
              .post("/api/products")
              .then()
              .log()
              .all()
              .extract();
      state.putProductId(productName, prodResponse.jsonPath().getLong("id"));
    }
  }

  @Given("{string} 상품에 재고가 {int}개인 {string} 옵션이 있다")
  public void 상품에_옵션이_있다(String productName, int quantity, String optionName) {
    Long productId = state.getProductId(productName);
    ExtractableResponse<Response> response =
        RestAssured.given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "name", optionName,
                    "quantity", quantity,
                    "productId", productId))
            .when()
            .post("/api/seed/options")
            .then()
            .log()
            .all()
            .extract();
    state.putOptionId(productName + ":" + optionName, response.jsonPath().getLong("id"));
  }

  @When("{string}이 {string}에게 {string}의 {string} 옵션 {int}개를 선물한다")
  public void 선물한다(
      String sender, String receiver, String productName, String optionName, int quantity) {
    String token = state.getToken(sender);
    Long optionId = state.getOptionId(productName + ":" + optionName);

    ExtractableResponse<Response> response =
        RestAssured.given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(
                Map.of(
                    "optionId", optionId,
                    "quantity", quantity,
                    "message", "선물입니다"))
            .when()
            .post("/api/orders")
            .then()
            .log()
            .all()
            .extract();
    state.setLastResponse(response);
  }

  @When("{string}이 {string}에게 존재하지 않는 옵션으로 선물한다")
  public void 존재하지_않는_옵션으로_선물한다(String sender, String receiver) {
    String token = state.getToken(sender);

    ExtractableResponse<Response> response =
        RestAssured.given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("optionId", 9999L, "quantity", 1, "message", "없는 옵션 테스트"))
            .when()
            .post("/api/orders")
            .then()
            .log()
            .all()
            .extract();
    state.setLastResponse(response);
  }

  @Then("선물하기가 성공한다")
  public void 선물하기가_성공한다() {
    assertThat(state.getLastResponse().statusCode()).isEqualTo(201);
  }

  @Then("선물하기가 실패한다")
  public void 선물하기가_실패한다() {
    assertThat(state.getLastResponse().statusCode()).isGreaterThanOrEqualTo(400);
  }
}
