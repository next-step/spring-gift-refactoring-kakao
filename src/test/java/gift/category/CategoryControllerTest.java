package gift.category;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() throws Exception {
        category = new Category("교환권", "#ffffff", "img.png", "설명");
        setId(category, 1L);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Nested
    @DisplayName("GET /api/categories")
    class GetCategories {

        @Test
        @DisplayName("카테고리 목록을 반환한다")
        void returnsCategories() throws Exception {
            given(categoryService.getCategories())
                .willReturn(List.of(CategoryResponse.from(category)));

            mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("교환권"))
                .andExpect(jsonPath("$[0].color").value("#ffffff"));
        }
    }

    @Nested
    @DisplayName("POST /api/categories")
    class CreateCategory {

        @Test
        @DisplayName("카테고리를 생성하고 201을 반환한다")
        void returnsCreated() throws Exception {
            var request = new CategoryRequest("상품권", "#000000", "new.png", "새 카테고리");

            given(categoryService.createCategory(any(CategoryRequest.class))).willReturn(category);

            mockMvc.perform(post("/api/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("교환권"));
        }

        @Test
        @DisplayName("이름이 빈 값이면 400을 반환한다")
        void returnsBadRequestWhenNameBlank() throws Exception {
            var body = """
                {"name": "", "color": "#000000", "imageUrl": "img.png", "description": ""}
                """;

            mockMvc.perform(post("/api/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/categories/{id}")
    class UpdateCategory {

        @Test
        @DisplayName("카테고리를 수정하고 200을 반환한다")
        void returnsOk() throws Exception {
            var request = new CategoryRequest("수정됨", "#000000", "new.png", "수정 설명");

            given(categoryService.updateCategory(eq(1L), any(CategoryRequest.class))).willReturn(category);

            mockMvc.perform(put("/api/categories/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("존재하지 않으면 404를 반환한다")
        void returnsNotFound() throws Exception {
            var request = new CategoryRequest("수정됨", "#000000", "new.png", "");

            given(categoryService.updateCategory(eq(999L), any(CategoryRequest.class)))
                .willThrow(new NoSuchElementException("카테고리가 존재하지 않습니다."));

            mockMvc.perform(put("/api/categories/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/categories/{id}")
    class DeleteCategory {

        @Test
        @DisplayName("카테고리를 삭제하고 204를 반환한다")
        void returnsNoContent() throws Exception {
            mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());
        }
    }
}
