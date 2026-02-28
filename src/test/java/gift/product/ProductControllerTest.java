package gift.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.category.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() throws Exception {
        var category = new Category("교환권", "#ffffff", "img.png", "");
        setId(category, 1L);

        product = new Product("아메리카노", 5000, "img.png", category);
        setId(product, 1L);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Nested
    @DisplayName("GET /api/products")
    class GetProducts {

        @Test
        @DisplayName("상품 목록을 반환한다")
        void returnsProducts() throws Exception {
            given(productService.getProducts(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(ProductResponse.from(product))));

            mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("아메리카노"));
        }
    }

    @Nested
    @DisplayName("GET /api/products/{id}")
    class GetProduct {

        @Test
        @DisplayName("상품을 반환한다")
        void returnsProduct() throws Exception {
            given(productService.getProduct(1L)).willReturn(product);

            mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("아메리카노"))
                .andExpect(jsonPath("$.price").value(5000));
        }

        @Test
        @DisplayName("존재하지 않으면 404를 반환한다")
        void returnsNotFound() throws Exception {
            given(productService.getProduct(999L))
                .willThrow(new NoSuchElementException("상품이 존재하지 않습니다."));

            mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/products")
    class CreateProduct {

        @Test
        @DisplayName("상품을 생성하고 201을 반환한다")
        void returnsCreated() throws Exception {
            var request = new ProductRequest("라떼", 6000, "img.png", 1L);

            given(productService.createProduct(anyString(), anyInt(), anyString(), anyLong(), eq(false)))
                .willReturn(product);

            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("아메리카노"));
        }

        @Test
        @DisplayName("카테고리가 존재하지 않으면 404를 반환한다")
        void returnsNotFound() throws Exception {
            var request = new ProductRequest("라떼", 6000, "img.png", 999L);

            given(productService.createProduct(anyString(), anyInt(), anyString(), anyLong(), eq(false)))
                .willThrow(new NoSuchElementException("카테고리가 존재하지 않습니다."));

            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("상품명 검증 실패 시 400을 반환한다")
        void returnsBadRequest() throws Exception {
            var request = new ProductRequest("라떼", 6000, "img.png", 1L);

            given(productService.createProduct(anyString(), anyInt(), anyString(), anyLong(), eq(false)))
                .willThrow(new IllegalArgumentException("상품명 검증 실패"));

            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("가격이 0이면 400을 반환한다")
        void returnsBadRequestWhenPriceZero() throws Exception {
            var body = """
                {"name": "라떼", "price": 0, "imageUrl": "img.png", "categoryId": 1}
                """;

            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/products/{id}")
    class UpdateProduct {

        @Test
        @DisplayName("상품을 수정하고 200을 반환한다")
        void returnsOk() throws Exception {
            var request = new ProductRequest("라떼", 6000, "new.png", 1L);

            given(productService.updateProduct(eq(1L), anyString(), anyInt(), anyString(), anyLong(), eq(false)))
                .willReturn(product);

            mockMvc.perform(put("/api/products/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 404를 반환한다")
        void returnsNotFound() throws Exception {
            var request = new ProductRequest("라떼", 6000, "new.png", 1L);

            given(productService.updateProduct(eq(999L), anyString(), anyInt(), anyString(), anyLong(), eq(false)))
                .willThrow(new NoSuchElementException("상품이 존재하지 않습니다."));

            mockMvc.perform(put("/api/products/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/products/{id}")
    class DeleteProduct {

        @Test
        @DisplayName("상품을 삭제하고 204를 반환한다")
        void returnsNoContent() throws Exception {
            mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
        }
    }
}
