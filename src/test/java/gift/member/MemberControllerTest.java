package gift.member;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/setup-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class MemberControllerTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("POST /api/members/register - 정상 등록하면 201과 토큰을 반환한다")
    void registerSuccess() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "new@test.com", "password", "password"))
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(201)
            .body("token", notNullValue());
    }

    @Test
    @DisplayName("POST /api/members/register - 중복 이메일이면 400을 반환한다")
    void registerDuplicateEmail() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "test@test.com", "password", "password"))
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/members/login - 정상 로그인하면 200과 토큰을 반환한다")
    void loginSuccess() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "test@test.com", "password", "password"))
        .when()
            .post("/api/members/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue());
    }

    @Test
    @DisplayName("POST /api/members/login - 잘못된 비밀번호면 400을 반환한다")
    void loginWrongPassword() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "test@test.com", "password", "wrongpassword"))
        .when()
            .post("/api/members/login")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/members/login - 존재하지 않는 이메일이면 400을 반환한다")
    void loginNonExistentEmail() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "nonexistent@test.com", "password", "password"))
        .when()
            .post("/api/members/login")
        .then()
            .statusCode(400);
    }
}
