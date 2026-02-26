package gift.acceptance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductStepDefinitions {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AcceptanceTestContext context;

    private final Map<String, Long> productIds = new HashMap<>();
    private final Map<String, Long> categoryIdsByName = new HashMap<>();

    @Given("상품 {string}, 가격 {int}, 이미지 {string}, 카테고리 {string}가 등록되어 있고")
    public void 상품이_등록되어_있고(String name, int price, String imageUrl, String categoryName) {
        Long categoryId = getCategoryId(categoryName);
        var response = restTemplate.postForEntity(
            "/api/products",
            Map.of("name", name, "price", price, "imageUrl", imageUrl, "categoryId", categoryId),
            String.class
        );
        productIds.put(name, extractId(response.getBody()));
    }

    @When("상품 {string}, 가격 {int}, 이미지 {string}, 카테고리 {string}로 생성을 요청하면")
    public void 상품_생성을_요청하면(String name, int price, String imageUrl, String categoryName) {
        Long categoryId = getCategoryId(categoryName);
        var response = restTemplate.postForEntity(
            "/api/products",
            Map.of("name", name, "price", price, "imageUrl", imageUrl, "categoryId", categoryId),
            String.class
        );
        context.setResponse(response);
    }

    @When("상품 {string}를 조회하면")
    public void 상품_단건_조회(String name) {
        var response = restTemplate.getForEntity(
            "/api/products/" + productIds.get(name),
            String.class
        );
        context.setResponse(response);
    }

    @When("상품 목록을 조회하면")
    public void 상품_목록을_조회하면() {
        var response = restTemplate.getForEntity("/api/products", String.class);
        context.setResponse(response);
    }

    @When("상품 {string}의 이름을 {string}으로 수정을 요청하면")
    public void 상품_수정을_요청하면(String name, String newName) {
        Long productId = productIds.get(name);
        Long categoryId = getCategoryId("전자기기");
        var response = restTemplate.exchange(
            "/api/products/" + productId,
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("name", newName, "price", 1200000, "imageUrl", "https://img.com/ip15.jpg", "categoryId", categoryId)),
            String.class
        );
        context.setResponse(response);
    }

    @When("상품 {string}의 삭제를 요청하면")
    public void 상품_삭제를_요청하면(String name) {
        var response = restTemplate.exchange(
            "/api/products/" + productIds.get(name),
            HttpMethod.DELETE,
            null,
            String.class
        );
        context.setResponse(response);
    }

    @Then("응답의 상품 이름은 {string}이다")
    public void 응답의_상품_이름_확인(String expectedName) throws Exception {
        Map<String, Object> body = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        assertThat(body.get("name")).isEqualTo(expectedName);
    }

    @Then("응답의 상품 목록에 {string}이 포함되어 있다")
    public void 상품_목록에_포함_확인(String expectedName) throws Exception {
        Map<String, Object> page = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).anyMatch(p -> expectedName.equals(p.get("name")));
    }

    @Then("응답의 상품 목록에 {string}이 포함되어 있지 않다")
    public void 상품_목록에_미포함_확인(String expectedName) throws Exception {
        Map<String, Object> page = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).noneMatch(p -> expectedName.equals(p.get("name")));
    }

    private Long getCategoryId(String categoryName) {
        Long cached = categoryIdsByName.get(categoryName);
        if (cached != null) {
            return cached;
        }
        try {
            var response = restTemplate.getForEntity("/api/categories", String.class);
            List<Map<String, Object>> categories = objectMapper.readValue(
                response.getBody(), new TypeReference<>() {}
            );
            for (Map<String, Object> c : categories) {
                if (categoryName.equals(c.get("name"))) {
                    Long id = ((Number) c.get("id")).longValue();
                    categoryIdsByName.put(categoryName, id);
                    return id;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new IllegalStateException("카테고리를 찾을 수 없습니다: " + categoryName);
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
