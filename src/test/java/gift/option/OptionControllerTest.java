package gift.option;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.category.Category;
import gift.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OptionController.class)
class OptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OptionService optionService;

    private Product product;
    private Option option;

    @BeforeEach
    void setUp() throws Exception {
        var category = new Category("교환권", "#ffffff", "img.png", "");
        setId(category, 1L);

        product = new Product("아메리카노", 5000, "img.png", category);
        setId(product, 1L);

        option = new Option(product, "Tall", 100);
        setId(option, 1L);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Nested
    @DisplayName("GET /api/products/{productId}/options")
    class GetOptions {

        @Test
        @DisplayName("옵션 목록을 반환한다")
        void returnsOptions() throws Exception {
            given(optionService.getOptions(1L))
                .willReturn(List.of(new OptionResponse(1L, "Tall", 100)));

            mockMvc.perform(get("/api/products/1/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tall"))
                .andExpect(jsonPath("$[0].quantity").value(100));
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 404를 반환한다")
        void returnsNotFoundWhenProductMissing() throws Exception {
            given(optionService.getOptions(999L))
                .willThrow(new NoSuchElementException("상품이 존재하지 않습니다."));

            mockMvc.perform(get("/api/products/999/options"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/products/{productId}/options")
    class CreateOption {

        @Test
        @DisplayName("옵션을 생성하고 201을 반환한다")
        void returnsCreated() throws Exception {
            var request = new OptionRequest("Grande", 50);

            given(optionService.createOption(eq(1L), any(OptionRequest.class))).willReturn(option);

            mockMvc.perform(post("/api/products/1/options")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tall"));
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 404를 반환한다")
        void returnsNotFoundWhenProductMissing() throws Exception {
            var request = new OptionRequest("Grande", 50);

            given(optionService.createOption(eq(999L), any(OptionRequest.class)))
                .willThrow(new NoSuchElementException("상품이 존재하지 않습니다."));

            mockMvc.perform(post("/api/products/999/options")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("옵션명 검증 실패 시 400을 반환한다")
        void returnsBadRequestWhenNameInvalid() throws Exception {
            var request = new OptionRequest("Grande", 50);

            given(optionService.createOption(eq(1L), any(OptionRequest.class)))
                .willThrow(new IllegalArgumentException("옵션명 검증 실패"));

            mockMvc.perform(post("/api/products/1/options")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("수량이 0이면 400을 반환한다")
        void returnsBadRequestWhenQuantityZero() throws Exception {
            var body = """
                {"name": "Grande", "quantity": 0}
                """;

            mockMvc.perform(post("/api/products/1/options")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/products/{productId}/options/{optionId}")
    class DeleteOption {

        @Test
        @DisplayName("옵션을 삭제하고 204를 반환한다")
        void returnsNoContent() throws Exception {
            mockMvc.perform(delete("/api/products/1/options/1"))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("옵션이 존재하지 않으면 404를 반환한다")
        void returnsNotFoundWhenOptionMissing() throws Exception {
            willThrow(new NoSuchElementException("옵션이 존재하지 않습니다."))
                .given(optionService).deleteOption(1L, 999L);

            mockMvc.perform(delete("/api/products/1/options/999"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("옵션이 1개뿐이면 400을 반환한다")
        void returnsBadRequestWhenLastOption() throws Exception {
            willThrow(new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다."))
                .given(optionService).deleteOption(1L, 1L);

            mockMvc.perform(delete("/api/products/1/options/1"))
                .andExpect(status().isBadRequest());
        }
    }
}
