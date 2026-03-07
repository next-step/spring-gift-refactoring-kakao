package gift;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

class MemberAcceptanceTest extends AcceptanceTestFixture {

    // --- POST /api/members/register ---

    @Test
    void 회원가입_성공() {
        // given
        var request = Map.of(
            "email", "test@example.com",
            "password", "password123"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/members/register");

        // then
        response.then()
            .statusCode(201)
            .body("token", is(not(emptyString())));

        var savedMember = memberRepository.findByEmail("test@example.com");
        assertThat(savedMember).isPresent();
        assertThat(savedMember.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void 회원가입_실패_이메일_누락() {
        // given
        var request = Map.of(
            "password", "password123"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/members/register");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 회원가입_실패_잘못된_이메일_형식() {
        // given
        var request = Map.of(
            "email", "not-an-email",
            "password", "password123"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/members/register");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 회원가입_실패_이미_등록된_이메일() {
        // given
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "dup@example.com", "password", "pass1"))
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(201);

        var request = Map.of(
            "email", "dup@example.com",
            "password", "pass2"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/members/register");

        // then
        response.then()
            .statusCode(409);

        assertThat(memberRepository.count()).isEqualTo(1);
    }

    // --- POST /api/members/login ---

    @Test
    void 로그인_성공() {
        // given
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "login@example.com", "password", "mypass"))
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(201);

        var request = Map.of(
            "email", "login@example.com",
            "password", "mypass"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/members/login");

        // then
        response.then()
            .statusCode(200)
            .body("token", is(not(emptyString())));
    }

    @Test
    void 로그인_실패_존재하지_않는_이메일() {
        // given
        var request = Map.of(
            "email", "nobody@example.com",
            "password", "pass"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/members/login");

        // then
        response.then()
            .statusCode(401);
    }

    @Test
    void 로그인_실패_잘못된_비밀번호() {
        // given
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "wrong@example.com", "password", "correct"))
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(201);

        var request = Map.of(
            "email", "wrong@example.com",
            "password", "incorrect"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/members/login");

        // then
        response.then()
            .statusCode(401);
    }
}
