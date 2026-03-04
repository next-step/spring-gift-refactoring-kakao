package gift.acceptance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import gift.auth.JwtProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class WishStepDefinitions {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AcceptanceTestContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    @Given("상품 {string}이 위시리스트에 등록되어 있고")
    public void 위시리스트에_등록되어_있고(String productName) {
        Long memberId = getMemberIdFromToken();
        Long productId = jdbcTemplate.queryForObject("SELECT id FROM product WHERE name = ?", Long.class, productName);
        new SimpleJdbcInsert(jdbcTemplate)
            .withTableName("wish")
            .usingGeneratedKeyColumns("id")
            .execute(Map.of("member_id", memberId, "product_id", productId));
    }

    @When("상품 {string}을 위시리스트에 추가를 요청하면")
    public void 위시리스트에_추가를_요청하면(String productName) {
        Long productId = jdbcTemplate.queryForObject("SELECT id FROM product WHERE name = ?", Long.class, productName);
        var response = restTemplate.exchange(
            "/api/wishes",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("productId", productId), authHeaders()),
            String.class
        );
        context.setResponse(response);
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
        Long productId = jdbcTemplate.queryForObject("SELECT id FROM product WHERE name = ?", Long.class, productName);
        Long wishId = jdbcTemplate.queryForObject(
            "SELECT id FROM wish WHERE member_id = ? AND product_id = ?", Long.class, getMemberIdFromToken(), productId
        );
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

    private Long getMemberIdFromToken() {
        String email = jwtProvider.getEmail(context.getToken());
        return jdbcTemplate.queryForObject("SELECT id FROM member WHERE email = ?", Long.class, email);
    }
}
