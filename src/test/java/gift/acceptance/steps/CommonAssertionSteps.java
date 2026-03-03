package gift.acceptance.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import gift.acceptance.context.ScenarioContext;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommonAssertionSteps {

    private final ScenarioContext context;

    @Then("응답 상태 코드는 {int}")
    public void statusCodeIs(int statusCode) {
        context.getResponse()
                .then()
                .statusCode(statusCode);
    }

    @Then("응답 body의 {string}가 null이 아니다")
    public void fieldIsNotNull(String field) {
        context.getResponse()
                .then()
                .body(field, notNullValue());
    }

    @Then("응답 body의 {string}이 null이 아니다")
    public void fieldIsNotNullSubject(String field) {
        context.getResponse()
                .then()
                .body(field, notNullValue());
    }

    @Then("응답 body의 {string}이 {string}이다")
    public void fieldEqualsString(String field, String value) {
        context.getResponse()
                .then()
                .body(field, equalTo(value));
    }

    @Then("응답 body의 {string}가 {string}이다")
    public void fieldEqualsStringAlt(String field, String value) {
        context.getResponse()
                .then()
                .body(field, equalTo(value));
    }

    @Then("응답 body의 {string}가 {int}이다")
    public void fieldEqualsInt(String field, int value) {
        context.getResponse()
                .then()
                .body(field, equalTo(value));
    }

    @Then("응답 body의 {string}이 null이다")
    public void fieldIsNull(String field) {
        context.getResponse()
                .then()
                .body(field, nullValue());
    }

    @Then("응답 body의 {string}가 null이다")
    public void fieldIsNullAlt(String field) {
        context.getResponse()
                .then()
                .body(field, nullValue());
    }

    @Then("응답 body에 {string} 가 포함된다")
    public void bodyContainsMessage(String message) {
        String body = context.getResponse()
                .getBody()
                .asString();

        assertThat(body).contains(message);
    }

    @Then("응답 body는 크기가 {int}인 배열이다")
    public void bodyIsArrayOfSize(int size) {
        context.getResponse()
                .then()
                .body("$", hasSize(size));
    }

    @Then("응답 body는 빈 배열이다")
    public void bodyIsEmptyArray() {
        context.getResponse()
                .then()
                .body("$", hasSize(0));
    }

    @Then("응답 body의 {string} 배열 크기가 {int}이다")
    public void nestedArraySize(String field, int size) {
        context.getResponse()
                .then()
                .body(field, hasSize(size));
    }

    @Then("응답 body의 {string}가 해당 카테고리 ID이다")
    public void fieldMatchesCategoryId(String field) {
        int actualId = context.getResponse()
                .jsonPath()
                .getInt(field);

        assertThat(context.currentCategoryId()).isEqualTo(actualId);
    }

    @Then("응답 body의 {string}가 해당 상품 ID이다")
    public void fieldMatchesProductId(String field) {
        int actualId = context.getResponse()
                .jsonPath()
                .getInt(field);

        assertThat(context.currentProductId()).isEqualTo(actualId);
    }

    @Then("응답 body의 {string}가 해당 옵션 ID이다")
    public void fieldMatchesOptionId(String field) {
        int actualId = context.getResponse()
                .jsonPath()
                .getInt(field);

        assertThat(context.currentOptionId()).isEqualTo(actualId);
    }

    @Then("응답 body의 {string}가 기존 위시 ID와 동일하다")
    public void fieldMatchesWishId(String field) {
        int actualId = context.getResponse()
                .jsonPath()
                .getInt(field);

        assertThat(context.currentWishId()).isEqualTo(actualId);
    }

    @Then("응답 헤더에 Location이 존재한다")
    public void locationHeaderExists() {
        String locationHeader = context.getResponse()
                .getHeader("Location");

        assertThat(locationHeader).isNotBlank();
    }

    @Then("응답 body가 없다")
    public void bodyIsEmpty() {
        String body = context.getResponse()
                .getBody()
                .asString();

        assertThat(body).isBlank();
    }

    @Then("응답 body의 {string} 필드가 비어있지 않다")
    public void fieldIsNotBlank(String field) {
        String value = context.getResponse()
                .jsonPath()
                .getString(field);

        assertThat(value).isNotBlank();
    }
}
