package gift.cucumber;

import java.util.HashMap;
import java.util.Map;

public class SharedState {

    private String token;
    private int statusCode;
    private Long categoryId;
    private Long productId;
    private Long optionId;
    private final Map<String, Long> categoryIds = new HashMap<>();
    private final Map<String, Long> productIds = new HashMap<>();
    private final Map<String, Long> optionIds = new HashMap<>();
    private final Map<String, Long> wishIds = new HashMap<>();

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getOptionId() { return optionId; }
    public void setOptionId(Long optionId) { this.optionId = optionId; }
    public Long getCategoryIdByName(String name) { return categoryIds.get(name); }
    public Long getProductIdByName(String name) { return productIds.get(name); }
    public Long getOptionIdByName(String name) { return optionIds.get(name); }
    public Long getWishIdByName(String name) { return wishIds.get(name); }
    public void putCategoryId(String name, Long id) { categoryIds.put(name, id); }
    public void putProductId(String name, Long id) { productIds.put(name, id); }
    public void putOptionId(String name, Long id) { optionIds.put(name, id); }
    public void putWishId(String name, Long id) { wishIds.put(name, id); }
}
