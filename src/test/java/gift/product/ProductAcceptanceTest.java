package gift.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class ProductAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("상품 목록을 페이지네이션으로 조회한다")
    void getProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("상품을 단건 조회한다")
    void getProduct() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").isNotEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하면 404를 반환한다")
    void getProduct_NotFound() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("새로운 상품을 생성한다")
    void createProduct() throws Exception {
        var request = new ProductRequest("테스트상품", 10000, "https://example.com/images/test.jpg", 1L);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("테스트상품"))
            .andExpect(jsonPath("$.price").value(10000));
    }

    @Test
    @DisplayName("상품을 수정한다")
    void updateProduct() throws Exception {
        var request = new ProductRequest("수정된상품", 5000, "https://example.com/images/updated.jpg", 1L);

        mockMvc.perform(put("/api/products/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("수정된상품"));
    }

    @Test
    @DisplayName("상품을 삭제한다")
    void deleteProduct() throws Exception {
        // 시드 데이터의 상품은 옵션/위시/주문 FK 제약이 있으므로 새로 생성 후 삭제
        var request = new ProductRequest("삭제용상품", 1000, "https://example.com/images/temp.jpg", 1L);

        String response = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/products/{id}", createdId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("존재하지 않는 상품을 수정하면 404를 반환한다")
    void updateProduct_NotFound() throws Exception {
        var request = new ProductRequest("없는상품", 5000, "https://example.com/images/none.jpg", 1L);

        mockMvc.perform(put("/api/products/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 상품을 생성하면 404를 반환한다")
    void createProduct_CategoryNotFound() throws Exception {
        var request = new ProductRequest("테스트상품", 10000, "https://example.com/images/test.jpg", 999L);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }
}
