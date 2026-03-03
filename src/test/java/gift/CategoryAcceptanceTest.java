package gift;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CategoryAcceptanceTest {

  @LocalServerPort int port;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
  }

  @Test
  void 카테고리를_생성하고_목록에서_확인한다() {
    // when — 카테고리 생성
    ExtractableResponse<Response> createResponse = AcceptanceTestSupport.카테고리를_생성한다("디저트");

    // then — 생성 응답 확인 (CategoryController returns 201)
    assertThat(createResponse.statusCode()).isEqualTo(201);
    assertThat(createResponse.jsonPath().getLong("id")).isNotNull();
    assertThat(createResponse.jsonPath().getString("name")).isEqualTo("디저트");

    // when — 목록 조회로 생성 결과 검증
    ExtractableResponse<Response> listResponse =
        RestAssured.given().log().all().when().get("/api/categories").then().log().all().extract();

    // then — 목록에 방금 생성한 카테고리 포함
    assertThat(listResponse.statusCode()).isEqualTo(200);
    assertThat(listResponse.jsonPath().getList("name", String.class)).contains("디저트");
  }

  @Test
  @Sql(
      scripts = {"classpath:cleanup.sql", "classpath:test-data.sql"},
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  void 카테고리_목록을_조회한다() {
    // when
    ExtractableResponse<Response> response =
        RestAssured.given().log().all().when().get("/api/categories").then().log().all().extract();

    // then
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.jsonPath().getList("name", String.class))
        .containsExactlyInAnyOrder("간식", "음료");
  }
}
