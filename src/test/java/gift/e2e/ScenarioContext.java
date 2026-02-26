package gift.e2e;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class ScenarioContext {

    private ExtractableResponse<Response> response;
    private final Map<String, Long> categoryIds = new HashMap<>();
    private final Map<String, Long> productIds = new HashMap<>();
    private final Map<String, Long> optionIds = new HashMap<>();
    private Long optionId;
    private String token;
    private String otherToken;
    private Long wishId;

    public ExtractableResponse<Response> getResponse() {
        return response;
    }

    public void setResponse(ExtractableResponse<Response> response) {
        this.response = response;
    }

    public Long getCategoryId(String name) {
        return categoryIds.get(name);
    }

    public void putCategoryId(String name, Long id) {
        categoryIds.put(name, id);
    }

    public Long getProductId(String name) {
        return productIds.get(name);
    }

    public void putProductId(String name, Long id) {
        productIds.put(name, id);
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }

    public Long getOptionId(String name) {
        return optionIds.get(name);
    }

    public void putOptionId(String name, Long id) {
        optionIds.put(name, id);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getOtherToken() {
        return otherToken;
    }

    public void setOtherToken(String otherToken) {
        this.otherToken = otherToken;
    }

    public Long getWishId() {
        return wishId;
    }

    public void setWishId(Long wishId) {
        this.wishId = wishId;
    }
}
