package gift.acceptance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderStepDefinitions {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AcceptanceTestContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Given("{string} 회원의 포인트가 {int}원이고")
    public void 포인트_설정(String email, int point) {
        jdbcTemplate.update("UPDATE member SET point = ? WHERE email = ?", point, email);
    }

    @Given("옵션 {string}를 {int}개 주문이 등록되어 있고")
    public void 주문이_등록되어_있고(String optionName, int quantity) {
        Long optionId = getOptionId(optionName);
        restTemplate.exchange(
            "/api/orders",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("optionId", optionId, "quantity", quantity, "message", ""), authHeaders()),
            String.class
        );
    }

    @When("옵션 {string}를 {int}개, 메시지 {string}로 주문을 요청하면")
    public void 주문을_요청하면(String optionName, int quantity, String message) {
        Long optionId = getOptionId(optionName);
        var response = restTemplate.exchange(
            "/api/orders",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("optionId", optionId, "quantity", quantity, "message", message), authHeaders()),
            String.class
        );
        context.setResponse(response);
    }

    @When("주문 목록을 조회하면")
    public void 주문_목록을_조회하면() {
        var response = restTemplate.exchange(
            "/api/orders",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            String.class
        );
        context.setResponse(response);
    }

    @Then("응답의 주문 수량은 {int}이다")
    public void 응답의_주문_수량_확인(int expectedQuantity) throws Exception {
        Map<String, Object> body = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        assertThat(((Number) body.get("quantity")).intValue()).isEqualTo(expectedQuantity);
    }

    @Then("응답의 주문 메시지는 {string}이다")
    public void 응답의_주문_메시지_확인(String expectedMessage) throws Exception {
        Map<String, Object> body = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        assertThat(body.get("message")).isEqualTo(expectedMessage);
    }

    @Then("응답의 주문 목록 크기는 {int}이다")
    public void 주문_목록_크기_확인(int expectedSize) throws Exception {
        Map<String, Object> page = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).hasSize(expectedSize);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + context.getToken());
        return headers;
    }

    private Long getOptionId(String optionName) {
        try {
            var productsResponse = restTemplate.getForEntity("/api/products", String.class);
            Map<String, Object> page = objectMapper.readValue(
                productsResponse.getBody(), new TypeReference<>() {}
            );
            List<Map<String, Object>> products = (List<Map<String, Object>>) page.get("content");
            for (Map<String, Object> product : products) {
                Long productId = ((Number) product.get("id")).longValue();
                var optionsResponse = restTemplate.getForEntity(
                    "/api/products/" + productId + "/options", String.class
                );
                List<Map<String, Object>> options = objectMapper.readValue(
                    optionsResponse.getBody(), new TypeReference<>() {}
                );
                for (Map<String, Object> option : options) {
                    if (optionName.equals(option.get("name"))) {
                        return ((Number) option.get("id")).longValue();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new IllegalStateException("옵션을 찾을 수 없습니다: " + optionName);
    }
}
