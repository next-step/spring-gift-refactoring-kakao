package gift;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class GiftAcceptanceTest {

  @LocalServerPort int port;

  String token;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    token = AcceptanceTestSupport.로그인하고_토큰을_받는다("sender@test.com", "password");
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

  /** G5: Authorization 헤더 없이 주문하면 실패한다. - Authorization 헤더 누락 → 400 응답 */
  @Test
  void 인증_헤더_없이_선물하면_실패한다() {
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

  /**
   * G6: 위시리스트 상품을 주문하면 위시가 자동 제거된다. - test-data.sql에 wish(member=1, product=1)가 존재 - 옵션1(상품1)로 주문 →
   * 201 - 위시 목록 조회 → productId=1 미포함
   */
  @Test
  void 위시리스트_상품을_주문하면_위시가_자동_제거된다() {
    // when — 위시에 있는 상품 주문
    ExtractableResponse<Response> orderResponse =
        RestAssured.given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(
                Map.of(
                    "optionId", 1,
                    "quantity", 1,
                    "message", "위시 자동 정리 테스트"))
            .when()
            .post("/api/orders")
            .then()
            .log()
            .all()
            .extract();

    assertThat(orderResponse.statusCode()).isEqualTo(201);

    // then — 위시 목록에서 productId=1이 제거됨
    ExtractableResponse<Response> wishResponse =
        RestAssured.given()
            .log()
            .all()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/wishes")
            .then()
            .log()
            .all()
            .extract();

    List<Long> productIds = wishResponse.jsonPath().getList("content.productId", Long.class);
    assertThat(productIds).doesNotContain(1L);
  }

  /**
   * G7: 포인트 부족으로 주문 실패 시 재고가 원복된다. - 옵션1(재고 10) 확인 - poor@test.com(포인트 0)으로 주문 → 400 - 재고 재조회 → 여전히
   * 10 (트랜잭션 rollback 증명)
   */
  @Test
  void 포인트_부족_주문_실패_시_재고가_원복된다() {
    // given — 현재 재고 확인
    ExtractableResponse<Response> beforeOptions =
        RestAssured.given()
            .log()
            .all()
            .when()
            .get("/api/products/1/options")
            .then()
            .log()
            .all()
            .extract();

    int stockBefore = beforeOptions.jsonPath().getInt("[0].quantity");
    assertThat(stockBefore).isEqualTo(10);

    // when — 포인트 0인 회원으로 주문 시도
    String poorToken = AcceptanceTestSupport.로그인하고_토큰을_받는다("poor@test.com", "password");

    ExtractableResponse<Response> orderResponse =
        RestAssured.given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + poorToken)
            .body(
                Map.of(
                    "optionId", 1,
                    "quantity", 1,
                    "message", "포인트 부족 테스트"))
            .when()
            .post("/api/orders")
            .then()
            .log()
            .all()
            .extract();

    // then — 주문 실패 (포인트 부족 → IllegalArgumentException → 400)
    assertThat(orderResponse.statusCode()).isEqualTo(400);

    // then — 재고가 원복됨 (트랜잭션 rollback)
    ExtractableResponse<Response> afterOptions =
        RestAssured.given()
            .log()
            .all()
            .when()
            .get("/api/products/1/options")
            .then()
            .log()
            .all()
            .extract();

    int stockAfter = afterOptions.jsonPath().getInt("[0].quantity");
    assertThat(stockAfter).isEqualTo(10);
  }
}
