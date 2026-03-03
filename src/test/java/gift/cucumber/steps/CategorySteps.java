package gift.cucumber.steps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import gift.cucumber.SharedState;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.먼저;
import io.cucumber.java.ko.만일;
import org.springframework.beans.factory.annotation.Autowired;

public class CategorySteps {

    @Autowired
    private SharedState state;

    @만일("카테고리 목록을 조회한다")
    public void 카테고리_목록_조회() {
        state.setResponse(
            given()
            .when()
                .get("/api/categories")
        );
    }

    @그리고("카테고리 목록에 {string}, {string}, {string}이 포함되어 있다")
    public void 카테고리_목록_포함_검증(String name1, String name2, String name3) {
        state.getResponse().then().body("name", hasItems(name1, name2, name3));
    }

    @만일("이름 {string}, 색상 {string}, 이미지 {string}, 설명 {string}로 카테고리를 생성한다")
    public void 카테고리_생성(String name, String color, String imageUrl, String description) {
        state.setResponse(
            given()
                .contentType("application/json")
                .body("{\"name\":\"" + name + "\",\"color\":\"" + color + "\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"" + description + "\"}")
            .when()
                .post("/api/categories")
        );
    }

    @먼저("이름이 {string}인 카테고리가 존재한다")
    public void 카테고리_존재(String name) {
        Long id = given()
            .contentType("application/json")
            .body("{\"name\":\"" + name + "\",\"color\":\"#000000\",\"imageUrl\":\"https://example.com/img.jpg\",\"description\":\"desc\"}")
        .when()
            .post("/api/categories")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
        state.setSavedId(id);
    }

    @만일("해당 카테고리를 이름 {string}, 색상 {string}, 이미지 {string}, 설명 {string}으로 수정한다")
    public void 카테고리_수정(String name, String color, String imageUrl, String description) {
        state.setResponse(
            given()
                .contentType("application/json")
                .body("{\"name\":\"" + name + "\",\"color\":\"" + color + "\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"" + description + "\"}")
            .when()
                .put("/api/categories/" + state.getSavedId())
        );
    }

    @만일("해당 카테고리를 삭제한다")
    public void 카테고리_삭제() {
        state.setResponse(
            given()
            .when()
                .delete("/api/categories/" + state.getSavedId())
        );
    }

    @만일("ID {long} 카테고리를 수정 요청한다")
    public void 카테고리_수정_요청(long id) {
        state.setResponse(
            given()
                .contentType("application/json")
                .body("{\"name\":\"없는카테고리\",\"color\":\"#000000\",\"imageUrl\":\"https://example.com/img.jpg\",\"description\":\"\"}")
            .when()
                .put("/api/categories/" + id)
        );
    }

    @그리고("응답의 카테고리 이름은 {string}이다")
    public void 카테고리_이름_검증(String name) {
        state.getResponse().then().body("name", equalTo(name));
    }

    @그리고("응답의 카테고리 색상은 {string}이다")
    public void 카테고리_색상_검증(String color) {
        state.getResponse().then().body("color", equalTo(color));
    }
}
