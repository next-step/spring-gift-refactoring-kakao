package gift.order;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OrderTransactionTest {

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
    @DisplayName("포인트 부족으로 주문 실패 시 옵션 재고가 롤백된다")
    void createOrder_InsufficientPoints_OptionQuantityUnchanged() throws Exception {
        String token = obtainAccessToken();
        long productId = 1L;

        // Given: 주문 전 옵션 재고 확인
        String beforeJson = mockMvc.perform(get("/api/products/{productId}/options", productId))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode beforeOptions = objectMapper.readTree(beforeJson);
        int beforeQuantity = beforeOptions.get(0).get("quantity").asInt();

        // When: 포인트 부족한 주문 시도
        // 맥북 옵션1 (가격 3,360,000) x 2 = 6,720,000 > 5,000,000 포인트
        var request = new OrderRequest(1L, 2, "포인트 부족 주문");

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        // Then: 옵션 재고가 차감되지 않았음을 재조회로 확인
        mockMvc.perform(get("/api/products/{productId}/options", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].quantity").value(beforeQuantity));
    }

    @Test
    @Transactional
    @DisplayName("주문 완료 시 해당 상품이 위시리스트에서 제거된다")
    void createOrder_RemovesWishForOrderedProduct() throws Exception {
        String token = obtainAccessToken();
        long productId = 3L;

        // Given: 위시리스트에 상품 추가 (시드 데이터에 user1-product3 위시 존재)
        mockMvc.perform(get("/api/wishes")
                .header("Authorization", "Bearer " + token)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.productId == 3)]").exists());

        // When: 해당 상품의 옵션으로 주문
        // 나이키 에어맥스 옵션5 (270mm, 가격 179,000, 재고 15)
        var request = new OrderRequest(5L, 1, "위시 정리 테스트");

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Then: 위시리스트에서 해당 상품이 제거되었음을 재조회로 확인
        mockMvc.perform(get("/api/wishes")
                .header("Authorization", "Bearer " + token)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].productId", everyItem(not(is(3)))));
    }
}
