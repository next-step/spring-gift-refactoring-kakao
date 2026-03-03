package gift.cucumber.steps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import gift.cucumber.SharedState;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import org.springframework.beans.factory.annotation.Autowired;

public class OrderSteps {

    @Autowired
    private SharedState state;

    @만일("주문 목록을 조회한다")
    public void 주문_목록_조회() {
        state.setResponse(
            given()
                .header("Authorization", "Bearer " + state.getToken())
            .when()
                .get("/api/orders")
        );
    }

    @만일("옵션 {long}, 수량 {int}, 메시지 {string}으로 주문한다")
    public void 주문_생성(long optionId, int quantity, String message) {
        state.setResponse(
            given()
                .header("Authorization", "Bearer " + state.getToken())
                .contentType("application/json")
                .body("{\"optionId\":" + optionId + ",\"quantity\":" + quantity + ",\"message\":\"" + message + "\"}")
            .when()
                .post("/api/orders")
        );
    }

    @그리고("응답에 주문 ID가 포함되어 있다")
    public void 주문_ID_포함() {
        state.getResponse().then().body("id", notNullValue());
    }
}
