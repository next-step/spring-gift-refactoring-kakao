package gift.order;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class OrderAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    private String obtainAccessToken() {
        // 시드 데이터의 user1@example.com (memberId=2, point=5000000)
        return jwtProvider.createToken("user1@example.com");
    }

    @Test
    @DisplayName("인증된 사용자가 주문 목록을 조회한다")
    void getOrders() throws Exception {
        String token = obtainAccessToken();

        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + token)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("인증 없이 주문 목록을 조회하면 401을 반환한다")
    void getOrders_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer invalid-token")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("주문을 생성한다")
    void createOrder() throws Exception {
        String token = obtainAccessToken();
        // optionId=3 (아이폰 블루/256GB, 수량 30, 가격 1350000)
        var request = new OrderRequest(3L, 1, "선물입니다");

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.optionId").value(3))
            .andExpect(jsonPath("$.quantity").value(1))
            .andExpect(jsonPath("$.message").value("선물입니다"));
    }

    @Test
    @DisplayName("존재하지 않는 옵션으로 주문하면 404를 반환한다")
    void createOrder_OptionNotFound() throws Exception {
        String token = obtainAccessToken();
        var request = new OrderRequest(999L, 1, null);

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ORDER_OPTION_NOT_FOUND"));
    }

    @Test
    @DisplayName("재고보다 많은 수량으로 주문하면 400을 반환한다")
    void createOrder_InsufficientStock() throws Exception {
        String token = obtainAccessToken();
        // optionId=1 (맥북 스페이스 블랙 / M1 Pro, 재고 10개)
        var request = new OrderRequest(1L, 9999, null);

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }
}
