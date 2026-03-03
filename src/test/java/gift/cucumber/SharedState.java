package gift.cucumber;

import io.restassured.response.Response;
import org.springframework.stereotype.Component;

@Component
public class SharedState {

    private String token;
    private String token2;
    private Response response;
    private Long savedId;
    private Long optionId;
    private Long optionProductId;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken2() {
        return token2;
    }

    public void setToken2(String token2) {
        this.token2 = token2;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public Long getSavedId() {
        return savedId;
    }

    public void setSavedId(Long savedId) {
        this.savedId = savedId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }

    public Long getOptionProductId() {
        return optionProductId;
    }

    public void setOptionProductId(Long optionProductId) {
        this.optionProductId = optionProductId;
    }

    public void reset() {
        this.token = null;
        this.token2 = null;
        this.response = null;
        this.savedId = null;
        this.optionId = null;
        this.optionProductId = null;
    }
}
