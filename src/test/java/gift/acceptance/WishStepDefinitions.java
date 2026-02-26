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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class WishStepDefinitions {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AcceptanceTestContext context;

    private final Map<String, Long> wishIds = new HashMap<>();



    @Given("상품 {string}이 위시리스트에 등록되어 있고")
    public void 위시리스트에_등록되어_있고(String productName) {
        Long productId = getProductId(productName);
        var response = restTemplate.exchange(
            "/api/wishes",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("productId", productId), authHeaders()),
            String.class
        );
        wishIds.put(productName, extractIdFromBody(response.getBody()));
    }

    @When("상품 {string}을 위시리스트에 추가를 요청하면")
    public void 위시리스트에_추가를_요청하면(String productName) {
        Long productId = getProductId(productName);
        var response = restTemplate.exchange(
            "/api/wishes",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("productId", productId), authHeaders()),
            String.class
        );
        context.setResponse(response);
        wishIds.put(productName, extractIdFromBody(response.getBody()));
    }

    @When("위시리스트를 조회하면")
    public void 위시리스트를_조회하면() {
        var response = restTemplate.exchange(
            "/api/wishes",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            String.class
        );
        context.setResponse(response);
    }

    @When("위시리스트에서 {string}의 삭제를 요청하면")
    public void 위시리스트에서_삭제를_요청하면(String productName) {
        Long wishId = wishIds.get(productName);
        var response = restTemplate.exchange(
            "/api/wishes/" + wishId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            String.class
        );
        context.setResponse(response);
    }

    @Then("응답의 위시 상품 이름은 {string}이다")
    public void 응답의_위시_상품_이름_확인(String expectedName) throws Exception {
        Map<String, Object> body = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        assertThat(body.get("name")).isEqualTo(expectedName);
    }

    @Then("응답의 위시리스트에 {string}이 포함되어 있다")
    public void 위시리스트에_포함_확인(String expectedName) throws Exception {
        Map<String, Object> page = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).anyMatch(w -> expectedName.equals(w.get("name")));
    }

    @Then("응답의 위시리스트에 {string}이 포함되어 있지 않다")
    public void 위시리스트에_미포함_확인(String expectedName) throws Exception {
        Map<String, Object> page = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).noneMatch(w -> expectedName.equals(w.get("name")));
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + context.getToken());
        return headers;
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

    private Long extractIdFromBody(String body) {
        try {
            Map<String, Object> map = objectMapper.readValue(body, new TypeReference<>() {});
            return ((Number) map.get("id")).longValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
