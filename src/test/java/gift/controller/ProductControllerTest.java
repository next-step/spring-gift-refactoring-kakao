package gift.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.dto.ProductRequest;
import gift.model.Category;
import gift.model.Product;
import gift.repository.CategoryRepository;
import gift.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        category = categoryRepository.save(new Category("전자기기", "#1E90FF", "https://example.com/img.jpg", "설명"));
    }

    @Test
    @DisplayName("GET /api/products - 상품 목록을 페이지네이션으로 조회한다")
    void getProducts() throws Exception {
        productRepository.save(new Product("테스트상품", 10000, "https://example.com/img.jpg", category));

        mockMvc.perform(get("/api/products").param("page", "0").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/products/{id} - 단일 상품을 조회한다")
    void getProduct() throws Exception {
        var saved = productRepository.save(new Product("테스트상품", 10000, "https://example.com/img.jpg", category));

        mockMvc.perform(get("/api/products/" + saved.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("테스트상품"))
            .andExpect(jsonPath("$.price").value(10000));
    }

    @Test
    @DisplayName("GET /api/products/{id} - 존재하지 않는 상품을 조회하면 404를 반환한다")
    void getProductNotFound() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/products - 상품을 생성한다")
    void createProduct() throws Exception {
        var request = new ProductRequest("새상품", 50000, "https://example.com/new.jpg", category.getId());

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("새상품"))
            .andExpect(jsonPath("$.price").value(50000))
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("POST /api/products - 이름이 15자를 초과하면 400을 반환한다")
    void createProductWithLongName() throws Exception {
        var request = new ProductRequest("가나다라마바사아자차카타파하거너", 10000, "https://example.com/img.jpg", category.getId());

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products - 허용되지 않는 특수 문자가 포함되면 400을 반환한다")
    void createProductWithInvalidChars() throws Exception {
        var request = new ProductRequest("상품!@#", 10000, "https://example.com/img.jpg", category.getId());

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products - 카카오가 포함된 이름은 400을 반환한다")
    void createProductWithKakaoName() throws Exception {
        var request = new ProductRequest("카카오선물", 10000, "https://example.com/img.jpg", category.getId());

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products - 존재하지 않는 카테고리로 생성하면 404를 반환한다")
    void createProductWithNonExistentCategory() throws Exception {
        var request = new ProductRequest("상품", 10000, "https://example.com/img.jpg", 999999L);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/products/{id} - 상품을 수정한다")
    void updateProduct() throws Exception {
        var saved = productRepository.save(new Product("원본상품", 10000, "https://example.com/old.jpg", category));
        var request = new ProductRequest("수정상품", 20000, "https://example.com/new.jpg", category.getId());

        mockMvc.perform(put("/api/products/" + saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("수정상품"))
            .andExpect(jsonPath("$.price").value(20000));
    }

    @Test
    @DisplayName("PUT /api/products/{id} - 존재하지 않는 상품을 수정하면 404를 반환한다")
    void updateNonExistentProduct() throws Exception {
        var request = new ProductRequest("수정상품", 20000, "https://example.com/new.jpg", category.getId());

        mockMvc.perform(put("/api/products/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - 상품을 삭제한다")
    void deleteProduct() throws Exception {
        var saved = productRepository.save(new Product("삭제상품", 10000, "https://example.com/img.jpg", category));

        mockMvc.perform(delete("/api/products/" + saved.getId()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/products?categoryId - 카테고리별 상품만 반환한다")
    void getProductsByCategoryId() throws Exception {
        var categoryB = categoryRepository.save(new Category("의류", "#FF6347", "https://example.com/b.jpg", "설명B"));
        productRepository.save(new Product("전자상품1", 10000, "https://example.com/1.jpg", category));
        productRepository.save(new Product("전자상품2", 20000, "https://example.com/2.jpg", category));
        productRepository.save(new Product("의류상품1", 30000, "https://example.com/3.jpg", categoryB));

        mockMvc.perform(get("/api/products")
                .param("categoryId", String.valueOf(category.getId()))
                .param("page", "0").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].categoryId").value(category.getId()))
            .andExpect(jsonPath("$.content[1].categoryId").value(category.getId()));
    }

    @Test
    @DisplayName("GET /api/products - categoryId 없이 조회하면 전체 상품을 반환한다")
    void getProductsWithoutCategoryId() throws Exception {
        var categoryB = categoryRepository.save(new Category("의류", "#FF6347", "https://example.com/b.jpg", "설명B"));
        productRepository.save(new Product("전자상품1", 10000, "https://example.com/1.jpg", category));
        productRepository.save(new Product("의류상품1", 30000, "https://example.com/3.jpg", categoryB));

        mockMvc.perform(get("/api/products")
                .param("page", "0").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2));
    }
}
