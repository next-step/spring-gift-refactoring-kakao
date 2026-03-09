package gift;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import gift.category.CategoryRepository;
import gift.member.MemberRepository;
import gift.option.OptionRepository;
import gift.order.OrderRepository;
import gift.product.ProductRepository;
import gift.wish.WishRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    WishRepository wishRepository;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // FK 역순으로 삭제 — 테스트 격리 보장
        orderRepository.deleteAll();
        wishRepository.deleteAll();
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    String registerMember(String email, String password) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", password))
            .when()
            .post("/api/members/register")
            .then()
            .statusCode(201)
            .extract().jsonPath().getString("token");
    }

    // ── 회원가입 ──

    @Test
    void 회원가입_성공() {
        // given
        var request = Map.of("email", "test@example.com", "password", "password123");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/members/register");

        // then
        response.then()
            .statusCode(201)
            .body("token", notNullValue());
    }

    @Test
    void 회원가입_실패_이메일_중복() {
        // given
        registerMember("dup@example.com", "password123");
        var request = Map.of("email", "dup@example.com", "password", "password456");

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
    void 회원가입_실패_이메일_형식_잘못됨() {
        // given
        var request = Map.of("email", "not-an-email", "password", "password123");

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
    void 회원가입_실패_이메일_누락() {
        // given
        var request = Map.of("password", "password123");

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
    void 회원가입_실패_비밀번호_누락() {
        // given
        var request = Map.of("email", "test@example.com");

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
    void 회원가입_실패_빈_본문() {
        // given — 빈 JSON

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(Map.of())
            .when()
            .post("/api/members/register");

        // then
        response.then()
            .statusCode(400);
    }

    // ── 로그인 ──

    @Test
    void 로그인_성공() {
        // given
        registerMember("user@example.com", "password123");
        var request = Map.of("email", "user@example.com", "password", "password123");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/members/login");

        // then
        response.then()
            .statusCode(200)
            .body("token", notNullValue());
    }

    @Test
    void 로그인_실패_존재하지_않는_이메일() {
        // given
        var request = Map.of("email", "nobody@example.com", "password", "password123");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/members/login");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 로그인_실패_비밀번호_불일치() {
        // given
        registerMember("user@example.com", "correct-password");
        var request = Map.of("email", "user@example.com", "password", "wrong-password");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/members/login");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 로그인_실패_이메일_형식_잘못됨() {
        // given
        var request = Map.of("email", "invalid-email", "password", "password123");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/members/login");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 로그인_실패_빈_본문() {
        // given — 빈 JSON

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(Map.of())
            .when()
            .post("/api/members/login");

        // then
        response.then()
            .statusCode(400);
    }
}
