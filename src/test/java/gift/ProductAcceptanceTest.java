package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ProductAcceptanceTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void 상품을_생성하고_목록에서_확인한다() {
        // given — 카테고리를 API로 먼저 생성
        long categoryId = AcceptanceTestSupport.카테고리를_생성한다("간식").jsonPath().getLong("id");

        // when — 상품 생성
        ExtractableResponse<Response> createResponse = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "초콜릿",
                        "price", 5000,
                        "imageUrl", "http://img.com/choco.png",
                        "categoryId", categoryId
                ))
                .when().post("/api/products")
                .then().log().all().extract();

        // then — 생성 응답 확인 (ProductController returns 201)
        assertThat(createResponse.statusCode()).isEqualTo(201);
        assertThat(createResponse.jsonPath().getLong("id")).isNotNull();
        assertThat(createResponse.jsonPath().getString("name")).isEqualTo("초콜릿");
        assertThat(createResponse.jsonPath().getInt("price")).isEqualTo(5000);
        assertThat(createResponse.jsonPath().getLong("categoryId")).isEqualTo(categoryId);

        // when — 목록 조회로 생성 결과 검증 (Page 응답)
        ExtractableResponse<Response> listResponse = 상품을_조회한다();

        // then — 목록에 방금 생성한 상품 포함
        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.jsonPath().getList("content.name", String.class)).contains("초콜릿");
    }

    @Test
    @Sql(scripts = {"classpath:cleanup.sql", "classpath:test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void 상품_목록을_조회한다() {
        // when
        ExtractableResponse<Response> response = 상품을_조회한다();

        // then (Page 응답의 content 배열에서 추출)
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("content.name", String.class))
                .containsExactlyInAnyOrder("초콜릿", "커피");
    }

    private ExtractableResponse<Response> 상품을_조회한다() {
        return RestAssured.given().log().all()
                .when().get("/api/products")
                .then().log().all().extract();
    }
}
