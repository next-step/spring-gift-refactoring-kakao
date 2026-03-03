package gift.cucumber.steps;

import static io.restassured.RestAssured.given;

import gift.cucumber.SharedState;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import org.springframework.beans.factory.annotation.Autowired;

public class WishSteps {

    @Autowired
    private SharedState state;

    @만일("위시리스트를 조회한다")
    public void 위시리스트_조회() {
        state.setResponse(
            given()
                .header("Authorization", "Bearer " + state.getToken())
            .when()
                .get("/api/wishes")
        );
    }

    @만일("상품 {long}를 위시리스트에 추가한다")
    public void 위시_추가(long productId) {
        state.setResponse(
            given()
                .header("Authorization", "Bearer " + state.getToken())
                .contentType("application/json")
                .body("{\"productId\":" + productId + "}")
            .when()
                .post("/api/wishes")
        );
    }

    @만일("상품 {long}을 위시리스트에 추가한다")
    public void 위시_추가2(long productId) {
        위시_추가(productId);
    }

    @그리고("상품 {long}를 위시리스트에 추가하고 ID를 저장한다")
    public void 위시_추가_ID_저장(long productId) {
        Long wishId = given()
            .header("Authorization", "Bearer " + state.getToken())
            .contentType("application/json")
            .body("{\"productId\":" + productId + "}")
        .when()
            .post("/api/wishes")
        .then()
            .extract().jsonPath().getLong("id");
        state.setSavedId(wishId);
    }

    @그리고("상품 {long}을 위시리스트에 추가하고 ID를 저장한다")
    public void 위시_추가_ID_저장2(long productId) {
        위시_추가_ID_저장(productId);
    }

    @만일("저장된 위시를 삭제한다")
    public void 위시_삭제() {
        state.setResponse(
            given()
                .header("Authorization", "Bearer " + state.getToken())
            .when()
                .delete("/api/wishes/" + state.getSavedId())
        );
    }
}
