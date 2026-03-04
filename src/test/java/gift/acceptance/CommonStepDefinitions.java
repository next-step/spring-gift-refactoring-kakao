package gift.acceptance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonStepDefinitions {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AcceptanceTestContext context;

    @Then("응답 코드는 {int}이다")
    public void 응답_코드_확인(int expectedStatusCode) {
        assertThat(context.getResponse().getStatusCode().value()).isEqualTo(expectedStatusCode);
    }

    @Then("응답에 토큰이 포함되어 있다")
    public void 응답에_토큰이_포함되어_있다() throws Exception {
        Map<String, Object> body = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        assertThat(body).containsKey("token");
        String token = (String) body.get("token");
        assertThat(token).isNotBlank();
        context.setToken(token);
    }
}
