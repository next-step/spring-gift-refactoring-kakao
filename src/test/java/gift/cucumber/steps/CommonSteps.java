package gift.cucumber.steps;

import static org.assertj.core.api.Assertions.assertThat;

import gift.cucumber.ScenarioState;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

public class CommonSteps {

  @Autowired private ScenarioState state;

  @Then("생성이 실패한다")
  public void 생성이_실패한다() {
    assertThat(state.getLastResponse().statusCode()).isGreaterThanOrEqualTo(400);
  }
}
