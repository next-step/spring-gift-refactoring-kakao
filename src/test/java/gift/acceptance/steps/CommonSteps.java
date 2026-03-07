package gift.acceptance.steps;

import gift.acceptance.ScenarioContext;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class CommonSteps {
    private static final Map<String, Integer> STATUS_CODE_MAP = Map.of(
        "성공", 200,
        "생성됨", 201,
        "잘못된 요청", 400,
        "인증 실패", 401,
        "권한 없음", 403,
        "찾을 수 없음", 404
    );

    @Autowired
    private ScenarioContext scenarioContext;

    @Then("응답 상태가 {string}이다")
    public void 응답_상태_검증(String statusText) {
        var statusCode = STATUS_CODE_MAP.get(statusText);
        if (statusCode == null) {
            throw new IllegalArgumentException("알 수 없는 응답 상태: " + statusText
                + " (사용 가능: " + STATUS_CODE_MAP.keySet() + ")");
        }
        scenarioContext.getLastResponse()
            .then()
            .statusCode(statusCode);
    }

    @Then("응답 바디의 {string} 필드 값이 {string}이다")
    public void 응답_바디_필드_값_검증(String field, String expectedValue) {
        scenarioContext.getLastResponse()
            .then()
            .body(field, equalTo(expectedValue));
    }

    @Then("응답 바디에 {string} 필드가 존재한다")
    public void 응답_바디_필드_존재_검증(String field) {
        scenarioContext.getLastResponse()
            .then()
            .body(field, notNullValue());
    }
}
