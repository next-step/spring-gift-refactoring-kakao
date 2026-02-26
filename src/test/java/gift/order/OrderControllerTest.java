package gift.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.auth.AuthenticationResolver;
import gift.category.Category;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private OptionRepository optionRepository;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private AuthenticationResolver authenticationResolver;

    @MockitoBean
    private KakaoMessageClient kakaoMessageClient;

    private Member member;
    private Category category;
    private Product product;
    private Option option;

    @BeforeEach
    void setUp() throws Exception {
        category = new Category("교환권", "#ffffff", "img.png", "");
        setId(category, 1L);

        product = new Product("스타벅스 아메리카노", 5000, "img.png", category);
        setId(product, 1L);

        option = new Option(product, "Tall", 100);
        setId(option, 1L);

        member = new Member("test@test.com", "password");
        setId(member, 1L);
        member.chargePoint(100000);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Nested
    @DisplayName("GET /api/orders")
    class GetOrders {

        @Test
        @DisplayName("인증된 사용자의 주문 목록을 반환한다")
        void returnsOrdersForAuthenticatedMember() throws Exception {
            var order = new Order(option, member.getId(), 2, "선물입니다");
            setId(order, 1L);

            given(authenticationResolver.extractMember(anyString())).willReturn(member);
            given(orderRepository.findByMemberId(eq(member.getId()), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(order)));

            mockMvc.perform(get("/api/orders")
                            .header("Authorization", "Bearer test-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].optionId").value(1))
                    .andExpect(jsonPath("$.content[0].quantity").value(2))
                    .andExpect(jsonPath("$.content[0].message").value("선물입니다"));
        }

        @Test
        @DisplayName("인증되지 않은 사용자는 401을 반환한다")
        void returnsUnauthorizedForInvalidToken() throws Exception {
            given(authenticationResolver.extractMember(anyString())).willReturn(null);

            mockMvc.perform(get("/api/orders")
                            .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/orders")
    class CreateOrder {

        @Test
        @DisplayName("주문을 성공적으로 생성한다")
        void createsOrderSuccessfully() throws Exception {
            var request = new OrderRequest(1L, 2, "생일 축하해!");

            var savedOrder = new Order(option, member.getId(), 2, "생일 축하해!");
            setId(savedOrder, 1L);

            given(authenticationResolver.extractMember(anyString())).willReturn(member);
            given(optionRepository.findById(1L)).willReturn(Optional.of(option));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

            mockMvc.perform(post("/api/orders")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.optionId").value(1))
                    .andExpect(jsonPath("$.quantity").value(2))
                    .andExpect(jsonPath("$.message").value("생일 축하해!"));

            then(optionRepository).should().save(option);
            then(memberRepository).should().save(member);
        }

        @Test
        @DisplayName("인증되지 않은 사용자는 401을 반환한다")
        void returnsUnauthorizedForInvalidToken() throws Exception {
            var request = new OrderRequest(1L, 2, "선물");

            given(authenticationResolver.extractMember(anyString())).willReturn(null);

            mockMvc.perform(post("/api/orders")
                            .header("Authorization", "Bearer invalid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("존재하지 않는 옵션으로 주문하면 404를 반환한다")
        void returnsNotFoundForInvalidOption() throws Exception {
            var request = new OrderRequest(999L, 1, "선물");

            given(authenticationResolver.extractMember(anyString())).willReturn(member);
            given(optionRepository.findById(999L)).willReturn(Optional.empty());

            mockMvc.perform(post("/api/orders")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("카카오 토큰이 있으면 카카오 메시지를 전송한다")
        void sendsKakaoMessageWhenTokenExists() throws Exception {
            member.updateKakaoAccessToken("kakao-token");
            var request = new OrderRequest(1L, 1, "선물");

            var savedOrder = new Order(option, member.getId(), 1, "선물");
            setId(savedOrder, 1L);

            given(authenticationResolver.extractMember(anyString())).willReturn(member);
            given(optionRepository.findById(1L)).willReturn(Optional.of(option));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

            mockMvc.perform(post("/api/orders")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            then(kakaoMessageClient).should().sendToMe(eq("kakao-token"), any(Order.class), eq(product));
        }

        @Test
        @DisplayName("카카오 토큰이 없으면 카카오 메시지를 전송하지 않는다")
        void doesNotSendKakaoMessageWhenNoToken() throws Exception {
            var request = new OrderRequest(1L, 1, "선물");

            var savedOrder = new Order(option, member.getId(), 1, "선물");
            setId(savedOrder, 1L);

            given(authenticationResolver.extractMember(anyString())).willReturn(member);
            given(optionRepository.findById(1L)).willReturn(Optional.of(option));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

            mockMvc.perform(post("/api/orders")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            then(kakaoMessageClient).should(never()).sendToMe(anyString(), any(Order.class), any());
        }

        @Test
        @DisplayName("optionId가 null이면 400을 반환한다")
        void returnsBadRequestWhenOptionIdIsNull() throws Exception {
            var body = """
                    {"quantity": 1, "message": "선물"}
                    """;

            given(authenticationResolver.extractMember(anyString())).willReturn(member);

            mockMvc.perform(post("/api/orders")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("수량이 0이면 400을 반환한다")
        void returnsBadRequestWhenQuantityIsZero() throws Exception {
            var request = new OrderRequest(1L, 0, "선물");

            given(authenticationResolver.extractMember(anyString())).willReturn(member);

            mockMvc.perform(post("/api/orders")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
