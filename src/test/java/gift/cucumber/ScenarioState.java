package gift.cucumber;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class ScenarioState {

  private final Map<String, Long> categoryIds = new HashMap<>();
  private final Map<String, Long> productIds = new HashMap<>();
  private final Map<String, Long> optionIds = new HashMap<>();
  private final Map<String, Long> memberIds = new HashMap<>();
  private final Map<String, String> tokens = new HashMap<>();
  private ExtractableResponse<Response> lastResponse;

  public void putCategoryId(String name, Long id) {
    categoryIds.put(name, id);
  }

  public Long getCategoryId(String name) {
    return categoryIds.get(name);
  }

  public void putProductId(String name, Long id) {
    productIds.put(name, id);
  }

  public Long getProductId(String name) {
    return productIds.get(name);
  }

  public void putOptionId(String key, Long id) {
    optionIds.put(key, id);
  }

  public Long getOptionId(String key) {
    return optionIds.get(key);
  }

  public void putMemberId(String name, Long id) {
    memberIds.put(name, id);
  }

  public Long getMemberId(String name) {
    return memberIds.get(name);
  }

  public void putToken(String name, String token) {
    tokens.put(name, token);
  }

  public String getToken(String name) {
    return tokens.get(name);
  }

  public ExtractableResponse<Response> getLastResponse() {
    return lastResponse;
  }

  public void setLastResponse(ExtractableResponse<Response> response) {
    this.lastResponse = response;
  }
}
