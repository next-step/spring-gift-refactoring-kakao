package gift.acceptance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OptionStepDefinitions {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AcceptanceTestContext context;

    private final Map<String, Long> optionIds = new HashMap<>();

    @Given("상품 {string}에 옵션 {string}, 수량 {int}이 등록되어 있고")
    public void 옵션이_등록되어_있고(String productName, String optionName, int quantity) {
        Long productId = getProductId(productName);
        var response = restTemplate.postForEntity(
            "/api/products/" + productId + "/options",
            Map.of("name", optionName, "quantity", quantity),
            String.class
        );
        optionIds.put(productName + ":" + optionName, extractId(response.getBody()));
    }

    @When("상품 {string}에 옵션 {string}, 수량 {int}으로 생성을 요청하면")
    public void 옵션_생성을_요청하면(String productName, String optionName, int quantity) {
        Long productId = getProductId(productName);
        var response = restTemplate.postForEntity(
            "/api/products/" + productId + "/options",
            Map.of("name", optionName, "quantity", quantity),
            String.class
        );
        context.setResponse(response);
    }

    @When("상품 {string}의 옵션 목록을 조회하면")
    public void 옵션_목록을_조회하면(String productName) {
        Long productId = getProductId(productName);
        var response = restTemplate.getForEntity(
            "/api/products/" + productId + "/options",
            String.class
        );
        context.setResponse(response);
    }

    @When("상품 {string}의 옵션 {string}를 삭제를 요청하면")
    public void 옵션_삭제를_요청하면(String productName, String optionName) {
        Long productId = getProductId(productName);
        Long optionId = optionIds.get(productName + ":" + optionName);
        var response = restTemplate.exchange(
            "/api/products/" + productId + "/options/" + optionId,
            HttpMethod.DELETE,
            null,
            String.class
        );
        context.setResponse(response);
    }

    @Then("응답의 옵션 이름은 {string}이다")
    public void 응답의_옵션_이름_확인(String expectedName) throws Exception {
        Map<String, Object> body = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        assertThat(body.get("name")).isEqualTo(expectedName);
    }

    @Then("응답의 옵션 목록에 {string}이 포함되어 있다")
    public void 옵션_목록에_포함_확인(String expectedName) throws Exception {
        List<Map<String, Object>> body = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        assertThat(body).anyMatch(o -> expectedName.equals(o.get("name")));
    }

    @Then("응답의 옵션 목록에 {string}이 포함되어 있지 않다")
    public void 옵션_목록에_미포함_확인(String expectedName) throws Exception {
        List<Map<String, Object>> body = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        assertThat(body).noneMatch(o -> expectedName.equals(o.get("name")));
    }

    private Long getProductId(String productName) {
        try {
            var response = restTemplate.getForEntity("/api/products", String.class);
            Map<String, Object> page = objectMapper.readValue(
                response.getBody(), new TypeReference<>() {}
            );
            List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
            for (Map<String, Object> p : content) {
                if (productName.equals(p.get("name"))) {
                    return ((Number) p.get("id")).longValue();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new IllegalStateException("상품을 찾을 수 없습니다: " + productName);
    }

    private Long extractId(String body) {
        try {
            Map<String, Object> map = objectMapper.readValue(body, new TypeReference<>() {});
            return ((Number) map.get("id")).longValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
