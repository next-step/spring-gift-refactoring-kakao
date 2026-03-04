package gift.wish;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.auth.JwtProvider;
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
class WishAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    private String obtainAccessToken() {
        // 시드 데이터의 user1@example.com (memberId=2, wish 2개 보유)
        return jwtProvider.createToken("user1@example.com");
    }

    @Test
    @DisplayName("인증된 사용자가 위시리스트를 조회한다")
    void getWishes() throws Exception {
        String token = obtainAccessToken();

        mockMvc.perform(get("/api/wishes")
                .header("Authorization", "Bearer " + token)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("인증 없이 위시리스트를 조회하면 401을 반환한다")
    void getWishes_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/wishes")
                .header("Authorization", "Bearer invalid-token")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("위시리스트에 상품을 추가한다")
    void addWish() throws Exception {
        String token = obtainAccessToken();
        // productId=5는 user1의 위시리스트에 없는 상품
        var request = new WishRequest(5L);

        mockMvc.perform(post("/api/wishes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.productId").value(5));
    }

    @Test
    @DisplayName("이미 위시리스트에 있는 상품을 추가하면 기존 항목을 반환한다")
    void addWish_Duplicate() throws Exception {
        String token = obtainAccessToken();
        // productId=1은 user1의 위시리스트에 이미 존재
        var request = new WishRequest(1L);

        mockMvc.perform(post("/api/wishes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId").value(1));
    }

    @Test
    @DisplayName("위시리스트에서 상품을 삭제한다")
    void removeWish() throws Exception {
        String token = obtainAccessToken();
        // wishId=1은 user1(memberId=2)의 위시
        mockMvc.perform(delete("/api/wishes/{id}", 1L)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("존재하지 않는 상품을 위시리스트에 추가하면 404를 반환한다")
    void addWish_ProductNotFound() throws Exception {
        String token = obtainAccessToken();
        var request = new WishRequest(999L);

        mockMvc.perform(post("/api/wishes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("WISH_PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("존재하지 않는 위시를 삭제하면 404를 반환한다")
    void removeWish_NotFound() throws Exception {
        String token = obtainAccessToken();

        mockMvc.perform(delete("/api/wishes/{id}", 999L)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("WISH_NOT_FOUND"));
    }
}
