package gift;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class GiftAcceptanceTest {

  @LocalServerPort int port;

  @Autowired JdbcTemplate jdbcTemplate;

  String token;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    ExtractableResponse<Response> response =
        AcceptanceTestSupport.회원을_등록한다("sender@test.com", "password");
    token = response.jsonPath().getString("token");
    Long memberId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM member WHERE email = ?", Long.class, "sender@test.com");
    jdbcTemplate.update("UPDATE member SET point = 100000 WHERE id = ?", memberId);
  }

  /** G1: 재고가 충분할 때 주문에 성공한다. - 옵션1(재고 10) 에 수량 1을 주문 → 201 응답 */
  @Test
  void 재고가_충분할_때_선물하기에_성공한다() {
    // when
    ExtractableResponse<Response> response =
        RestAssured.given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(
                Map.of(
                    "optionId", 1,
                    "quantity", 1,
                    "message", "생일 축하해!"))
            .when()
            .post("/api/orders")
            .then()
            .log()
            .all()
            .extract();

    // then (OrderController returns 201)
    assertThat(response.statusCode()).isEqualTo(201);
  }

  /** G2: 주문을 하면 재고가 감소한다. - 옵션1(재고 10)을 10개 전부 주문 → 성공 - 같은 옵션에 1개 추가 주문 → 재고 부족으로 실패 (500) */
  @Test
  void 선물을_보내면_재고가_감소한다() {
    // when — 재고 10개 전부 소진
    ExtractableResponse<Response> firstResponse =
        RestAssured.given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(
                Map.of(
                    "optionId", 1,
                    "quantity", 10,
                    "message", "전부 보낸다"))
            .when()
            .post("/api/orders")
            .then()
            .log()
            .all()
            .extract();

    // then — 첫 번째 요청 성공
    assertThat(firstResponse.statusCode()).isEqualTo(201);

    // when — 같은 옵션에 1개 추가 요청
    ExtractableResponse<Response> secondResponse =
        RestAssured.given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(
                Map.of(
                    "optionId", 1,
                    "quantity", 1,
                    "message", "하나 더"))
            .when()
            .post("/api/orders")
            .then()
            .log()
            .all()
            .extract();

    // then — 두 번째 요청 실패 (재고 부족 → IllegalArgumentException → 400)
    assertThat(secondResponse.statusCode()).isEqualTo(400);
  }

  /** G3: 재고보다 많은 수량을 주문하면 실패한다. - 옵션2(재고 1)에 수량 2를 요청 → 500 응답 */
  @Test
  void 재고보다_많은_수량을_선물하면_실패한다() {
    // when
    ExtractableResponse<Response> response =
        RestAssured.given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(
                Map.of(
                    "optionId", 2,
                    "quantity", 2,
                    "message", "재고 초과 테스트"))
            .when()
            .post("/api/orders")
            .then()
            .log()
            .all()
            .extract();

    // then (IllegalArgumentException from subtractQuantity → 400)
    assertThat(response.statusCode()).isEqualTo(400);
  }

  /** G4: 존재하지 않는 옵션으로 주문하면 실패한다. - 옵션 ID 9999 (존재하지 않음) → 404 응답 */
  @Test
  void 존재하지_않는_옵션으로_선물하면_실패한다() {
    // when
    ExtractableResponse<Response> response =
        RestAssured.given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(
                Map.of(
                    "optionId", 9999,
                    "quantity", 1,
                    "message", "없는 옵션 테스트"))
            .when()
            .post("/api/orders")
            .then()
            .log()
            .all()
            .extract();

    // then (NoSuchElementException → GlobalExceptionHandler → 404)
    assertThat(response.statusCode()).isEqualTo(404);
  }

  /** G5: 위시에 담은 상품을 주문하면 위시에서 자동 삭제된다. */
  @Test
  void 위시에_담은_상품을_주문하면_위시에서_삭제된다() {
    // given — 위시에 상품 등록
    RestAssured.given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + token)
        .body(Map.of("productId", 1))
        .when()
        .post("/api/wishes")
        .then()
        .statusCode(201);

    // when — 해당 상품의 옵션으로 주문
    RestAssured.given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + token)
        .body(
            Map.of(
                "optionId", 1,
                "quantity", 1,
                "message", "위시 삭제 테스트"))
        .when()
        .post("/api/orders")
        .then()
        .statusCode(201);

    // then — 위시 목록에서 해당 상품이 사라졌는지 확인
    ExtractableResponse<Response> wishList =
        RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/wishes")
            .then()
            .extract();

    assertThat(wishList.statusCode()).isEqualTo(200);
    assertThat(wishList.jsonPath().getList("content")).isEmpty();
  }

  /** G6: Authorization 헤더 없이 주문하면 실패한다. - Authorization 헤더 누락 → 400 응답 */
  @Test
  void Member_Id_헤더_없이_선물하면_실패한다() {
    // when — Authorization 헤더 없이 요청
    ExtractableResponse<Response> response =
        RestAssured.given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "optionId", 1,
                    "quantity", 1,
                    "message", "헤더 누락 테스트"))
            .when()
            .post("/api/orders")
            .then()
            .log()
            .all()
            .extract();

    // then (MissingRequestHeaderException → 400)
    assertThat(response.statusCode()).isEqualTo(400);
  }
}
