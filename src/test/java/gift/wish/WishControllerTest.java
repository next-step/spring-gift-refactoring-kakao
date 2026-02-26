package gift.wish;

import gift.auth.JwtProvider;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/setup-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class WishControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    JwtProvider jwtProvider;

    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        token = "Bearer " + jwtProvider.createToken("test@test.com");
    }

    @Test
    @DisplayName("GET /api/wishes - 인증 있으면 200을 반환한다")
    void getWishesAuthenticated() {
        given()
            .header("Authorization", token)
        .when()
            .get("/api/wishes")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("GET /api/wishes - 인증 없으면 401을 반환한다")
    void getWishesUnauthenticated() {
        given()
            .header("Authorization", "Bearer invalid-token")
        .when()
            .get("/api/wishes")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("POST /api/wishes - 위시를 추가하면 201을 반환한다")
    void addWish() {
        // setup-data.sql에 이미 wish (member=1, product=1) 존재하므로 삭제 후 추가
        given().header("Authorization", token).delete("/api/wishes/1");

        given()
            .header("Authorization", token)
            .contentType(ContentType.JSON)
            .body(Map.of("productId", 1))
        .when()
            .post("/api/wishes")
        .then()
            .statusCode(201)
            .body("productId", equalTo(1));
    }

    @Test
    @DisplayName("POST /api/wishes - 중복 위시는 200과 기존 위시를 반환한다")
    void addWishDuplicate() {
        // setup-data.sql에 이미 wish (member=1, product=1) 존재
        given()
            .header("Authorization", token)
            .contentType(ContentType.JSON)
            .body(Map.of("productId", 1))
        .when()
            .post("/api/wishes")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("DELETE /api/wishes/{id} - 본인 위시를 삭제하면 204를 반환한다")
    void removeOwnWish() {
        // setup-data.sql에 wish id=1 (member=1, product=1) 존재
        given()
            .header("Authorization", token)
        .when()
            .delete("/api/wishes/1")
        .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("DELETE /api/wishes/{id} - 타인 위시를 삭제하면 403을 반환한다")
    void removeOtherMemberWish() {
        // other@test.com (member=2)의 위시를 생성
        String otherToken = "Bearer " + jwtProvider.createToken("other@test.com");
        int otherWishId = given()
            .header("Authorization", otherToken)
            .contentType(ContentType.JSON)
            .body(Map.of("productId", 1))
        .when()
            .post("/api/wishes")
        .then()
            .extract().path("id");

        // test@test.com (member=1)이 삭제 시도
        given()
            .header("Authorization", token)
        .when()
            .delete("/api/wishes/" + otherWishId)
        .then()
            .statusCode(403);
    }
}
