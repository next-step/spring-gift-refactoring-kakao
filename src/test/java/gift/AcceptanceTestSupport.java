package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.util.Map;

class AcceptanceTestSupport {

    static ExtractableResponse<Response> 카테고리를_생성한다(String name) {
        return RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "color", "#000000",
                        "imageUrl", "http://img.com/default.png"
                ))
                .when().post("/api/categories")
                .then().log().all().extract();
    }

    static String 로그인하고_토큰을_받는다(String email, String password) {
        return RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when().post("/api/members/login")
                .then().log().all().extract()
                .jsonPath().getString("token");
    }
}
