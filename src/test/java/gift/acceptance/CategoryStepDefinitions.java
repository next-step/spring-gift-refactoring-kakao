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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CategoryStepDefinitions {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AcceptanceTestContext context;

    private final Map<String, Long> categoryIds = new java.util.HashMap<>();

    @Given("카테고리 {string}, 색상 {string}, 이미지 {string}가 등록되어 있고")
    public void 카테고리가_등록되어_있고(String name, String color, String imageUrl) {
        var response = restTemplate.postForEntity(
            "/api/categories",
            Map.of("name", name, "color", color, "imageUrl", imageUrl, "description", ""),
            String.class
        );
        categoryIds.put(name, extractId(response.getBody()));
    }

    @When("카테고리 {string}, 색상 {string}, 이미지 {string}로 생성을 요청하면")
    public void 카테고리_생성을_요청하면(String name, String color, String imageUrl) {
        var response = restTemplate.postForEntity(
            "/api/categories",
            Map.of("name", name, "color", color, "imageUrl", imageUrl, "description", ""),
            String.class
        );
        context.setResponse(response);
    }

    @When("카테고리 목록을 조회하면")
    public void 카테고리_목록을_조회하면() {
        var response = restTemplate.getForEntity("/api/categories", String.class);
        context.setResponse(response);
    }

    @When("카테고리 {string}의 이름을 {string}로 수정을 요청하면")
    public void 카테고리_수정을_요청하면(String name, String newName) {
        var response = restTemplate.exchange(
            "/api/categories/" + categoryIds.get(name),
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("name", newName, "color", "#FF6347", "imageUrl", "https://img.com/fa.jpg", "description", "")),
            String.class
        );
        context.setResponse(response);
    }

    @When("카테고리 {string}의 삭제를 요청하면")
    public void 카테고리_삭제를_요청하면(String name) {
        var response = restTemplate.exchange(
            "/api/categories/" + categoryIds.get(name),
            HttpMethod.DELETE,
            null,
            String.class
        );
        context.setResponse(response);
    }

    @Then("응답의 카테고리 이름은 {string}이다")
    public void 응답의_카테고리_이름_확인(String expectedName) throws Exception {
        Map<String, Object> body = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        assertThat(body.get("name")).isEqualTo(expectedName);
    }

    @Then("응답의 카테고리 목록에 {string}이 포함되어 있다")
    public void 카테고리_목록에_포함_확인(String expectedName) throws Exception {
        List<Map<String, Object>> body = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        assertThat(body).anyMatch(c -> expectedName.equals(c.get("name")));
    }

    @Then("응답의 카테고리 목록에 {string}이 포함되어 있지 않다")
    public void 카테고리_목록에_미포함_확인(String expectedName) throws Exception {
        List<Map<String, Object>> body = objectMapper.readValue(
            context.getResponse().getBody(), new TypeReference<>() {}
        );
        assertThat(body).noneMatch(c -> expectedName.equals(c.get("name")));
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
