package gift.category;

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
class CategoryAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("카테고리 목록을 조회한다")
    void getCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("새로운 카테고리를 생성한다")
    void createCategory() throws Exception {
        var request = new CategoryRequest("도서", "#8B4513", "https://example.com/images/book.jpg", "소설, 에세이, 전문서적");

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("도서"))
            .andExpect(jsonPath("$.color").value("#8B4513"));
    }

    @Test
    @DisplayName("카테고리를 수정한다")
    void updateCategory() throws Exception {
        var request = new CategoryRequest("가전제품", "#0000FF", "https://example.com/images/appliance.jpg", "가전제품 카테고리");

        mockMvc.perform(put("/api/categories/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("가전제품"));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리를 수정하면 404를 반환한다")
    void updateCategory_NotFound() throws Exception {
        var request = new CategoryRequest("없는카테고리", "#000000", "https://example.com/images/none.jpg", "설명");

        mockMvc.perform(put("/api/categories/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    @DisplayName("카테고리를 삭제한다")
    void deleteCategory() throws Exception {
        // 시드 데이터의 카테고리는 product FK 제약이 있으므로 새로 생성 후 삭제
        var request = new CategoryRequest("임시", "#999999", "https://example.com/images/temp.jpg", "삭제 테스트용");

        String response = mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/categories/{id}", createdId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("이름이 비어있으면 카테고리 생성 시 400을 반환한다")
    void createCategory_InvalidRequest() throws Exception {
        var request = new CategoryRequest("", "#000000", "https://example.com/images/none.jpg", "설명");

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
