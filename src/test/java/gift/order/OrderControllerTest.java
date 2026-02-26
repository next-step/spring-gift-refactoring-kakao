package gift.order;

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
class OrderControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    JwtProvider jwtProvider;

    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // setup-data.sql: member id=1, email=test@test.com, point=100000
        token = "Bearer " + jwtProvider.createToken("test@test.com");
    }

    @Test
    @DisplayName("GET /api/orders - 인증 있으면 200을 반환한다")
    void getOrdersAuthenticated() {
        given()
            .header("Authorization", token)
        .when()
            .get("/api/orders")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("GET /api/orders - 인증 없으면 401을 반환한다")
    void getOrdersUnauthenticated() {
        given()
            .header("Authorization", "Bearer invalid-token")
        .when()
            .get("/api/orders")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("POST /api/orders - 주문 생성하면 201을 반환하고 재고와 포인트가 차감된다")
    void createOrder() {
        // setup-data.sql: option id=1, quantity=100, product price=1000
        given()
            .header("Authorization", token)
            .contentType(ContentType.JSON)
            .body(Map.of("optionId", 1, "quantity", 2, "message", "선물입니다"))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .body("quantity", equalTo(2));

        // 재고 확인: 100 - 2 = 98
        given()
        .when()
            .get("/api/products/1/options")
        .then()
            .body("[0].quantity", equalTo(98));
    }

    @Test
    @DisplayName("POST /api/orders - 인증 없으면 401을 반환한다")
    void createOrderUnauthenticated() {
        given()
            .header("Authorization", "Bearer invalid-token")
            .contentType(ContentType.JSON)
            .body(Map.of("optionId", 1, "quantity", 1, "message", "선물"))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("POST /api/orders - 존재하지 않는 옵션이면 404를 반환한다")
    void createOrderOptionNotFound() {
        given()
            .header("Authorization", token)
            .contentType(ContentType.JSON)
            .body(Map.of("optionId", 99999, "quantity", 1, "message", "선물"))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(404);
    }
}
