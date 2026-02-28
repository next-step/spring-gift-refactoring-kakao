package gift.acceptance.client;

import static io.restassured.RestAssured.given;

import gift.acceptance.context.ScenarioContext;
import io.cucumber.spring.ScenarioScope;
import io.restassured.http.ContentType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
@Profile("acceptance-test")
@RequiredArgsConstructor
public class ApiClient {

    private final ScenarioContext context;

    // --- Non-authenticated ---

    public void post(String path, Object body) {
        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(body)
                        .when()
                        .post(path)
        );
    }

    public void get(String path) {
        context.setResponse(
                given()
                        .when()
                        .get(path)
        );
    }

    public void get(String path, int page, int size) {
        context.setResponse(
                given()
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .when()
                        .get(path)
        );
    }

    public void put(String path, Object body) {
        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(body)
                        .when()
                        .put(path)
        );
    }

    public void delete(String path) {
        context.setResponse(
                given()
                        .when()
                        .delete(path)
        );
    }

    // --- Authenticated (context.authToken) ---

    public void authPost(String path, Object body) {
        tokenPost(context.getAuthToken(), path, body);
    }

    public void tokenPost(String token, String path, Object body) {
        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + token)
                        .body(body)
                        .when()
                        .post(path)
        );
    }

    public void authGet(String path) {
        tokenGet(context.getAuthToken(), path);
    }

    public void tokenGet(String token, String path) {
        context.setResponse(
                given()
                        .header("Authorization", "Bearer " + token)
                        .when()
                        .get(path)
        );
    }

    // --- Arbitrary token ---

    public void authGet(String path, int page, int size) {
        context.setResponse(
                given()
                        .header("Authorization", "Bearer " + context.getAuthToken())
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .when()
                        .get(path)
        );
    }

    public void authDelete(String path) {
        tokenDelete(context.getAuthToken(), path);
    }

    public void tokenDelete(String token, String path) {
        context.setResponse(
                given()
                        .header("Authorization", "Bearer " + token)
                        .when()
                        .delete(path)
        );
    }
}
