package gift.option;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class OptionAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("상품의 옵션 목록을 조회한다")
    void getOptions() throws Exception {
        mockMvc.perform(get("/api/products/{productId}/options", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("존재하지 않는 상품의 옵션을 조회하면 404를 반환한다")
    void getOptions_ProductNotFound() throws Exception {
        mockMvc.perform(get("/api/products/{productId}/options", 999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("상품에 새로운 옵션을 추가한다")
    void createOption() throws Exception {
        var request = new OptionRequest("새 옵션", 100);

        mockMvc.perform(post("/api/products/{productId}/options", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("새 옵션"))
            .andExpect(jsonPath("$.quantity").value(100));
    }

    @Test
    @DisplayName("옵션이 2개 이상일 때 옵션을 삭제한다")
    void deleteOption() throws Exception {
        // productId=1에는 옵션 2개 존재 (optionId=1, 2)
        mockMvc.perform(delete("/api/products/{productId}/options/{optionId}", 1L, 1L))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("옵션이 1개뿐인 상품에서 옵션을 삭제하면 400을 반환한다")
    void deleteOption_LastOption() throws Exception {
        // productId=3에는 옵션 1개만 존재 (optionId=5)
        mockMvc.perform(delete("/api/products/{productId}/options/{optionId}", 3L, 5L))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("CANNOT_DELETE_LAST_OPTION"));
    }

    @Test
    @DisplayName("이미 존재하는 옵션명으로 추가하면 409를 반환한다")
    void createOption_DuplicateName() throws Exception {
        // productId=1에 이미 "스페이스 블랙 / M1 Pro" 옵션이 존재
        var request = new OptionRequest("스페이스 블랙 / M1 Pro", 10);

        mockMvc.perform(post("/api/products/{productId}/options", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_OPTION_NAME"));
    }

    @Test
    @DisplayName("존재하지 않는 상품에 옵션을 추가하면 404를 반환한다")
    void createOption_ProductNotFound() throws Exception {
        var request = new OptionRequest("새 옵션", 100);

        mockMvc.perform(post("/api/products/{productId}/options", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("존재하지 않는 옵션을 삭제하면 404를 반환한다")
    void deleteOption_OptionNotFound() throws Exception {
        // productId=1에 존재하지 않는 optionId=999
        mockMvc.perform(delete("/api/products/{productId}/options/{optionId}", 1L, 999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("OPTION_NOT_FOUND"));
    }
}
