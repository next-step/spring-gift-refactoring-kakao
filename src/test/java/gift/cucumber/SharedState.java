package gift.cucumber;

import io.restassured.response.Response;
import org.springframework.stereotype.Component;

@Component
public class SharedState {

    private String token;
    private Response response;
    private Long savedId;
    private Long categoryId;
    private Long optionId;
    private Long wishId;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }

    public Long getWishId() {
        return wishId;
    }

    public void setWishId(Long wishId) {
        this.wishId = wishId;
    }

    public void reset() {
        this.token = null;
        this.response = null;
        this.savedId = null;
        this.categoryId = null;
        this.optionId = null;
        this.wishId = null;
    }
}
