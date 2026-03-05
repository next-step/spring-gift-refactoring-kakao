package gift.wish;

import gift.auth.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class WishQueryAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

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
}
