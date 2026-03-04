package gift.restassured;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

class MemberAcceptanceTest extends AcceptanceTest {
    @Test
    void 회원을_등록하면_토큰을_반환한다() {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "email", "new@example.com",
                "password", "pass1234"
            ))
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(201)
            .body("token", notNullValue());
    }

    @Test
    void 이미_등록된_이메일로_가입하면_실패한다() {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "email", "dup@example.com",
                "password", "pass1234"
            ))
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(201);

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "email", "dup@example.com",
                "password", "pass1234"
            ))
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(409);
    }

    @Test
    void 등록한_회원으로_로그인하면_토큰을_반환한다() {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "email", "login@example.com",
                "password", "pass1234"
            ))
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(201);

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "email", "login@example.com",
                "password", "pass1234"
            ))
        .when()
            .post("/api/members/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue());
    }

    @Test
    void 잘못된_비밀번호로_로그인하면_실패한다() {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "email", "wrong@example.com",
                "password", "pass1234"
            ))
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(201);

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "email", "wrong@example.com",
                "password", "wrongpass"
            ))
        .when()
            .post("/api/members/login")
        .then()
            .statusCode(400);
    }
}
