package gift.cucumber.support;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public final class ApiClient {

    private ApiClient() {}

    public static ExtractableResponse<Response> get(String path) {
        return baseRequest().when().get(path).then().log().ifValidationFails().extract();
    }

    public static ExtractableResponse<Response> get(String path, String token) {
        return authRequest(token)
                .when()
                .get(path)
                .then()
                .log()
                .ifValidationFails()
                .extract();
    }

    public static ExtractableResponse<Response> post(String path, String body) {
        return baseRequest()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(path)
                .then()
                .log()
                .ifValidationFails()
                .extract();
    }

    public static ExtractableResponse<Response> post(String path, String body, String token) {
        return authRequest(token)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(path)
                .then()
                .log()
                .ifValidationFails()
                .extract();
    }

    public static ExtractableResponse<Response> put(String path, String body) {
        return baseRequest()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put(path)
                .then()
                .log()
                .ifValidationFails()
                .extract();
    }

    public static ExtractableResponse<Response> delete(String path) {
        return baseRequest()
                .when()
                .delete(path)
                .then()
                .log()
                .ifValidationFails()
                .extract();
    }

    public static ExtractableResponse<Response> delete(String path, String token) {
        return authRequest(token)
                .when()
                .delete(path)
                .then()
                .log()
                .ifValidationFails()
                .extract();
    }

    private static RequestSpecification baseRequest() {
        return RestAssured.given().log().ifValidationFails();
    }

    private static RequestSpecification authRequest(String token) {
        return baseRequest().header("Authorization", "Bearer " + token);
    }
}
