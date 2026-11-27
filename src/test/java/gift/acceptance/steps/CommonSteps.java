package gift.acceptance.steps;

import static org.assertj.core.api.Assertions.assertThat;

import gift.acceptance.ScenarioContext;
import io.cucumber.java.en.Then;

public class CommonSteps {

    private final ScenarioContext context;

    public CommonSteps(ScenarioContext context) {
        this.context = context;
    }

    @Then("^(\\d+)(?:이|가) 온다$")
    public void statusIs(int status) {
        assertThat(context.lastResponse().statusCode()).isEqualTo(status);
    }

    @Then("^응답 본문에 \"([^\"]*)\"(?:이|가) 들어 있다$")
    public void bodyContains(String text) {
        assertThat(context.lastResponse().asString()).contains(text);
    }
}
